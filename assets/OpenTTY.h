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
    seed = (seed * seed) * 1162025;

    if (seed < max) {
        
    } else {
        while (seed > max) {
            seed = seed - (max / 2);
        }
    };

    setenv("SEED", seed); 
    return seed; 
}