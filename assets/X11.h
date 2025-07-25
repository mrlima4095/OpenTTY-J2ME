/* 
	Graphics C2ME API
	
*/

#include "OpenTTY.h"


int title(char text) { return exec("title %text"); }
int WindowTitle(char text) { return exec("x11 title %text"); }

int alert(char text) { return exec("warn %text"); }
int gauge(char text) { return exec("gauge %text"); }
int tick(char text) {
	if (text == "hide") { return exec("tick"); } 
	else { return exec("tick %text"); }
}


