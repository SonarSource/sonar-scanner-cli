
function FetchAndUnzip
{
        param ([string]$Url, [string]$Out)

        $tmp = [System.IO.Path]::GetTempFileName()
        [System.Reflection.Assembly]::LoadWithPartialName('System.Net.Http') | Out-Null
        $client = (New-Object System.Net.Http.HttpClient)
        try
        {
                if (-not([string]::IsNullOrEmpty($env:GITHUB_TOKEN)))
                {
                        $credentials = [string]::Format([System.Globalization.CultureInfo]::InvariantCulture, "{0}:", $env:GITHUB_TOKEN);
                        $credentials = [Convert]::ToBase64String([System.Text.Encoding]::ASCII.GetBytes($credentials));
                        $client.DefaultRequestHeaders.Authorization = (New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Basic", $credentials));
                }
                $contents = $client.GetByteArrayAsync($url).Result;
                [System.IO.File]::WriteAllBytes($tmp, $contents);
        }
        finally
        {
                $client.Dispose()
        }

        if (-not(Test-Path $Out))
        {
                mkdir $Out | Out-Null
        }
        [System.Reflection.Assembly]::LoadWithPartialName('System.IO.Compression.FileSystem') | Out-Null
        [System.IO.Compression.ZipFile]::ExtractToDirectory($tmp, $Out)
}

function InstallAppveyorTools
{
        $travisUtilsVersion = "57"
        $localPath = "$env:USERPROFILE\.local"
        $travisUtilsPath = "$localPath\travis-utils-$travisUtilsVersion"
        if (Test-Path $travisUtilsPath)
        {
                echo "Reusing the Travis Utils version $travisUtilsVersion already downloaded under $travisUtilsPath"
        }
        else
        {
                $url = "https://github.com/SonarSource/travis-utils/archive/v$travisUtilsVersion.zip"
                echo "Downloading Travis Utils version $travisUtilsVersion from $url into $localPath"
                FetchAndUnzip $url $localPath
        }

        $mavenLocalSettings = "$env:USERPROFILE\.m2\settings.xml"
        echo "Installating settings.xml into $mavenLocalSettings"
        Copy-Item "$travisUtilsPath\m2\settings-public.xml" $mavenLocalSettings -Force -Recurse

        $env:ORCHESTRATOR_CONFIG_URL = ""
        $env:TRAVIS = "ORCH-332"
}


function CheckLastExitCode
{
    param ([int[]]$SuccessCodes = @(0))

    if ($SuccessCodes -notcontains $LastExitCode)
	{
        $msg = @"
EXE RETURNED EXIT CODE $LastExitCode
CALLSTACK:$(Get-PSCallStack | Out-String)
"@
        throw $msg
    }
}

InstallAppveyorTools
mvn verify "--batch-mode" "-B" "-e" "-V"
CheckLastExitCode
