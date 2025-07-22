/* 
	OpenTTY C2ME APi
	


*/

char getuser() { return "$USERNAME"; }



// Text API 
char append(char text, char buffer) { return "%buffer %text"; }
char trim(char text) {  }
int length(char text) {  }


int call(int phone) { return exec("call %phone"); }
int exec(char cmd) { exec("%cmd"); return getenv("OUTPUT"); }

char getenv(char key) {  }

int main() {
	printf("OpenTTY C2ME API");
}