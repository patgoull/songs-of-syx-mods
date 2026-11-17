<#
.SYNOPSIS
  Screenshots the same spot at fixed times of day with two mod sets (A and B) and puts them side by side.

.DESCRIPTION
  Runs .\run.ps1 -Auto -Mode look once per side, on the pinned save (build\auto\save.txt): sunrise, morning, noon,
  afternoon, sunset, twilight and night at zoom 1, and night at zoom 0. The shots go to build\look\<label>\ and the
  pairs (A left, B right) to build\look\compare\. About 1 minute per side. Needs Python with Pillow for the pairs.

.EXAMPLE
  .\tools\look.ps1 -B "Goul - Smoke" -LabelB smoke -Focus REFINER_SMELTER
  -Focus <room key> looks at the busiest room of that type instead of the middle of the map.
#>
param(
	[string[]]$A = @(),
	[string[]]$B = @(),
	[string]$LabelA = "",
	[string]$LabelB = "B",
	[string]$Save = "",
	[string]$Focus = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$run = Join-Path $root "run.ps1"
$outDir = Join-Path $root "build\look"
if (-not $LabelA) { $LabelA = if ($A.Count) { "A" } else { "vanilla" } }
if (Test-Path $outDir) { Remove-Item $outDir -Recurse -Force }

foreach ($side in @(@($LabelA, $A), @($LabelB, $B))) {
	$label = $side[0]
	$mods = @($side[1])
	Write-Host ("{0}: {1}" -f $label, $(if ($mods.Count) { $mods -join ", " } else { "no mods" }))
	$params = @{ Auto = $true; Mode = "look"; Mods = $mods }
	if ($Save) { $params.Save = $Save }
	if ($Focus) { $params.Focus = $Focus }
	& $run @params *> $null
	$dir = Join-Path $outDir $label
	New-Item -ItemType Directory -Force $dir | Out-Null
	Get-ChildItem (Join-Path $root "build\auto") -Filter *.png | Move-Item -Destination $dir
	Copy-Item (Join-Path $root "build\game.log") (Join-Path $dir "game.log")
	Write-Host ("  {0} screenshots" -f @(Get-ChildItem $dir -Filter *.png).Count)
}

python (Join-Path $PSScriptRoot "look-compare.py") (Join-Path $outDir $LabelA) (Join-Path $outDir $LabelB) (Join-Path $outDir "compare")
