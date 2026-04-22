# Guardian Watch Tablet — Agent Handoff Brief

> **How to use this file:**
> - **Claude Code:** drop this at repo root as `CLAUDE.md`, then run `claude` in the repo. Claude Code auto-loads it.
> - **Factory Forge / Droid:** drop this at repo root as `AGENTS.md`, then open a Code Droid. Spec Mode recommended.
> - **Cursor:** drop at `.cursor/rules/guardian-watch.md` with `alwaysApply: true` frontmatter.
> - **Paste-as-prompt:** paste the whole file as your first message.

---

## 1. Mission

You are continuing an Android tablet app — **Guardian Watch** — a tactical health-monitoring
system for military medical evacuation. The "broken telephone" problem it solves: critical
patient data getting distorted as a casualty moves from Point-of-Injury → Medic → CASEVAC
→ Role-2 → Role-3. Guardian Watch creates an immutable chain-of-custody + live vitals stream.

A skeleton project is already in place. Your job is to **get to a working MVP demo on a
Samsung Galaxy Tab A9+ 5G** within 3 days. Scope is bounded — do not invent features.

## 2. Hardware — locked

| Component | Model | Constraint that affects your code |
|---|---|---|
| Tablet | **Samsung Galaxy Tab A9+ 5G (SM-X216B)** | 11" 1920×1200 landscape; Android 14 (One UI 6); Snapdragon 695; BLE 5.1; Knox-capable |
| Watch | **Garmin Instinct 2** | **No SpO2 sensor, no skin-temp sensor.** HR + accelerometer only. Never display false SpO2/temp values — use `"--"` placeholder and the string `"sensor: unavailable"` |
| BLE profile | Custom `f1ac0000-...` service | 20-byte packed telemetry, schema in `GuardianWatchProfile.kt`. Falls back to standard HR (180d/2a37) when custom app not loaded. |

## 3. Current State — what exists

```
guardian_watch_tablet/
├── build.gradle.kts, settings, gradle.properties, gradle/libs.versions.toml  [DONE]
├── app/
│   ├── build.gradle.kts  [DONE — Compose BOM 2026.03, Kotlin 2.2.20, Nordic BLE 2.11]
│   └── src/main/
│       ├── AndroidManifest.xml  [DONE — all permissions, FGS, kiosk, HOME intent]
│       ├── res/values/{strings,themes,colors}.xml  [DONE]
│       ├── res/xml/{device_admin,data_extraction_rules}.xml  [DONE]
│       ├── res/drawable/ + res/mipmap-anydpi-v26/  [DONE — vector adaptive icon]
│       └── java/com/gltech/guardianwatch/
│           ├── GuardianWatchApp.kt                      [DONE]
│           ├── MainActivity.kt                          [DONE — bind service + kiosk start]
│           ├── ui/theme/{Color,Type,Theme}.kt           [DONE — 35 tokens from oklch CSS]
│           ├── ui/components/*.kt                       [DONE — Shell, VitalTile,
│           │                                             BiometricChart, CriticalComponents]
│           ├── ui/screens/*.kt                          [DONE — ModeSelector,
│           │                                             MedicDashboard, ModeScreens]
│           ├── casualty/Casualty.kt                     [DONE]
│           ├── mode/AppMode.kt                          [DONE — 3-mode DataStore]
│           ├── anomaly/AnomalyDetector.kt               [DONE — HR+accel heuristics]
│           ├── ble/GuardianWatchProfile.kt              [DONE — UUIDs + parser]
│           ├── ble/WatchBleManager.kt                   [DONE — Nordic BleManager]
│           ├── ble/VitalsRepository.kt                  [DONE]
│           ├── ble/BleService.kt                        [DONE — foreground service]
│           └── kiosk/{KioskAdminReceiver,KioskController}.kt  [DONE]
```

**Theme is immutable.** Never hardcode a color or size. Always reference `GwColors`,
`GwSpacing`, `GwTypography`, `GwRadii`, `GwMotion`. They're generated from the design
system's `colors_and_type.css` (oklch → sRGB conversion already done).

## 4. The build→verify loop (run this constantly)

You must be able to compile and sideload an APK without human intervention. If any of
these commands fails, **your first priority is to fix them before doing any feature work.**

```bash
# 1. Generate wrapper once (if missing)
gradle wrapper --gradle-version 8.10.2

# 2. Build. MUST pass cleanly.
./gradlew assembleDebug

# 3. Install on connected Tab A9+ (USB debugging on)
adb devices    # expect exactly one Samsung SM-X216B line
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. Launch
adb shell am start -n com.gltech.guardianwatch.debug/com.gltech.guardianwatch.MainActivity

# 5. Watch logcat for our tag
adb logcat -s WatchBleManager:I BleService:I KioskController:I GuardianWatch:I *:W
```

If `assembleDebug` fails: read the error, fix the specific file, rebuild. Do **not** change
library versions in `libs.versions.toml` without first searching for the error exactly —
versions there are verified current as of April 2026.

## 5. Task list — ordered, each with Definition-of-Done

### TASK A — Green build [est. 30–60 min]
**DoD:** `./gradlew assembleDebug` prints `BUILD SUCCESSFUL` on a clean clone.

Known missing pieces that may break the first build:
- Gradle wrapper files (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`,
  `gradle-wrapper.jar`). Generate via `gradle wrapper --gradle-version 8.10.2`.
- `proguard-rules.pro` in `app/`. Create empty file with a one-line comment.
- `local.properties` with `sdk.dir=...`. DO NOT commit this file — add to `.gitignore`
  if missing.
- Nordic BLE library's `ble-common` may need a Nordic Maven repo entry. If Maven Central
  404s, add in `settings.gradle.kts`:
  ```kotlin
  maven { url = uri("https://maven.google.com") }
  ```
  before Nordic's `mavenCentral()`. Nordic's Android-BLE-Library v2.11 is currently on
  Maven Central, so this should not be needed — but verify.

### TASK B — First launch on Tab A9+ [est. 30 min]
**DoD:** APK installs, launches, displays `ModeSelectorScreen` with three mode cards.
Tapping **Medic Dashboard** transitions to the dashboard with one demo casualty (CAS-0147).

Known first-launch issues to preemptively check:
- Runtime permissions prompt. On Tab A9+ (Android 14), you need `BLUETOOTH_SCAN` +
  `BLUETOOTH_CONNECT` + `POST_NOTIFICATIONS`. Handled in `MainActivity.requestRuntimePermissions()`.
- Foreground service type `connectedDevice`. Android 14+ requires this declared AND the
  permission `FOREGROUND_SERVICE_CONNECTED_DEVICE`. Both declared in manifest.
- System bars. We use immersive fullscreen in `MainActivity.onCreate`. On One UI there's
  a Samsung Edge Panel that may intrude — if it does, disable via Settings → Display →
  Edge panels → off. DO NOT try to disable it programmatically without DPC — that's Knox.

### TASK C — Real BLE from Garmin Instinct 2 [est. 4–8 hours]
**DoD:**
- Flash the existing Guardian Watch Connect IQ `.prg` to the Instinct 2 (separate repo
  at `https://github.com/GilDowSamSonMAV/Guardian-watch.git`).
- Watch advertises service `f1ac0000-4743-4c54-4543-48475700754d`.
- Tablet scans, connects, receives telemetry notifications.
- `VitalTile` for HR updates live (watch the logcat filter `WatchBleManager:I`).
- `BiometricChart` draws a moving trace (last 10 min window).
- `AnomalyDetector` fires `HEMORRHAGE_SUSPECTED` when HR > 140 bpm sustained while
  movement class = STILL. `CriticalBanner` appears.

Known BLE gotchas on Samsung:
- Samsung devices aggressively cache GATT service discovery. If you change the watch's
  GATT structure and reconnect, the tablet may see stale services. Fix: toggle Bluetooth
  off→on, or `adb shell dumpsys bluetooth_manager | grep -i gatt` to verify.
- The `GATT 133` error is the bane of Android BLE. Nordic's `WatchBleManager` already
  handles it with `.retry(3, 100)`. If you see it persistently, it's usually: watch
  already connected to another phone, or the watch's advertisement interval is too long
  for Samsung's scan window. Lower advertisement interval to 500ms on the watch side.
- CCCDs must be enabled explicitly (Nordic lib does this in `initialize()`). If
  notifications arrive silently dropped, check `setNotificationCallback()` was called
  before `enableNotifications()`.

### TASK D — Kiosk mode on reset tablet [est. 2 hours]
**DoD:** After `adb shell dpm set-device-owner ...`, the tablet:
- Boots directly into Guardian Watch (HOME intent wins).
- Back button + home button stay in the app.
- Recents is hidden.
- Notifications from other apps are suppressed.
- `adb shell pm list packages -d` shows NO disabled packages unexpectedly.

Prerequisites:
1. Factory-reset the Tab A9+. Skip Google account. Skip Samsung account. **This is
   non-negotiable** — `set-device-owner` silently fails if any account exists.
2. Enable USB debugging via Developer Options (tap Build Number 7× in Settings → About).
3. `adb shell dpm set-device-owner com.gltech.guardianwatch.debug/com.gltech.guardianwatch.kiosk.KioskAdminReceiver`
   — must return `Success:` on stdout.
4. Relaunch app. `KioskController.startLockTask()` will succeed silently if device owner.
5. Verify via `adb shell dumpsys device_policy | grep -i device-owner`.

To **exit** kiosk for debugging:
```bash
adb shell dpm remove-active-admin com.gltech.guardianwatch.debug/com.gltech.guardianwatch.kiosk.KioskAdminReceiver
```

### TASK E — Samsung Knox alternative path (optional, for fleet) [est. 2 hours]
If user has > 1 Tab A9+ to deploy, set up Knox Configure (free ≤ 30 devices):
1. Sign up at https://seap.samsungknox.com
2. Create a Knox Configure profile: kiosk mode, allowlist our APK, brand customization.
3. Generate QR code.
4. Factory-reset tablet → scan QR from setup wizard → auto-installs + kiosks.
This eliminates the ADB dance for Day-3 field deployment.

### TASK F — Demo polish [est. 2–4 hours]
- Seed two or three demo casualties with different triage colors so the dashboard isn't
  a single row. (Currently `MainActivity.maybeSeedDemoCasualty` seeds only CAS-0147.)
- Wire the `ConfirmBar` "Confirm Handoff" tap to actually append an `AuditEntry` to the
  casualty's chain (currently a TODO). Hash scheme: SHA-256 of prior state + event.
- Bundle IBM Plex Sans + IBM Plex Mono + Heebo fonts into `res/font/` and uncomment
  the font family declarations in `Type.kt`. Current fallback to `FontFamily.SansSerif`
  renders wrong weight/metrics for the mono numerals — it's visible immediately.

## 6. Architectural constraints — do not violate

1. **No ViewModels** in MVP. Repository state → `collectAsState` directly in Composables.
   Add ViewModels only if a screen grows state that doesn't belong in repository.
2. **All BLE goes through `BleService`.** Never open a `BluetoothGatt` outside it.
3. **Theme lock:** never hardcode `Color(0x...)` or literal dp values outside of
   `ui/theme/`. Reference `GwColors` / `GwSpacing` / etc.
4. **Never surface nonexistent sensor data.** Instinct 2 has no SpO2 or skin temp.
   The VitalTile for these MUST show `"--"` / `"sensor: unavailable"`. Fabricating
   synthetic data is a hard fail.
5. **Dark theme only.** No dynamic color. No light mode. Military UI policy.
6. **Kiosk-first thinking.** Every destructive action needs a ConfirmBar. No toasts
   that disappear — a medic's hand is on the patient.

## 7. Libraries you may add (verified versions, April 2026)

| Need | Library | Version |
|---|---|---|
| Charts (if Canvas isn't enough) | `com.patrykandpatrick.vico:compose-m3` | 2.0.2 |
| Dates/times | `kotlinx-datetime` | 0.6.1 |
| Structured logging | `io.github.oshai:kotlin-logging-jvm` | 7.0.0 |

Do NOT add: RxJava (we use coroutines), Retrofit (no REST in MVP), Room (no DB in MVP;
audit log fits in a flat file).

## 8. When you are blocked

If you hit a wall for more than 30 minutes:
1. Write what you tried + the exact error to `docs/blockers/YYYY-MM-DD-<slug>.md`.
2. Keep going on another task. Come back after 2 hours.
3. If still blocked, ask the human (Gil) — do not fabricate a fix that hides the error.

## 9. Files you will touch most

- `app/src/main/java/.../ble/WatchBleManager.kt` — BLE quirks live here
- `app/src/main/java/.../ui/screens/MedicDashboardScreen.kt` — main UI
- `app/src/main/java/.../anomaly/AnomalyDetector.kt` — threshold tuning from field data

## 10. Acceptance — the demo that decides MVP is done

Run these in order on a real Tab A9+ with a real Instinct 2:

1. `./gradlew assembleDebug` → BUILD SUCCESSFUL in < 90s.
2. `adb install -r ...` → Success.
3. App launches into ModeSelectorScreen.
4. Pick Medic Dashboard. CAS-0147 placeholder shows.
5. Turn on Instinct 2 with Guardian Watch app running.
6. Within 15 seconds, CAS-0147's HR number starts updating from the watch.
7. Run in place to elevate HR. BiometricChart trace rises on screen.
8. Simulate HR > 140 held for 30s (or hard-set threshold lower in `AnomalyThresholds`
   for demo). CriticalBanner fires.
9. Tap ACKNOWLEDGE → ConfirmBar appears → Confirm → chain entry appended.
10. Kill the app. Reopen. CAS-0147 is still there (state persists).

**If all 10 pass, the MVP is demo-ready.**
