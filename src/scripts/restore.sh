#!/java/bin/sh
# -*- coding: utf-8 -*-
#    
#    . /scripts/restore.sh 
#    
#  Copyright (C) 2024 "Mr. Lima"
#  
#  This script save OpenTTY Console
#  content and load it when you start
#  MIDlet. Note: exit with command 'quit'

execute install nano; get initd; add install nano; add get last-session; add rraw; add get nano; add . /scripts/restore.sh; install initd; get nano;

alias exit=execute install nano; getty; install last-session; get nano; builtin exit; 
