; Script Inno Setup usato dalla pipeline GitHub Actions (.github/workflows/release.yml) per generare
; l'installer Windows. AppId, AppName e DefaultDirName sono identici alla copia usata per le build
; locali (InstallerGiacenzeCrypto/InnoScript.iss, fuori da questo repository): è quello che garantisce
; che l'installer prodotto qui possa aggiornare in-place un'installazione fatta con l'altra copia,
; e viceversa. Se cambi questi tre valori qui, cambiali anche là.

#define MyAppName "Giacenze_Crypto"
#define MyAppPublisher "Luca Passelli"
#define MyAppURL "https://sourceforge.net/projects/giacenze-crypto-com/"
#define MyAppExeName "Giacenze_Crypto.exe"

[Setup]
AppId={{B80B628D-FA70-40DB-9527-0B5768294F53}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={autopf}\{#MyAppName}
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=commandline
OutputDir={#SourcePath}Output
OutputBaseFilename=Giacenze_Crypto
SetupIconFile={#SourcePath}..\logo.ico
Compression=lzma
SolidCompression=yes
WizardStyle=modern

[Languages]
Name: "italian"; MessagesFile: "compiler:Languages\Italian.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "{#SourcePath}Giacenze_Crypto\{#MyAppExeName}"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#SourcePath}Giacenze_Crypto\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent
