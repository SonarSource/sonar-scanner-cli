$ErrorActionPreference = 'Stop'

function Install-Chocolatey {
  # Run the installer.
  Set-ExecutionPolicy Bypass -Scope Process -Force; Invoke-Expression ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))
}

function Install-NodeJs {
  choco install -y nodejs
}

function Install-Buildtools {
  $path = "${env:Temp}\buildTools.zip"

  # Fetch the build tools archive.
  [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
  (New-Object System.Net.WebClient).DownloadFile('https://github.com/SonarSource/buildTools/archive/docker.zip', $path)

  # Extract the archive to the C drive.
  Add-Type -AssemblyName System.IO.Compression.FileSystem
  [System.IO.Compression.ZipFile]::ExtractToDirectory($path, 'C:\')

  # Update global PATH.
  $currentPath = (Get-ItemProperty -Path 'Registry::HKEY_LOCAL_MACHINE\System\CurrentControlSet\Control\Session Manager\Environment' -Name PATH).Path
  $updatedPath = $currentPath+';C:\buildTools-docker\bin'
  Set-ItemProperty -Path 'Registry::HKEY_LOCAL_MACHINE\System\CurrentControlSet\Control\Session Manager\Environment' -Name PATH -Value $updatedPath

  # Remove archive.
  del $path
}

function Install-Maven {
  choco install -y openjdk11 --version 11.0.4.11
  choco install -y maven --version 3.6.2
}

function Install-Git {
  # We use Git to enable Unix Tools. This will allow us to use Bash-style
  # commands in .cirrus.yml, like "source".
  choco install -y git --version 2.23.0 --package-parameters "/GitAndUnixToolsOnPath"
}

Write-Host "Install chocolatey"
Install-Chocolatey

Write-Host "Install Maven"
Install-Maven

Write-Host "Install NodeJs"
Install-NodeJs

Write-Host "Install Unix Tools"
Install-Git

Write-Host "Set up build tools"
Install-Buildtools

# Disable antivirus analysis on C drive.
Write-Host "Finalize VM configuration"
Set-MpPreference -ScanAvgCPULoadFactor 5 -ExclusionPath "C:\"
