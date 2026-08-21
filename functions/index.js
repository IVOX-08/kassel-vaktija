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
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
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

/**
 * When the admin edits the community prayer/Iqamah times (config/community), tell every device.
 * Sent as a data-only message carrying `updatedAt`; the app builds a notification in each user's own
 * language and watermark-gates it (so the admin who made the change isn't notified, and a later
 * poll-on-wake for the same change never double-notifies).
 */
exports.onConfigUpdated = onDocumentUpdated("config/community", (event) => {
  const after = event.data && event.data.after;
  if (!after) return null;
  const updatedAt = after.data().updatedAt;
  return getMessaging().send({
    topic: "announcements",
    android: { priority: "high" },
    // iOS drops a data-only message unless it is explicitly marked as a background wake-up.
    // Without this block prayer-time changes never reach iPhones — silently, no error.
    // Priority 5 is required by APNs for content-available pushes; 10 gets rejected.
    apns: {
      headers: { "apns-priority": "5" },
      payload: { aps: { "content-available": 1 } },
    },
    data: { type: "config", updatedAt: String(updatedAt || "") },
  });
});
