/*
    Network C2ME Library
    Author: Mr. Lima
    Version: 1.0
*/

#include "OpenTTY.h"

int Query(char url, char file) {
    exec("execute set OLD_QUERY=$QUERY; set QUERY=%file;");
    exec("execute query %url; set QUERY=$OLD_QUERY; unset OLD_QUERY; ");
}