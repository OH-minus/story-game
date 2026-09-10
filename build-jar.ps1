$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$buildDirectory = Join-Path $projectRoot "out\jar-build"
$distributionDirectory = Join-Path $projectRoot "dist"
$jarPath = Join-Path $distributionDirectory "storyweave.jar"
$temporaryJarPath = Join-Path $distributionDirectory "storyweave.building.jar"

Remove-Item $buildDirectory -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $buildDirectory, $distributionDirectory | Out-Null

if (Test-Path $jarPath) {
    try {
        $jarLockCheck = [System.IO.File]::Open($jarPath, "Open", "ReadWrite", "None")
        $jarLockCheck.Dispose()
    } catch {
        throw "Cannot replace '$jarPath'. Stop any running Storyweave server using this JAR, then run the build again."
    }
}

$sources = (Get-ChildItem (Join-Path $projectRoot "src\backend\storyweave\*.java")).FullName
javac --release 21 --add-modules jdk.httpserver -d $buildDirectory $sources
if ($LASTEXITCODE -ne 0) {
    throw "Java compilation failed"
}

Copy-Item (Join-Path $projectRoot "src\frontend") $buildDirectory -Recurse
New-Item -ItemType Directory -Force (Join-Path $buildDirectory "backend\prompts") | Out-Null
Copy-Item (Join-Path $projectRoot "src\backend\prompts\*") (Join-Path $buildDirectory "backend\prompts")

Remove-Item $temporaryJarPath -Force -ErrorAction SilentlyContinue
jar --create --file $temporaryJarPath --main-class backend.storyweave.Main -C $buildDirectory .
if ($LASTEXITCODE -ne 0) {
    throw "JAR packaging failed"
}
Move-Item $temporaryJarPath $jarPath -Force

Write-Host "Built $jarPath"