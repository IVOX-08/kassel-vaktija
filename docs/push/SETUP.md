# Instant push notifications — setup

The app side is **done**: every install subscribes to the `announcements` topic and shows a
notification the moment a push arrives (`PushMessagingService` + the news channel). It only needs the
server side switched on — that's the part below.

## What's still needed (one-time, needs the imam's account)
Sending push requires a **Cloud Function**, which requires the Firebase **Blaze (pay-as-you-go)**
plan. At this community's volume it is **effectively free** (the free monthly quota far exceeds a few
announcements), but Blaze requires a **billing account with a payment method** — so an **adult**
(the imam / the account holder) has to enable it.

> Until this is enabled, announcements still arrive via the existing **poll-on-wake** check — just
> not instantly (they show up within a few hours, at the next prayer alarm / refresh).

## Steps (do together when the Firebase account is ready)
1. In the [Firebase console](https://console.firebase.google.com) → the `kassel-vaktija` project →
   **Upgrade** to the **Blaze** plan (add the imam's payment method; you can set a low budget alert).
2. Install the Firebase CLI on the PC: `npm install -g firebase-tools`, then `firebase login`.
3. From the project root: `firebase use kassel-vaktija`, then `cd functions && npm install`.
4. Deploy: `firebase deploy --only functions`.
5. Test: post a community announcement in the app → every device with the app should get an instant
   notification.

The function code is in **`functions/index.js`** (triggers on new `news` documents, sends an FCM push
to the `announcements` topic with the source-language title/body; the app opens to the translated
content).

## Possible later refinement
For the **tray text itself** to be in each user's language (not just the in-app content), switch to
per-language topics: the app subscribes to `news_<lang>` and the function loops over the per-language
maps, sending each translation to its `news_<lang>` topic. Small change on both sides — not needed
for v1.
