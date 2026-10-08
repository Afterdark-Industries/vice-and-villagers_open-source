param([string]$NativeStateDir = '', [string]$PreviousShaderPath = '', [string]$CandidateShaderPath = '', [switch]$CandidateOnly)
$ErrorActionPreference = 'Stop'
if ($CandidateOnly -and -not $CandidateShaderPath) { throw '-CandidateOnly requires -CandidateShaderPath.' }
$lsdRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$lsdReport = Join-Path $lsdRoot 'build/lsd-shader-validation.log'
if ($CandidateOnly) { $lsdReport = Join-Path $lsdRoot 'build/lsd-candidate-shader-validation.log' }
$lsdOutput = Join-Path $lsdRoot 'build/lsd-shader-validation'
New-Item -ItemType Directory -Path $lsdOutput -Force | Out-Null
@('Lsd desktop/VR production shaders: hidden GPU validation',
  ('UTC: ' + [DateTime]::UtcNow.ToString('yyyy-MM-dd HH:mm:ss')),
  'Perspective-correct dark bark/books/stone; oblique24-bit depth; origins0/4090; autonomous60Hz animation and fixed-world camera reprojection.',
  'Optional native-state JSON reproduces actual server channels and exported eased shader uniforms; otherwise exact default server equations at46% peak are used.',
  'Use -NativeStateDir build/<native smoke run>/native-states to replay actual day/tavern colored-source180-second cycles.',
  'Thresholds: meanRGB dose separation>.008, stationary16s>.0012;60Hz change<.003 synthetic/<.0045 native; source-chroma ratio>=.90; pan error<.001; D24 curvature<.008 and grain/signal<.52.',
  'Native replays use384x256 samples; same-view500ug depth is explicitly labeled when an exact dose depth export is unavailable.',
  'This does not launch Minecraft or test headset comfort.',
  '') | Set-Content -LiteralPath $lsdReport -Encoding utf8
& python (Join-Path $PSScriptRoot 'generate-vr-shaders.py') --check | Tee-Object -FilePath $lsdReport -Append
if ($LASTEXITCODE -ne 0) { throw 'Generated VR parity check failed.' }
$lsdFragmentDir = Join-Path $lsdRoot 'src/main/resources/assets/trippycraft/shaders/program'
foreach ($lsdFragmentName in @('lsd_world_warp.fsh', 'lsd_world_warp_vr.fsh')) {
    $lsdHash = (Get-FileHash -LiteralPath (Join-Path $lsdFragmentDir $lsdFragmentName) -Algorithm SHA256).Hash
    ('SHA256 ' + $lsdFragmentName + ': ' + $lsdHash) | Tee-Object -FilePath $lsdReport -Append
}
if ($CandidateShaderPath) {
    $lsdCandidateHash = (Get-FileHash -LiteralPath $CandidateShaderPath -Algorithm SHA256).Hash
    ('SHA256 TEMP candidate: ' + $lsdCandidateHash) | Tee-Object -FilePath $lsdReport -Append
}
. (Join-Path $PSScriptRoot 'tooling.ps1')
$lsdEnvironment = Get-ToolingEnvironment -RepoRoot $lsdRoot
$lsdClasspath = [string]::Join([IO.Path]::PathSeparator, $lsdEnvironment.gpuClasspath)
& $lsdEnvironment.javacExecutable -cp $lsdClasspath -d $lsdOutput (Join-Path $PSScriptRoot 'ValidateLsdShaders.java')
if ($LASTEXITCODE -ne 0) { throw 'Lsd GPU verification compilation failed.' }
$lsdJavaArgs = @('-cp', ($lsdOutput + [IO.Path]::PathSeparator + $lsdClasspath), 'ValidateLsdShaders', $lsdRoot)
if ($NativeStateDir -or $PreviousShaderPath -or $CandidateShaderPath) {
    $lsdNativeDirectory = if ($NativeStateDir) { (Resolve-Path -LiteralPath $NativeStateDir).Path } else { '' }
    $lsdJavaArgs += $lsdNativeDirectory
}
if ($PreviousShaderPath -or $CandidateShaderPath) {
    $lsdPreviousFragment = if ($PreviousShaderPath) { (Resolve-Path -LiteralPath $PreviousShaderPath).Path } else { '' }
    $lsdJavaArgs += $lsdPreviousFragment
}
if ($CandidateShaderPath) {
    $lsdJavaArgs += (Resolve-Path -LiteralPath $CandidateShaderPath).Path
    if ($CandidateOnly) { $lsdJavaArgs += '--candidate-only' }
}
& $lsdEnvironment.javaExecutable @lsdJavaArgs | Tee-Object -FilePath $lsdReport -Append
if ($LASTEXITCODE -ne 0) { throw 'Lsd GPU verification failed.' }
