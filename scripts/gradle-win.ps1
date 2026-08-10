param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GradleArguments = @("build")
)

$projectRoot = Split-Path -Parent $PSScriptRoot
$hasNonAsciiPath = $projectRoot -match '[^\x00-\x7F]'

if (-not $hasNonAsciiPath) {
    & (Join-Path $projectRoot "gradlew.bat") @GradleArguments
    exit $LASTEXITCODE
}

$driveLetter = $null
foreach ($candidate in @("Z:", "Y:", "X:", "W:", "V:")) {
    if (-not (Test-Path -LiteralPath "$candidate\")) {
        $driveLetter = $candidate
        break
    }
}

if ($null -eq $driveLetter) {
    throw "No free drive letter is available for the Windows Gradle path workaround."
}

subst $driveLetter $projectRoot
if ($LASTEXITCODE -ne 0) {
    throw "Could not map $driveLetter to $projectRoot."
}

$exitCode = 1
try {
    Push-Location "$driveLetter\"
    & ".\gradlew.bat" @GradleArguments
    $exitCode = $LASTEXITCODE
}
finally {
    Pop-Location
    subst $driveLetter /D
}

exit $exitCode
