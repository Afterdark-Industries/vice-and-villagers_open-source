$ErrorActionPreference = 'Stop'
$taskRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
. (Join-Path $PSScriptRoot 'tooling.ps1')
$taskEnvironment = Get-ToolingEnvironment -RepoRoot $taskRoot
$taskClasspath = [string]::Join([IO.Path]::PathSeparator, $taskEnvironment.runtimeClasspath)
$taskOutput = Join-Path $taskRoot 'build/substance-audio-validation'
New-Item -ItemType Directory -Force -Path $taskOutput | Out-Null
& $taskEnvironment.javacExecutable -proc:none -cp $taskClasspath -d $taskOutput (Join-Path $PSScriptRoot 'ValidateSubstanceAudio.java')
if ($LASTEXITCODE -ne 0) { throw 'Audio harness compilation failed' }
& $taskEnvironment.javaExecutable -cp ($taskOutput + [IO.Path]::PathSeparator + $taskClasspath) com.trippycraft.client.ValidateSubstanceAudio |
    Tee-Object -FilePath (Join-Path $taskRoot 'build/substance-audio-validation.log')
if ($LASTEXITCODE -ne 0) { throw 'Audio harness failed' }
