# Protocol Alerts — Android app

Time-based notifications for your beach-volleyball protocol. Each day the app reads its
weekly schedule, decides whether today is **Training**, **Match**, or **Rest**, and fires
native reminders (supplements, meals, caffeine, hydration, recovery) at the right times.

Content is sourced from `app/src/main/assets/schedule.json`, generated from the
MetabolicIntelligence_Dashboard_v4 Training/Match/Rest timelines.

---

## Build the APK in the cloud (no Android Studio needed)

1. **Create a free GitHub account** at https://github.com if you don't have one.
2. **Create a new repository** (the green *New* button). Name it e.g. `protocol-alerts`.
   Leave it empty (no README).
3. **Upload the project:** on the new repo page click *uploading an existing file*,
   then drag in **all the contents of this folder** (the `app` folder, `.github` folder,
   `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, etc.). Commit.
   - Tip: keep the folder structure intact. The `.github/workflows/build.yml` file is what
     triggers the build, so make sure the `.github` folder uploads too.
4. **Wait for the build.** Go to the **Actions** tab. A run called *Build APK* starts
   automatically and takes ~3–5 minutes. A green check means success.
5. **Download the APK.** Open the finished run → scroll to **Artifacts** →
   download **protocol-alerts-debug-apk**. Unzip it to get `app-debug.apk`.
   (Download it on your phone, or transfer it to your phone.)
6. **Install on the phone:**
   - Open the APK file. Android will ask to allow *Install unknown apps* for your
     browser/Files app — enable it, then tap **Install**.
7. **First launch:** open **Protocol Alerts** and in the *Setup* card:
   - Grant **Notifications**.
   - Grant **Exact alarms**.
   - Tap **Disable battery optimization** and allow it.
   - Tap **Send test notification** to confirm it works.

That's it — alerts will fire automatically based on the weekly schedule. Adjust any day
by tapping its chip on the *Weekly schedule* card (cycles Training → Match → Rest).

---

## Alternative: build locally in Android Studio

1. Install **Android Studio** (Hedgehog or newer).
2. *File → Open* this folder. Let it sync (it will fetch Gradle/SDK automatically; if it
   offers to create the Gradle wrapper, accept).
3. Plug in your phone with **USB debugging** enabled (*Settings → Developer options*).
4. Press **Run** (▶). The app installs and launches on the phone.

---

## Editing the protocol

All alert content lives in **`app/src/main/assets/schedule.json`**:

- `weekdayDefault` maps each weekday to a day type.
- `days.TRAINING / MATCH / REST` each hold an `events` list.
- Each event: `time` ("HH:mm", 24-hour), optional `leadMinutes` (fire early), `title`, `body`.

Change the JSON, re-upload it to GitHub (or rebuild in Android Studio), download the new
APK, and reinstall. To keep this in sync with the dashboard, regenerate the JSON whenever
the v4 timelines change.

---

## How it works (architecture)

- **AlarmManager exact alarms** fire each reminder (precise, survive Doze via
  `setExactAndAllowWhileIdle`).
- **A daily WorkManager job (~00:05)** plus boot/update/timezone receivers re-materialize
  the day's alarms so the app self-heals.
- **Three notification channels** (Training / Match / Rest) — tune sound/importance in
  Android settings. Notifications carry **Done** and **Snooze 10m** actions.
- **Offline-first, no server, no account.** Everything runs on-device.

Min Android 8.0 (API 26). Target API 34.
