package de.igbdsandzakkassel.vaktija.ui.zakat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGold
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGreen
import java.text.NumberFormat
import java.util.Locale

/**
 * Zakat calculator.
 *
 * Deliberately a calculator and nothing more: it adds up what the user enters, subtracts debts,
 * compares the result against the nisab they give, and takes 2.5 %. It does not fetch a gold price,
 * because a wrong nisab silently produces a wrong obligation — and the community's imam is the
 * right source for that figure, not a web service this app happened to reach.
 *
 * Everything is entered by hand for the same reason: a calculator that guesses at someone's assets
 * would be worse than useless.
 */
@Composable
fun ZakatScreen(modifier: Modifier = Modifier) {
    var cash by rememberSaveable { mutableStateOf("") }
    var bank by rememberSaveable { mutableStateOf("") }
    var gold by rememberSaveable { mutableStateOf("") }
    var business by rememberSaveable { mutableStateOf("") }
    var receivable by rememberSaveable { mutableStateOf("") }
    var debts by rememberSaveable { mutableStateOf("") }
    var nisab by rememberSaveable { mutableStateOf("") }

    val assets = listOf(cash, bank, gold, business, receivable).sumOf { it.toAmount() }
    val net = assets - debts.toAmount()
    val nisabValue = nisab.toAmount()
    // Below nisab there is no zakat at all — showing a small figure anyway would invite paying
    // something that is not owed.
    val due = if (nisabValue > 0 && net >= nisabValue) net * RATE else 0.0
    val aboveNisab = nisabValue > 0 && net >= nisabValue

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.zakat_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SectionLabel(stringResource(R.string.zakat_assets))
                AmountField(cash, { cash = it }, R.string.zakat_cash)
                AmountField(bank, { bank = it }, R.string.zakat_bank)
                AmountField(gold, { gold = it }, R.string.zakat_gold)
                AmountField(business, { business = it }, R.string.zakat_business)
                AmountField(receivable, { receivable = it }, R.string.zakat_receivable)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SectionLabel(stringResource(R.string.zakat_deductions))
                AmountField(debts, { debts = it }, R.string.zakat_debts)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SectionLabel(stringResource(R.string.zakat_nisab))
                AmountField(nisab, { nisab = it }, R.string.zakat_nisab_value)
                Text(
                    text = stringResource(R.string.zakat_nisab_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ResultRow(stringResource(R.string.zakat_total_assets), assets)
                ResultRow(stringResource(R.string.zakat_net), net)
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.zakat_due),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = due.money(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreen,
                    )
                }
                Text(
                    text = stringResource(
                        when {
                            nisabValue <= 0 -> R.string.zakat_enter_nisab
                            aboveNisab -> R.string.zakat_above_nisab
                            else -> R.string.zakat_below_nisab
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (aboveNisab) BrandGold else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = stringResource(R.string.zakat_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = BrandGreen,
    )
}

@Composable
private fun AmountField(value: String, onChange: (String) -> Unit, labelRes: Int) {
    OutlinedTextField(
        value = value,
        // Filtered on the way in, so the field can never hold something the maths would silently
        // read as zero. A comma is accepted because that is how the amount is written here.
        onValueChange = { raw -> onChange(raw.filter { it.isDigit() || it == '.' || it == ',' }) },
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        suffix = { Text("€") },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ResultRow(label: String, amount: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(text = amount.money(), fontWeight = FontWeight.Medium)
    }
}

/** Accepts both "1.234,50" and "1234.50" — people type whichever their keyboard offers. */
private fun String.toAmount(): Double =
    replace(".", "").replace(",", ".").toDoubleOrNull()
        ?: replace(",", "").toDoubleOrNull()
        ?: 0.0

@Composable
private fun Double.money(): String =
    NumberFormat.getCurrencyInstance(Locale.GERMANY).format(this)

private const val RATE = 0.025
