;NSIS Modern User Interface
;Basic Example Script
;Written by Joost Verburg

;--------------------------------
;Include Modern UI

  !include "MUI.nsh"

!define JRE_VERSION "1.5"
!define JRE_VERSION2 "1.6"
!define JRE_VERSION3 "1.7"
!define JRE_VERSION4 "1.8"
!define JRE_VERSION5 "1.9"
!define JRE_VERSION6 "2.0"

!define JRE_URL "http://maciek.tv/alliance/latestjava.exe"

;--------------------------------
;General


  ;Name and file
  Name "Alliance"
  OutFile "Alliance-setup.exe"

; god damn! can't get the freaking icons to work

;  Icon "alliance.ico"
;  UninstallIcon "uninstall.ico"

  ;Default installation folder
  InstallDir "$PROGRAMFILES\Alliance"
  
  ;Get installation folder from registry if available
  InstallDirRegKey HKCU "Software\Alliance" ""


; ------------------- splash --------------------
Function .onInit
        # the plugins dir is automatically deleted when the installer exits
        InitPluginsDir
        File /oname=$PLUGINSDIR\splash.bmp "splash.bmp"
;        File /oname=$PLUGINSDIR\splash.wav "splash.wav"

        advsplash::show 3000 2000 500 -1 $PLUGINSDIR\splash

        Pop $0          ; $0 has '1' if the user closed the splash screen early,
                        ; '0' if everything closed normally, and '-1' if some error occurred.

        Delete $PLUGINSDIR\splash.bmp
;        Delete $PLUGINSDIR\splash.wav
FunctionEnd


Function DetectJRE
  ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" \
             "CurrentVersion"
  StrCmp $2 ${JRE_VERSION} done
  StrCmp $2 ${JRE_VERSION2} done
  StrCmp $2 ${JRE_VERSION3} done
  StrCmp $2 ${JRE_VERSION4} done
  StrCmp $2 ${JRE_VERSION5} done
  StrCmp $2 ${JRE_VERSION6} done
  
  Call GetJRE
  
  done:
FunctionEnd


Function GetJRE
        MessageBox MB_OK "Alliance uses Java, it will now be downloaded. Once downloaded you will be asked to install it. When this is complete the Alliance installation will continue."
 
        StrCpy $2 "$TEMP\Java Runtime Environment.exe"
        nsisdl::download /TIMEOUT=30000 ${JRE_URL} $2
        Pop $R0 ;Get the return value
                StrCmp $R0 "success" +3
                MessageBox MB_OK "Aborting installation: $R0"
                Quit
        ExecWait $2
        Delete $2
FunctionEnd


;--------------------------------
;Interface Settings

  !define MUI_HEADERIMAGE
  !define MUI_HEADERIMAGE_BITMAP "header.bmp" ; optional
  !define MUI_ABORTWARNING
;  !define MUI_FINISHPAGE_RUN "$INSTDIR\alliance.exe"


;--------------------------------
;Pages

  !insertmacro MUI_PAGE_LICENSE "license.txt"
  !insertmacro MUI_PAGE_COMPONENTS
  !insertmacro MUI_PAGE_DIRECTORY
  !insertmacro MUI_PAGE_INSTFILES
  !insertmacro MUI_PAGE_FINISH


  !insertmacro MUI_UNPAGE_CONFIRM
  !insertmacro MUI_UNPAGE_INSTFILES
  
;--------------------------------
;Languages
 
  !insertmacro MUI_LANGUAGE "English"

;--------------------------------
;Installer Sections

Section "" ; empty string makes it hidden, so would starting with -
  SectionIn RO

  call DetectJRE

  SetOutPath "$INSTDIR"
  
  File "alliance.exe"
  File "alliance.dat"
  File "tray.dll"
  File "jnotify.dll"
  
  ;Store installation folder
  WriteRegStr HKCU "Software\Modern UI Test" "" $INSTDIR
  
  ;Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"

  ;Create download diractory
  CreateDirectory "$INSTDIR\downloads"

SectionEnd

Section "Background (Recommended)" SecBG

  CreateShortCut "$SMSTARTUP\Alliance background mode.lnk" "$INSTDIR\alliance.exe" "/min" "$INSTDIR\alliance.exe" 0
  
SectionEnd

Section "Start menu shortcuts" SecSM
  CreateDirectory "$SMPROGRAMS\Alliance"
  CreateShortCut "$SMPROGRAMS\Alliance\Uninstall.lnk" "$INSTDIR\uninstall.exe" "" "$INSTDIR\uninstall.exe" 0
  CreateShortCut "$SMPROGRAMS\Alliance\Alliance.lnk" "$INSTDIR\alliance.exe" "" "$INSTDIR\alliance.exe" 0
SectionEnd


Section "Desktop shortcuts" SecDT
  CreateShortCut "$DESKTOP\Alliance.lnk" "$INSTDIR\alliance.exe"
  CreateShortCut "$DESKTOP\My Alliance Downloads.lnk" "$INSTDIR\downloads"
SectionEnd


;--------------------------------

  ;Assign language strings to sections
  !insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${SecBG} "This MUST be enabled for your alliance to work well. Runs Alliance in the background while you are logged on. Your alliance network will break down if you have this disabled."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecDT} "Creates a shortcuts for starting Alliance and to the Alliance Download folder on your Desktop"
  !insertmacro MUI_DESCRIPTION_TEXT ${SecSM} "Creates shortcuts to Alliance in Start Menu"
  !insertmacro MUI_FUNCTION_DESCRIPTION_END

;--------------------------------
;Uninstaller Section

Section "Uninstall"

  Delete $INSTDIR\alliance.exe
  Delete $INSTDIR\alliance.dat
  Delete "$INSTDIR\Uninstall.exe"

  RMDir "$INSTDIR"

  Delete "$SMPROGRAMS\Alliance\*.*"
  Delete "$SMSTARTUP\Alliance background mode.lnk"
  Delete "$DESKTOP\Alliance.lnk"
  RMDir "$SMPROGRAMS\Alliance"

  DeleteRegKey /ifempty HKCU "Software\Alliance"

SectionEnd

