param(
    [Parameter(Mandatory=$true)]
    [ValidateSet('CE','EE')]
    [string]$Edition,

    [switch]$DebugLib
)

$RootDir="$PSScriptRoot\..\.."
$CoreDir="$RootDir\common\lite-core"

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

Remove-Item .\.penv -Force  -Recurse -ErrorAction SilentlyContinue
python -m venv .\.penv
.\.penv\Scripts\activate

pip -q install GitPython

Write-Host "=== Fetching artifacts for Core-${CoreVersion} on Win64"
New-Item -Type directory -ErrorAction Ignore "${CoreDir}"
python "${RootDir}\core_tools\fetch_litecore_version.py" "${DebugOpt}" -x "${RootDir}\etc\core" -b "${CoreVersion}" -v windows-win64 $DebugOpt -o "${CoreDir}"

deactivate
Remove-Item .\.penv -Force  -Recurse -ErrorAction SilentlyContinue

# Ugly little kludge to get the DLL to the right place
Copy-Item "${CoreDir}\windows\x86_64\bin\*.dll" -Destination "${CoreDir}\windows\x86_64\lib"

