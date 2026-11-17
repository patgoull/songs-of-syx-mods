<#
.SYNOPSIS
  Compares two mod sets (A and B) on the autopilot: runs each several times, alternating, and reports medians.

.DESCRIPTION
  Each run is .\run.ps1 -Auto with exactly the given mods (plus the autopilot), on the pinned save
  (build\auto\save.txt). Rounds alternate the order (A B, then B A, ...) so neither side always runs first. The logs
  are kept in build\ab\<label>-<n>.log and the report is written to build\ab\report.md.
  One tour run takes about 2 minutes, a load run about 45 s, a sim run about 1 minute.

.EXAMPLE
  .\tools\ab.ps1 -B "Goul - Faster Load" -Mode load -Runs 3
  .\tools\ab.ps1 -A "Goul - Live Zoom (test)" -B "Goul - Live Zoom (test)","Goul - Faster Render" -LabelB render
  .\tools\ab.ps1 -B "Goul - Faster Render" -Mode sim
#>
param(
	[string[]]$A = @(),
	[string[]]$B = @(),
	[string]$LabelA = "",
	[string]$LabelB = "",
	[ValidateSet("tour", "load", "sim", "save", "look")][string]$Mode = "tour",
	[int]$Runs = 3,
	[string]$Save = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$run = Join-Path $root "run.ps1"
$outDir = Join-Path $root "build\ab"
if (-not $LabelA) { $LabelA = if ($A.Count) { "A" } else { "vanilla" } }
if (-not $LabelB) { $LabelB = "B" }
if (Test-Path $outDir) { Remove-Item $outDir -Recurse -Force }
New-Item -ItemType Directory -Force $outDir | Out-Null

$sides = @{ $LabelA = $A; $LabelB = $B }
# label -> list of runs; a run is an ordered dictionary "metric" -> value
$results = @{ $LabelA = @(); $LabelB = @() }
$metricOrder = New-Object System.Collections.Generic.List[string]
$inv = [Globalization.CultureInfo]::InvariantCulture

function Read-Run([string]$log) {
	$r = [ordered]@{}
	$done = $false
	foreach ($line in Get-Content $log) {
		if ($line -match "GOUL auto: timing startup ([\d.]+) s, load ([\d.]+) s") {
			$r["startup s"] = [double]::Parse($Matches[1], $inv)
			$r["load s"] = [double]::Parse($Matches[2], $inv)
		} elseif ($line -match "GOUL auto: (\S+)\s+fps\s+([\d.]+), updates/s\s+([\d.]+), worst gap\s+([\d.]+) ms, game x\s*([\d.]+), ms update\s+([\d.]+) render\s+([\d.]+) flush\s+([\d.]+)") {
			$step = $Matches[1]
			if ($step -eq "settle" -or $step -eq "sim-warmup") { continue }
			$r["$step fps"] = [double]::Parse($Matches[2], $inv)
			if ($step -like "sim-*" -or $step -like "*speed*") { $r["$step game x"] = [double]::Parse($Matches[5], $inv) }
			$r["$step update ms"] = [double]::Parse($Matches[6], $inv)
			$r["$step render ms"] = [double]::Parse($Matches[7], $inv)
			$r["$step worst gap ms"] = [double]::Parse($Matches[4], $inv)
		} elseif ($line -match "GOUL auto: save (\d+) took ([\d.]+) ms") {
			$r["save $($Matches[1]) ms"] = [double]::Parse($Matches[2], $inv)
		} elseif ($line -match "GOUL auto: done") {
			$done = $true
		}
	}
	if (-not $done) { return $null }
	return $r
}

function Median([double[]]$v) {
	$s = $v | Sort-Object
	$n = $s.Count
	if ($n -eq 0) { return [double]::NaN }
	if ($n % 2) { return $s[($n - 1) / 2] }
	return ($s[$n / 2 - 1] + $s[$n / 2]) / 2
}

$total = 2 * $Runs
$k = 0
for ($i = 1; $i -le $Runs; $i++) {
	$order = if ($i % 2) { @($LabelA, $LabelB) } else { @($LabelB, $LabelA) }
	foreach ($label in $order) {
		$k++
		$mods = @($sides[$label])
		Write-Host ("[{0}/{1}] {2} run {3}: {4}" -f $k, $total, $label, $i, $(if ($mods.Count) { $mods -join ", " } else { "no mods" }))
		$params = @{ Auto = $true; Mode = $Mode; Mods = $mods }
		if ($Save) { $params.Save = $Save }
		& $run @params *> $null
		$log = Join-Path $outDir "$label-$i.log"
		Copy-Item (Join-Path $root "build\game.log") $log
		$r = Read-Run $log
		if ($null -eq $r) {
			Write-Host "  run didn't finish, see $log" -ForegroundColor Yellow
			continue
		}
		foreach ($m in $r.Keys) { if (-not $metricOrder.Contains($m)) { $metricOrder.Add($m) } }
		$results[$label] += , $r
		$fpsKeys = @($r.Keys | Where-Object { $_ -like "*fps" })
		Write-Host ("  load {0} s{1}" -f $r["load s"], $(if ($fpsKeys) { ", " + (($fpsKeys | Select-Object -Last 2 | ForEach-Object { "$_ $($r[$_])" }) -join ", ") } else { "" }))
	}
}

# Report: medians, the change from A to B, and each side's min-max spread
$na = $results[$LabelA].Count
$nb = $results[$LabelB].Count
$lines = @()
$lines += "# A/B: $LabelA vs $LabelB ($Mode, $na + $nb runs)"
$lines += ""
$lines += "- **${LabelA}**: $(if ($A.Count) { $A -join ', ' } else { 'no mods' })"
$lines += "- **${LabelB}**: $(if ($B.Count) { $B -join ', ' } else { 'no mods' })"
$lines += "- Save: $((Get-Content (Join-Path $root 'build\auto\save.txt') -TotalCount 1))"
$lines += ""
$lines += "Medians; the range is min-max. For fps and game x higher is better, for times lower is better."
$lines += ""
$lines += "| Metric | $LabelA | $LabelB | Change | $LabelA range | $LabelB range |"
$lines += "|---|---|---|---|---|---|"
foreach ($m in $metricOrder) {
	$va = @($results[$LabelA] | ForEach-Object { $_[$m] } | Where-Object { $null -ne $_ })
	$vb = @($results[$LabelB] | ForEach-Object { $_[$m] } | Where-Object { $null -ne $_ })
	$ma = Median $va
	$mb = Median $vb
	$chg = if ($va.Count -and $vb.Count -and $ma -ne 0) { "{0:+0.0;-0.0;0.0}%" -f (100 * ($mb - $ma) / $ma) } else { "" }
	$ra = if ($va.Count) { "{0:0.##}-{1:0.##}" -f ($va | Measure-Object -Minimum).Minimum, ($va | Measure-Object -Maximum).Maximum } else { "" }
	$rb = if ($vb.Count) { "{0:0.##}-{1:0.##}" -f ($vb | Measure-Object -Minimum).Minimum, ($vb | Measure-Object -Maximum).Maximum } else { "" }
	$lines += "| $m | {0:0.##} | {1:0.##} | $chg | $ra | $rb |" -f $ma, $mb
}
$report = Join-Path $outDir "report.md"
[IO.File]::WriteAllLines($report, $lines)
$lines | ForEach-Object { Write-Host $_ }
Write-Host "Report saved to $report"
