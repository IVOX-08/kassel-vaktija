package de.igbdsandzakkassel.vaktija.ui.onboarding

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import de.igbdsandzakkassel.vaktija.core.locale.AppLanguage
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGold
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGoldLight
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGreen

private val DeepGreen = Color(0xFF0B3D2E)
private val Teal = Color(0xFF0E6B5C)

/** A Coil image loader configured to decode animated GIFs. */
@Composable
private fun rememberGifImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
            }
            .build()
    }
}

/** A waving flag (animated GIF) for a language. */
@Composable
fun WavingFlag(language: AppLanguage, imageLoader: ImageLoader, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data("file:///android_asset/flags/${language.tag}.gif")
            .build(),
        imageLoader = imageLoader,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.4f), shape),
    )
}

/**
 * Full-screen, modern language picker: an animated colourful gradient with a grid of waving-flag
 * cards. One tap selects (and applies) the language. Used by onboarding and the in-app switcher.
 */
@Composable
fun LanguagePickerScreen(
    onSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
    showClose: Boolean = false,
    onClose: () -> Unit = {},
) {
    val imageLoader = rememberGifImageLoader()

    // Slowly flowing colour gradient.
    val transition = rememberInfiniteTransition(label = "bg")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "t",
    )
    val top = lerp(DeepGreen, Teal, t)
    val mid = lerp(BrandGreen, DeepGreen, t)
    val bottom = lerp(Teal, BrandGreen, t)
    val gradient = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(top, mid, bottom))

    Box(modifier = modifier.fillMaxSize().background(gradient)) {
        if (showClose) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "اللغة · Language",
                color = BrandGoldLight,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 56.dp),
            )
            Text(
                text = "Jezik · Sprache · Gjuha · زبان · Язык",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                itemsIndexed(AppLanguage.entries, key = { _, l -> l.tag }) { index, language ->
                    LanguageCard(language, imageLoader, index) { onSelected(language) }
                }
            }
        }
    }
}

@Composable
private fun LanguageCard(
    language: AppLanguage,
    imageLoader: ImageLoader,
    index: Int,
    onClick: () -> Unit,
) {
    // Staggered fade + rise entrance.
    var appeared by remember { mutableStateOf(false) }
    val anim by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 420, delayMillis = index * 55),
        label = "card",
    )
    androidx.compose.runtime.LaunchedEffect(Unit) { appeared = true }

    Column(
        modifier = Modifier
            .graphicsLayer {
                alpha = anim
                translationY = (1f - anim) * 40f
            }
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WavingFlag(
            language = language,
            imageLoader = imageLoader,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 2f),
        )
        Text(
            text = stringResourceCompat(language),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Endonym shown on the card (avoids needing a Context-locale lookup). */
@Composable
private fun stringResourceCompat(language: AppLanguage): String =
    androidx.compose.ui.res.stringResource(language.displayNameRes)

/**
 * Hosts the picker in-app (used by the change-language pill). The chosen language is reported UP via
 * [onSelected] so the caller can apply it AFTER this dialog has been dismissed — calling
 * setApplicationLocales from inside a Compose Dialog window failed to take effect on Android < 13.
 */
@Composable
fun InAppLanguagePicker(onSelected: (AppLanguage) -> Unit, onClose: () -> Unit) {
    LanguagePickerScreen(
        onSelected = onSelected,
        showClose = true,
        onClose = onClose,
    )
}
