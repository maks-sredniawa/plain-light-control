# Vocabulary

**The rule this file exists to enforce: one concept, one word, everywhere.** The entity, the DAO, the
route key, the string resource and the sentence in the UI all use it. When the domain word and the
user-facing word genuinely differ, both are written here with the difference stated.

## The domain

| Term | Means | Not |
| --- | --- | --- |
| **Light** | one lamp, bulb or plug the app controls, with its settings kept per light. The user-facing word and the code word | device, bulb, lamp |
| **Simple mode** | the setting, on by default, that leaves a light's screen with only On, Off and brightness | basic mode, easy mode |
| **Full controls** | Simple mode switched off: colour, scenes, the timer, rename and remove | advanced mode, expert mode |
| **Scene** | a named brightness and colour for one light, applied with one tap; three come ready-made | preset (in UI copy), mood, favourite |
| **Dashboard** | the separate personal dashboard a light can also be added to. How it reaches the light is still open (`docs/phase-0.md`) | home, favourites |
| **Tip** | a one-off payment to the developer through Google Play that **unlocks nothing** (ADR-0007). The home screen mentions it once per install; Help and feedback offers it for good | donation, support the project, premium, supporter |

## Words we deliberately avoid

- **"Device"** means the phone, never a light. The idea's "settings kept per device" is *per light*
  here.
- **"Pair"** is the step inside *Add a light* where the lamp learns this app's controller ID. The lamp
  is never *connected*: it only listens (`docs/phase-0.md`).
- **"Status", "is on"**: the app cannot know a light's state, so no copy claims it.

## Naming conventions in code

Some names in this codebase are deliberately generic — `AppDatabase`, `AppTheme`, `AppPreferences`,
`AppContainer`, `MainApplication`, `MainActivity`. They stay correct whatever the app is called, so
renaming the app is a package move rather than a repo-wide find-and-replace. Don't rename them to
match the product; name the *domain* types after the domain instead.
