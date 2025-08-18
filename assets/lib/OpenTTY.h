/*
    OpenTTY C2ME Standard Library
    Author: Mr. Lima
    Version: 1.0
*/

int build(char code) {
    printf("int main() { %code }", ".c2me")
    
    return exec("build .c2me");
}
