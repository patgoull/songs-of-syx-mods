# SongOfGoul: Songs of Syx modding workspace

The user is new to Java. Explain Java/Maven concepts briefly when they come up, and prefer data (config) mods over code when either works.
Full guide: [docs/MODDING_GUIDE.md](docs/MODDING_GUIDE.md).

**Purpose:** mods are suggestions the dev (Jake) can inspect and integrate. Prefer patch-style mods: a minimal, `// GOUL:`-marked
edit of a copied vanilla class, in Jake's style, with no Lombok and one feature per patch. Not using the template's Maven. See [docs/STRATEGY.md](docs/STRATEGY.md).

## Environment
- Game: **V71.44**, `C:\Steam\steamapps\common\Songs of Syx` (NOT `Program Files (x86)`; template paths must be changed)
- Game source: `info/SongsOfSyx-sources.jar` (zip). Vanilla data: `base/data.zip` → `data/assets/{init,text,sprite,audio}`
- Dev mods folder: `%APPDATA%\songsofsyx\mods`; logs: `%APPDATA%\songsofsyx\logs`
- Workshop: `C:\Steam\steamapps\workshop\content\1162750`
- JDK 21 (Zulu) on PATH; IntelliJ IDEA CE 2024.1.1; no global Maven, so use the template's `mvnw.cmd`
- Template: https://github.com/4rg0n/songs-of-syx-mod-example (targets 71.44)

## Facts verified in V71.44 source (trust over template docs)
- Mod layout: `<Mod>/_Info.txt` + `<Mod>/V71/{assets,script,saves,examples,campaigns}`
- Audio folder is `assets/audio` (template skeleton wrongly says `sound`)
- Config merge (`snake2d/util/file/Json.java`): same-named mod file layered on vanilla; top-level keys replace;
  `_JSON_ADD` = recursive object merge; `_ARRAY_ADD` = append arrays; `_IgnoreVanilla.txt` in a folder drops vanilla files.
  `__OVERWRITE` (in template docs) isn't present in 71.44 code.
- `TEXTURE_CACHE_SIZE` ∈ {4096, 8192, 16384}, default 4096 (`init/paths/ModInfo.java`)
- Scripts (`script/ScriptLoad.java`): all classes in `V71/script/*.jar` are loaded on the system classloader; every non-abstract
  `script.SCRIPT` with a public no-arg ctor is a script. `createInstance()` runs after VIEW exists, so inject UI there.
- `UIPanelTopSett.addExtraElement` doesn't work in V71.

## Repo layout and build
- `vanilla/<version>/`: extracted game source (gitignored). `features/<name>/{src,_Info.txt,README.md,assets?}`: one mod per feature.
- `.\build.ps1 <feature> [-Install]`: javac against `SongsOfSyx.jar`, jar into `build/`, writes `patches/<feature>.patch`, installs to `%APPDATA%\songsofsyx\mods`.
- `.\run.ps1 [-Launcher]`: runs the game from the console (no launcher = mods from `settings/LauncherSettings.txt`), output saved to `build/game.log`. Read that log when debugging.
- To change a vanilla class: copy it from `vanilla/` into the feature's `src/` at the same path, then edit. Keep edits additive and marked `// GOUL`.
- Check the diff after editing: the Edit tool strips tabs on blank lines, which shows up as fake `-\t` lines in the patch. Restore them so vanilla lines stay untouched.

## Conventions when writing mod code
- No game resources in `static` fields (static init runs before the game loads)
- Wrap reflection in try/catch; game collections are `snake2d.util.sets.*`, not `java.util`
- Check behaviour in the extracted game source before relying on template docs
