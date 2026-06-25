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

## Most likely NOTHING to do — the deployed catch-all already covers it

The rule currently published for this project is the recursive **catch-all** form:

```firestore
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read: if true;
      allow write: if request.auth != null
        && request.auth.uid == '1a7xqRgIYDR0RZqa3KghBlz98PK2';
    }
  }
}
```

`match /{document=**}` matches **every** collection — including `news_images`. So if this is still the
deployed rule, the flyer feature works with **no change at all** (public read, admin-only write already
apply to `news_images`).

**To be 100 % sure (≈1 minute):** Firebase console → **kassel-vaktija** → **Build → Firestore Database**
→ **Rules** tab. If you see `match /{document=**}` with `read: if true` and the admin-UID write check,
you're done — close it. Only if the rules instead list collections **one by one** (a `match /news/...`
block, a `match /config/...` block, but **no** `news_images` block) do you need the step below.

## If (and only if) the rules list collections individually

Add a `news_images` block identical to the `news` one, then **Publish**. Until then a posted flyer is
rejected server-side: the announcement text still works, the picture just won't appear (the card quietly
shows no image). The full per-collection ruleset:

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
