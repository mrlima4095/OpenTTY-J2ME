char append(char text, char buffer) { return "%buffer%text"; }

int main() {
    int i = 0, j, n = 6;
    char line;

    j = 0; line = "";
    while (j < (2 * n)) {
        line = append("*", line);
        j = j + 1;
    };
    printf(line);

    while (i < n) {
        line = "*"; 
        j = 0;
        while (j < n - i - 1) {
            line = append(" ", line);
            j = j + 1;
        };
        j = 0;
        while (j < (2 * i + 1)) {
            line = append("*", line);
            j = j + 1;
        };
        j = 0;
        while (j < n - i - 1) {
            line = append(" ", line);
            j = j + 1;
        };
        line = append("*", line);
        printf(line);
        i = i + 1;
    };

    j = 0; line = "";
    while (j < (2 * n)) {
        line = append("*", line);
        j = j + 1;
    };
    printf(line);
}
