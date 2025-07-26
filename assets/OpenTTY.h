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

