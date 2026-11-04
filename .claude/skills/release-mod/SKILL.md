---
name: release-mod
description: Release one of the repo's mods (features/<name>) - bump the version, build, zip, write GitHub release notes, tag, publish with gh, and draft the Discord post (edit the first post, or also a reply, depending on what changed). Use when the user says "release <mod>", asks for release notes, or asks what to post on Discord after a mod update.
---

# Releasing a mod

Argument: the feature folder name, e.g. `move-room`. Release one mod at a time, one GitHub release per mod per version.
The repo is public: get the user's OK before pushing or publishing, unless they already said "release" for this mod.

## 1. Decide what kind of release it is

This decides the version bump and what goes on Discord.

| Kind | Example | Version | GitHub release | Discord |
|---|---|---|---|---|
| **New mod** | first release of Move Room | `0.1.0` | full notes | **new post** (the first post of its thread) |
| **Rebuild for a game version**, no behaviour change | 71.44 → 71.84 | patch: `0.2.0` → `0.2.1` | short "New in" + full feature list | **edit the first post only**, no reply |
| **Bug fix** players should know about | crash fix, undo fix | patch | "New in" lists the fixes | edit the first post **and** post a short reply |
| **New feature** | Nudge room | minor: `0.1.x` → `0.2.0` | "New in" lists the feature | edit the first post **and** post a reply about the feature |

Why: Discord doesn't notify anyone about an edit. A reply is only worth it when there's something new for players.
A rebuild with the same features only needs the first post's download links updated (the user's call, 2026-11-04).

## 2. Build and package

1. Set `VERSION` in `features/<name>/_Info.txt` (and `GAME_VERSION_MINOR` if the game version changed).
2. `.\build.ps1 <name> -Install` (writes `patches/<name>.patch`). Check `git diff patches/` only shows the intended changes.
3. Zip the built mod folder with Windows tar (forward-slash entries; don't use Compress-Archive):
   ```powershell
   & "$env:SystemRoot\System32\tar.exe" -a -c -f "build\release-71.<minor>\Goul-<Name-With-Dashes>-v<ver>-V71.<minor>.zip" -C "build\<name>\mod" "Goul - <Name>"
   ```
   It must contain `Goul - <Name>/_Info.txt` and `Goul - <Name>/V71/script/<name>.jar`.
4. Spot-check the jar is the new build, e.g. `javap -v` on a class that uses a changed game API.

## 3. GitHub release notes

Write `build/release-71.<minor>/<name>-v<ver>.md` (gitignored), in the user's voice, Discord-friendly (• bullets, links in `<>`):
- One summary line: what the mod is, and the game version.
- `New in v<ver>:` bullets (skip for a new mod). For a game-version rebuild: why the old version breaks, "no other changes",
  and a link to the release to keep using on the other game version.
- What it adds: feature bullets (copy from the feature README, short).
- `For Jake:` what changed vs vanilla (counts of changed lines/classes) and the patch link **at the tag**:
  `https://github.com/patgoull/songs-of-syx-mods/blob/<name>-v<ver>/patches/<name>.patch`. Never link `blob/main`
  or `releases/latest`: they drift.
- `Repo:` line, then install steps (download the zip, extract into `%APPDATA%\songsofsyx\mods`, enable "Goul - <Name>"),
  and "Disable it after a game update until I release a new version. Feedback welcome!"

## 4. Commit, tag, publish

1. Commit the version bump (`Release <Name> v<ver>` + the Co-Authored-By line).
2. Create the tag **locally**, annotated, on that commit: `git tag -a <name>-v<ver> -m "<Name> v<ver> (V71.<minor>)"`.
   Never let GitHub create a tag from the web UI or by renaming a release's tag: it tags the tip of `main`, not the
   release's code. Moving a tag that's already on GitHub needs `git push --force`, which the auto-mode check blocks,
   so the user has to run that command.
3. `git push origin main` and `git push origin <name>-v<ver>`.
4. Publish (`gh` is not on the bash PATH, call it by full path):
   ```bash
   "/c/Program Files/GitHub CLI/gh.exe" release create <name>-v<ver> <zip> --repo patgoull/songs-of-syx-mods \
     --title "<Name> v<ver> (game V71.<minor>)" --notes-file <notes.md> --verify-tag [--prerelease]
   ```
   Use `--prerelease` for a build that only works on a beta game version, so the stable build stays "Latest".
5. Check: `gh release view <tag> --json isPrerelease,assets`, and the patch link returns 200.

## 5. Discord

The user posts it themselves. Give each message in its own code block, ready to paste, and check the length:
**Discord's limit is 2000 characters** (`wc -m`). If it's too long, cut detail that's on the release page anyway.

Thread title (each mod has its own thread): `[Mod] <Mod name>: <what it does, a few words> (<game versions>)`, with
`POC, ` before the versions while the mod is a proof of concept, e.g.
`[Mod] Move Room: move a built room in one click (POC, V71.84 beta)` or `... (V71.44 + V71.84)`. Use the mod's real name
(as in the launcher, without "Goul - "). Max 100 characters (`wc -m`). When the supported game versions change, tell the
user to rename the thread along with editing the first post.

First post (new mod, or the text to put in the edited first post). Keep this order and wording style:
1. The problem, 2-3 sentences, from the player's view.
2. `What it adds:` • bullets, with hotkeys in parentheses.
3. Optional: limits and behaviour players should know (what it doesn't do, costs, safety).
4. The AI note, verbatim: `Note: I'm a web developer, not a game developer. The code was written largely with Claude (AI); I designed the features and tested them in game.`
5. `For Jake:` what changed vs vanilla, and the patch link at the tag. Then `Repo: https://github.com/patgoull/songs-of-syx-mods`
6. `Download, pick your game version:` one bullet per supported game version, `• V71.44 (stable): <release link>`.
   Then: `Extract the zip into %APPDATA%\songsofsyx\mods and enable "Goul - <Name>" in the launcher. Disable it after a game update until I release a new version.`
7. A closing line, e.g. `Feedback welcome!` (plus "works with/without <other mod>" if relevant).

When editing an existing first post: ask the user to paste its current text, keep their wording, and change only what
the release changed (download section, new feature bullets, patch link). Point any `blob/main` or `releases/latest`
links at tags.

Reply (only for bug fixes and new features): one line with the version and its release link, 1-3 lines on what's new
or fixed and who should update, and `For Jake:` only if the code changed in a way worth reading, with the patch link at the tag.
