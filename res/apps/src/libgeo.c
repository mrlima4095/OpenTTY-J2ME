int triple(int x) { return x * 3; }
int fib(int n) { int a=0,b=1,i; for(i=0;i<n;i++){int t=a+b;a=b;b=t;} return a; }
int counter = 0;
int bump(void) { return ++counter; }