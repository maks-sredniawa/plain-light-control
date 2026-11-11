# Phase 0 — the lamp is known, and the template is cut down to the app it controls

Sequence lives in [`PLAN.md`](PLAN.md); the live worklist lives in [`DOD.md`](DOD.md). This phase
reopens ADR-0001 (Room), ADR-0002 (photos), ADR-0003 (reminders), ADR-0005 (backup) and, whatever the
lamp turns out to be, ADR-0007's *`INTERNET` stays out*. [`CONTEXT.md`](../CONTEXT.md) gets its first
domain terms: **Light**, **Simple mode**, **Dashboard**.

**What closing this phase means.** Today the repo is the android-starter template with a placeholder
*Item* domain, and nobody has written down which lamp the app is for. After this phase the lamp and
the way the phone reaches it are named and proven by one reading, the modules the app does not need
are gone, and the palette and mark are chosen. Asked of a device: the debug build opens on an empty
*Lights* screen, and a throwaway command has switched the real lamp on and off from a computer on the
same network (or over Bluetooth) without the vendor's app.

---

## 0. What this phase inherits

- **The first user's phone is a Motorola** (owner, 2026-11-11): near-stock Android, so the app is
  Android-specific rather than vendor-ROM-specific. The Xiaomi notes in `CLAUDE.md` describe the
  developer's test phone, not hers; the emulators (stock Android) are a fair stand-in for the
  Motorola, which lowers the case for any real-phone reading. The phone is about two years old
  (owner, 2026-11-11), so it shipped with Android 13 or later; the exact version is still unread.
- **Polish ships from the first build** (owner, 2026-11-11), beside English. The template already
  carries it: `values-pl` complete, `pl` in `locales_config.xml` and the in-app switcher (ADR-0004),
  and a Polish section in `store-listing.md`. Every Phase's copy is translated before it merges,
  which `scripts/translation-gate.py` enforces; nothing extra to build.
- **The template forbids `INTERNET`** (ADR-0007), on the grounds that an offline app needs none. Any
  Wi-Fi lamp would have needed it; the verdict (Bluetooth) keeps the ban.
- **`applicationId` is `com.maksforge.plainlight`**, written by bootstrap and following the MaksForge
  rule. Nothing reached Play yet, so it can still change; it freezes the day the Play entry exists.

## What is in, and what is deliberately not

**In:** naming the lamp and its control path (the gate), the module strip it implies, `minSdk`, the
palette and the mark, and the vocabulary.

**Not in, and each with a reason rather than a phase:**

- Any protocol code in `main/`. The gate's apparatus is a throwaway script outside the app; the first
  real client belongs to Phase 1, whose whole point is that one mechanism.
- The pipeline (repository settings, keystore, Play entry). It does not depend on the lamp, but it is
  its own phase (`PLAN.md` rule 2): a day of Console work against a day of code.

---

## Checkpoints

| | Checkpoint | Merges | Depends on |
| --- | --- | --- | --- |
| **A** | **The gate** — which lamp, and can the phone reach it without the vendor's app and account? | `docs:` the verdict here | — |
| **B** | **The strip** — delete the modules section 2 names, amend their ADRs ✅ | `refactor:` | owner's yes on section 2 |
| **C** | **`minSdk`** and the permission table for the verdict | `build:` | A, her Android version |
| **D** | **Palette and mark** | `feat:` | — |
| **E** | **The placeholder domain becomes an empty *Lights* screen** ✅ (done with B) | `feat:` | B |

A goes first because a cloud-only lamp vetoes the app as pitched (verdict 3). B waits only on the
owner, not on A: none of the four modules it removes is needed by any verdict.

---

## 1. The gate: which lamp, and how the phone reaches it

### The question, in the form that can be measured

*Brand, model and vendor app of the lamp she owns; and does one command from a computer on her network
(or a BLE write from a laptop) switch it on, off, and to 30 % brightness, with the vendor's app closed
and no vendor account in the loop after setup?*

### The apparatus

A script in the scratchpad, never committed, speaking the candidate protocol: `python-miio` /
Yeelight LAN mode for Xiaomi, `tinytuya` for Tuya, `bleak` for a Bluetooth lamp.

### The verdicts, decided now rather than argued about later

1. **A local Wi-Fi API answers** (Yeelight LAN mode, miIO with a token, Tuya local with its local
   key, WiZ UDP, Shelly HTTP). → The app talks to the lamp over the LAN. ADR-0007 is amended:
   `INTERNET` is declared, used for local sockets only, and the privacy policy says no data leaves the
   network. Getting a token or local key may need the vendor cloud **once**, at *Add a light*; how the
   user does that without reading Chinese becomes Phase 1's hardest screen, and is written up there.
2. **Only Bluetooth answers.** → BLE from the phone. ADR-0007 stands. `BLUETOOTH_SCAN`
   (`neverForLocation`) and `BLUETOOTH_CONNECT` on API 31+; on 26–30 scanning needs **location**, an
   ask that makes no sense to a non-technical user — which argues for `minSdk` 31 if her phone allows.
   The protocol may have to be read off the vendor app's traffic first; that cost is estimated before
   Phase 1 opens.
3. **Only the vendor's cloud answers**, with no local path. → The app as pitched (no account) cannot
   control this lamp. Stop and bring the choice to the owner: a different lamp the app supports, or an
   app that signs in to the vendor's cloud, which reopens *no account*.

### The verdict, taken 2026-11-11: 2, Bluetooth, in its one-way form

**The lamp** is a generic square 72 W LED ceiling lamp, 3000–6500 K, dimmable, sold with a remote
([Allegro offer 18931523331](https://allegro.pl/oferta/kwadratowa-lampa-sufitowa-led-3000-6500k-sciemniana-72-w-z-pilotem-czarny-18931523331)).
Its app is **智美灯控PRO** (*Zhi Mei Deng Kong PRO*), version 2.9, read off her phone's App info
screen. That screen shows **0 B of network data**, so the app never reaches a cloud: verdict 3 is
out, and with it any account.

**The protocol is BLE advertising, not a Bluetooth connection.** The phone does what the remote does:
it broadcasts a short advertisement carrying the command, and every lamp paired to that controller ID
acts on it. There is no connection, no scan and no answer. The encoding is public:
[`NicoIIT/ha-ble-adv`](https://github.com/NicoIIT/ha-ble-adv) (MIT) implements it as the `zhimei_v0`,
`zhimei_v1` and `zhimei_v2` codecs in `codecs/zhimei.py`, and its ESPHome sibling
[`NicoIIT/esphome-components`](https://github.com/NicoIIT/esphome-components) (MIT) lists
*ZhiMeiDengKong* among its apps. The light codecs put the payload in the advertisement's
16-bit service-UUID list (AD type `0x03`), which Android's `AdvertiseData` can emit; the vendor app
is itself an Android app doing exactly that, which is the existence proof.

What the verdict does to the rest:

- **Permissions:** `BLUETOOTH_ADVERTISE` on API 31+ (the *Nearby devices* ask), and `BLUETOOTH` with
  `BLUETOOTH_ADMIN` capped at `maxSdkVersion 30` below that. **No location on any version:**
  advertising never needed it, only scanning does, and the app never scans. Turning Bluetooth on
  from the app needs `BLUETOOTH_CONNECT` on 31+, so the app sends the user to Bluetooth settings
  instead of asking for a second permission.
- **ADR-0007 stands.** No `INTERNET`.
- **`minSdk` has no permission reason to move** (section 3).
- **The app cannot read anything back from the lamp.** The idea's *reads each light's details* is not
  possible for this lamp: what the app knows about a light is what the user told it (name, room) and
  how it reaches it (codec variant, controller ID). It also cannot know whether the lamp is on, since
  the remote and the Chinese app keep working beside it. So **Simple mode shows commands, not state**:
  an *On* button and an *Off* button, never a switch that claims to know.
- **Adding a light is pairing.** These lamps accept a pair command for a few seconds after they get
  power. The flow: switch the lamp off at the wall, on again, then tap *Pair*. Her existing remote and
  app pairings stay.
- **Which variant (`v0`, `v1`, `v2`) her lamp speaks is not known until a pair succeeds.** Phase 1's
  first checkpoint pairs each in turn.
- **Emulators cannot emit BLE**, so every reading of the light path is a real phone near the lamp:
  the exception the emulator rule allows. The rest of the app still tests on emulators.
- **The dashboard must be in Bluetooth range of the lamp**: this app on a second device near it, or a
  bridge such as an ESP32 running the ESPHome component. Phase 3 decides.
- **The codec is a port of MIT code**: the licence notice goes into the Licences screen, and a unit
  test pins the encoded bytes against the reference implementation's output for the same command.

## 2. The strip: what goes, decided against the app's shape

`optional-modules.md` lists the files; this is the decision. **Owner said yes on 2026-11-11** and the
strip is done: 59 files gone, the home screen is an empty *Lights* screen, and every gate passes
(`test lint assembleDebug`, translation, schema, `aab-permissions.py` on a local bundle).

⚠ **`app/src/androidTest/` is now empty, so CI's instrumented job fails on its zero-test guard.** It
stays red until Phase 1 brings the first real device test (the light-control path is the natural
one); deleting the job is the alternative, and the owner's call.

| Module | Proposal | Why |
| --- | --- | --- |
| Room and the schema gate | **Delete** | Her lights are a small list the user makes: JSON under one DataStore key, per *How to decide*. No querying, no relations. Room comes back at version 1 the day a feature needs a table. |
| Photos (media pipeline) | **Delete** | Nothing in the idea takes a picture. |
| Backup export/restore | **Delete**; Auto Backup takes over | The lights list is a setting and is welcome on her next phone. ⚠ A lamp's token or local key is a secret: decide in Phase 1 whether it is excluded and re-fetched, and judge any live state at the read. |
| Reminders and WorkManager | **Delete** | No schedules in the idea. If *lights on at 7* is ever wanted, it comes back with ADR-0003. |
| The tip | **Keep** (owner, 2026-11-11) | It costs Brazil's 14+ rating. Its ask now measures one light added and 30 days installed (ADR-0007 amendment). |
| Placeholder domain | **Replace** with an empty *Lights* screen (E) | |

## 3. `minSdk`

Stays 26 for now. The verdict needs no location ask on any version, so no permission argues for
raising it. What still might: every `SDK_INT` branch below the phones in the loop ships unvalidated,
and the Bluetooth permissions split at 31. Her two-year-old Motorola shipped with Android 13 or later,
so any `minSdk` up to 33 still reaches her. Decide before the first upload, with that trade-off. Write the reason beside `minSdk` in
`app/build.gradle.kts` the day it is decided, and move CI's floor legs with it.

## 4. Palette and mark

Design decisions go to a Fable subagent before options reach the owner (global rule). Not branded:
MaksForge colours only if the owner says so.

---

## Tests

None in this phase: nothing here computes a safety value. The strip is proven by
`./gradlew test lint`, `scripts/aab-permissions.py` against a local bundle, and the five places
*Around the code* lists.

## Documents this phase amends

ADR-0001, 0002, 0003, 0005 (each a dated amendment on the strip), ADR-0007 (under verdict 1),
`CONTEXT.md`, `CLAUDE.md`'s stack table and layout, the privacy policy with `play-app-content.md`.

## Known gaps, deliberately

- The Motorola itself is not read in this phase; the emulators stand in for stock Android.

## Readings

| # | Reading | Command | Result |
| --- | --- | --- | --- |
| R1 | The lamp switches on, off and to 30 % without the vendor app | moved to Phase 1's first checkpoint: a debug-build pair attempt per `zhimei` variant, on a phone near the lamp | **app identified as 智美灯控PRO 2.9, 0 B network data** (2026-11-11); the physical reading is Phase 1's |
| R2 | Her phone's Android version | Settings → About phone, read by her | **not known; about two years old, so 13+** (2026-11-11) |
