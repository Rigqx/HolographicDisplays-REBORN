param([string]$Version = '26.3', [string]$ServerDirectory = '', [switch]$NoCompile,
      [string]$ServerPackage = '', [switch]$Spigot, [string]$JavaExecutable = 'java')
$ErrorActionPreference = 'Stop'
$workspace = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
if (-not $ServerDirectory) { $ServerDirectory = Join-Path $workspace ".build/server-$Version" }
$jars = @((Join-Path $workspace 'plugin/target/HolographicDisplays-REBORN-3.0.6.jar'))
$jars += @(Get-ChildItem (Join-Path $ServerDirectory 'versions') -Recurse -Filter '*.jar' | ForEach-Object FullName)
$jars += @(Get-ChildItem (Join-Path $ServerDirectory 'libraries') -Recurse -Filter '*.jar' | ForEach-Object FullName)
$classpath = $jars -join ';'
$classes = Join-Path $workspace '.build/probe-classes'
New-Item -ItemType Directory -Force $classes | Out-Null
if (-not $NoCompile) {
    & javac --release 8 -classpath $classpath -d $classes (Join-Path $PSScriptRoot 'PacketProbe.java')
    if ($LASTEXITCODE -ne 0) { throw 'Probe compilation failed' }
}
if ($ServerPackage) {
    $marker = Join-Path $workspace '.build/ProbeServerMarker.java'
    "package $ServerPackage; interface ProbeServerMarker {}" | Set-Content $marker
    & javac --release 8 -d $classes $marker
    if ($LASTEXITCODE -ne 0) { throw 'Marker compilation failed' }
}
Push-Location $ServerDirectory
try {
    $arguments = @($Version)
    if ($ServerPackage) { $arguments += $ServerPackage }
    if ($Spigot) { $arguments += 'spigot' }
    & $JavaExecutable -classpath "$classes;$classpath" org.bukkit.craftbukkit.PacketProbe @arguments *> probe.log
    $probeExit = $LASTEXITCODE
    Get-Content probe.log -Tail 24
    if ($probeExit -ne 0) { throw "Native packet probe failed: $Version" }
    Get-Content probe-result.txt
} finally { Pop-Location }
