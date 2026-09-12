/* cowsay.c - port do cowsay em C (github.com/lemosgabriel20/cowsay) para o
 * emulador ELF RV32IM do OpenTTY.
 *
 * Ajustes do port:
 *   - remove #include's de sistema; printf/strlen vem da libc do emulador
 *     (res/lib/libc.s + src/ELF.java);
 *   - desenho e' o mesmo boi ASCII do cowsay original.
 *
 * Compilar:
 *   ./build-elf.sh res/apps/src/cowsay.c -stdlib -o res/apps/dist/cowsay
 *
 * Uso no dispositivo: cowsay <mensagem...>
 */
int printf(const char *fmt, ...);
int strlen(const char *s);

#define WIDTH 39
#define ONE_LINE 0
#define DOUBLE_LINE 1
#define MULTI_LINE 2

void mode(int, int, char**, int);
void oneLine(int, char**);
void doubleLine(int, char**);
void multiLine(int, char**, int);
int findWidth(int, char**);

int main (int argc, char **argv) {
  int flag;
  int size = findWidth(argc, argv);
  int print = 0;

  // set mode
  if (size <= WIDTH) flag = ONE_LINE;
  if (size > WIDTH && size <= WIDTH * 2) flag = DOUBLE_LINE;
  if (size > WIDTH * 2) flag = MULTI_LINE;

  // top
  print = (size <= WIDTH) ? size + 2: WIDTH + 2;
  printf(" ");
  for (int i = print; i > 0; i--) printf("_");

  printf("\n");
  mode(flag, argc, argv, size);
  printf("\n");

  // bottom
  print = (size <= WIDTH) ? size + 2: WIDTH + 2;
  printf(" ");
  for (int i = print; i > 0; i--) printf("-");

  // cow
  printf("\n");
  printf("\t\\   ^__^\n");
  printf("\t \\  (oo)\\_______\n");
  printf("\t    (__)\\       )\\/\\\n");
  printf("\t        ||----w |\n");
  printf("\t        ||     ||\n");

  return 0;
}

void mode(int flag, int argc, char**argv, int size) {
  if (flag == ONE_LINE) oneLine(argc, argv);
  if (flag == DOUBLE_LINE) doubleLine(argc, argv);
  if (flag == MULTI_LINE) multiLine(argc, argv, size);
}

void oneLine(int argc, char**argv) {
  printf("< ");
  for (int i = 1; i < argc; i += 1) {
    for (int j = 0; j < strlen(argv[i]); j += 1) printf("%c", argv[i][j]);
    if (i != argc -1) printf(" ");
  }
  printf(" >");
}

void doubleLine(int argc, char**argv) {
  int count = 0;
  printf("/ ");
  for (int i = 1; i < argc; i += 1) {
    for (int j = 0; j < strlen(argv[i]); j += 1) {
      if (count == WIDTH) {
        printf(" \\\n\\ ");
        count = 0;
      }
      printf("%c", argv[i][j]);
      count++;
    }
    if (i != argc - 1) {
      printf(" ");
      count++;
    }
  }
  for (int i = 0; i < WIDTH - count; i += 1) printf(" ");
  printf(" /");
}

void multiLine(int argc, char**argv, int size) {
  int left = size;
  int count = 0;
  printf("/ ");
  for (int i = 1; i < argc; i += 1) {
    for (int j = 0; j < strlen(argv[i]); j += 1) {
      if (count == WIDTH) {
        left = left - count;
        // ends
        printf("%s", ((left == size - WIDTH) ? " \\" : " |"));
        printf("\n");
        // starts
        printf("%s", ((left <= WIDTH) ? "\\ " : "| "));
        count = 0;
      }
      printf("%c", argv[i][j]);
      count++;
    }
    if (i != argc-1){
      printf(" ");
      count++;
    }
  }
  for (int i = 0; i < WIDTH - count; i += 1) printf(" ");
  printf(" /");
}

int findWidth(int argc, char**argv) {
  int size = 0;
  for (int i = 1; i < argc; i += 1) {
    for (int j = 0; j < strlen(argv[i]); j += 1) size++;
    if (i != argc-1) size++;
  }
  return size;
}