# Phase 2 — every light in one place, with her usual settings one tap away

Sequence lives in [`PLAN.md`](PLAN.md); the live worklist lives in [`DOD.md`](DOD.md). Builds on
[`phase-1.md`](phase-1.md) (the codec, the transmitter, the lights list, Simple mode). Owes
[`CONTEXT.md`](../CONTEXT.md) two terms: **Scene** and **Full controls**, and a sharper **Simple mode**.

**What closing this phase means.** Today each light has On, Off, two sliders and one remembered
setting. After this phase: a light can be renamed; it carries named **scenes** (three ready-made, and
any she saves) applied with one tap; it can be told to switch itself off in one or two hours; the
list has *All off*; and **Simple mode** is a real setting that strips the light's screen down to On,
Off and brightness. Asked of a device: the emulator walk at the end, since no lamp is in reach.

The door (closed testing) opens after this phase (`PLAN.md`). Nothing here adds a permission.

---

## What is in, and what is deliberately not

**In:** rename, scenes, the switch-off timer, *All off*, the Simple mode setting.

**Not in, and each with a reason rather than a phase:**

- **Rooms or groups.** One person, a handful of lights: *All off* covers the case that matters.
- **A timer in minutes.** The only timer the vendor app's captures show is whole hours (`0xA5`,
  `arg0` = hours); anything finer is a guess at the protocol.
- **Showing whether a light is on.** The lamp cannot say (phase 0).
- **Scenes shared across lights.** A scene belongs to the light it was saved on; a lamp in the
  bedroom and one in the kitchen want different *Evening*s.

---

## Checkpoints

| | Checkpoint | Merges | Depends on |
| --- | --- | --- | --- |
| **A** | **Scenes in the stored light** — the field, its defaults for lights stored by 0.2.0 | `feat:` | — |
| **B** | **Scenes on the light's screen** — apply, save current as a scene, delete | `feat:` | A |
| **C** | **Rename** | `feat:` | — |
| **D** | **Timer** — *Off in 1 hour*, *Off in 2 hours* | `feat:` | — |
| **E** | **All off** | `feat:` | — |
| **F** | **The Simple mode setting** | `feat:` | B, D |

## 1. Scenes

```kotlin
@Serializable data class Scene(val id: String, val name: String? = null, val preset: ScenePreset? = null,
                               val setting: LightSetting)
@Serializable enum class ScenePreset { EVENING, READING, NIGHT_LIGHT }
```

A preset scene has no `name`: its label is a string resource, so it follows the app's language. A
scene she saves has a `name` and no `preset`. `Light.scenes` defaults to the three presets, so every
light stored by 0.2.0 reads them on upgrade with no migration (`LightsJsonTest` gains the case).

| Preset | Brightness | Coldness |
| --- | --- | --- |
| Evening | 0.30 | 0.10 |
| Reading | 1.00 | 0.50 |
| Night light | 0.05 | 0.00 |

Coldness assumes 0 is warm; R3 in `phase-1.md` is still owed, and if it reads backwards the codec
flips, not these numbers.

Applying a scene sends On, then colour, then brightness, like *On*, and records the setting as last
sent. *Save as a scene* asks for a name and stores the sliders' current values. Deleting asks first;
a preset can be deleted too, and does not come back by itself.

## 2. Rename

From the light's screen, a pencil in the top bar opens a dialog with the name. Blank is refused.

## 3. The timer

Two buttons on the light's screen, *Off in 1 hour* and *Off in 2 hours*, sending `0xA5` with
`arg0` = hours. The lamp keeps the time, so the app holds no alarm and needs no scheduling
permission. What the app says afterwards is that it **asked** the light: it cannot read back that
the timer is running.

## 4. All off

An action in the Lights top bar when there are two or more lights. It sends Off to each in turn and
reports the first problem, if any, in the usual banner.

## 5. The Simple mode setting

A switch in Settings, **off by default**: the app opens with Full controls, and whoever sets it up
for someone non-technical turns Simple mode on. Stored by name in `AppPreferences`, listed in `Keys.SETTINGS`.

| | Simple mode on | Simple mode off (*Full controls*) |
| --- | --- | --- |
| Lights list | On / Off per light, *All off* | the same |
| A light's screen | On, Off, Brightness | + Colour, Scenes, Timer, *Remember this setting*, Rename, Remove |

*Add a light* is the same in both. Remove and Rename sit under Full controls so that nothing in Simple
mode can lose a light.

---

## Tests

- `LightsJsonTest`: a light stored without `scenes` reads the three presets; a saved scene round-trips.
- `LightCodecTest`: the timer frame for 2 hours matches the captured vector (`0xA5`, args `[2,0,0]`).
- `LightViewModelTest`: applying a scene sends On, colour, brightness in that order and records it.

## Documents this phase amends

`CONTEXT.md` (Scene, Full controls), `store-listing.md` (0.3.0 notes, the feature list), nothing in
the privacy documents: no new data leaves the phone and no permission is added.

## Readings

Emulator only (owner, 2026-11-11). Each row: `pixel-api36`, which can advertise.

| # | Reading | Command | Result |
| --- | --- | --- | --- |
| R1 | A 0.2.0 light gets the three presets after upgrading | install 0.2.0 build, add a light, install this build over it | **pass**: the light, its remembered 30 % and the three presets all there (pixel-api36, 2026-11-11) |
| R2 | Simple mode hides everything but On, Off, Brightness | Settings, then a light | **pass**, on by default; *Restore defaults* turns it back on (pixel-api36, urgent-api26); read when it was on by default, which is now off |
| R3 | Scene apply, save, delete; rename; timer; All off | Full controls | **pass** (pixel-api36); sent, not heard: no lamp |
| R4 | Polish copy fits on a small screen | pixel-api36 at `wm size 720x1280`, density 320 (small-api30 cannot add a light) | **pass** after one fix: the timer buttons wrap to two lines and were left-aligned; now centred |
