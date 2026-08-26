package de.igbdsandzakkassel.vaktija.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Text
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

            // The list gets the space that is left, so the hint below can never sit on top of a row.
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                // Not loaded yet: the catalogue comes from the network on a fresh install, and the
                // TV may not be on the mosque's Wi-Fi yet.
                !loaded && communities.isEmpty() -> CentredNote(
                    "Gemeinden werden geladen …\nUčitavanje džemata …",
                )
                communities.isEmpty() -> CentredNote(
                    "Keine Gemeinde verfügbar — bitte Internetverbindung prüfen\n" +
                        "Nema dostupnih džemata — provjeri internet vezu",
                )
                picked == null -> PickerList(
                    rows = communities.map { community ->
                        Row(
                            key = community.id,
                            title = community.name,
                            subtitle = community.locations.joinToString(" · ") { it.name },
                            onSelect = { chosen = community },
                        )
                    },
                )
                else -> PickerList(
                    rows = picked.locations.map { location ->
                        Row(
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

private data class Row(
    val key: String,
    val title: String,
    val subtitle: String,
    val onSelect: () -> Unit,
)

@Composable
private fun PickerList(rows: List<Row>) {
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
private fun PickerRow(row: Row, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Column(
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
            .padding(horizontal = 22.dp, vertical = 16.dp),
    ) {
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
