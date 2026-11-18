param([Parameter(Mandatory)][string] $Launcher)

$exe = (Get-Item -LiteralPath $Launcher -ErrorAction Stop).FullName
$key = 'HKCU:\Software\Classes\klyx'
New-Item -Path "$key\shell\open\command" -Force | Out-Null
Set-Item -Path $key -Value 'URL:Klyx'
New-ItemProperty -Path $key -Name 'URL Protocol' -Value '' -Force | Out-Null
Set-Item -Path "$key\shell\open\command" -Value ('"{0}" "%1"' -f $exe)
