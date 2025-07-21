/* 
	OpenTTY C2ME APi
	


*/

int root() { char user = exec("whoami"); if (user == "root") { return 0; } else { return 1; } }
char getuser() { return "$USERNAME"; }


int main() {
	printf("OpenTTY C2ME API");
}