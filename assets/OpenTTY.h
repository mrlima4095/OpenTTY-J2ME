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
    int seed = exec("case !key (SEED) set SEED=116"); 

    seed = getenv("$SEED"); 
    seed = ((seed * 1103515245 + 12345) - (seed + 12345 / 2025) * 1162025) / (max);

    while (seed > max) {
        seed = seed - max;
    };

    setenv("SEED", seed); 

    return seed; 
}