/* 
	OpenTTY C2ME APi
	


*/

char getuser() { return "$USERNAME"; }



// Text API 
char append(char text, char buffer) { return "%buffer %text"; }


int call(int phone) { return exec("call %phone"); }

int main() {
	printf("OpenTTY C2ME API");
}