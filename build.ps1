<#
.SYNOPSIS
  Builds one feature mod, optionally installs it, and writes its patch against the vanilla sources.

.EXAMPLE
  .\build.ps1 room-builder-plus -Install
#>
param(
	[Parameter(Mandatory = $true)][string]$Feature,
	[switch]$Install,
	[string]$GameDir = "C:\Steam\steamapps\common\Songs of Syx",
	[string]$GameVersion = "71.44"
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$featureDir = Join-Path $root "features\$Feature"
$srcDir = Join-Path $featureDir "src"
$vanillaDir = Join-Path $root "vanilla\$GameVersion"
$gameJar = Join-Path $GameDir "SongsOfSyx.jar"
$major = $GameVersion.Split(".")[0]
$utf8 = New-Object System.Text.UTF8Encoding $false

if (-not (Test-Path $featureDir)) { throw "No feature folder: $featureDir" }
if (-not (Test-Path $gameJar)) { throw "Game jar not found: $gameJar" }

# Mod name comes from the NAME line of the feature's _Info.txt
$info = Join-Path $featureDir "_Info.txt"
$modName = ([regex]::Match((Get-Content $info -Raw), 'NAME:\s*"([^"]+)"')).Groups[1].Value
if (-not $modName) { throw "NAME missing in $info" }

# Vanilla sources use CRLF, keep ours the same so patches only show real changes
$sources = @(Get-ChildItem $srcDir -Recurse -Filter *.java)
foreach ($f in $sources) {
	$text = [IO.File]::ReadAllText($f.FullName, $utf8)
	$fixed = $text -replace "`r?`n", "`r`n"
	if ($fixed -ne $text) { [IO.File]::WriteAllText($f.FullName, $fixed, $utf8) }
}

# Compile
$buildDir = Join-Path $root "build\$Feature"
if (Test-Path $buildDir) { Remove-Item $buildDir -Recurse -Force }
$classes = Join-Path $buildDir "classes"
New-Item -ItemType Directory -Force $classes | Out-Null

Write-Host "Compiling $($sources.Count) files..."
& javac -encoding UTF-8 --release 21 -nowarn -cp $gameJar -d $classes @($sources | ForEach-Object { $_.FullName })
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

# Package the mod folder: <mod>/_Info.txt and <mod>/V<major>/script/<feature>.jar
$modDir = Join-Path $buildDir "mod\$modName"
$scriptDir = Join-Path $modDir "V$major\script"
New-Item -ItemType Directory -Force $scriptDir | Out-Null
Copy-Item $info $modDir
$assets = Join-Path $featureDir "assets"
if (Test-Path $assets) { Copy-Item $assets (Join-Path $modDir "V$major") -Recurse }
& jar --create --file (Join-Path $scriptDir "$Feature.jar") -C $classes .
if ($LASTEXITCODE -ne 0) { throw "jar failed" }
Write-Host "Built $modDir"

# Patch against vanilla, with paths relative to the game source root
$patchDir = Join-Path $root "patches"
New-Item -ItemType Directory -Force $patchDir | Out-Null
$patch = New-Object System.Text.StringBuilder
Push-Location $root
try {
	foreach ($f in $sources) {
		$rel = $f.FullName.Substring($srcDir.Length + 1).Replace("\", "/")
		$old = "vanilla/$GameVersion/$rel"
		if (-not (Test-Path $old)) { $old = "/dev/null" }
		$diff = & git -c core.autocrlf=false diff --no-index --no-color -- $old "features/$Feature/src/$rel"
		if ($diff) {
			$text = ($diff -join "`n") -replace "a/vanilla/$GameVersion/", "a/" -replace "a/features/$Feature/src/", "a/" -replace "b/features/$Feature/src/", "b/"
			[void]$patch.Append($text).Append("`n")
		}
	}
} finally {
	Pop-Location
}
$patchFile = Join-Path $patchDir "$Feature.patch"
[IO.File]::WriteAllText($patchFile, $patch.ToString(), $utf8)
Write-Host "Wrote $patchFile"

if ($Install) {
	$target = Join-Path $env:APPDATA "songsofsyx\mods\$modName"
	# The game keeps the mod's jars open while it runs
	foreach ($jar in @(Get-ChildItem $target -Recurse -Filter *.jar -ErrorAction SilentlyContinue)) {
		try { ([IO.File]::Open($jar.FullName, 'Open', 'ReadWrite', 'None')).Close() }
		catch { Write-Host "Not installed: the game is running and has $($jar.Name) open. Close it and run again." -ForegroundColor Yellow; exit 1 }
	}
	if (Test-Path $target) { Remove-Item $target -Recurse -Force }
	Copy-Item $modDir $target -Recurse
	Write-Host "Installed to $target"
}

# git diff returns 1 when files differ, that's not a failure
exit 0
