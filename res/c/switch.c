#include <stdio.h>

int main() {
    int opcao = 2;
    
    switch (opcao) {
        case 1:
            printf("Opcao 1\n");
            break;
        case 2:
            printf("Opcao 2\n");
            break;
        default:
            printf("Outra opcao\n");
            break;
    }
    return 0;
}