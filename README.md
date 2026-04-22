# Guardian Watch — Tablet App

Tactical health-monitoring tablet, Android Kotlin + Jetpack Compose.
Target device: **Samsung Galaxy Tab A9+ 5G (SM-X216B)**.

## What it does

Three user-selectable modes (set on first launch, persisted in DataStore):

| Mode | Who | BLE topology |
|---|---|---|
| **Medic Dashboard** | Role-1/2 medic | 1..16 Garmin watches → this tablet |
| **Single Paired** | Buddy-aid | 1 watch → this tablet (offline) |
| **Relay Node** | Ruck/vehicle mount | N watches → this tablet → cellular upstream |

BLE input: Garmin Instinct 2 running the Guardian Watch Connect IQ app
(published on characteristic `f1ac0001-...`, 20-byte packed telemetry
schema — see `GuardianWatchProfile.kt`). Falls back to standard HR service
(180d/2a37) when the watch app isn't running, so nRF Connect emulation
works for demo.

On-device anomaly detection: `AnomalyDetector.kt` — HR + accel heuristics
only (Instinct 2 has no SpO2/skin-temp sensor). Targets ~85% hemorrhage
detection from HR magnitude + trend + movement class.

## Build & install

```bash
# One-time: generate gradle wrapper
gradle wrapper --gradle-version 8.10.2

# Build debug APK
./gradlew assembleDebug

# Sideload to connected Tab A9+ (USB debugging enabled)
adb install app/build/outputs/apk/debug/app-debug.apk

# Launch
adb shell am start -n com.gltech.guardianwatch.debug/com.gltech.guardianwatch.MainActivity
```

APK will be at `app/build/outputs/apk/debug/app-debug.apk` (~8 MB).

## Enabling Kiosk Mode (3-day plan, Day 3)

**Tab A9+ is a Samsung device, so you have two paths:**

### Path A — ADB Device-Owner (free, works today)
Requires factory-reset tablet with no Google/Samsung account signed in:

```bash
adb shell dpm set-device-owner com.gltech.guardianwatch.debug/com.gltech.guardianwatch.kiosk.KioskAdminReceiver
```

On next launch the app calls `startLockTask()` → tablet is pinned.
To exit: `adb shell dpm remove-active-admin ...`.

### Path B — Samsung Knox Configure (recommended for fleet)
Knox Configure is free for < 30 devices. Scan a QR code during tablet setup
and the app is installed + pinned in kiosk mode in 30 seconds, no factory
reset needed for devices not yet enrolled. See `DEPLOYMENT.md`.

## 3-Day MVP Plan

### Day 1 — Scaffold & local run
- [x] Project scaffold, theme, BLE stack, mode selector
- [x] All 3 screens composable
- [ ] `gradle wrapper` + `./gradlew assembleDebug` passes
- [ ] APK installs on Tab A9+
- [ ] App launches, shows ModeSelectorScreen, lets you pick Medic Dashboard
- [ ] Demo casualty CAS-0147 appears in the list

### Day 2 — Real BLE from Instinct 2
- [ ] Flash the Guardian Watch Connect IQ `.prg` to your Instinct 2
- [ ] Instinct 2 advertises `f1ac0000-...` service
- [ ] Tablet scans, pairs, receives telemetry frames
- [ ] HR numbers update live in VitalTile
- [ ] BiometricChart draws a moving trace
- [ ] Anomaly engine fires `HEMORRHAGE_SUSPECTED` at HR > 140 while still
- [ ] CriticalBanner shows; Acknowledge → ConfirmBar → chain entry appended

### Day 3 — Field hardening
- [ ] Factory-reset Tab A9+, skip all accounts
- [ ] `adb shell dpm set-device-owner ...`
- [ ] Reboot — Guardian Watch becomes HOME launcher
- [ ] Verify: home button stays in app, back button stays in app,
      recents hidden, notifications suppressed
- [ ] Stress test: watch goes out of range + comes back → auto-reconnect
- [ ] Battery drain test: 8 hours continuous streaming → log result
- [ ] Demo dry-run with stakeholder

## Architecture notes for contributors

- **Single-instance BLE service** (`BleService`) owns all watch connections.
  `MainActivity` only binds + reads state. Service survives config changes.
- **VitalsRepository** is a `@Singleton` provided by Hilt. All composables
  read state from it via `collectAsState`.
- **No ViewModels** in the MVP — repository state is directly consumed.
  Add ViewModels if the mode screens grow logic.
- **Theme is a hard lock** — never hardcode a color/size, always reference
  `GwColors` / `GwSpacing` / `GwTypography`. These are generated from
  `design_system/colors_and_type.css` (oklch → sRGB).

## Known gaps (for the follow-up agent)

See `HANDOFF.md`.
