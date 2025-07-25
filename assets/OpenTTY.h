/* 
	OpenTTY C2ME APi
	


*/

// Text API 
char append(char text, char buffer) { return "%buffer %text"; }

// MIDlet
int ttysize() { return len(open("stdout")); }
int clear() { return exec("clear stdout"); }

int main() {
	printf("OpenTTY C2ME API");
}