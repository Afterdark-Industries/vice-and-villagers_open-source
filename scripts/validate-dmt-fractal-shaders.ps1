$ErrorActionPreference = 'Stop'
$taskRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskReport = Join-Path $taskRoot 'build/dmt-fractal-shader-validation.log'
New-Item -ItemType Directory -Path (Join-Path $taskRoot 'build') -Force | Out-Null
@(
    'Vice and Villagers: isolated DMT fractal world GPU validation',
    ('UTC: ' + [DateTime]::UtcNow.ToString('yyyy-MM-dd HH:mm:ss')),
    'Render scope: hidden OpenGL 3.2 context; 128x96 functional/temporal checks, 40-second 30fps cinematic entry, dose/ecology/lane probes, 640x360 previews/GIFs, warmed 720p/1080p GPU timing.',
    'Checks include every player preset, return settling, world-hue continuity, sampled demanding chapters, same-revision 2048-step nearest-hit reference, and stereo correspondence.',
    'Minecraft and a VR headset are not launched, attached to, or controlled.',
    'These checks validate shader execution and world-space stereo geometry, not live integration, headset comfort, or total game frame time.',
    ''
) | Set-Content -LiteralPath $taskReport -Encoding utf8
. (Join-Path $PSScriptRoot 'tooling.ps1')
$taskEnvironment = Get-ToolingEnvironment -RepoRoot $taskRoot
$taskClasspath = [string]::Join([IO.Path]::PathSeparator, $taskEnvironment.gpuClasspath)
$taskOutput = Join-Path $taskRoot 'build/dmt-fractal-shader-validation'
New-Item -ItemType Directory -Path $taskOutput -Force | Out-Null
& $taskEnvironment.javacExecutable -cp $taskClasspath -d $taskOutput (Join-Path $PSScriptRoot 'ValidateDmtFractalShaders.java')
if ($LASTEXITCODE -ne 0) { throw 'DMT fractal GPU harness compilation failed.' }
$taskRunClasspath = $taskOutput + [IO.Path]::PathSeparator + $taskClasspath
& $taskEnvironment.javaExecutable '-Djava.awt.headless=true' -cp $taskRunClasspath ValidateDmtFractalShaders $taskRoot | Tee-Object -FilePath $taskReport -Append
if ($LASTEXITCODE -ne 0) { throw 'DMT fractal GPU validation failed.' }
