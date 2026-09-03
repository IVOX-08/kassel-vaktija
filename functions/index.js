/**
 * Cloud Functions für IGBD Vaktija — Push zu Mitteilungen und Zeitänderungen.
 *
 * WARUM DIESE DATEI NEU GESCHRIEBEN WURDE
 *
 * Sie horchte auf `news/{id}` und `config/community`. Beim Umbau auf mehrere Gemeinden
 * (26.08.2026) sind beide Pfade verschwunden: Mitteilungen liegen seitdem unter
 * `communities/{id}/news` bzw. `broadcasts`, die Regeln unter `communities/{id}/config/rules`.
 * Die Auslöser konnten also gar nicht mehr feuern — seit dem Umbau ging zu KEINER Mitteilung
 * eine Push-Meldung raus, auf beiden Plattformen. Die Mitteilung stand in der App, und niemand
 * erfuhr davon.
 *
 * Zweitens hing jedes Gerät an dem einen Thema `announcements`. Selbst mit richtigen Pfaden
 * hätte jeder Nutzer in Deutschland jede Mitteilung jeder der 81 Gemeinden bekommen.
 *
 * DIE THEMEN
 *
 * Jedes Gerät hängt an genau drei (siehe iosApp/iosApp/PushService.swift):
 *
 *   c_<gemeinde>            — Datenmeldung: die Gebetszeiten dieser Gemeinde haben sich geändert
 *   c_<gemeinde>_<sprache>  — Mitteilungen dieser Gemeinde, in der Sprache des Nutzers
 *   b_<sprache>             — verbandsweite Mitteilungen, in der Sprache des Nutzers
 *
 * Die Sprache steckt im Thema, weil der Text der Meldung schon beim Verfassen in alle acht
 * Sprachen übersetzt wird. Ein Gerät bekommt dadurch die Mitteilung in seiner Sprache in der
 * Leiste — nicht erst nach dem Öffnen der App. Acht Sendungen je Mitteilung, das ist alles.
 *
 * Bereitstellen: firebase deploy --only functions
 * Braucht den Blaze-Tarif. Siehe docs/push/SETUP.md.
 */
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

/** Die acht Sprachen der App. Muss zu AppLanguage (Kotlin/Swift) passen. */
const LANGS = ["bs", "de", "ar", "tr", "sq", "en", "ur", "ru"];

/**
 * Die Heimatgemeinde der App.
 *
 * Die veröffentlichte Android-Version 1.1.3 kennt keine Gemeindeauswahl und hängt am alten Thema
 * `announcements`. Damit ihre Nutzer nicht stumm bleiben, geht eine Mitteilung DIESER Gemeinde
 * zusätzlich dorthin. Die Zeile darf weg, sobald niemand mehr auf 1.1.3 ist.
 */
const LEGACY_COMMUNITY = "igbd-gemeinde-sandzak-kassel";
const LEGACY_TOPIC = "announcements";

/** Der leise Ton für Mitteilungen. Liegt als tone_soft.wav im App-Paket. */
const IOS_NEWS_SOUND = "tone_soft.wav";

/**
 * Der Text in einer Sprache — mit demselben Rückfall wie die App: erst die gewünschte Sprache,
 * dann die Sprache, in der geschrieben wurde, dann irgendeine.
 */
function pick(map, lang, sourceLang) {
  if (!map || typeof map !== "object") return "";
  return map[lang] || map[sourceLang] || Object.values(map)[0] || "";
}

/**
 * Eine Mitteilung an eine Liste von Themen — je Sprache eine Sendung.
 *
 * `topicFor(lang)` liefert das Thema; gibt es nichts zu senden, gibt es `null` zurück.
 */
async function sendPerLanguage(snap, topicFor, extraTopicsBySourceLang) {
  const data = snap.data() || {};
  const sourceLang = data.sourceLang || "bs";
  const type = snap.ref.parent.id; // "news" oder "broadcasts" — die App springt danach

  const messages = [];
  for (const lang of LANGS) {
    const topic = topicFor(lang);
    if (!topic) continue;
    const title = pick(data.title, lang, sourceLang);
    const body = pick(data.body, lang, sourceLang);
    // Ohne Titel keine Meldung. Eine leere Benachrichtigung ist schlechter als keine.
    if (!title) continue;
    messages.push({
      topic,
      notification: { title, body },
      android: { priority: "high", notification: { channelId: "news_announcements_v2" } },
      // Auf iOS bestimmt der ABSENDER den Ton: Apple lässt eine App den Ton einer eingehenden
      // Push-Meldung nicht selbst wählen. Ohne diese Zeile klingelt jede Mitteilung mit dem
      // Standardton des Systems, egal was in den Einstellungen steht.
      apns: { payload: { aps: { sound: IOS_NEWS_SOUND } } },
      data: { type, id: snap.id },
    });
  }

  for (const topic of extraTopicsBySourceLang || []) {
    const title = pick(data.title, sourceLang, sourceLang);
    if (!title) continue;
    messages.push({
      topic,
      notification: { title, body: pick(data.body, sourceLang, sourceLang) },
      android: { priority: "high", notification: { channelId: "news_announcements_v2" } },
      apns: { payload: { aps: { sound: IOS_NEWS_SOUND } } },
      data: { type, id: snap.id },
    });
  }

  // Einzeln senden statt sendEach: Eine Sendung, die scheitert, darf die übrigen sieben nicht
  // mitnehmen. Ein Thema ohne Abonnenten ist kein Fehler — Firebase nimmt die Nachricht an.
  await Promise.all(
    messages.map((m) =>
      getMessaging()
        .send(m)
        .catch((e) => console.error(`Senden an ${m.topic} fehlgeschlagen:`, e.message))
    )
  );
}

/** Mitteilung einer Gemeinde → nur an deren Geräte. */
exports.onNewsCreated = onDocumentCreated(
  "communities/{communityId}/news/{id}",
  (event) => {
    if (!event.data) return null;
    const community = event.params.communityId;
    return sendPerLanguage(
      event.data,
      (lang) => `c_${community}_${lang}`,
      community === LEGACY_COMMUNITY ? [LEGACY_TOPIC] : []
    );
  }
);

/**
 * Verbandsweite Mitteilung des Hauptadministrators.
 *
 * `audience` leer heißt alle — dann reicht ein Thema je Sprache. Steht dort eine Liste von
 * Gemeinden, geht die Meldung an genau deren Themen; das ist derselbe Empfängerkreis, nach dem
 * die App die Liste filtert (NewsItem.reaches).
 */
exports.onBroadcastCreated = onDocumentCreated("broadcasts/{id}", (event) => {
  if (!event.data) return null;
  const audience = event.data.data().audience;
  if (!Array.isArray(audience) || audience.length === 0) {
    return sendPerLanguage(event.data, (lang) => `b_${lang}`, []);
  }
  return Promise.all(
    audience.map((community) =>
      sendPerLanguage(event.data, (lang) => `c_${community}_${lang}`, [])
    )
  );
});

/**
 * Der Vorstand hat Ikamet, Džuma oder den Bajram-Termin geändert.
 *
 * Datenmeldung ohne Text: Die App holt die offiziellen Zeiten neu und stellt ihre geplanten
 * Meldungen um. Deshalb sprachunabhängig und still.
 */
exports.onRulesUpdated = onDocumentUpdated(
  "communities/{communityId}/config/rules",
  (event) => {
    const after = event.data && event.data.after;
    if (!after) return null;
    const community = event.params.communityId;
    return getMessaging()
      .send({
        topic: `c_${community}`,
        android: { priority: "high" },
        // iOS verwirft eine reine Datenmeldung, wenn sie nicht ausdrücklich als Weckruf im
        // Hintergrund gekennzeichnet ist. Ohne diesen Block erreichen Zeitänderungen kein
        // iPhone — still, ohne Fehler. Priorität 5 verlangt APNs für content-available; 10
        // wird abgelehnt.
        apns: {
          headers: { "apns-priority": "5" },
          payload: { aps: { "content-available": 1 } },
        },
        data: { type: "config", updatedAt: String(after.data().updatedAt || "") },
      })
      .catch((e) => console.error("Senden der Zeitaenderung fehlgeschlagen:", e.message));
  }
);
