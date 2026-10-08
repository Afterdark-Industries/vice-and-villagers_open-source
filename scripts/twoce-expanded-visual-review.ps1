param([int]$Width = 640, [switch]$CompileOnly)
$ErrorActionPreference = 'Stop'
$taskRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskOutput = Join-Path $taskRoot 'build/twoce-expanded-strength-review'
New-Item -ItemType Directory -Path $taskOutput -Force | Out-Null
. (Join-Path $PSScriptRoot 'tooling.ps1')
$taskEnvironment = Get-ToolingEnvironment -RepoRoot $taskRoot
$taskClasspath = [string]::Join([IO.Path]::PathSeparator, $taskEnvironment.gpuClasspath)
& $taskEnvironment.javacExecutable -cp $taskClasspath -d $taskOutput (Join-Path $taskRoot 'src/main/java/com/trippycraft/substance/twoce/TwoCeVisualDoseResponse.java') (Join-Path $taskRoot 'src/main/java/com/trippycraft/substance/twoce/TwoCeVisualPresentation.java') (Join-Path $PSScriptRoot 'twoce-expanded-visual-review.java')
if ($LASTEXITCODE -ne 0) { throw '2C-E GPU review harness compilation failed' }
if ($CompileOnly) { return }
& $taskEnvironment.javaExecutable -cp ($taskOutput + [IO.Path]::PathSeparator + $taskClasspath) TwoCeExpandedVisualReview $taskRoot $Width $taskEnvironment.minecraftResources | Tee-Object -FilePath (Join-Path $taskOutput 'focused-gpu-validation.log')
if ($LASTEXITCODE -ne 0) { throw '2C-E GPU visual review failed' }
