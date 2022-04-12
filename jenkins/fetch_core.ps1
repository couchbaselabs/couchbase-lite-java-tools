param(
    [Parameter(Mandatory=$true)]
    [ValidateSet('CE','EE')]
    [string]$Edition,

    [switch]$DebugLib
)

$RootDir="$PSScriptRoot/../.."
$CoreDir="$RootDir/common/lite-core"

$DebugOpt = ""
if($DebugLib) {
    $DebugOpt = "-d"
}

$CoreVersion = Get-Content "$RootDir\core_version.txt"
if($CoreVersion -eq $null) {
    Write-Host "Cannot get core version"
    exit 1
}

if($Edition -eq 'EE') {
    $CoreVersion = "${CoreVersion}-EE"
}

Remove-Item ./.penv -Force  -Recurse -ErrorAction SilentlyContinue
python3 -m venv ./.penv
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser -Force
.\.penv\Scripts\Activate.ps1

pip -q install GitPython

Write-Host "=== Fetching artifacts for Core-${CoreVersion}-${Edition} on Win64"
New-Item -Type directory -ErrorAction Ignore $OutputDir
python "${RootDir}/core_tools/fetch_litecore_version.py" -x "${RootDir}/etc/core" -b "${CoreVersion}" -v windows-win64 $DebugOpt -o "${OutputDir}"

Deactivate
Remove-Item ./.penv -Force  -Recurse -ErrorAction SilentlyContinue

