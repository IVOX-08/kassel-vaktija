/**
 * Cloud Functions for Kassel Vaktija — instant announcement push.
 *
 * When the admin posts a document to the Firestore `news` collection, this trigger sends a Firebase
 * Cloud Messaging push to the "announcements" topic, which every installed app is subscribed to. The
 * push carries the source-language title/body; the app opens to the fully translated content.
 * (Future: send per-language to "news_<lang>" topics for translated tray text.)
 *
 * Requires the Firebase Blaze (pay-as-you-go) plan. Deploy with: firebase deploy --only functions
 * See docs/push/SETUP.md.
 */
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

/** Pick the source-language text from a {"bs":"…","de":"…"} per-language map. */
function pick(map, sourceLang) {
  if (!map || typeof map !== "object") return "";
  return map[sourceLang] || Object.values(map)[0] || "";
}

async function notify(snap, fallbackTitle) {
  const data = snap.data() || {};
  const sourceLang = data.sourceLang || "bs";
  const title = pick(data.title, sourceLang) || fallbackTitle;
  const body = pick(data.body, sourceLang) || "";
  await getMessaging().send({
    topic: "announcements",
    notification: { title, body },
    android: {
      priority: "high",
      notification: { channelId: "news_announcements_v2" },
    },
    data: { type: snap.ref.parent.id, id: snap.id },
  });
}

exports.onNewsCreated = onDocumentCreated("news/{id}", (event) => {
  if (!event.data) return null;
  return notify(event.data, "Neue Mitteilung");
});
