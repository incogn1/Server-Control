# Server-Control
_**Native Velocity plugin version:** 3.3.0-SNAPSHOT_

A Minecraft Velocity plugin to manage multiple backend servers through a list of simple commands. Define startup scripts for the servers on your network to allow players in game to start them up (from a fully shut down state) without your intervention!

## Why this plugin?
This plugin was made to allow players on a Velocity server network to start backend Minecraft servers without the need for manual intervention by a server admin, even if the backend server is completely shut down. This is done by defining a batch script or any other preferred program that will run the servers run.bat file. When an in-game player uses the startup or join commands, the server will be started up without the need for any admin to manually boot the server.

## Commands:
- **/sc help** - Shows a help menu
- **/sc list** - Returns a list of all the servers on the network
- **/sc info [server]** - Shows some details about the provided server
- **/sc start [server]** - Runs the startup script for the given server (if defined)
- **/sc join [server]** - Tries to redirect the player running the command to the given server. If the server is not online, the startup script will be ran and the player will be redirected to the server once it has started up.
- **/sc cancel_join** - Cancels any pending "delayed joins" as a result of using the /sc join command
- **/sc run_as [player] [command]** - Runs a Server Control command as another player but runs the permission checks through the player running the run_as command. This would for example allow you to send a player to a specific server even if they do not have the required permissions.


## Permissions:
- **servercontrol.base** - The basic permission to do anything with the plugin, if a player doesn't have this, they cannot use /sc.
- **servercontrol.info** - Permission to run the /sc info command
- **servercontrol.list** - Permission to run the /sc list command
- **servercontrol.start** - Basic permission needed to start any server with /sc start. If a player has this permission, they cannot yet start a server, they will also need the permission for the specific server itself. (see below)
- **servercontrol.start.<server>** - Allows the player to start the given server with /sc start <server>
- **servercontrol.join** - Basic permission needed to join any server with /sc join as well as permission to use /sc cancel_join. If a player has this permission, they cannot yet start a server, they will also need the permission for the specific server itself. (see below)
- **servercontrol.join.<server>** - Allows the player to join the given server with /sc start <server>
- **servercontrol.run_as** - Permission to run the /sc run_as command. The permission check for the command that is executed is handled normally as if the original user of the run_as command (so not the player the command is run for) would've used the command. This means that a player with join permissions for a specific server can make a player without those permissions still join that server if they have the run_as permission.

## Questions or feature requests?
Please use the github repo for this plugin for any support, feature requests, etc. as I will likely not read this Modrinth page much.

## Config
The config should be self explanatory however if you do not know how to setup a script file, here is an example. Paste it in a file within the scripts folder of the plugin and name the file according to the script-pattern setting in the config.yml so the plugin can locate it.

<details>
<summary>Example server startup script</summary>

```
@echo off
setlocal enabledelayedexpansion

:: ---
:: Settings
:: ---

:: Unique server name (used to check if the server isn't already running)
set serverName=my_server_name

:: The batch file containing the startup script of the minecraft server
set batchDir=..\..\..\..\servers\my_server_folder
set batchFile=run.bat




:: !!!
:: Do not change anything below this line, unless you know what you are doing of course :)
:: !!!

set "windowName=[server-control] %serverName%"
set outFile=temp_%serverName%.tmp

:: ---
:: Check if server isn't already running
:: ---
powershell -Command "(Get-Process -Name cmd, powershell | Where-Object {$_.MainWindowTitle -eq '%windowName%'}).Count" | findstr /C:"0" >nul
if errorlevel 1 (
  echo Server process window is already running, has it crashed?
  exit /b 10
)

:: ---
:: Start the batchFile in batchDir
:: ---
set "originalDir=%cd%"
pushd "%batchDir%"
start /min "%windowName%" cmd /c "%batchFile%
popd

exit /b 0
```

</details>

## Quick security note!
Only use scripts that you yourself have created or that you have received from trusted sources. Scripting languages such as batch can be used to do harm to your system.

## License
This project is licensed under the GNU General Public License v3.0 License - see the [LICENSE](LICENSE) file for details.

