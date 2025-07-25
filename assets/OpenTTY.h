/* 
	OpenTTY C2ME APi
	


*/

char getuser() { return "$USERNAME"; }



// Text API 
char append(char text, char buffer) { return "%buffer %text"; }


int random(int max, int seed) {
    int i;
    if (seed != 0) { i = seed; } 
    else { i = 1; };

    i = i * 1103515245 + 12345;

    int q = i / max;
    int r = i - (q * max);

    return r;
}


int main() {
	printf("OpenTTY C2ME API");
}