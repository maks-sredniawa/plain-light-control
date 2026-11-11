# Phase 1 — one light, switched on and off and dimmed from the phone

Sequence lives in [`PLAN.md`](PLAN.md); the live worklist lives in [`DOD.md`](DOD.md). This phase
rests on [`phase-0.md`](phase-0.md)'s verdict (BLE advertising, one-way) and keeps ADR-0007's ban on
`INTERNET`. [`CONTEXT.md`](../CONTEXT.md) already has **Light**, **Simple mode** and **Pair**.

**What closing this phase means.** Today the app opens on an empty *Lights* screen. After this phase,
someone standing by her lamp adds it in the app, and then switches it on, off, dims it and changes
its colour from the phone without the Chinese app; *On* brings back the setting she remembered.
Asked of a device: the readings below, taken beside the real lamp.

The code was written before the lamp was in reach (owner, 2026-11-11: "assume you are able to
connect"), so the gate below is taken **after** the code, against the usual order. What makes that a
fair bet: the codec reproduces 30 advertisements captured from the vendor app byte for byte, and the
vendor app is itself an Android app emitting the same bytes.

---

## Checkpoints

| | Checkpoint | Merges | Depends on |
| --- | --- | --- | --- |
| **A** | **The codec** — `ble/LightCodec.kt`, a port of `zhimei_v1`/`zhimei_v2` from NicoIIT/ha-ble-adv (MIT, 7b40e85), pinned by `LightCodecTest` | `feat:` | phase 0 |
| **B** | **Sending** — `BleLightTransmitter`: one legacy, connectable advertisement per command (connectable so Android keeps the flags structure, R1) for 750 ms (ha-ble-adv's default duration), serialised by a lock; a start the stack never answers fails after 3 s (found on the small-api30 emulator, which hung on *Pairing…*) | `feat:` | A |
| **C** | **The lights list** — JSON under one DataStore key (`AppPreferences.lights`), kept by *Restore defaults* | `feat:` | — |
| **D** | **Add a light** — pair, blink, ask | `feat:` | B, C |
| **E** | **Simple mode** — On, Off, brightness, colour, *Remember this setting*, remove | `feat:` | B, C |
| **F** | **The gate, beside the lamp** — readings R1–R6 | `docs:` | A–E |

## 1. Add a light

The lamp cannot answer, so the app cannot know which variant it speaks or whether pairing worked.
The flow makes the person beside it the sensor:

1. Name the light (optional; *Lamp* / *Lampa* if left blank).
2. Switch it off at the wall, wait 5 seconds, switch it on, then tap **Pair** within 5 seconds.
3. The app sends *Pair*, then *Off*, waits 1.5 s, then *On*, in the current variant (`ZHIMEI_V2`
   first), and asks: *Did the light switch off and back on?*
4. **Yes** saves the light with that variant and the controller ID. **No** moves to the next variant
   and asks for the wall-switch step again. After the last, it says the light did not answer.

The controller ID is a random 16-bit number made once per *Add a light*, so every variant tried uses
the same one. Her remote and the Chinese app stay paired; the lamp holds several controllers.

The *Nearby devices* ask (API 31+) is made on the **Pair** tap, the moment it is needed
(`PLAN.md` rule 4). A denial shows the permission banner with *Open settings*. A grant does not pair
there and then: the dialog has outlasted the lamp's 5-second window, so the wall-switch step is asked
for again before *Pair*.

## 2. Simple mode

- **On** sends On, then the colour, then the brightness: the favourite once *Remember this setting*
  has been tapped, otherwise the last ones sent, stored with the light because the lamp itself resets
  to full brightness when switched on at the wall (owner, 2026-11-11). Brightness goes last
  because some lamps reset it on a colour change (ha-ble-adv's `br_after_ct`).
- **Off** sends Off.
- **Brightness** and **Colour** sliders send their value when the finger lifts. A colour change is
  followed by the brightness for the same reason. Brightness bottoms out at 5 %, so the slider cannot
  be mistaken for Off.
- **Remember this setting** stores the sliders' values as the light's favourite.
- The sliders show what the app last sent, never the lamp's state: the remote can change it unseen.
- The list screen has On and Off per light, so the everyday use never opens a second screen.

## 3. What a send can fail with, and what the screen says

| `SendResult` | Banner | Action |
| --- | --- | --- |
| `PermissionMissing` | *Plain Light can't reach your lights* | *Open settings* (app details) |
| `BluetoothOff` | *Bluetooth is off* | *Bluetooth settings*; turning it on from the app would need `BLUETOOTH_CONNECT` too |
| `AdvertisingUnsupported` | *This phone can't control the lights* | *Close* |
| `Failed(code)` | *The light did not get that* | *Close* |

`Sent` means the advertisement went out, not that the lamp heard it. Nothing can say that.

---

## Tests

- `LightCodecTest` — the 30 captured vectors, byte for byte; the command-to-code map; every payload
  fits one service-UUID structure within the 31-byte legacy limit. A wrong byte here fails silently
  on the lamp, which is why it is the one test this phase owes.
- `LightsJsonTest` — the stored list's tolerance: one bad entry dropped, unknown fields ignored, a
  later field defaulted.

## Documents this phase amends

The privacy policy and `play-app-content.md` (Bluetooth: advertise only, no data), the permission
gate's EXPECTED rows and its first `uses-feature` (`bluetooth_le`, required: the app does nothing
without it), and the Licences screen, which now lists the ported code under MIT
(`app/licences-vendored.json`, merged into Licensee's report by `BundleLicences`).

## Known gaps, deliberately

- **The light path has no device test.** An emulator can sometimes advertise (API 36 did, API 26 and
  30 did not), but no lamp hears it, so only R1–R6 prove it. CI's instrumented job runs
  `LightsStoreTest` instead: the lights list survives its store being reopened.
- **Android writes its own flags byte**; the vendor app's captures show `0x19` and `0x1A`, so the lamp
  is not reading it. R1 confirms.
- **Index 4** is copied from the vendor app's own commands; ha-ble-adv's default differs. R1 confirms
  a pairing made with it holds.

## Readings

Taken beside the lamp with a debug build. **Only R1 has been read.** On 2026-11-11 the owner
chose to test on emulators only that day and to assume the real lamp behaves; each row below says
which. An assumed row is still owed a reading the first time the app meets the lamp, and R3 decides
whether the colour slider is backwards.

| # | Reading | Command | Result |
| --- | --- | --- | --- |
| R1 | Which variant pairs, and the blink happens | *Add a light* on the phone | **`ZHIMEI_V1` pairs and blinks**, on a Redmi Note 14 Pro+ 5G, once the advertisement is connectable; non-connectable, Android drops the flags structure and neither variant pairs (owner, 2026-11-11) |
| R2 | On, Off, and brightness at 5 %, 50 %, 100 % | Simple mode | **assumed by the owner, not read** (2026-11-11) |
| R3 | Which end of the colour slider is warm | Simple mode, slider to each end | **assumed by the owner, not read** (2026-11-11) |
| R4 | The pairing window is about 5 seconds, as the copy says | *Add a light*, tap at 3 s and at 10 s | **assumed by the owner, not read** (2026-11-11) |
| R5 | The remote still works after the app pairs | the remote | **assumed by the owner, not read** (2026-11-11) |
| R6 | A send with Bluetooth off, and with the permission denied, shows the banner | Simple mode | **read on the pixel-api36 emulator**: both banners show (QA, 2026-11-11) |
