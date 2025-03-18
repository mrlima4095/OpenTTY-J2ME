#!/java/bin/sh

install nano
proxy raw.githubusercontent.com/mrlima4095/OpenTTY-J2ME/main/assets/lib/jbuntu
install jbuntu

touch

add screen.title=JBuntu Updater
add screen.content=JBuntu have been updated! Restart MIDlet (Recommended)
add screen.back=execute @reload; log add info JBuntu have been updated in current session
add screen.back.label=Reload
add screen.button=Close MIDlet
add screen.button.cmd=execute install nano; touch; get initd; add import jbuntu; add jbuntu; add warn JBuntu updated!; add cp initd-backup initd; add rm initd-backup; cp initd initd-backup; install initd; get nano; reset; exit; 

x11 make nano
get nano
