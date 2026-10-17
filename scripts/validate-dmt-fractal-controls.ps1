$ErrorActionPreference = 'Stop'
$taskRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
. (Join-Path $PSScriptRoot 'tooling.ps1')
$taskEnvironment = Get-ToolingEnvironment -RepoRoot $taskRoot
$taskOutput = Join-Path $taskRoot 'build/dmt-fractal-control-validation'
New-Item -ItemType Directory -Path $taskOutput -Force | Out-Null
# Java source mode compiles the harness; its JavaCompiler then compiles the actual
# production controller/blend and world sampler sources against isolated dependency doubles.
& $taskEnvironment.javaExecutable (Join-Path $PSScriptRoot 'ValidateDmtFractalControls.java') $taskRoot |
    Tee-Object -FilePath (Join-Path $taskOutput 'validation.log')
if ($LASTEXITCODE -ne 0) { throw 'DMT fractal controller validation failed.' }
