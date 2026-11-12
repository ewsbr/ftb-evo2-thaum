# Runs the Thaumaturge restore with the first Java 17+ found: JAVA_HOME, PATH, then launcher-bundled runtimes.
# All arguments go to the installer; see .\install.cmd --help.
$ErrorActionPreference = 'Stop'

$jar = Join-Path $PSScriptRoot 'evo2-thaumaturge-restore.jar'
if (-not (Test-Path -LiteralPath $jar)) { $jar = Join-Path $PSScriptRoot 'dist\evo2-thaumaturge-restore.jar' }
if (-not (Test-Path -LiteralPath $jar)) { Write-Error "evo2-thaumaturge-restore.jar not found next to $PSCommandPath"; exit 1 }

# Windows PowerShell 5.1 turns redirected native stderr into error records, and $ErrorActionPreference = 'Stop'
# makes the first one fatal, so `& java -version 2>&1` would end the script. Unverified: found in code review, not
# yet reproduced on Windows. Reading the process's stderr directly avoids PowerShell's stream handling.
function Get-JavaMajor([string] $java) {
    try {
        $psi = New-Object System.Diagnostics.ProcessStartInfo $java, '-version'
        $psi.UseShellExecute = $false
        $psi.RedirectStandardError = $true
        $psi.RedirectStandardOutput = $true
        $p = [System.Diagnostics.Process]::Start($psi)
        $out = $p.StandardError.ReadToEnd() + $p.StandardOutput.ReadToEnd()
        $p.WaitForExit()
    } catch {
        return 0
    }
    if ($out -match 'version "(\d+)') { return [int] $Matches[1] }
    return 0
}

$candidates = New-Object System.Collections.Generic.List[string]
if ($env:JAVA_HOME) { $candidates.Add((Join-Path $env:JAVA_HOME 'bin\java.exe')) }
$onPath = Get-Command java.exe -ErrorAction SilentlyContinue
if ($onPath) { $candidates.Add($onPath.Source) }
$patterns = @(
    "$env:APPDATA\PrismLauncher\java\*\bin\java.exe",
    "$env:LOCALAPPDATA\.ftba\bin\runtime\*\bin\java.exe",
    "$env:LOCALAPPDATA\.ftba\runtime\*\bin\java.exe",
    "$env:LOCALAPPDATA\Packages\Microsoft.4297127D64EC6_8wekyb3d8bbwe\LocalCache\Local\runtime\java-runtime-*\windows-x64\java-runtime-*\bin\java.exe",
    "${env:ProgramFiles(x86)}\Minecraft Launcher\runtime\java-runtime-*\windows-x64\java-runtime-*\bin\java.exe",
    "$env:USERPROFILE\curseforge\minecraft\Install\runtime\java-runtime-*\windows-x64\java-runtime-*\bin\java.exe",
    "$env:APPDATA\ModrinthApp\meta\java_versions\*\bin\java.exe",
    "$env:ProgramFiles\Eclipse Adoptium\*\bin\java.exe",
    "$env:ProgramFiles\Java\*\bin\java.exe",
    "$env:ProgramFiles\Microsoft\jdk-*\bin\java.exe"
)
foreach ($p in $patterns) {
    Get-ChildItem -Path $p -ErrorAction SilentlyContinue | Sort-Object FullName -Descending | ForEach-Object { $candidates.Add($_.FullName) }
}

$java = $null
foreach ($c in $candidates) {
    if ((Test-Path -LiteralPath $c) -and (Get-JavaMajor $c) -ge 17) { $java = $c; break }
}
if (-not $java) {
    Write-Error 'No Java 17 or newer found. Install one (for example from adoptium.net) or set JAVA_HOME.'
    exit 1
}

& $java -jar $jar @args
exit $LASTEXITCODE
