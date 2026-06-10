# Kassel Vaktija — the road to "Live" (personal account)

Because the developer account is **personal**, Google requires one extra step before the app can go
public: a **closed test with at least 12 testers, running for 14 days in a row**. After that you can
apply for production and publish. Here's the whole path — most of it I'll do with you; only the
**bold "YOU"** parts need you.

## Right now (while the account verifies)
- **YOU: collect at least 12 testers' Gmail addresses.** Family, friends, people from the Gemeinde —
  anyone with a Google/Gmail account who'll install the app on their Android phone. Aim for ~15 to be
  safe (in case one or two drop out). Just gather the email addresses in a list; nothing to install yet.
- **YOU: host the privacy page** (`privacy-site/HOW-TO-HOST.md`) and send me the link.

## Once the account is verified (I'll guide each click)
1. **Create the app** in Play Console — name *Kassel Vaktija*, German, "App", Free.
2. **Fill in the store listing** — paste from `store-listing.md`; upload `play_icon_512.png`,
   `feature_graphic_1024x500.png`, and the 7 screenshots from `screenshots/`.
3. **App content forms** — privacy policy URL, Data safety, content rating, target audience,
   the **foreground-service declaration** (paste-text in `launch-readiness.md`, upload
   `video/adhan_fgs_demo.mp4`). I have answers ready for all of them.
4. **Upload the app** — the signed `app-release.aab` goes to the **Closed testing** track.
5. **Add your 12+ testers** — paste their Gmail addresses as the tester list.
6. **Send testers the opt-in link** (Play Console gives you one). Each tester:
   - opens the link, taps **"Become a tester"**, then installs Kassel Vaktija from the Play Store.
   - They should keep it installed for the 14 days. Tell them to actually open it a few times.
7. **Wait 14 days** with 12+ testers opted in. Then Play unlocks **"Apply for production access."**
8. **Apply for production** → short Google review → **publish**. 🎉

## Message you can send your testers (once we have the link)
> Selam! Ich teste gerade unsere neue Gemeinde-App **Kassel Vaktija** (Gebetszeiten, Adhan, Qibla,
> Koran u. v. m.). Magst du mithelfen? 1) Diesen Link öffnen: [LINK], 2) auf „Tester werden" tippen,
> 3) die App aus dem Play Store installieren und ein paar Tage drauflassen. Vielen Dank! 🤲

## Notes
- The 14-day clock only counts while **12+ testers stay opted in** — if people uninstall and you drop
  below 12, it can pause. That's why ~15 is a safer target.
- Android TV: we can add the TV form-factor *after* the phone launch (separate review).
