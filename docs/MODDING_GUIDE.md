# Songs of Syx Modding Guide (V71.44)

Written from an analysis of the [songs-of-syx-mod-example](https://github.com/4rg0n/songs-of-syx-mod-example)
template **and** checked against the game source shipped with this install (`info/SongsOfSyx-sources.jar`, V71.44).
Where the template docs and the actual game code disagree, this guide follows the code and says so.

---

## 1. The big picture

There are two kinds of Songs of Syx mods, and you can mix them:

| Kind | What you write | Needs Java? | Good for |
|---|---|---|---|
| **Data mod** | `.txt` config files, `.png` sprites, `.ogg`/`.wav` audio | No | New races, rooms, resources, techs, balance tweaks, new buildings that reuse existing mechanics |
| **Code mod** ("script") | Java code compiled into a `.jar` | Yes | New mechanics, new UI, behaviour the configs can't express |

**Start with data mods.** A huge amount of the game (races, rooms, resources, religions, techs, events,
diseases, animals) is defined in plain text files. You only need Java when you want to change *behaviour*.

The example repo is mainly a **build pipeline for code mods**: it compiles your Java, assembles the mod folder,
writes `_Info.txt`, and copies everything into the game's mod directory, so you never copy files by hand.

---

## 2. Your machine (verified)

| What | Path |
|---|---|
| Game install | `C:\Steam\steamapps\common\Songs of Syx` |
| Game jar (all game code, compiled) | `...\Songs of Syx\SongsOfSyx.jar` |
| Game source code (readable Java) | `...\Songs of Syx\info\SongsOfSyx-sources.jar` (it is a zip) |
| Vanilla data (configs, sprites, audio) | `...\Songs of Syx\base\data.zip` → `data/assets/...` (1800 files) |
| Official example scripts (by the dev) | `...\Songs of Syx\base\script\000_Tutorial.jar`, `001_Tutorial.jar` |
| Bundled Java runtime | `...\Songs of Syx\jre` (Temurin 21.0.11) |
| JVM options used by the launcher | `...\Songs of Syx\jvmargs-launcher.txt` (currently `-Xmx4096m` etc.) |
| **Local mods folder** (use this while developing) | `%APPDATA%\songsofsyx\mods` = `C:\Users\pattt\AppData\Roaming\songsofsyx\mods` (empty) |
| Steam Workshop mods | `C:\Steam\steamapps\workshop\content\1162750` (doesn't exist yet: nothing subscribed) |
| Game logs | `%APPDATA%\songsofsyx\logs` |
| Saves | `%APPDATA%\songsofsyx\saves` |

**Installed tools:** Java 21 JDK (Azul Zulu 21.0.6) on PATH, IntelliJ IDEA Community 2024.1.1, VS Code, Git.
**No global Maven**, but the template ships a Maven wrapper (`mvnw.cmd`) and IntelliJ bundles Maven, so you don't need to install it.

**Game version: 71.44**, which is exactly what the template's `pom.xml` targets (`game.version.major=71`, `minor=44`).

> ⚠️ The template assumes Steam lives in `C:\Program Files (x86)\Steam`. **Yours is `C:\Steam`.**
> You must change the paths (see [§6.4](#64-adapting-the-template-to-this-machine)).

---

## 3. Mod folder structure

```
Song Of Goul/                 ← mod folder name (anything you like)
├── _Info.txt                 ← REQUIRED, exact name & case
└── V71/                      ← game MAJOR version, capital V
    ├── assets/               ← mirrors data.zip/data/assets
    │   ├── init/             ← game definitions (race, room, resource, tech, ...)
    │   ├── text/             ← display text, names, dictionary, translations
    │   ├── sprite/           ← .png graphics
    │   └── audio/            ← sounds/music  (see note below)
    ├── script/               ← your compiled .jar file(s) go here
    ├── saves/                ← optional: ship save games
    ├── examples/             ← optional: example saves
    └── campaigns/            ← optional: campaigns
```

You can have several version folders (`V69/`, `V71/`) to support several game versions at once.

> **Note: `sound` vs `audio`.** The template's skeleton has `assets/sound/...`, but V71.44 reads
> audio from `assets/audio/{mono,music,ambience,config}` (see `init/paths/PATHS.java`, `PATHS_AUDIO`),
> and that's the folder `data.zip` uses. Use **`audio`**.

### `_Info.txt`

```
VERSION: "1.0.0",
GAME_VERSION_MAJOR: 71,
GAME_VERSION_MINOR: 44,
NAME: "Song Of Goul",
DESC: "What the mod does.",
AUTHOR: "Patrick",
INFO: "Extra info",
```

- Must be named exactly `_Info.txt`. Watch out for Windows hiding extensions (`_Info.txt.txt`).
- Optional `TEXTURE_CACHE_SIZE`: default **4096**; only **4096, 8192 or 16384** are accepted
  (power of two, checked in `init/paths/ModInfo.java`). Raise it only if you add a lot of sprites.
- "Unsupported mod" in the launcher usually means a bad `_Info.txt` or a wrongly named `V71` folder.

### What's in vanilla `data/assets/init/`

`animal, battle, config, disease, event, player, race, religion, resource, room, script, settlement, stats, tech, world`

Races in vanilla: `ARGONOSH, CANTOR, CRETONIAN, DONDORIAN, GARTHIMI, HUMAN, Q_AMEVIA, TILAPI`.

**Tip:** extract `data.zip` somewhere and open it in VS Code. It's the best documentation there is:
copy a vanilla file, change it, look at how other files do things.

---

## 4. The config file format

Configs look like JSON without quotes on keys, with trailing commas:

```
PLAYABLE: false,
PROPERTIES: {
	HEIGHT: 12,
	WIDTH: 13,
	SLEEPS: false,
},
PREFERRED: {
	FOOD: [ EGG, MEAT, FISH, ],
	ROAD: { *: 0.1, STONE1: 0.5, },
},
```

- `KEY: value,`, objects `{ }`, arrays `[ ]`, every entry ends with a comma.
- Files must be **UTF-8**.
- Unknown keys print `unknown key: ...` to the error log along with the list of valid keys.
  Handy for finding typos and discovering options.

### 4.1 How mod files combine with vanilla (verified in code)

When the game loads e.g. `init/race/CANTOR.txt`, it collects **every** `CANTOR.txt` from all active mods plus vanilla,
starts from vanilla, and layers the mod files on top (`snake2d/util/file/Json.java`, `JsonValue.java`).

| You want to… | Do this |
|---|---|
| **Change one value** | Create a same-named file with only that key. E.g. `V71/assets/init/race/CANTOR.txt` containing just `PLAYABLE: true,` makes Cantors playable. Everything else stays vanilla. |
| **Replace a whole section** | Write the top-level key in your file. By default a top-level key you define **replaces** vanilla's value completely (objects and arrays included). |
| **Merge into nested objects** instead of replacing | Add `_JSON_ADD: true,` to your file. Objects are then merged key by key, recursively. |
| **Append to arrays** instead of replacing | Add `_ARRAY_ADD: true,` to your file. Your array items are added after vanilla's. |
| **Add new content** | Use a new file name (e.g. `init/race/GOUL.txt`). |
| **Remove all vanilla files in a folder** | Put an empty `_IgnoreVanilla.txt` in that folder of your mod (e.g. only your races exist). |

> **About `__OVERWRITE: true,`**: the template docs say V71 requires it to fully replace a vanilla file.
> I couldn't find that key anywhere in the V71.44 source; the merge behaviour above is what the code does.
> `_JSON_ADD` and `_ARRAY_ADD` aren't mentioned in the template docs at all.
> If something behaves oddly, test it in-game. The source is the ground truth.

### 4.2 Boosters (`BOOST:` / `BONUS:` blocks)

```
BOOST: {
	PHYSICS_RESISTANCE_COLD>ADD: -0.15,
	PHYSICS_DEATH_AGE>MUL: 0.8,
	ROOM_FARM*>MUL: 1.1,          ← wildcard: every room starting with ROOM_FARM
},
```

Syntax: `{BOOSTER_KEY}>{ADD|MUL}: number,`. Full key list:
[template doc/config/boosters.md](https://github.com/4rg0n/songs-of-syx-mod-example/blob/main/doc/config/boosters.md).

### 4.3 Requirements (`REQUIRES:`)

```
REQUIRES: {
	GREATER: { WORKFORCE: 3500, },
},
```

Comparators: `GREATER`, `GREATERE` (>=), `LESS`, `EQUAL`, `NEQUAL` (`LESSE` isn't implemented).
Value list: [comparator_values_all.md](https://github.com/4rg0n/songs-of-syx-mod-example/blob/main/doc/res/comparator_values_all.md).

### 4.4 Rooms

The room file **prefix** picks the game code that runs it:

- `WORKSHOP_*`, `FARM_*`, `PASTURE_*`, `MINE_*`, `REFINER_*`, `MONUMENT_*`, `TEMPLE_*`, `SHRINE_*`, `POOL_*`, ...
  → you can add new ones via config only, e.g. `WORKSHOP_CROSSBOW.txt` produces crossbows using workshop logic.
- Files starting with `_` (`_HOME`, `_MILITARY`, `_DUMP`, ...) are **unique** rooms; no new variants without code.
- A brand-new room *type* (new prefix) needs Java: see the template's
  [make_custom_room.md](https://github.com/4rg0n/songs-of-syx-mod-example/blob/main/doc/howto/make_custom_room.md).

### 4.5 Config references in the template

Field-by-field references (required/default/min/max):
[race](https://github.com/4rg0n/songs-of-syx-mod-example/blob/main/doc/config/race.md) ·
[room](https://github.com/4rg0n/songs-of-syx-mod-example/blob/main/doc/config/room.md) ·
[resource](https://github.com/4rg0n/songs-of-syx-mod-example/blob/main/doc/config/resource.md) ·
[religion](https://github.com/4rg0n/songs-of-syx-mod-example/blob/main/doc/config/religion.md) ·
[disease](https://github.com/4rg0n/songs-of-syx-mod-example/blob/main/doc/config/disease.md) ·
[animal](https://github.com/4rg0n/songs-of-syx-mod-example/blob/main/doc/config/animal.md) ·
[race sprite sheet layout](https://github.com/4rg0n/songs-of-syx-mod-example/blob/main/doc/explanation/race_sprite_rundown.md) ·
[annotated example race](https://github.com/4rg0n/songs-of-syx-mod-example/blob/main/doc/res/race/_EXAMPLE.txt)

---

## 5. Code mods: how the game runs your Java

### 5.1 Loading (verified in `script/ScriptLoad.java`)

1. The game scans every active mod's `V71/script/*.jar`.
2. It adds the jars to the classpath and loads **every class**.
3. Each non-abstract class that implements `script.SCRIPT` and has a **public no-argument constructor**
   becomes a "script". It shows up in the **Scripts** list when you start a new game.
4. Your jar is on the *same* classpath as the game. That's why the "replace a game class" trick works (§5.5).

### 5.2 The two interfaces you implement

`script.SCRIPT` is the **factory / entry point** (template: `MainScript.java`):

| Method | When it's called | Notes |
|---|---|---|
| `name()`, `desc()` | Script list | Required |
| `initBeforeGameCreated()` | Starting or loading a game, before the game world exists | Hooks/reflection; register custom rooms + work AI here |
| `initBeforeGameInited()` | Game created, not finalized yet | `SETT.ROOMS()` etc. exist here |
| `createInstance()` | Leaving the main menu and on every load, after **all** resources and the `VIEW` (UI) exist | Required. Return your instance. **Inject UI elements here.** |
| `isSelectable()` | Default `true` | `false` = auto-inserted, not shown in the list |
| `forceInit()` | Default `false` | `true` = forces itself into all new games **and existing saves** (may break save/load) |

`SCRIPT.SCRIPT_INSTANCE` is your **running logic** (template: `InstanceScript.java`):

| Method | When |
|---|---|
| `update(double ds)` | Every game tick (~60/s); `ds` = in-game seconds since last tick. Main logic goes here. |
| `save(FilePutter f)` / `load(FileGetter f)` | Game saves/loads. Write/read your own variables, **in the same order**. |
| `render(Renderer r, float ds)` | After the game renders: draw extra stuff |
| `hover(COORDINATE mCoo, boolean moved)`, `hoverTimer(double t, GBox text)` | Mouse hover; `hoverTimer` lets you edit tooltips |
| `keyPush(KEYS key)`, `mouseClick(MButt button)` | Input |
| `handleBrokenSavedState()` | Your `load` failed. Return `true` if you can recover, otherwise the script is disabled. |

### 5.3 What exists when

- **Always/early** (`initBeforeGameCreated`): `CORE`, `PATHS`, `TERRAINS`, `UI` fonts/panels, `TECHS.INFO()`.
- **After game created**: `GAME`, `FACTIONS`, `TIME`, `SETT` (the settlement), `STATS`, `RACES`, `RESOURCES`,
  `BOOSTABLES`, `LAW`, `STANDINGS`, `SPRITES`, `TECHS.ALL()`.
- **Running game** (`createInstance`, `update`): `VIEW` (all UI), `GAME.texture()`.

Full lists: [template doc/README.md "Game and Mod Initialization"](https://github.com/4rg0n/songs-of-syx-mod-example/blob/main/doc/README.md#game-and-mod-initialization).

### 5.4 Logging and dev tools

```java
LOG.ln("message");        // snake2d.LOG: normal log line
LOG.err("message");       // error log line
GAME.Notify("message");   // game.GAME: in-game notification + stack trace
System.out.println(...);  // shows in console/IDE output
IDebugPanel.add("My command", () -> { ... });   // view.interrupter.IDebugPanel
```

Turn on **Developer mode** in the launcher **Settings** for in-game dev tools (upper right, above the minimap),
where your `IDebugPanel` commands appear. **Debug mode** adds extra info but pauses occasionally; only use it when needed.

### 5.5 Getting into the game's code

The game isn't built to be extended: many classes are package-private, few hooks exist. Ways in, best first:

1. **Public API**: UPPERCASE "entry" classes: `GAME`, `SETT`, `STATS`, `RACES`, `RESOURCES`, `FACTIONS`, `TIME`,
   `VIEW`, `BOOSTABLES`, `PATHS`, `SPRITES`, `UI`... Start every search here.
2. **Reflection**: read/write private fields and call private methods at runtime
   (`Field f = X.class.getDeclaredField("name"); f.setAccessible(true); f.get(obj)`).
   Always wrap it in try/catch so a game update breaks your feature, not the game.
3. **Same-package subclass**: put your class in the game's package (e.g. `package settlement.job;`)
   to see package-private things.
4. **Replace a game class**: copy the vanilla class to the same package + name in your mod and edit it.
   Most powerful, least compatible: it conflicts with other mods doing the same and must be redone after updates.
   Note: `static final` primitive constants get inlined into *other* classes at compile time,
   so changing them in a replaced class doesn't affect callers.

Workflow for finding *where* to hook: find a visible UI text near the thing → search the source for it →
"Find usages" (Alt+F7 in IntelliJ) until you reach the class that builds it. Full walk-through:
[modding_strategy.md](https://github.com/4rg0n/songs-of-syx-mod-example/blob/main/doc/howto/modding_strategy.md),
[add_ui_element.md](https://github.com/4rg0n/songs-of-syx-mod-example/blob/main/doc/howto/add_ui_element.md).

### 5.6 Game code conventions (things that will surprise you)

- **Engine** is custom ("Snake2D", on LWJGL 3). Code was written for Java 8, runs on Java 21.
- **Custom collections**: `snake2d.util.sets.ArrayList`, `LIST`, `KeyMap`, `LinkedList`... are *not* `java.util` ones.
  Streams etc. won't work on them directly; loop or convert.
- **`¤¤` fields** (e.g. `¤¤name`) are translatable UI strings injected from
  `data/assets/text/dictionary/Dic.txt` via `D.ts(SomeClass.class)`. They're great search targets.
- **Static init trap**: never put game resources in `static` fields (`static Font F = UI.FONT().H2;`).
  Static fields initialize before the game is ready, which gives you `NullPointerException`s or broken UI.
  Get them inside methods/constructors instead.
- **UI**: `GuiSection` is the layout container (like an HTML `<div>`): `add`, `addDown`, `addRight`, `addRightC`, `addRelBody`.
  Buttons: `util.gui.misc.GButt.Panel`. Interfaces: `RENDEROBJ` → `HOVERABLE` → `CLICKABLE`.
- `UIPanelTopSett.addExtraElement(...)` looks like a mod hook but **doesn't work in V71**. Use reflection instead.

### 5.7 Important packages

| Package | What |
|---|---|
| `game` | core game, `GAME`, `VERSION`, events, factions, time, boosting |
| `init` | loading of the `.txt` data (races, resources, techs, paths, sprites, text) |
| `settlement` | city building: rooms, entities, AI (`settlement.entity.humanoid.ai`), jobs, stats |
| `world` | world map |
| `view` | game UIs (`VIEW`) |
| `util` | GUI widgets and helpers |
| `snake2d` | engine: rendering, input, files, collections |
| `script` | mod script loading |
| `menu`, `launcher` | main menu, launcher |

---

## 6. The example template: what it does

### 6.1 Files that matter

| File | Purpose |
|---|---|
| `pom.xml` | Maven build config: mod name/author/version, game version, paths, build steps |
| `src/main/java/your/mod/MainScript.java` | `SCRIPT` entry point |
| `src/main/java/your/mod/InstanceScript.java` | `SCRIPT_INSTANCE` with every hook stubbed (prints to console) |
| `src/main/resources/mod-files/` | **Non-code mod content** (`assets/...`, `_Info.txt` template). Copied into `V71/` at build time. |
| `.run/*.run.xml` | IntelliJ run configs: `Main` (skip launcher), `MainLaunchLauncher` (with launcher), `DEBUG` (breakpoints) |
| `mvnw`, `mvnw.cmd` | Maven wrapper: downloads Maven 3.9.5 on first use |
| `doc/` | Their documentation (config references, how-tos) |

### 6.2 Maven in one paragraph

Maven is Java's equivalent of npm/NuGet + MSBuild. `pom.xml` lists dependencies and build steps.
The steps run in a fixed order (`validate → compile → test → package → install`); running one runs all earlier ones.

### 6.3 The commands

| Command | What it does here |
|---|---|
| `mvnw validate` | Registers your local `SongsOfSyx.jar` + sources jar as a Maven dependency (`com.songsofsyx:songsofsyx:71.44`) in `~/.m2`. **Run once**, and again after each game update. |
| `mvnw package` | Compiles, runs tests, builds `target/Song Of Goul.jar` (with Lombok/other deps shaded in, game excluded), assembles the full mod into `target/out/<mod name>/`, fills `_Info.txt` from `pom.xml`. |
| `mvnw install` | `package` + copies the mod into `%APPDATA%\songsofsyx\mods\<mod name>`. **This is your normal dev loop.** |
| `mvnw clean` | Deletes `target/` **and removes the mod from the mods folder**. |
| `mvnw install -P mods-uploader` | Also copies into the Steam Workshop uploader folder. |

In IntelliJ you don't type these: click the **m** (Maven) tab on the right and double-click the phase.

### 6.4 Adapting the template to this machine

1. In `pom.xml`, `windows` profile:
   ```xml
   <game.workshop.directory>C:/Steam/steamapps/workshop/content/1162750</game.workshop.directory>
   <game.install.directory>C:/Steam/steamapps/common/Songs of Syx</game.install.directory>
   ```
2. In `.run/*.run.xml`: replace `C:/Program Files (x86)/Steam/steamapps/common/Songs of Syx`
   (and the backslash version) with `C:/Steam/steamapps/common/Songs of Syx`.
   In `DEBUG.run.xml`, rename `Example Mod.jar` to your mod name.
3. Rename the mod: `groupId`/`artifactId`/`version` and `mod.name`, `mod.description`, `mod.author`, `mod.info` in `pom.xml`.
4. Rename the Java package `your.mod` to something of yours (e.g. `com.goul.songofgoul`) and update `PATTERN` in the run configs.
5. Data-only mod? You can delete `src/main/java/your` and `src/test/java/your`.

> The `mac` profile also activates on `unix`; irrelevant on Windows.

### 6.5 Running and debugging

- First time: run the game via the launcher (or the `MainLaunchLauncher` config) and **enable your mod**.
  After that, `Main`/`DEBUG` skip the launcher and use the enabled mods.
- The run configs automatically do `clean` + `install` before launching.
- `DEBUG` (green bug icon) lets you set breakpoints in your code and the game's code.
- Without an IDE: in the game folder run `.\jre\bin\java.exe -jar SongsOfSyx.jar` (skips the launcher, logs to the console).

---

## 7. Java survival kit (if you come from C#)

| C# | Java |
|---|---|
| `namespace X.Y` + folders optional | `package x.y;` **must** match folder `x/y/` |
| `using X;` | `import x.ClassName;` (per class, or `x.*`) |
| `class A : B, IFoo` | `class A extends B implements IFoo` |
| `public`/`internal`/`private` | `public` / *(no keyword = package-private)* / `private`; `protected` also allows the package |
| Properties `{ get; set; }` | Methods `getX()`/`setX()`; Lombok `@Getter`/`@Setter` generates them |
| `sealed` / `readonly` | `final` |
| `override` keyword | `@Override` annotation (optional but use it) |
| `string` | `String` (`CharSequence` = any text, used a lot by the game) |
| `List<T>` | `java.util.List<T>`, but the game uses `snake2d.util.sets.LIST<T>` |
| lambdas `() => x` | `() -> x` |
| NuGet + .csproj | Maven + `pom.xml` |
| DLL | JAR (a zip of `.class` files) |
| Default interface methods | `default` methods (that's why most `SCRIPT_INSTANCE` methods are optional) |
| Anonymous class: n/a | `new Butt(...) { @Override protected void clickA() {...} }`, used **everywhere** in the game UI |

**Lombok** (in the template) generates boilerplate from annotations: `@NoArgsConstructor`, `@Getter`, `@RequiredArgsConstructor`...
IntelliJ needs the Lombok plugin (bundled in recent versions).

Learning: [MOOC Java course](https://java-programming.mooc.fi/) · [Baeldung](https://www.baeldung.com/get-started-with-java-series).

---

## 8. Gotchas checklist

- [ ] `_Info.txt` exact name, not `_Info.txt.txt`
- [ ] Version folder is `V71` (capital V) and matches the game major version
- [ ] Config files are UTF-8; every entry ends with `,`
- [ ] Audio goes in `assets/audio`, not `assets/sound`
- [ ] After a game update: bump `game.version.*` in `pom.xml`, `mvnw clean validate`, reload Maven in IntelliJ
- [ ] "Cryptic compile errors" almost always = game version mismatch between `pom.xml` and install
- [ ] No game objects in `static` fields
- [ ] Wrap reflection in try/catch
- [ ] Save/load fields in the same order
- [ ] `mvnw clean` deletes the installed mod from `%APPDATA%\songsofsyx\mods`

## 9. Resources

- Template: https://github.com/4rg0n/songs-of-syx-mod-example (docs in `doc/`)
- Mod SDK (optional helper library by the template author): https://github.com/4rg0n/songs-of-syx-mod-more-options/tree/main/mod-sdk
- Songs of Syx Discord (modding channels): https://discord.com/eacfCuE
- Production graph / room planner: https://nobody0.github.io/syx-vis/
- Official (older) docs in the install: `info/MAKE_A_MOD.txt`, `info/scriptGuide.odt`
- Steam Workshop uploader: https://github.com/VizardAlpha/SteamWorkshopManager/releases/

## 10. Not yet verified

- The template's Maven build wasn't run on this machine yet (the sandbox blocked running the downloaded wrapper).
  Versions line up (template 71.44 = game 71.44, Java 21 present), so it *should* build once the paths are fixed.
