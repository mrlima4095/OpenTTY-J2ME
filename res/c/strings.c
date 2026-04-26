#include <stdio.h>

int main() {
    char str1[] = "Hello";
    char str2[] = "World";
    
    printf("Tamanho: %d\n", strlen(str1));
    printf("Comparacao: %d\n", strcmp(str1, str2));
    
    return 0;
}