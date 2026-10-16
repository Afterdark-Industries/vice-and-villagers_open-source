$ErrorActionPreference = 'Stop'
$taskRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskReport = Join-Path $taskRoot 'build/dmt-fractal-shader-validation.log'
New-Item -ItemType Directory -Path (Join-Path $taskRoot 'build') -Force | Out-Null
@(
    'Vice and Villagers: isolated DMT fractal world GPU validation',
    ('UTC: ' + [DateTime]::UtcNow.ToString('yyyy-MM-dd HH:mm:ss')),
    'Render scope: hidden OpenGL 3.2 context; 128x96 floating-point functional/60fps temporal checks, 640x360 beauty panels/GIFs, warmed 720p/1080p GPU timing.',
    'Minecraft and a VR headset are not launched, attached to, or controlled.',
    'These checks validate shader execution and world-space stereo geometry, not live integration, headset comfort, or game-resolution performance.',
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
