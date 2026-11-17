<#
.SYNOPSIS
  Runs the game from the console, with its output saved to build\game.log.

.DESCRIPTION
  Without -Launcher the launcher is skipped and the mods enabled in the launcher last time are used.
  Use -Launcher once to enable a new mod.
  With -Profile the game is recorded with Java Flight Recorder into build\profile-<time>-<pid>.jfr (summarize it with tools\jfr-hot.py).
  The game runs in a second java process, which gets the setting through JAVA_TOOL_OPTIONS.
  With -Auto the game plays itself (features\autopilot): it loads a save, plays a fixed camera tour (zoom levels by day,
  night, a pan, high speed), logs a "GOUL auto:" line per step, saves screenshots into build\auto and quits. The
  autopilot mod is enabled for that run only. The save is pinned in build\auto\save.txt (the newest save on the first
  run, or -Save <name>) so runs stay comparable even if the game autosaves.
  -Mode load only times startup and load; -Mode sim runs the simulation at 250x; -Mode save saves 3 times into
  build\auto (never the game's saves) and logs how long each took; -Mode look screenshots one spot at sunrise,
  morning, noon, afternoon, sunset, twilight and night (tools\look.ps1 compares two mod sets). -Mods "A","B" runs
  with exactly those mods (plus the autopilot) instead of the launcher's list, e.g. -Mods @() for vanilla. Used by
  tools\ab.ps1 and tools\look.ps1. -Focus <room key> (e.g. REFINER_SMELTER) looks at the busiest room of that type
  instead of the middle of the map.

.EXAMPLE
  .\run.ps1 -Launcher
  .\build.ps1 room-builder-plus -Install; .\run.ps1
  .\run.ps1 -Profile
  .\run.ps1 -Auto -Profile
#>
param(
	[switch]$Launcher,
	[switch]$Profile,
	[switch]$Auto,
	[string]$Save = "",
	[ValidateSet("tour", "load", "sim", "save", "look")][string]$Mode = "tour",
	[string[]]$Mods = $null,
	[string]$Focus = "",
	[string]$GameDir = "C:\Steam\steamapps\common\Songs of Syx"
)

$ErrorActionPreference = "Stop"
$java = Join-Path $GameDir "jre\bin\java.exe"
$logDir = Join-Path $PSScriptRoot "build"
New-Item -ItemType Directory -Force $logDir | Out-Null
$logFile = Join-Path $logDir "game.log"

$gameArgs = @("-jar", "SongsOfSyx.jar")
if ($Launcher) { $gameArgs += "launcher" }
$javaOptions = @()
if ($Profile) {
	$profileStart = Get-Date
	$javaOptions += "-XX:StartFlightRecording=filename=$logDir\profile-%t-%p.jfr,settings=profile,dumponexit=true"
}
$settingsFile = Join-Path $env:APPDATA "songsofsyx\settings\LauncherSettings.txt"
$modsPattern = "MODS: \[[^\]]*\],"
# The launcher's own MODS block, kept on disk while an -Auto run has changed it. Only that block is put back, so other
# launcher settings changed meanwhile are kept. A run that was cut off (window closed, task stopped) is cleaned up here.
$modsBackupFile = Join-Path $logDir "auto\launcher-mods-backup.txt"
function Restore-Mods {
	if (-not (Test-Path $modsBackupFile)) { return }
	$orig = [IO.File]::ReadAllText($modsBackupFile)
	$text = [IO.File]::ReadAllText($settingsFile)
	[IO.File]::WriteAllText($settingsFile, [regex]::Replace($text, $modsPattern, { param($m) $orig }))
	Remove-Item $modsBackupFile
}
Restore-Mods
if ($Auto) {
	$autoDir = Join-Path $logDir "auto"
	New-Item -ItemType Directory -Force $autoDir | Out-Null
	Get-ChildItem $autoDir -Filter *.png | Remove-Item
	$pin = Join-Path $autoDir "save.txt"
	if ($Save) { Set-Content $pin $Save }
	elseif (-not (Test-Path $pin)) {
		$newest = Get-ChildItem (Join-Path $env:APPDATA "songsofsyx\saves\saves") -Filter *.save | Sort-Object LastWriteTime -Descending | Select-Object -First 1
		Set-Content $pin $newest.BaseName
	}
	$Save = (Get-Content $pin -TotalCount 1).Trim()
	Write-Host "Autopilot on save $Save"
	# Enable the autopilot mod (and with -Mods, exactly those mods) for this run only, the mod list is put back afterwards
	$text = [IO.File]::ReadAllText($settingsFile)
	$origMods = [regex]::Match($text, $modsPattern).Value
	if (-not $origMods) { throw "No MODS list in $settingsFile" }
	$eol = if ($text.Contains("`r`n")) { "`r`n" } else { "`n" }
	$list = if ($null -ne $Mods) { @("Goul - Autopilot (test)") + @($Mods | Where-Object { $_ }) }
		else { @("Goul - Autopilot (test)") + @([regex]::Matches($origMods, '"([^"]+)"') | ForEach-Object { $_.Groups[1].Value } | Where-Object { $_ -ne "Goul - Autopilot (test)" }) }
	$block = "MODS: [" + $eol + (($list | ForEach-Object { "`t`"$_`"," + $eol }) -join "") + "],"
	[IO.File]::WriteAllText($modsBackupFile, $origMods)
	[IO.File]::WriteAllText($settingsFile, [regex]::Replace($text, $modsPattern, { param($m) $block }))
	Write-Host "Mods for this run: $($list -join ', ')"
	$javaOptions += "-Dgoul.autopilot=$Mode", "-Dgoul.autopilot.save=$Save", "-Dgoul.autopilot.out=$autoDir"
	if ($Focus) { $javaOptions += "-Dgoul.autopilot.focus=$Focus" }
}
if ($javaOptions) { $env:JAVA_TOOL_OPTIONS = $javaOptions -join " " }

$log = New-Object System.IO.StreamWriter($logFile, $false, (New-Object System.Text.UTF8Encoding $false))
$log.AutoFlush = $true
Push-Location $GameDir
try {
	# Java's own stderr lines (e.g. "Picked up JAVA_TOOL_OPTIONS") are log text, not errors: with "Stop", PowerShell 5.1
	# turns them into a terminating error when the output of this script is redirected (tools\ab.ps1 does)
	$ErrorActionPreference = "Continue"
	& $java @gameArgs 2>&1 | ForEach-Object {
		$line = "$_"
		$line
		$log.WriteLine($line)
	}
	$ErrorActionPreference = "Stop"
} finally {
	Pop-Location
	$log.Close()
	if ($javaOptions) { Remove-Item Env:JAVA_TOOL_OPTIONS }
	Restore-Mods
}
Write-Host "Log saved to $logFile"
if ($Auto) {
	Select-String -Path $logFile -Pattern "GOUL auto:" | ForEach-Object { $_.Line.Trim() }
	Get-ChildItem $autoDir -Filter *.png | ForEach-Object { Write-Host "Screenshot $($_.FullName)" }
}
if ($Profile) {
	# Every java process the game starts writes one, the game's is the biggest
	Get-ChildItem (Join-Path $logDir "profile-*.jfr") | Where-Object { $_.LastWriteTime -ge $profileStart } |
		Sort-Object Length -Descending | ForEach-Object { Write-Host "Profile saved to $($_.FullName) ($([int]($_.Length / 1KB)) KB)" }
}
