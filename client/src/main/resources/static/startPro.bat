@echo off
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Internet Settings" /v ProxyEnable /t REG_DWORD /d 1 /f
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Internet Settings" /v ProxyServer /d "127.0.0.1:9091" /f
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Internet Settings" /v ProxyOverride /t REG_SZ /d "*.cnpc;*.petrochina;10.*.*.*;11.*.*.*;localhost*;<local>" /f
ipconfig /flushdns
start "" "C:\Program Files\Internet Explorer\iexplore.exe"
taskkill /f /im iexplore.exe
