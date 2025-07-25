/* 
	OpenTTY C2ME APi
	


*/

char getuser() { return "$USERNAME"; }



// Text API 
char append(char text, char buffer) { return "%buffer %text"; }


int random(int max, int seed) {
    if (seed == 0) { seed = 4095; };

    seed = seed * 1162025 + 12345;

    return (seed / max) * (seed / 8);
}


int main() {
	printf("OpenTTY C2ME API");
}