param([switch]$NitrousOnly)
$ErrorActionPreference = 'Stop'
$taskRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskReport = Join-Path $taskRoot $(if ($NitrousOnly) { 'build/nitrous-bloom-gpu.log' } else { 'build/vr-shader-validation.log' })
New-Item -ItemType Directory -Path (Join-Path $taskRoot 'build') -Force | Out-Null
@(
    'Vice and Villagers VR2: generated-art parity and isolated GPU functional validation',
    ('UTC: ' + [DateTime]::UtcNow.ToString('yyyy-MM-dd HH:mm:ss')),
    'Render scope: hidden 96x64 OpenGL 3.2 context, RGBA32F output and synthetic color/depth inputs.',
    'The current Minecraft instance and headset were not launched, attached to, or controlled.',
    'These checks validate shader execution and mathematical mapping; they do not validate headset comfort, live integration, or VR performance.',
    ''
) | Set-Content -LiteralPath $taskReport -Encoding utf8
& python (Join-Path $PSScriptRoot 'generate-vr-shaders.py') --check | Tee-Object -FilePath $taskReport -Append
if ($LASTEXITCODE -ne 0) { throw 'Generated VR shader artistic parity validation failed.' }
. (Join-Path $PSScriptRoot 'tooling.ps1')
$taskEnvironment = Get-ToolingEnvironment -RepoRoot $taskRoot
$taskClasspath = [string]::Join([IO.Path]::PathSeparator, $taskEnvironment.gpuClasspath)
$taskOutput = Join-Path $taskRoot 'build/vr-shader-validation'
New-Item -ItemType Directory -Path $taskOutput -Force | Out-Null
& $taskEnvironment.javacExecutable -cp $taskClasspath -d $taskOutput (Join-Path $PSScriptRoot 'ValidateVrShaders.java')
if ($LASTEXITCODE -ne 0) { throw 'GPU shader harness compilation failed.' }
$taskRunClasspath = $taskOutput + [IO.Path]::PathSeparator + $taskClasspath
$taskValidationArgs = @($taskRoot)
if ($NitrousOnly) { $taskValidationArgs += '--nitrous-only' }
& $taskEnvironment.javaExecutable -cp $taskRunClasspath ValidateVrShaders @taskValidationArgs | Tee-Object -FilePath $taskReport -Append
if ($LASTEXITCODE -ne 0) { throw 'GPU shader validation failed.' }
