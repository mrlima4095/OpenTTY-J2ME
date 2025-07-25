/* 
	OpenTTY C2ME APi
	


*/

char getuser() { return "$USERNAME"; }



// Text API 
char append(char text, char buffer) { return "%buffer %text"; }


char random(int max, int seed) {
    if (seed == 0) { seed = 4095; };

    seed = seed * 1162025 + 12345;

    while (seed > max) {
    	seed = seed / 2;
    };
    return seed;
}


int main() {
	printf("OpenTTY C2ME API");
}