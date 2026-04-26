#include <stdio.h>

int fatorial(int n) {
    int resultado = 1;
    for (int i = 1; i <= n; i++) {
        resultado = resultado * i;
    }
    return resultado;
}

int main() {
    int num = 5;
    printf("Fatorial de %d = %d\n", num, fatorial(num));
    return 0;
}