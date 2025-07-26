/*
    OpenTTY C2ME Standard Library
    Author: Mr. Lima
    Version: 1.0
*/

// Environment Keys
char setenv(char key, char value) { return exec("set %key=%value") }
char getuser() { return getenv("USERNAME"); }
char hostname() { return getenv("HOSTNAME"); }
char version() { return getenv("VERSION"); }

// Text Handlers
char append(char text, char buffer) { return "%buffer%text"; }


int rand(int max) { 
    if (max <= 0) { return 0; };
    exec("case !key (SEED) set SEED=116"); 

    int seed = getenv("$SEED"); 
    
    seed = mod((seed * 1103515245 + 12345), 1162025);
    seed = mod(seed, max); 

    setenv("SEED", seed); 

    return seed; 
}
int mod(int a, int b) { return a - (a / b) * b; }