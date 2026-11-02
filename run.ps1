<#
.SYNOPSIS
  Runs the game from the console, with its output saved to build\game.log.

.DESCRIPTION
  Without -Launcher the launcher is skipped and the mods enabled in the launcher last time are used.
  Use -Launcher once to enable a new mod.

.EXAMPLE
  .\run.ps1 -Launcher
  .\build.ps1 room-builder-plus -Install; .\run.ps1
#>
param(
	[switch]$Launcher,
	[string]$GameDir = "C:\Steam\steamapps\common\Songs of Syx"
)

$ErrorActionPreference = "Stop"
$java = Join-Path $GameDir "jre\bin\java.exe"
$logDir = Join-Path $PSScriptRoot "build"
New-Item -ItemType Directory -Force $logDir | Out-Null
$logFile = Join-Path $logDir "game.log"

$gameArgs = @("-jar", "SongsOfSyx.jar")
if ($Launcher) { $gameArgs += "launcher" }

$log = New-Object System.IO.StreamWriter($logFile, $false, (New-Object System.Text.UTF8Encoding $false))
$log.AutoFlush = $true
Push-Location $GameDir
try {
	& $java @gameArgs | ForEach-Object {
		$_
		$log.WriteLine($_)
	}
} finally {
	Pop-Location
	$log.Close()
}
Write-Host "Log saved to $logFile"
