/*
    OpenTTY C2ME Standard Library
    Author: Mr. Lima
    Version: 1.0
*/

int seed=1162025;


// Environment Keys
char getenv(char key) { return "$%key"; }
char getuser() { return getenv("USERNAME"); }
char hostname() { return getenv("HOSTNAME"); }
char version() { return getenv("VERSION"); }

// Text Handlers
char append(char text, char buffer) { return "%buffer%text"; }


int rand(int max) { seed = (seed * 1103515245 + 12345) % 2147483648; return seed % max; }