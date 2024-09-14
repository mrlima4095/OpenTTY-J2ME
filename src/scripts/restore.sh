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

execute install nano; rnano; add install nano; add load last-session; add rraw; add load nano; add . /scripts/restore.sh; install initd; load nano;

alias exit=execute install nano; getty; install last-session; load nano; quit; 
