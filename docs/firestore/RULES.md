# Firestore security rules

The app stores three things in Cloud Firestore, all **publicly readable** (the app reads them without
signing anyone in) and **writable only by the admin** (the single account whose UID is baked into the
app as `ADMIN_UID`). The rules are enforced **on Google's servers**, so they are the real protection —
the in-app admin check is only there to show/hide buttons.

| Collection     | What it holds                                   | Read   | Write |
|----------------|-------------------------------------------------|--------|-------|
| `news`         | announcements (per-language title/body)         | anyone | admin |
| `news_images`  | **flyer/image attached to an announcement** (Base64 JPEG, same document id as the announcement) | anyone | admin |
| `config`       | admin-edited community settings                 | anyone | admin |

## ⚠️ Required for the flyer/image feature

`news_images` is the collection added for **attaching pictures to a Mitteilung**. Until the rule
below is published, a posted flyer can't be saved or shown — the announcement text still works, the
picture just won't appear (the card quietly shows no image). So this rule must be added **once**.

## How to publish (one-time, ~2 minutes)

1. Open the [Firebase console](https://console.firebase.google.com) → the **kassel-vaktija** project.
2. Left menu → **Build → Firestore Database** → top tab **Rules**.
3. Make sure the rules include a block for **`news_images`** identical to the `news` block. The full
   correct ruleset is below — if your current rules already match, just confirm `news_images` is
   present; otherwise paste this over what's there.
4. Click **Publish**.

```firestore
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // The single admin account (same UID the app is built with).
    function isAdmin() {
      return request.auth != null
        && request.auth.uid == '1a7xqRgIYDR0RZqa3KghBlz98PK2';
    }

    // Community announcements — everyone reads, only the admin writes.
    match /news/{doc} {
      allow read: if true;
      allow write: if isAdmin();
    }

    // Flyer/image attached to an announcement (same id as the news doc).
    match /news_images/{doc} {
      allow read: if true;
      allow write: if isAdmin();
    }

    // Admin-edited community settings.
    match /config/{doc} {
      allow read: if true;
      allow write: if isAdmin();
    }
  }
}
```

> If the admin UID ever changes, update both this rule and `ADMIN_UID` in
> `app/build.gradle.kts` so they stay in sync.

## Why images live in Firestore (not Cloud Storage)

The flyer is compressed on the admin's phone to a small JPEG and stored Base64-encoded inside a
`news_images` document (capped well under Firestore's 1 MB per-document limit). This means the feature
works on the **free Firestore plan** with **no Cloud Storage and no Blaze billing** — the same plan the
app already uses. Each picture is fetched lazily and cache-first, so a reader downloads a given flyer
at most once. If the community ever needs very large/high-resolution images, the clean upgrade path is
to move the bytes to Cloud Storage and store only a download URL here.
