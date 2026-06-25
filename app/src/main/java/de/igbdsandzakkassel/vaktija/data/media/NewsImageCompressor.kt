package de.igbdsandzakkassel.vaktija.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Turns a picked image [Uri] (a flyer the admin attaches to an announcement) into a compact JPEG
 * that fits inside a single Firestore document.
 *
 * It is deliberately self-contained so the announcement image needs **no Cloud Storage**: the bytes
 * are Base64-encoded into a `news_images` doc, which Firestore caps at 1 MB. The result is therefore
 * a HARD-bounded ≤ [MAX_BYTES] (Base64 inflates by ~4/3, plus field overhead — see the constant).
 * The picture is decoded with bounds-sampling (OOM-safe), rotated/flipped upright per its full EXIF
 * orientation, downscaled to at most [MAX_DIM] px on its longest side, then JPEG-quality-reduced and
 * — only if a stubborn image still overshoots — downscaled further (down to [HARD_MIN_DIM]) until it
 * fits. Bitmaps are recycled deterministically, including on the exception/OOM path.
 */
@Singleton
class NewsImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Returns a compressed JPEG (guaranteed ≤ [MAX_BYTES]) for [uri], or null if it can't be read. */
    suspend fun compress(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            // `working` always points at the single live bitmap; the finally frees it on any throw,
            // and each transform that allocates a NEW bitmap recycles the one it replaced.
            var working = decodeDownsampled(uri, MAX_DIM) ?: return@runCatching null
            try {
                orientedFor(uri)?.let { matrix ->
                    val rotated = Bitmap.createBitmap(working, 0, 0, working.width, working.height, matrix, true)
                    if (rotated !== working) {
                        working.recycle()
                        working = rotated
                    }
                }
                val scaled = scaleLongestSideTo(working, MAX_DIM)
                if (scaled !== working) {
                    working.recycle()
                    working = scaled
                }
                encodeUnderLimit(working)
            } finally {
                working.recycle()
            }
        }.getOrNull()
    }

    /** Decodes [uri] with an inSampleSize chosen so the result is near (but ≥) [maxDim] on its long side. */
    private fun decodeDownsampled(uri: Uri, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val longest = max(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return null

        var sample = 1
        while (longest / (sample * 2) >= maxDim) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }

    /**
     * Returns a fresh copy of [bitmap] scaled so its longest side is [maxDim], or the SAME instance
     * (no allocation, no recycle) if it is already small enough. The caller owns recycling.
     */
    private fun scaleLongestSideTo(bitmap: Bitmap, maxDim: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).roundToInt().coerceAtLeast(1),
            (bitmap.height * ratio).roundToInt().coerceAtLeast(1),
            true,
        )
    }

    /**
     * The transform [Matrix] that makes [uri]'s pixels upright per its EXIF orientation — covering
     * rotation AND the mirrored/transposed cases (front-camera selfies, scanners) — or null when the
     * image is already normal (no work needed).
     */
    private fun orientedFor(uri: Uri): Matrix? {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> Matrix().apply { postRotate(90f) }
            ExifInterface.ORIENTATION_ROTATE_180 -> Matrix().apply { postRotate(180f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> Matrix().apply { postRotate(270f) }
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Matrix().apply { postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> Matrix().apply { postScale(1f, -1f) }
            ExifInterface.ORIENTATION_TRANSPOSE -> Matrix().apply { postRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> Matrix().apply { postRotate(270f); postScale(-1f, 1f) }
            else -> null
        }
    }

    /**
     * JPEG-encodes [base], lowering quality to [QUALITY_FLOOR]; if it still overshoots [MAX_BYTES],
     * downscales by 15 % and retries, down to [HARD_MIN_DIM]. Guarantees the returned bytes are
     * ≤ [MAX_BYTES] for any real image (a ~200 px JPEG is a few KB). Does NOT recycle [base]; the
     * shrink copies it creates are recycled here (including on the exception path).
     */
    private fun encodeUnderLimit(base: Bitmap): ByteArray {
        var attempt = encodeAtLadder(base)
        if (attempt.size <= MAX_BYTES) return attempt

        var current = base
        var ownsCurrent = false // true once `current` is a copy we allocated (and must recycle)
        try {
            while (attempt.size > MAX_BYTES && max(current.width, current.height) > HARD_MIN_DIM) {
                val longest = max(current.width, current.height)
                val smaller = scaleLongestSideTo(current, (longest * 0.85f).roundToInt())
                if (smaller === current) break // can't shrink further
                if (ownsCurrent) current.recycle()
                current = smaller
                ownsCurrent = true
                attempt = encodeAtLadder(current)
            }
            return attempt
        } finally {
            if (ownsCurrent) current.recycle()
        }
    }

    /** Encodes [bitmap], stepping quality from [QUALITY_START] down to exactly [QUALITY_FLOOR]. */
    private fun encodeAtLadder(bitmap: Bitmap): ByteArray {
        var quality = QUALITY_START
        var bytes = encode(bitmap, quality)
        while (bytes.size > MAX_BYTES && quality > QUALITY_FLOOR) {
            quality = (quality - QUALITY_STEP).coerceAtLeast(QUALITY_FLOOR)
            bytes = encode(bitmap, quality)
        }
        return bytes
    }

    private fun encode(bitmap: Bitmap, quality: Int): ByteArray =
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }

    companion object {
        /** Longest-side cap in pixels — plenty for a phone-screen flyer with legible text. */
        const val MAX_DIM = 1440

        /** Absolute shrink floor: only reached by pathological images, and guarantees the loop ends. */
        const val HARD_MIN_DIM = 200

        /**
         * Max encoded JPEG size. Base64 adds ~33 %, so 600 KB → ~800 KB of text in the Firestore
         * doc, safely under the 1 MB document limit even with field-name overhead. Callers can rely
         * on [compress] never returning more than this.
         */
        const val MAX_BYTES = 600_000

        const val QUALITY_START = 85
        const val QUALITY_FLOOR = 40
        const val QUALITY_STEP = 8
    }
}
