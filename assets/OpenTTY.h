/* 
	OpenTTY C2ME API
	


*/

char getenv(char key) { return "$%key"; }
char getuser() { return getenv("USERNAME"); }


// Text 
char append(char text, char buffer) { return "%buffer %text"; }



int main() {
    printf("OpenTTY C2ME API");
}