--[[

config=execute touch /home/.yang-lock; case !key (REPO) set REPO=opentty.xyz:31522; 
command=yang,setrepo

yang=execute x11 list /java/lib/yang;
setrepo=execute set VALUE=REPO; set LABEL=IP Adress (OpenTTY Server); export RETURN; cfg run;

shell.name=yang
shell.args=install,update,query,setrepo,info

install=execute case file (.yang-lock) exec rm /home/.yang-lock & set OLD_QUERY=$QUERY & set QUERY=/home/$RESOURCE & tick Installing... & query socket://$REPO get lib/$RESOURCE & tick & set QUERY=$OLD_QUERY & unset OLD_QUERY & unset RESOURCE & touch /home/.yang-lock; case !file (.yang-lock) exec log add error Yang - Broken pipe (Blocked duplicated) & echo [ Yang ] Command failed! & echo [ Yang ] See logs to more info.;
update=execute case file (.yang-lock) exec rm /home/.yang-lock & set OLD_QUERY=$QUERY & set QUERY=/home/yang & tick Updating... & query socket://$REPO get lib/yang & tick & set QUERY=$OLD_QUERY & unset OLD_QUERY & touch /home/.yang-lock & cd & import /home/yang; case !file (.yang-lock) exec log add error Yang - Broken pipe (Blocked duplicated) & echo [ Yang ] Command failed! & echo [ Yang ] See logs to more info.;
query=execute x11 quest /java/lib/yang;

info=execute echo PackJ 1.4 (Default);

]]

local repo = {
    ["Android ME"] = "android",
    ["Armitage"] = "armitage",
    ["Auto Clean"] = "autogc",
    ["Auto Syntax"] = "tab",
    ["Back Previous"] = "bprevious",
    ["BoxME"] = "boxme",
    ["CMatrix"] = "cmatrix",
    ["Discord (MIDlet)"] = "http://146.59.80.3/discord_midp2_beta.jar",
    ["Forge"] = "forge",
    ["Github (MIDlet)"] = "http://nnp.nnchan.ru/dl/GH2ME.jar",
    ["GoBuster (Word list)"] = "gobuster",
    ["Graphics (Lua)"] = "graphics.lua",
    ["ImmersiveShell"] = "sh2me",
    ["JBuntu"] = "jbuntu",
    ["JBenchmark"] = "debuggers",
    ["J2ME Loader"] = "modme",
    ["Math (Lua)"] = "math.lua",
    ["MobiX Loader"] = "mxos",
    ["PackJ (Update)"] = "yang",
    ["PackJ (Proxy)"] = "yang-proxy",
    ["PasteBin"] = "pastebin",
    ["SmartME SDK"] = "sdkme",
    ["Updater"] = "sync",
    ["ViaVersion"] = "viaversion"
}

local function install(pkg)
    if string.match(pkg, "MIDlet") then
        os.execute("") os.exit
    end
end