/*
    Graphics C2ME Library
    Author: Mr. Lima
    Version: 1.0
*/

#include "OpenTTY.h"

int Title(char title) { exec("title %title"); }
int WindowTitle(char title) { exec("x11 title %text"); }
int Alert(char msg) { exec("warn %msg"); }

int SaveWindow(char name) { exec("x11 set %name"); }
int LoadWindow(char name) { exec("x11 load %name"); }

int SetTicker(char msg) { exec("x11 tick %msg"); }

