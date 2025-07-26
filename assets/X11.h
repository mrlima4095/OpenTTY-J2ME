/*
    Graphics C2ME Library
    Author: Mr. Lima
    Version: 1.0
*/

#include "OpenTTY.h"

int WindowTitle(char text) {
    return exec("x11 title %text");
}
int Alert(char msg) {
    return exec("warn %msg");
}

int SaveWindow(char name) {
    return exec("x11 set %name");
}
int LoadWindow(char name) {
    return exec("x11 load %name");
}

int SetTicker(char msg) {
    return exec("x11 tick %msg");
}

