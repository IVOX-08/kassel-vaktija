package de.igbdsandzakkassel.vaktija.ui.onboarding

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.core.locale.LocaleController
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGreen
import kotlinx.coroutines.launch

/**
 * First-launch onboarding. Step 1: pick a language (no locale set yet) — applying it recreates the
 * Activity. Step 2 (locale now set): a short swipeable intro; finishing marks onboarding complete.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Survives the Activity recreate that LocaleController.set triggers. We don't rely on
    // AppCompatDelegate.getApplicationLocales() here because it can read empty right after the
    // recreate (out of sync with the framework). Existing users (locale already set) skip step 1.
    var languageChosen by rememberSaveable {
        mutableStateOf(!AppCompatDelegate.getApplicationLocales().isEmpty)
    }
    var introDone by rememberSaveable { mutableStateOf(false) }
    when {
        !languageChosen -> LanguagePickerScreen(onSelected = {
            languageChosen = true
            LocaleController.set(context, it) // persist the tag + apply (recreates the Activity)
        })
        // Language → short intro → permissions (notifications, exact alarm, battery, Do-Not-Disturb).
        !introDone -> OnboardingIntro(onFinished = { introDone = true })
        else -> OnboardingPermissions(onFinished = onFinished)
    }
}

private data class Slide(
    val emblem: Boolean,
    val icon: ImageVector?,
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
)

@Composable
private fun OnboardingIntro(onFinished: () -> Unit) {
    val slides = remember {
        listOf(
            Slide(emblem = true, icon = null, R.string.onb_welcome_title, R.string.onb_welcome_body),
            Slide(false, Icons.Outlined.Notifications, R.string.onb_notif_title, R.string.onb_notif_body),
            Slide(false, Icons.AutoMirrored.Outlined.MenuBook, R.string.onb_content_title, R.string.onb_content_body),
            Slide(false, Icons.Outlined.Explore, R.string.onb_more_title, R.string.onb_more_body),
        )
    }
    val pager = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TextButton(
            onClick = onFinished,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) { Text(stringResource(R.string.onb_skip)) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                SlideContent(slides[page])
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 16.dp),
            ) {
                repeat(slides.size) { i ->
                    val width by animateDpAsState(if (pager.currentPage == i) 22.dp else 8.dp, label = "dot")
                    Box(
                        modifier = Modifier
                            .size(width = width, height = 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (pager.currentPage == i) BrandGreen
                                else MaterialTheme.colorScheme.outlineVariant,
                            ),
                    )
                }
            }
            Button(
                onClick = {
                    if (pager.currentPage == slides.lastIndex) onFinished()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (pager.currentPage == slides.lastIndex) R.string.onb_start else R.string.onb_next,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SlideContent(slide: Slide) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (slide.emblem) {
            Image(
                // logo_community has both a light and a dark (drawable-night) variant, so it adapts
                // to the onboarding background — the plain logo_emblem only ships the light version
                // and looked wrong (washed-out/white lettering) on the dark onboarding screen.
                painter = painterResource(R.drawable.logo_community),
                contentDescription = null,
                modifier = Modifier.size(150.dp),
            )
        } else if (slide.icon != null) {
            Icon(
                imageVector = slide.icon,
                contentDescription = null,
                tint = BrandGreen,
                modifier = Modifier.size(96.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(slide.titleRes),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(slide.bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
