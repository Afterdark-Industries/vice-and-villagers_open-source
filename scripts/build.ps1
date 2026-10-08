param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GradleArgs = @('build')
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'tooling.ps1')
$taskRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Invoke-ProjectGradle -RepoRoot $taskRoot -GradleArgs $GradleArgs
