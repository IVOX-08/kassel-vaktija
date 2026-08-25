package de.igbdsandzakkassel.vaktija.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.data.model.CommunityRules
import de.igbdsandzakkassel.vaktija.data.model.DailyTimes
import de.igbdsandzakkassel.vaktija.data.community.CommunityCatalog
import de.igbdsandzakkassel.vaktija.data.community.CommunityRepository
import de.igbdsandzakkassel.vaktija.data.model.CommunityStatus
import de.igbdsandzakkassel.vaktija.data.model.Prayer
import de.igbdsandzakkassel.vaktija.data.repository.CommunityRuleProvider
import de.igbdsandzakkassel.vaktija.data.repository.PrayerTimesRepository
import de.igbdsandzakkassel.vaktija.domain.PrayerScheduleCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val timesRepository: PrayerTimesRepository,
    private val communityRepository: CommunityRepository,
    ruleProvider: CommunityRuleProvider,
) : ViewModel() {

    // Emits once per second to drive the live clock + countdown.
    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(1000)
        }
    }

    init {
        // Pull the latest times on open (idempotent; WorkManager also refreshes in the background).
        viewModelScope.launch { timesRepository.refresh() }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        timesRepository.observeToday(),
        timesRepository.observeFreshness(),
        ruleProvider.observeRules(),
        communityRepository.observeSelection(),
        ticker,
    ) { times, fresh, rules, selection, _ ->
        if (times == null) DashboardUiState(loading = true)
        else buildState(times, rules, fresh, LocalDateTime.now()).copy(
            locationName = selection?.location?.name.orEmpty(),
            // Town address only when that town has its own mosque; otherwise the
            // community's, so all three Kassel towns point at Schwanenweg 13.
            locationAddress = selection?.location?.address ?: selection?.community?.address,
            // A suspended community keeps its prayer times but loses its presence, so the donation
            // link goes with the rest of its branding.
            donationUrl = selection?.community?.donationUrl
                ?.takeIf { selection.community.status.showsCommunityContent },
            communityStatus = selection?.community?.status ?: CommunityStatus.ACTIVE,
            // Branding goes away with the rest when a community is switched off.
            communityLogoUrl = selection?.community?.logoUrl
                ?.takeIf { selection.community.status.showsCommunityContent },
            isHomeCommunity = selection?.community?.id == CommunityCatalog.KASSEL_ID,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(loading = true),
    )

    private fun buildState(
        times: DailyTimes,
        rules: CommunityRules,
        fresh: Boolean,
        now: LocalDateTime,
    ): DashboardUiState {
        val locale = Locale.getDefault()
        // On Fridays the Dhuhr (Podne) congregation is replaced by Jumu'ah at the community time,
        // so the Dhuhr slot is shown — and counted down to — at the Jumua time.
        val isFriday = now.dayOfWeek == DayOfWeek.FRIDAY
        val effectiveTimes = if (isFriday) times.copy(dhuhr = rules.jumua) else times
        val schedule = PrayerScheduleCalculator.compute(effectiveTimes, now)

        val nowTime = now.toLocalTime()
        val rows = Prayer.entries.map { prayer ->
            val adhan = effectiveTimes.adhan(prayer)
            val jumuaRow = isFriday && prayer == Prayer.DHUHR
            val iqamah = if (jumuaRow) null else rules.iqamah(prayer, adhan)
            PrayerRowUi(
                prayer = prayer,
                labelRes = if (jumuaRow) R.string.prayer_jumua else prayer.labelRes,
                adhan = adhan,
                iqamah = iqamah,
                // Highlight the NEXT upcoming prayer (matches the countdown above).
                isHighlighted = prayer == schedule.nextPrayer,
                // Glow now if we're in this prayer's Adhan→Iqamah window (congregation gathering).
                inIqamahWindow = iqamah != null && !nowTime.isBefore(adhan) && nowTime.isBefore(iqamah),
            )
        }

        // Zone-aware countdown: across a DST changeover night (Europe/Berlin, 2×/year) a naive
        // LocalDateTime difference to tomorrow's Fajr is off by exactly 1 hour.
        val zone = java.time.ZoneId.systemDefault()
        val remaining = Duration.between(now.atZone(zone), schedule.nextAdhan.atZone(zone))

        return DashboardUiState(
            loading = false,
            clock = now.format(CLOCK),
            gregorianDate = now.toLocalDate().format(dateFormatter(locale)),
            hijriDate = HijrahDate.from(now.toLocalDate()).format(hijriFormatter(locale)),
            rows = rows,
            nextPrayer = schedule.nextPrayer,
            // Label for the countdown card: names WHICH prayer is being counted down to (Jumu'ah on
            // the Friday Dhuhr slot).
            nextPrayerLabelRes =
                if (isFriday && schedule.nextPrayer == Prayer.DHUHR) R.string.prayer_jumua
                else schedule.nextPrayer.labelRes,
            countdown = formatCountdown(remaining),
            // On Friday the Jumua time is shown in the list (no separate card).
            jumua = if (isFriday) null else rules.jumua,
            bajram = rules.activeBajram(now.toLocalDate()),
            isStale = !fresh,
        )
    }

    private fun formatCountdown(d: Duration): String {
        val total = d.seconds.coerceAtLeast(0)
        return String.format(Locale.US, "%02d:%02d:%02d", total / 3600, (total % 3600) / 60, total % 60)
    }

    private companion object {
        val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

        // Locale-AWARE date patterns: the old fixed "d. MMMM" (Bosnian/German ordinal-dot style)
        // produced wrong-looking dates in 6 of the 8 languages ("Wednesday, 2. July 2026",
        // "2. июля 2026"). getBestDateTimePattern picks each locale's own convention.
        fun dateFormatter(locale: Locale): DateTimeFormatter =
            DateTimeFormatter.ofPattern(
                android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEEdMMMMy"),
                locale,
            )
        fun hijriFormatter(locale: Locale): DateTimeFormatter =
            DateTimeFormatter.ofPattern(
                android.text.format.DateFormat.getBestDateTimePattern(locale, "GdMMMMy"),
                locale,
            )
    }
}
