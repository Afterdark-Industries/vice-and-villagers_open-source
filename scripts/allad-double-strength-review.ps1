param([int]$Width = 640, [switch]$CompileOnly, [switch]$BaselineOnly, [switch]$PanelsOnly, [switch]$PreviewOnly, [switch]$ClockOnly, [switch]$NumericOnly)
$ErrorActionPreference = 'Stop'
$alladTaskRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$alladTaskOutput = Join-Path $alladTaskRoot 'build/allad-double-strength-review'
New-Item -ItemType Directory -Path $alladTaskOutput -Force | Out-Null
. (Join-Path $PSScriptRoot 'tooling.ps1')
$alladTaskEnvironment = Get-ToolingEnvironment -RepoRoot $alladTaskRoot
$alladTaskClasspath = [string]::Join([IO.Path]::PathSeparator, $alladTaskEnvironment.gpuClasspath)
$alladBaselineSource = Join-Path $alladTaskRoot 'tests/fixtures/allad-before/AlLadVisualDrive.java'
$alladBaselineAlias = Join-Path $alladTaskOutput 'BaselineAlLadVisualDrive.java'
$alladBaselineText = [IO.File]::ReadAllText($alladBaselineSource).Replace('AlLadVisualDrive', 'BaselineAlLadVisualDrive')
[IO.File]::WriteAllText($alladBaselineAlias, $alladBaselineText, [Text.UTF8Encoding]::new($false))
& $alladTaskEnvironment.javacExecutable -cp $alladTaskClasspath -d $alladTaskOutput $alladBaselineAlias (Join-Path $alladTaskRoot 'src/main/java/com/trippycraft/substance/allad/AlLadVisualDrive.java') (Join-Path $PSScriptRoot 'allad-double-strength-review.java')
if ($LASTEXITCODE -ne 0) { throw 'AL-LAD GPU harness compilation failed' }
if ($CompileOnly) { return }
$alladArguments = @($alladTaskRoot, $Width, $alladTaskEnvironment.minecraftResources)
$alladLogName = 'focused-gpu-validation.log'
if ($BaselineOnly) { $alladArguments += 'baseline'; $alladLogName = 'baseline-gpu-validation.log' }
if ($PanelsOnly) { $alladArguments += 'panels'; $alladLogName = 'film-panels-gpu-validation.log' }
if ($PreviewOnly) { $alladArguments += 'preview'; $alladLogName = 'preview-gpu-validation.log' }
if ($ClockOnly) { $alladArguments += 'clock'; $alladLogName = 'clock-diagnostics.log' }
if ($NumericOnly) { $alladArguments += 'numeric'; $alladLogName = 'numeric-validation.log' }
& $alladTaskEnvironment.javaExecutable -cp ($alladTaskOutput + [IO.Path]::PathSeparator + $alladTaskClasspath) AlLadDoubleStrengthReview @alladArguments | Tee-Object -FilePath (Join-Path $alladTaskOutput $alladLogName)
if ($LASTEXITCODE -ne 0) { throw 'AL-LAD GPU visual review failed' }
