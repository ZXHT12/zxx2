# ZXHT Client (Fabric, Minecraft Java 1.21.11)

A PvP-themed visual client: black/white glass UI, ZXHT logo, account manager, wings, player rings,
step/jump logos, calm sounds, HUD and a performance mode. **No cheats.**

## Build
Needs Java 21 and internet access (Gradle downloads Minecraft + Fabric).

    ./gradlew build          # Windows: gradlew.bat build

The jar is `build/libs/zxht-client-1.0.0.jar`. Put it in your `.minecraft/mods` folder with
**Fabric Loader 0.18.1+** and **Fabric API 0.141.6+1.21.11** (from fabricmc.net / modrinth.com).

## Use
- **Right Shift** opens ZXHT Settings (rebindable in Controls).
- Title screen: Singleplayer, Multiplayer, ZXHT Settings, Accounts. Your skin is shown small on the left.
- **Accounts**: add an *Offline* account (any name 3-16 chars) or a *Microsoft* account (several allowed).
  Offline accounts only work on offline-mode servers/LAN. Servers with online-mode need a Microsoft account.

### Microsoft login setup (one time)
1. Go to portal.azure.com -> *App registrations* -> *New registration*.
2. Supported account types: "Personal Microsoft accounts only".
3. Under *Authentication* enable **"Allow public client flows"**.
4. Copy the *Application (client) ID* into `config/zxht-client.json` as `"msaClientId"`.
5. In Accounts click *Add Microsoft*, open the shown link and enter the code (it is copied for you).
Tokens are stored in plain text in `config/zxht-accounts.json`. Never share that file.

## Settings
Wings, step/jump logo, player rings, hit/kill/click sounds, HUD (FPS/CPS/ping), glass buttons,
performance mode. Everything is saved to `config/zxht-client.json`.

## Not included
- True motion blur (needs a custom post-processing shader).
- Anything that gives a combat advantage.

## Get the jar without installing anything
1. Create a free GitHub repository and upload all files of this folder (keep `.github/workflows/build.yml`).
2. Open the **Actions** tab -> *Build ZXHT Client* -> wait ~3 minutes.
3. Download **zxht-client-jar** from the finished run. That zip contains the mod jar.
