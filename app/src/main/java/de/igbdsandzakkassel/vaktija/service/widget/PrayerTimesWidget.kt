package de.igbdsandzakkassel.vaktija.service.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import de.igbdsandzakkassel.vaktija.MainActivity
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.data.model.DailyTimes
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.data.repository.PrayerTimesRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Green = Color(0xFF2E7D32)
private val Gold = Color(0xFFD4AF37)
private val White = Color(0xFFFFFFFF)
private val WhiteDim = Color(0xCCFFFFFF)
private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")

/** Hilt access into the (non-component) Glance widget. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun prayerTimesRepository(): PrayerTimesRepository
}

/**
 * Home-screen widget: the day's prayer times with the next one highlighted in gold. Reads the same
 * offline-first cache as the app and opens the app when tapped. Refreshed by the daily worker, on
 * each prayer alarm, and on the system's periodic widget update.
 */
class PrayerTimesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .prayerTimesRepository()
        val times = repository.observeToday().first()
        provideContent { WidgetContent(times) }
    }

    companion object {
        suspend fun refresh(context: Context) = PrayerTimesWidget().updateAll(context)
    }
}

@Composable
private fun WidgetContent(times: DailyTimes?) {
    val context = LocalContext.current
    val next = times?.let(::nextObligatory)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Green))
            .cornerRadius(16.dp)
            .padding(14.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = context.getString(R.string.app_name),
                style = TextStyle(color = ColorProvider(Gold), fontWeight = FontWeight.Bold, fontSize = 15.sp),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = LocalDate.now().format(DateTimeFormatter.ofPattern("d. MMM", Locale.getDefault())),
                style = TextStyle(color = ColorProvider(WhiteDim), fontSize = 12.sp),
            )
        }
        Spacer(GlanceModifier.height(8.dp))

        if (times == null) {
            Text(
                text = "—",
                style = TextStyle(color = ColorProvider(White), fontSize = 14.sp),
            )
        } else {
            Prayer.OBLIGATORY.forEach { prayer ->
                val isNext = prayer == next
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = context.getString(prayer.labelRes),
                        style = TextStyle(
                            color = ColorProvider(if (isNext) Gold else WhiteDim),
                            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp,
                        ),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Text(
                        text = times.adhan(prayer).format(TIME_FMT),
                        style = TextStyle(
                            color = ColorProvider(if (isNext) Gold else White),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        ),
                    )
                }
            }
        }
    }
}

/** The next obligatory prayer today, or Fajr if they've all passed. */
private fun nextObligatory(times: DailyTimes): Prayer {
    val now = LocalTime.now()
    return Prayer.OBLIGATORY.firstOrNull { times.adhan(it).isAfter(now) } ?: Prayer.FAJR
}
