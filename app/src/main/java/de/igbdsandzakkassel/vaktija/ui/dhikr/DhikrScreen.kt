package de.igbdsandzakkassel.vaktija.ui.dhikr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.igbdsandzakkassel.vaktija.core.locale.LocaleController

/**
 * Dhikr (remembrances): each shown in Arabic, with Latin pronunciation and the meaning in the user's
 * selected app language (Arabic users see only the Arabic + pronunciation). Content was authored with
 * standard Arabic/transliteration and native-reviewed meanings.
 */
@Composable
fun DhikrScreen(modifier: Modifier = Modifier) {
    val locales = LocalConfiguration.current.locales
    val lang = if (locales.isEmpty) LocaleController.current().tag else locales[0].language
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        items(DhikrData.ITEMS) { dhikr -> DhikrCard(dhikr, lang) }
    }
}

@Composable
private fun DhikrCard(dhikr: Dhikr, lang: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = dhikr.arabic,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 40.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = dhikr.transliteration,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            val meaning = if (lang == "ar") null else dhikr.meaning[lang] ?: dhikr.meaning["en"]
            if (meaning != null) {
                Text(
                    text = meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

data class Dhikr(
    val arabic: String,
    val transliteration: String,
    /** Meaning per app-language tag (always includes "en" as a fallback). */
    val meaning: Map<String, String>,
)

/** Common dhikr. Arabic + transliteration are standard; meanings native-reviewed via a workflow. */
object DhikrData {
    val ITEMS: List<Dhikr> = listOf(
        Dhikr(
            "سُبْحَانَ اللَّهِ", "Subhan Allah",
            mapOf(
                "en" to "Glory be to Allah", "bs" to "Slava Allahu", "de" to "Gepriesen sei Allah",
                "tr" to "Allah'ı her türlü eksiklikten tenzih ederim",
                "sq" to "I lartësuar qoftë Allahu (i pastër nga çdo e metë)",
                "ur" to "اللہ پاک ہے", "ru" to "Пречист Аллах",
            ),
        ),
        Dhikr(
            "الْحَمْدُ لِلَّهِ", "Alhamdulillah",
            mapOf(
                "en" to "All praise is due to Allah", "bs" to "Hvala Allahu",
                "de" to "Alles Lob gebührt Allah", "tr" to "Hamd, Allah'a mahsustur",
                "sq" to "Falënderimi i takon Allahut", "ur" to "تمام تعریفیں اللہ ہی کے لیے ہیں",
                "ru" to "Хвала Аллаху",
            ),
        ),
        Dhikr(
            "لَا إِلَٰهَ إِلَّا اللَّهُ", "La ilaha illa Allah",
            mapOf(
                "en" to "There is no god but Allah", "bs" to "Nema boga osim Allaha",
                "de" to "Es gibt keinen Gott außer Allah", "tr" to "Allah'tan başka ilah yoktur",
                "sq" to "Nuk ka zot tjetër që meriton të adhurohet përveç Allahut",
                "ur" to "اللہ کے سوا کوئی معبود نہیں",
                "ru" to "Нет божества, достойного поклонения, кроме Аллаха",
            ),
        ),
        Dhikr(
            "اللَّهُ أَكْبَرُ", "Allahu Akbar",
            mapOf(
                "en" to "Allah is the Greatest", "bs" to "Allah je najveći",
                "de" to "Allah ist der Größte", "tr" to "Allah en büyüktür",
                "sq" to "Allahu është më i Madhi", "ur" to "اللہ سب سے بڑا ہے",
                "ru" to "Аллах Велик",
            ),
        ),
        Dhikr(
            "أَسْتَغْفِرُ اللَّهَ", "Astaghfirullah",
            mapOf(
                "en" to "I seek forgiveness from Allah", "bs" to "Tražim oprost od Allaha",
                "de" to "Ich bitte Allah um Vergebung", "tr" to "Allah'tan bağışlanma dilerim",
                "sq" to "Kërkoj falje nga Allahu", "ur" to "میں اللہ سے بخشش مانگتا ہوں",
                "ru" to "Прошу прощения у Аллаха",
            ),
        ),
        Dhikr(
            "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "Subhan Allahi wa bihamdihi",
            mapOf(
                "en" to "Glory and praise be to Allah", "bs" to "Slava Allahu i Njemu hvala",
                "de" to "Gepriesen sei Allah und alles Lob gebührt Ihm",
                "tr" to "Allah'ı hamd ile tesbih ederim",
                "sq" to "I lartësuar qoftë Allahu dhe Atij i takon falënderimi",
                "ur" to "اللہ پاک ہے اور اُسی کی حمد کے ساتھ", "ru" to "Пречист Аллах и хвала Ему",
            ),
        ),
        Dhikr(
            "سُبْحَانَ اللَّهِ الْعَظِيمِ", "Subhan Allah al-Azim",
            mapOf(
                "en" to "Glory be to Allah, the Most Great", "bs" to "Slava Allahu Veličanstvenom",
                "de" to "Gepriesen sei Allah, der Allmächtige",
                "tr" to "Yüce Allah'ı her türlü eksiklikten tenzih ederim",
                "sq" to "I lartësuar qoftë Allahu i Madhërishëm", "ur" to "اللہ پاک ہے، جو سب سے عظیم ہے",
                "ru" to "Пречист Аллах Великий",
            ),
        ),
        Dhikr(
            "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", "La hawla wa la quwwata illa billah",
            mapOf(
                "en" to "There is no might nor power except with Allah",
                "bs" to "Nema snage ni moći osim u Allaha",
                "de" to "Es gibt keine Macht und keine Kraft außer bei Allah",
                "tr" to "Güç ve kuvvet ancak Allah'a aittir",
                "sq" to "Nuk ka forcë e as fuqi përveçse me ndihmën e Allahut",
                "ur" to "اللہ کی مدد کے سوا نہ کوئی طاقت ہے اور نہ قوت",
                "ru" to "Нет силы и мощи ни у кого, кроме Аллаха",
            ),
        ),
        Dhikr(
            "حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ", "Hasbunallahu wa ni'mal-wakil",
            mapOf(
                "en" to "Allah is sufficient for us, and He is the best disposer of affairs",
                "bs" to "Dovoljan nam je Allah i divan li je On Zaštitnik",
                "de" to "Allah genügt uns, und Er ist der beste Sachwalter",
                "tr" to "Allah bize yeter, O ne güzel vekildir",
                "sq" to "Na mjafton Allahu dhe sa Mbrojtës i mrekullueshëm është Ai",
                "ur" to "ہمارے لیے اللہ کافی ہے اور وہی بہترین کارساز ہے",
                "ru" to "Достаточно нам Аллаха, и Он — наилучший Покровитель",
            ),
        ),
        Dhikr(
            "اللَّهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ", "Allahumma salli ala Muhammad",
            mapOf(
                "en" to "O Allah, send blessings upon Muhammad", "bs" to "Allahu moj, blagoslovi Muhammeda",
                "de" to "O Allah, segne Muhammad", "tr" to "Allah'ım, Muhammed'e salât eyle",
                "sq" to "O Allah, dërgo bekime mbi Muhamedin", "ur" to "اے اللہ! محمد صلی اللہ علیہ وسلم پر درود بھیج",
                "ru" to "О Аллах, благослови Мухаммада",
            ),
        ),
        Dhikr(
            "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ", "La ilaha illa Allahu wahdahu la sharika lah",
            mapOf(
                "en" to "There is no god but Allah, alone, without partner",
                "bs" to "Nema boga osim Allaha, Jedinog, koji nema saučesnika",
                "de" to "Es gibt keinen Gott außer Allah, Er allein, ohne Teilhaber",
                "tr" to "Allah'tan başka ilah yoktur; O tektir, ortağı yoktur",
                "sq" to "Nuk ka zot tjetër që meriton të adhurohet përveç Allahut, Një e të Vetëm, që nuk ka ortak",
                "ur" to "اللہ کے سوا کوئی معبود نہیں، وہ اکیلا ہے، اُس کا کوئی شریک نہیں",
                "ru" to "Нет божества, достойного поклонения, кроме одного лишь Аллаха, у Которого нет сотоварища",
            ),
        ),
    )
}
