package de.igbdsandzakkassel.vaktija.ui.tv

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Draws a QR code for [content] at exactly [sizePx] pixels.
 *
 * Drawn here rather than shipped as a picture so the link can come from the database — see
 * [de.igbdsandzakkassel.vaktija.data.store.StoreLinksRepository].
 *
 * Two choices are deliberate, and both come from where this is read: a code on a wall board,
 * scanned from the middle of the prayer hall with a phone held at arm's length.
 *
 * - **Lowest error correction.** Counter-intuitive, but right here. A higher level packs more
 *   modules into the same square, so each module gets physically smaller — and on a wall the size
 *   of a module is what decides whether a camera three metres away can resolve it. The board is
 *   clean glass, not a smudged paper flyer; there is nothing for the redundancy to repair.
 * - **Encoded at the exact pixel size it is shown at.** Scaling a QR afterwards blurs the module
 *   edges. Asking for the final size means zxing lays the modules out on whole pixels itself.
 *
 * Returns null while the code is still being built, and for blank content.
 */
@Composable
fun rememberQrBitmap(content: String, sizePx: Int): State<ImageBitmap?> =
    produceState<ImageBitmap?>(initialValue = null, content, sizePx) {
        if (content.isBlank() || sizePx <= 0) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.Default) { encodeQr(content, sizePx) }
    }

private fun encodeQr(content: String, sizePx: Int): ImageBitmap? = runCatching {
    val matrix = QRCodeWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        sizePx,
        sizePx,
        mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
            // Quiet zone in modules. Scanners need it; 3 is the smallest that stays reliable
            // against the white card the code sits on.
            EncodeHintType.MARGIN to 3,
            // No CHARACTER_SET hint on purpose. Asking for UTF-8 makes zxing prepend an ECI
            // header, and a store link is plain ASCII that needs none — it only makes the code
            // denser and gives older scanner apps something extra to get wrong.
        ),
    )
    val width = matrix.width
    val height = matrix.height
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        val row = y * width
        for (x in 0 until width) {
            pixels[row + x] = if (matrix[x, y]) BLACK else WHITE
        }
    }
    Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
    // A link too long for a QR, or a broken one, must not take the whole board down with it: the
    // prayer times are why the TV is on the wall. Null just falls back to the placeholder.
}.getOrNull()

private const val BLACK = 0xFF000000.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()
