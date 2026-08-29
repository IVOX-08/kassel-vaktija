package de.igbdsandzakkassel.vaktija.ui.tv

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.data.community.CommunityCatalog
import de.igbdsandzakkassel.vaktija.data.model.Community
import de.igbdsandzakkassel.vaktija.ui.community.CommunityPickerViewModel
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGold
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGreen
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGreenDark
import de.igbdsandzakkassel.vaktija.ui.theme.PageBackgroundLight

/**
 * Which community's times this board shows — asked once, when the TV is first mounted.
 *
 * Built for a remote control, not a finger: no search field (there is no keyboard on a wall), rows
 * big enough to read from the back of the prayer hall, and the first row already focused so the
 * installer only has to press down and OK.
 *
 * Bilingual labels are hard-coded here for the same reason the board's are: the board shows German
 * and Bosnian side by side rather than following one device language, and whoever mounts the TV
 * may not have set its language at all.
 */
@Composable
fun TvCommunityPicker(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CommunityPickerViewModel = hiltViewModel()
    val communities by viewModel.results.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()

    // Set once a community with several towns is picked; the board can only show one town's times,
    // so Kassel (Kassel, Hann. Münden, Korbach) has to be narrowed down before we can continue.
    var chosen by remember { mutableStateOf<Community?>(null) }

    val picked = chosen
    // A community that runs a single town needs no second question.
    LaunchedEffect(picked) {
        val only = picked?.locations?.singleOrNull()
        if (picked != null && only != null) viewModel.select(picked, only, onDone)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackgroundLight)
            .padding(horizontal = 48.dp, vertical = 32.dp), // overscan-safe margin
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = if (picked == null) "Gemeinde wählen" else "Ort wählen",
                color = BrandGreenDark,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (picked == null) "Odaberi džemat" else "Odaberi mjesto",
                color = BrandGold,
                fontSize = 26.sp,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            // Eighty-one communities is far too many to walk past with a D-pad. Typing uses the
            // TV's own on-screen keyboard, which the remote drives — slow to type on, but far
            // faster than pressing DOWN sixty times. Only shown on the community step; a community
            // has a handful of towns at most.
            if (picked == null) {
                val query by viewModel.query.collectAsStateWithLifecycle()
                TextField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    singleLine = true,
                    label = { Text("Suchen · Traži", fontSize = 20.sp) },
                    textStyle = LocalTextStyle.current.copy(fontSize = 26.sp),
                    // The board is a light page; Material's default dark field would sit on it as
                    // a black slab.
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = BrandGreenDark,
                        unfocusedTextColor = BrandGreenDark,
                        focusedLabelColor = BrandGold,
                        unfocusedLabelColor = Color(0xFF777777),
                        focusedIndicatorColor = BrandGold,
                        unfocusedIndicatorColor = Color(0xFFDDDDDD),
                        cursorColor = BrandGreen,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                )
            }

            // The list gets the space that is left, so the hint below can never sit on top of a row.
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                // Not loaded yet: the catalogue comes from the network on a fresh install, and the
                // TV may not be on the mosque's Wi-Fi yet.
                !loaded && communities.isEmpty() -> CentredNote(
                    "Gemeinden werden geladen …\nUčitavanje džemata …",
                )
                communities.isEmpty() && viewModel.query.value.isNotBlank() -> CentredNote(
                    "Keine Gemeinde gefunden\nNema pronađenih džemata",
                )
                communities.isEmpty() -> CentredNote(
                    "Keine Gemeinde verfügbar — bitte Internetverbindung prüfen\n" +
                        "Nema dostupnih džemata — provjeri internet vezu",
                )
                picked == null -> PickerList(
                    rows = communities.map { community ->
                        PickerEntry(
                            key = community.id,
                            title = community.name,
                            subtitle = viewModel.subtitleFor(community, communities),
                            logo = if (community.id == CommunityCatalog.KASSEL_ID) {
                                R.drawable.logo_emblem
                            } else {
                                R.drawable.logo_igbd_positive
                            },
                            logoFocused = if (community.id == CommunityCatalog.KASSEL_ID) {
                                // Kassel's crest sits on a white disc, so it carries on green as it is.
                                R.drawable.logo_emblem
                            } else {
                                R.drawable.logo_igbd_negative
                            },
                            onSelect = { chosen = community },
                        )
                    },
                )
                else -> PickerList(
                    rows = picked.locations.map { location ->
                        PickerEntry(
                            key = location.id,
                            title = location.name,
                            subtitle = location.address.orEmpty(),
                            onSelect = { viewModel.select(picked, location, onDone) },
                        )
                    },
                )
                }
            }

            // The way back, said here rather than on the board: the board is full, and anything
            // drawn over it covers the hadith. This is also the moment the remote is in hand.
            Text(
                text = "Später ändern: OK gedrückt halten · Kasnije promijeniti: držite OK",
                color = BrandGold,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textAlign = TextAlign.End,
            )
        }
    }
}

private data class PickerEntry(
    val key: String,
    val title: String,
    val subtitle: String,
    /** The community's mark, or null on the town step where every row is the same community. */
    @DrawableRes val logo: Int? = null,
    /**
     * The mark to use while the row is focused, when its card turns IZ green. The federation's
     * crescent is green too and would vanish into it; the standards' white negative is what that
     * background calls for.
     */
    @DrawableRes val logoFocused: Int? = null,
    val onSelect: () -> Unit,
)

@Composable
private fun PickerList(rows: List<PickerEntry>) {
    val firstRow = remember { FocusRequester() }
    // Focus the first row so the remote works immediately: without it the D-pad has nothing to
    // move from and the screen looks frozen.
    LaunchedEffect(rows.firstOrNull()?.key) {
        if (rows.isNotEmpty()) runCatching { firstRow.requestFocus() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        items(rows, key = { it.key }) { row ->
            PickerRow(
                row = row,
                modifier = if (row.key == rows.first().key) {
                    Modifier.focusRequester(firstRow)
                } else {
                    Modifier
                },
            )
        }
    }
}

@Composable
private fun PickerRow(row: PickerEntry, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) BrandGreen else Color.White)
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = if (focused) BrandGold else Color(0xFFDDDDDD),
                shape = RoundedCornerShape(10.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            // clickable makes the row focusable and answers the remote's OK button.
            .clickable(onClick = row.onSelect)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val logo = if (focused) (row.logoFocused ?: row.logo) else row.logo
        if (logo != null) {
            Image(
                painter = painterResource(logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(46.dp).padding(end = 18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
        Text(
            text = row.title,
            color = if (focused) Color.White else BrandGreenDark,
            fontSize = 30.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (row.subtitle.isNotBlank()) {
            Text(
                text = row.subtitle,
                color = if (focused) Color(0xFFE8F5E9) else Color(0xFF666666),
                fontSize = 19.sp,
            )
        }
        }
    }
}

@Composable
private fun CentredNote(text: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = text,
            color = BrandGreenDark,
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
