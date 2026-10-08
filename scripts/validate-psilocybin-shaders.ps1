$ErrorActionPreference = 'Stop'
$psiloRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$psiloReport = Join-Path $psiloRoot 'build/psilocybin-shader-validation.log'
$psiloOutput = Join-Path $psiloRoot 'build/psilocybin-shader-validation'
New-Item -ItemType Directory -Path $psiloOutput -Force | Out-Null
@('Psilocybin desktop/VR production shaders: hidden GPU validation',
  ('UTC: ' + [DateTime]::UtcNow.ToString('yyyy-MM-dd HH:mm:ss')),
  'Synthetic texture/depth inputs; this does not launch Minecraft or test headset comfort.',
  '') | Set-Content -LiteralPath $psiloReport -Encoding utf8
& python (Join-Path $PSScriptRoot 'generate-vr-shaders.py') --check | Tee-Object -FilePath $psiloReport -Append
if ($LASTEXITCODE -ne 0) { throw 'Generated VR parity check failed.' }
. (Join-Path $PSScriptRoot 'tooling.ps1')
$psiloEnvironment = Get-ToolingEnvironment -RepoRoot $psiloRoot
$psiloClasspath = [string]::Join([IO.Path]::PathSeparator, $psiloEnvironment.gpuClasspath)
& $psiloEnvironment.javacExecutable -cp $psiloClasspath -d $psiloOutput (Join-Path $PSScriptRoot 'ValidatePsilocybinShaders.java') (Join-Path $psiloRoot 'src/main/java/com/trippycraft/substance/psilocybin/PsilocybinDoseCurve.java') (Join-Path $psiloRoot 'src/main/java/com/trippycraft/substance/psilocybin/PsilocybinPhase.java')
if ($LASTEXITCODE -ne 0) { throw 'Psilocybin GPU verification compilation failed.' }
& $psiloEnvironment.javaExecutable -cp ($psiloOutput + [IO.Path]::PathSeparator + $psiloClasspath) ValidatePsilocybinShaders $psiloRoot | Tee-Object -FilePath $psiloReport -Append
if ($LASTEXITCODE -ne 0) { throw 'Psilocybin GPU verification failed.' }
