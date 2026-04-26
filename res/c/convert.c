#include <stdio.h>

int main() {
    char numStr[] = "123";
    int valor = atoi(numStr);
    
    printf("String: %s\n", numStr);
    printf("Inteiro: %d\n", valor);
    printf("Dobro: %d\n", valor * 2);
    
    return 0;
}