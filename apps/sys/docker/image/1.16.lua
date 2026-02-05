return {
    ["name"] = "1.16-conteiner",
    ["version"] = "1.16.1",
    ["patch"] = "Absurd Anvil",
    ["release"] = "beta",
    ["lts"] = false,
    ["fs"] = {
        ["/"] = { "mnt/", "home/", "java/", "scripts/" },
        ["/java/"] = { "bin/", "etc/", "lib/", "sounds/" },
        ["/java/bin/"] = { ["java"] = "[ Config ]\n\nname=Java\nversion=1.2", ["sh"] = "[ Config ]\n\nname=OpenTTY\nversion=$VERSION\ndescription=MIDlet Shell\n\nconfig=execute title; cd; buff; clear; echo Welcome to OpenTTY $VERSION; echo Copyright (C) 2025 - Mr. Lima; echo; clear history;" },
        ["/java/etc/"] = { "icons/", ["fstab"] = "/\n/mnt/\n/home/\n/java/\n/java/bin/\n/java/bin/java\n/java/bin/sh\n/java/etc/\n/java/etc/icons/\n/java/etc/icons/app.png\n/java/etc/icons/cursor.png\n/java/etc/icons/dir.png\n/java/etc/icons/exec.png\n/java/etc/icons/file.png\n/java/etc/icons/icon.png\n/java/etc/icons/up.png\n/java/etc/fstab\n/java/etc/help.txt\n/java/etc/initd.sh\n/java/lib/\n/java/lib/jauth2\n/java/lib/netkit\n/java/lib/settings\n/java/lib/yang\n/java/sounds/\n/scripts/\n/scripts/debug.sh", ["help.txt"] = "OpenTTY $VERSION - $PATCH\nCopyright (C) 2026 - Mr. Lima\n[ Commands Overview ]\n\nNetwork API:\n  bind  curl  fw  gaddr  gobuster\n  ifconfig  nc  netstat  prscan\n  ping  pong  query  wget\n\nFile API:\n  about  add  audio  basename  cat\n  cd  chmod  clone  cp  dir  du\n  find  get getopt  getty  grep\n  hash  head  html  install  json\n  ls  lsblk  mkdir  mount  nano\n  open  ph2s pinc  pjnc  pwd  read\n  rm  sed  tail  touch  wc\n\nSession API:\n  login  logout  passwd  sudo  su\n  who  whoami\n\nProcess API:\n  bg  exec  execute  kill  ps\n  sleep  start  stop  htop  top\n\nMIDlet Builtins:\n  alias  builtin  buff  call  case\n  clear  date  debug  echo  env\n  eval  export  false  help  history\n  hostname  hostid  locale  log\n  logcat  man  pkg  report  return\n  set  sh  title  true  tty  uname\n  unalias  unset  warn\n\nOthers API:\n  audio  java  lua prg  wrl  x11\n\n\n[ Manual ]\n\n  To read more usage command 'man [page]'. You can replace PAGE by a command or a default package.\n\n[ Package Manager ]\n\n  Run 'import /java/lib/yang' to load PackJ.\n  Usage: yang\n\n  The downloaded packages will be installed at home folder.\n\n[ Web Project ]\n\nMirror: http://opentty.xyz/\nGithub: https://github.com/mrlima4095/OpenTTY-J2ME", ["initd.sh"] = "#!/java/bin/sh\n# -*- coding: utf-8 -*-\n#\n\nset TTY=/java/optty1\nset HOSTNAME=localhost\n\nset PORT=31522\nset QUERY=nano\n\nmount /java/etc/fstab\n\nx11 init\nx11 term\n\nstart sh" },
        ["/java/etc/icons/"] = { ["app.png"] = "", ["cursor.png"] = "", ["dir.png"] = "", ["exec.png"] = "", ["file.png"] = "", ["icon.png"] = "", ["up.png"] = "", },
        ["/java/lib/"] = { ["jauth2"] = "", ["netkit"] = "", ["settings"] = "", ["yang"] = "" },
        ["/scripts/"] = { ["debug.sh"] = "" },
    },
    ["scope"] = {
        ["PWD"] = "/home/",
        ["USER"] = "guest",
        ["ROOT"] = "/"
    },
    ["password"] = "password"
}