# Shared Windows development tooling. Keep generated machine paths under build/.
function Get-DevelopmentJava {
    if ($env:JAVA_HOME) {
        $taskJava = Join-Path $env:JAVA_HOME 'bin/java.exe'
        if (-not (Test-Path -LiteralPath $taskJava)) { throw 'JAVA_HOME must point to an installed Java 21 JDK.' }
        return $taskJava
    }
    $taskCommand = Get-Command java.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $taskCommand) { throw 'Install a Java 21 JDK and set JAVA_HOME or add its bin directory to PATH.' }
    return $taskCommand.Source
}

function Invoke-ProjectGradle {
    param([string]$RepoRoot, [string[]]$GradleArgs)
    $taskJava = Get-DevelopmentJava
    $taskWrapper = Join-Path $RepoRoot 'gradle/wrapper/gradle-wrapper.jar'
    Push-Location -LiteralPath $RepoRoot
    try {
        # Direct Java invocation also supports ampersands in Windows project paths.
        & $taskJava '-Xms64m' '-Xmx64m' '-classpath' $taskWrapper 'org.gradle.wrapper.GradleWrapperMain' @GradleArgs
        if ($LASTEXITCODE -ne 0) { throw "Gradle failed ($LASTEXITCODE)." }
    } finally {
        Pop-Location
    }
}

function Get-ToolingEnvironment {
    param([string]$RepoRoot)
    Invoke-ProjectGradle -RepoRoot $RepoRoot -GradleArgs @('writeToolingEnvironment', '--console=plain') | Out-Host
    $taskEnvironment = Get-Content -LiteralPath (Join-Path $RepoRoot 'build/tooling/environment.json') -Raw | ConvertFrom-Json
    foreach ($taskExecutable in @($taskEnvironment.javaExecutable, $taskEnvironment.javacExecutable)) {
        if (-not (Test-Path -LiteralPath $taskExecutable)) { throw "Missing resolved Java toolchain executable: $taskExecutable" }
    }
    return $taskEnvironment
}
