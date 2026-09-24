/* c4cc.c - small, self-hosted C subset compiler for OpenTTY RV32IM.
 *
 * Build: ./build-elf.sh res/apps/src/c4cc.c -stdlib -o res/apps/dist/c4cc
 * Use:   c4cc source.c [output]
 *
 * This deliberately implements a useful subset, not ISO C: one int main,
 * int/char/pointer locals, expressions, if/else, while, selected OpenTTY
 * calls, and a small line-oriented preprocessor (#define/#undef/#if/#ifdef/
 * #ifndef/#elif/#else/#endif/#error, object-like macro expansion). The
 * include pass splices quoted #include fragments first.  See docs/ELF/README.md.
 */
int open(const char *p, int f, int m); int read(int f, void *b, int n);
int write(int f, const void *b, int n); int close(int f); void *malloc(int n);
int printf(const char *f, ...);

#define BASE 0x10000
#define MAXSRC 32768
#define MAXCODE 24576
#define MAXDATA 16384
#define MAXSYM 128
#define MAXPATCH 4096

enum { Eof=256, Id, Num, Str, Chr, Eq, Ne, Le, Ge, And, Or };
char *src, *p, *data; int *code, *apatch, *atarget;
int line, tok, num, str, nd, nc, na, failed, slots, laddr, lchar; char *word;
char names[MAXSYM][32]; int soff[MAXSYM], schar[MAXSYM], ns;

void error(char *s) { if (!failed) printf("c4cc:%d: %s\n", line, s); failed=1; }
void put16(char *b,int o,int n) { b[o]=n; b[o+1]=n>>8; }
void put32(char *b,int o,int n) { b[o]=n; b[o+1]=n>>8; b[o+2]=n>>16; b[o+3]=n>>24; }
void emit(int n) { if (nc>=MAXCODE) error("generated code is too large"); else code[nc++]=n; }
void addi(int d,int a,int n) { emit(((n&4095)<<20)|(a<<15)|(d<<7)|0x13); }
void alu(int op,int d,int a,int b) { emit(op|(b<<20)|(a<<15)|(d<<7)|0x33); }
void load(int d,int a,int n,int byte) { emit(((n&4095)<<20)|(a<<15)|((byte?0:2)<<12)|(d<<7)|3); }
void store(int v,int a,int n,int byte) { emit((((n>>5)&127)<<25)|(v<<20)|(a<<15)|((byte?0:2)<<12)|((n&31)<<7)|0x23); }
void li(int d,int n) { int h=(n+0x800)>>12; emit((h<<12)|(d<<7)|0x37); addi(d,d,n-(h<<12)); }
void push() { addi(2,2,-4); store(10,2,0,0); }
void pop(int r) { load(r,2,0,0); addi(2,2,4); }
void ecall(int n) { li(17,n); emit(0x73); }
void addrdata(int d,int off) { emit((d<<7)|0x37); emit((d<<7)|(d<<15)|0x13); if(na>=MAXPATCH) error("too many data references"); else { apatch[na]=nc-2; atarget[na++]=off; } }

int alpha(int c) { return (c>='a'&&c<='z')||(c>='A'&&c<='Z')||c=='_'; }
int alnum(int c) { return alpha(c)||(c>='0'&&c<='9'); }
void next()
{
 int c,q;
again:
 while(*p==' '||*p=='\t'||*p=='\r'||*p=='\n') if(*p++=='\n') line++;
 if(p[0]=='/'&&p[1]=='/') { p+=2; while(*p&&*p!='\n') p++; goto again; }
 if(p[0]=='/'&&p[1]=='*') { p+=2; while(*p&&!(p[0]=='*'&&p[1]=='/')) if(*p++=='\n') line++; if(!*p) { error("unterminated comment"); tok=Eof; return; } p+=2; goto again; }
 c=*p++; if(!c) { tok=Eof; return; }
 if(alpha(c)) { word=p-1; while(alnum(*p)) p++; tok=Id; return; }
 if(c>='0'&&c<='9') { num=0; if(c=='0'&&(*p=='x'||*p=='X')) { p++; while((*p>='0'&&*p<='9')||(*p>='a'&&*p<='f')||(*p>='A'&&*p<='F')) { c=*p++; num=num*16+(c<='9'?c-'0':(c<='F'?c-'A'+10:c-'a'+10)); } } else { num=c-'0'; while(*p>='0'&&*p<='9') num=num*10+*p++-'0'; } tok=Num; return; }
 if(c=='"') { q=nd; while(*p&&*p!='"') { c=*p++; if(c=='\\') { c=*p++; if(c=='n') c='\n'; else if(c=='t') c='\t'; else if(c=='0') c=0; } if(nd>=MAXDATA-1) error("string data is too large"); else data[nd++]=c; } if(*p!='"') error("unterminated string"); else p++; data[nd++]=0; str=q; tok=Str; return; }
 if(c=='\'') { c=*p++; if(c=='\\') { c=*p++; if(c=='n') c='\n'; } if(*p!='\'') error("bad character literal"); else p++; num=c; tok=Chr; return; }
 if(c=='='&&*p=='=') {p++;tok=Eq;return;} if(c=='!'&&*p=='=') {p++;tok=Ne;return;} if(c=='<'&&*p=='=') {p++;tok=Le;return;} if(c=='>'&&*p=='=') {p++;tok=Ge;return;} if(c=='&'&&*p=='&') {p++;tok=And;return;} if(c=='|'&&*p=='|') {p++;tok=Or;return;}
 tok=c;
}
int named(char *s) { char *a=word; while(*s&&*a==*s) {s++;a++;} return !*s&&!alnum(*a); }
void need(int t,char *s) { if(tok!=t) error(s); else next(); }
int symbol()
{ int i,j; for(i=0;i<ns;i++) { j=0; while(names[i][j]&&word[j]==names[i][j]) j++; if(!names[i][j]&&!alnum(word[j])) return i; } return -1; }
void addsym(int ischar)
{ int i,j; if(ns>=MAXSYM) {error("too many local variables");return;} for(i=0;i<ns;i++) if(symbol()==i) {error("duplicate local variable");return;} for(j=0;j<31&&alnum(word[j]);j++) names[ns][j]=word[j]; names[ns][j]=0; soff[ns]=-(++slots*4); schar[ns]=ischar; ns++; }

/* Relative branch and jal instructions are patched after their destination is known. */
void branch(int funct,int reg) { emit((reg<<15)|(funct<<12)|0x63); }
void jump() { emit(0x6f); }
void patchat(int at,int to)
{ int d=(to-at)*4, x=code[at]; if((x&127)==0x6f) code[at]=((d&0x100000)<<11)|((d&0x7fe)<<20)|((d&0x800)<<9)|((d&0xff000)<<0)|0x6f; else code[at]=(x&0x01fff07f)|((d&0x1000)<<19)|((d&0x7e0)<<20)|((d&0x1e)<<7)|((d&0x800)>>4); }
void expr();
int same(char *a,char *b) { int i=0; while(a[i]&&a[i]==b[i])i++;return !a[i]&&!b[i]; }
void call(char *name)
{ int n=0,id=-1; need('(' ,"expected '('");
  while(tok!=')'&&tok!=Eof) { if(n>=7) {error("at most seven call arguments");break;} expr(); push(); n++; if(tok==',') next(); else break; }
  need(')',"expected ')' after arguments");
  while(n) { n--; pop(5); addi(10+n,5,0); }
  /* Calls use a0..a6.  Raw file syscalls share the OpenTTY EABI. */
  if(same(name,"puts")) id=1018;
  else if(same(name,"putchar")) id=1017;
  else if(same(name,"printf")) id=1019;
  else if(same(name,"malloc")) id=1023;
  else if(same(name,"free")) id=1026;
  else if(same(name,"memset")) id=1012;
  else if(same(name,"open")) id=5;
  else if(same(name,"read")) id=3;
  else if(same(name,"write")) id=4;
  else if(same(name,"close")) id=6;
  else {error("unsupported call (puts, putchar, printf, malloc, free, memset, open, read, write, close)");return;}
  ecall(id);
}
void primary()
{ int s; char name[32];
 if(tok==Num||tok==Chr) {li(10,num);next();laddr=0;return;}
 if(tok==Str) {addrdata(10,str);next();laddr=0;return;}
 if(tok=='(') {next();expr();need(')',"expected ')'");return;}
 if(tok==Id) { for(s=0;s<31&&alnum(word[s]);s++) name[s]=word[s]; name[s]=0; s=symbol(); next(); if(tok=='(') { call(name); laddr=0; return; } if(s<0) {error("unknown identifier");return;} addi(5,8,soff[s]); laddr=5; lchar=schar[s]; load(10,5,0,lchar); return; }
 error("expected expression");
}
void unary()
{ if(tok=='+') {next();unary();laddr=0;} else if(tok=='-') {next();unary(); emit((0x20<<25)|(10<<20)|(10<<7)|0x33);laddr=0;} else if(tok=='!') {next();unary(); emit((1<<20)|(10<<15)|3<<12|(10<<7)|0x13);laddr=0;} else if(tok=='&') {next();unary();if(!laddr) error("'&' needs an lvalue"); else {emit((laddr<<15)|(10<<7)|0x13);laddr=0;}} else if(tok=='*') {next();unary(); emit((10<<15)|(5<<7)|0x13); laddr=5;lchar=0;load(10,5,0,0);} else primary(); }
void mul() { int o; unary(); while(tok=='*'||tok=='/'||tok=='%') {o=tok;next();push();unary();pop(5);alu(o=='*'?(1<<25):(o=='/'?((1<<25)|(4<<12)):((1<<25)|(6<<12))),10,5,10);laddr=0;} }
void add() { int o;mul();while(tok=='+'||tok=='-'){o=tok;next();push();mul();pop(5);alu(o=='+'?0:(0x20<<25),10,5,10);laddr=0;} }
void rel() { int o;add();while(tok=='<'||tok=='>'||tok==Le||tok==Ge){o=tok;next();push();add();pop(5);if(o=='<') alu(2<<12,10,5,10); else if(o=='>') alu(2<<12,10,10,5); else {alu(2<<12,10,o==Le?10:5,o==Le?5:10);emit((10<<15)|(10<<20)|4<<12|(10<<7)|0x13);}laddr=0;} }
void equal() {int o;rel();while(tok==Eq||tok==Ne){o=tok;next();push();rel();pop(5);alu(4<<12,10,5,10);if(o==Eq) emit((1<<20)|(10<<15)|3<<12|(10<<7)|0x13);else alu(3<<12,10,0,10);laddr=0;} }
void land() {equal();while(tok==And){next();push();equal();pop(5);alu(3<<12,5,0,5);alu(3<<12,10,0,10);alu(7<<12,10,5,10);laddr=0;} }
void lor() {land();while(tok==Or){next();push();land();pop(5);alu(3<<12,5,0,5);alu(3<<12,10,0,10);alu(6<<12,10,5,10);laddr=0;} }
void expr() { int a,c; lor(); if(tok=='=') {a=laddr;c=lchar; if(!a) error("left side of '=' is not assignable"); else { emit((a<<15)|(10<<7)|0x13); push(); } next();expr(); if(a) {pop(5);store(10,5,0,c);}laddr=0;} }

void statement();
void block() { need('{',"expected '{'"); while(tok!='}'&&tok!=Eof&&!failed) statement(); need('}',"expected '}'"); }
void declaration()
{ int ischar=named("char"); next(); while(tok=='*') {ischar=0;next();} if(tok!=Id) {error("expected local name");return;} addsym(ischar); next(); if(tok=='=') {next();expr();addi(5,8,soff[ns-1]);store(10,5,0,ischar);} need(';',"expected ';' after declaration"); }
void statement()
{ int a,b;
 if(tok==Id&&named("int")) {declaration();return;} if(tok==Id&&named("char")) {declaration();return;}
 if(tok==Id&&named("return")) {next();expr();need(';',"expected ';' after return"); ecall(1);return;}
 if(tok==Id&&named("if")) {next();need('(',"expected '(' after if");expr();need(')',"expected ')' after if");branch(0,10);a=nc-1;statement();if(tok==Id&&named("else")){jump();b=nc-1;patchat(a,nc);next();statement();patchat(b,nc);}else patchat(a,nc);return;}
 if(tok==Id&&named("while")) {next();a=nc;need('(',"expected '(' after while");expr();need(')',"expected ')' after while");branch(0,10);b=nc-1;statement();jump();patchat(nc-1,a);patchat(b,nc);return;}
 if(tok=='{') {block();return;} if(tok==';') {next();return;} expr();need(';',"expected ';' after expression");
}

int copyname(char *out,char *in) { int n=0; while(in[n]) {out[n]=in[n];n++;} out[n]=0;return n; }
/* Load source and splice quoted includes.  Direct absolute paths work; relative
 * names are resolved next to the including file.  Splicing is textual; macro
 * expansion happens in the preprocessor pass below. */
int sourcefile(char *path,int depth,int *used)
{ int fd,n,i,j,k,bol; char *b,*full; if(depth>8){error("include nesting is too deep");return 0;} b=malloc(MAXSRC);full=malloc(256);if(!b||!full){error("out of memory reading source");return 0;} fd=open(path,0,0);if(fd<0){error("cannot open source or include");return 0;}n=read(fd,b,MAXSRC-1);close(fd);if(n<0){error("cannot read source or include");return 0;}b[n]=0;bol=1;
 for(i=0;i<n&&!failed;) { if(bol&&(b[i]==' '||b[i]=='\t')) {if(*used>=MAXSRC-1){error("combined source is too large");break;}src[(*used)++]=b[i++];continue;} if(bol&&b[i]=='#') {j=i+1;while(b[j]==' '||b[j]=='\t')j++;if(b[j]=='i'&&b[j+1]=='n'&&b[j+2]=='c'&&b[j+3]=='l'&&b[j+4]=='u'&&b[j+5]=='d'&&b[j+6]=='e'){j+=7;while(b[j]==' '||b[j]=='\t')j++;if(b[j]=='<'){error("system headers are not supported; use a quoted c4cc include fragment");break;}if(b[j]!='"'){error("expected quoted include path");break;}j++;k=0;while(b[j]&&b[j]!='"'&&k<240)full[k++]=b[j++];full[k]=0;if(b[j]!='"'){error("unterminated include path");break;} if(full[0]!='/'){int base=k,len=0;while(path[len])len++;while(len&&path[len-1]!='/')len--;if(len+base>=255){error("include path is too long");break;}{char q[256];for(j=0;j<len;j++)q[j]=path[j];for(j=0;j<base;j++)q[len+j]=full[j];q[len+j]=0;copyname(full,q);} } if(!sourcefile(full,depth+1,used))break;while(b[i]&&b[i]!='\n')i++;continue;} else {while(b[i]&&b[i]!='\n'){if(*used>=MAXSRC-1){error("combined source is too large");break;}src[(*used)++]=b[i++];}if(*used<MAXSRC-1){src[(*used)++]=b[i];}bol=(b[i]=='\n');i++;continue;} } if(*used>=MAXSRC-1){error("combined source is too large");break;}src[(*used)++]=b[i];bol=b[i++]=='\n'; } return !failed; }

/* --- minimal line-oriented preprocessor -----------------------------------
 * Each input line becomes exactly one output line (skipped text collapses to
 * a bare newline), so parser line numbers stay accurate.  {define,undef,if,
 * ifdef,ifndef,elif,else,endif,error} are handled; #pragma/#line are ignored.
 * #include was already spliced by the include pass.  Object-like macros
 * expand at token level (never inside strings or comments); function-like
 * macros are rejected clearly.  Comments are replaced by a single space, and
 * strings / char literals pass through untouched.
 */
#define MAXDEF 128
#define MAXIF 16
#define MAXLINE 2048

char defname[MAXDEF][32]; char *defval[MAXDEF]; int ndf, fltop;
char ql[MAXLINE], ebuf[MAXLINE], *dst; int dused, pline, incmt;
int flin[MAXIF], ftaken[MAXIF], felse[MAXIF];
char *pe; int evtok; long evnum; char evname[32];

int finddef(char *w)
{ int i,j; for(i=0;i<ndf;i++){ j=0; while(defname[i][j]&&w[j]==defname[i][j])j++; if(!defname[i][j]&&!alnum(w[j]))return i; } return -1; }
int spskip(char *b,int i)
{ while(b[i]==' '||b[i]=='\t'||b[i]=='\r')i++; return i; }
int rid(char *b,char *o,int i)
{ int k=0; while(alpha(b[i])){ if(k<31)o[k++]=b[i]; i++; } o[k]=0; return i; }
void putout(char c)
{ if(failed)return; if(dused>=MAXSRC-1)error("preprocessed source is too large"); else dst[dused++]=c; }
void putb(char c,char *b,int *n,int cap)
{ if(*n<cap)b[(*n)++]=c; else if(*n==cap){(*n)++;error("expanded text is too long");} }
int defword(char *s)
{ int k=0; while(s[k]&&alnum(s[k]))k++; return k==7&&s[0]=='d'&&s[1]=='e'&&s[2]=='f'&&s[3]=='i'&&s[4]=='n'&&s[5]=='e'&&s[6]=='d'; }
void expandb(char *s,int depth,char *b,int *n,int cap)
{ while(*s&&!failed){ if(depth>10){error("macro expansion is too deep");return;} if(alpha(*s)){ if(defword(s)){ int m=0; while(m<7){putb(*s,b,n,cap);s++;m++;} while(*s==' '||*s=='\t'||*s=='\r')s++; if(*s=='('){putb(*s++,b,n,cap); while(*s==' '||*s=='\t'||*s=='\r')s++; while(alnum(*s))putb(*s++,b,n,cap); while(*s==' '||*s=='\t'||*s=='\r')s++; if(*s==')')putb(*s++,b,n,cap);} else while(alnum(*s))putb(*s++,b,n,cap); continue; } int i=finddef(s); if(i>=0){ expandb(defval[i],depth+1,b,n,cap); while(alnum(*s))s++; continue; } while(alnum(*s))putb(*s++,b,n,cap); continue; } putb(*s++,b,n,cap); } }
void expand(char *s,int depth)
{ int i; while(*s&&!failed){ if(depth>10){error("macro expansion is too deep");return;}
  if(s[0]=='/'&&s[1]=='/')return;
  if(s[0]=='/'&&s[1]=='*'){ s+=2; while(*s&&!(s[0]=='*'&&s[1]=='/'))s++; if(*s)s+=2; continue; }
  if(*s=='"'){ putout(*s++); while(*s){ putout(*s); if(*s=='\\'&&s[1]){putout(s[1]);s+=2;continue;} if(*s=='"'){s++;break;} s++; } continue; }
  if(*s=='\''){ putout(*s++); while(*s){ putout(*s); if(*s=='\\'&&s[1]){putout(s[1]);s+=2;continue;} if(*s=='\''){s++;break;} s++; } continue; }
  if(alpha(*s)){ i=finddef(s); if(i>=0){ expand(defval[i],depth+1); while(alnum(*s))s++; continue; } while(alnum(*s))putout(*s++); continue; } putout(*s++); } }
int active(void)
{ int i; for(i=0;i<fltop;i++)if(!flin[i])return 0; return 1; }
void pushif(int t)
{ if(fltop>=MAXIF)error("too many nested conditional directives"); else { flin[fltop]=t; ftaken[fltop]=t; felse[fltop]=0; fltop++; } }

void evnext(void)
{ char *s=pe; for(;;){ while(*s==' '||*s=='\t'||*s=='\r')s++; if(s[0]=='/'&&s[1]=='/'){ while(*s&&*s!='\n')s++; continue; } if(s[0]=='/'&&s[1]=='*'){ s+=2; while(*s&&!(s[0]=='*'&&s[1]=='/'))s++; if(*s)s+=2; continue; } break; } pe=s;
  if(!*pe){evtok=0;return;}
  if(alpha(*pe)){ int k=0; while(alnum(*pe)&&k<31)evname[k++]=*pe++; evname[k]=0; evtok=same(evname,"defined")?300:100; return; }
  if(*pe>='0'&&*pe<='9'){ long n=0; if(*pe=='0'&&(pe[1]=='x'||pe[1]=='X')){ pe+=2; while((*pe>='0'&&*pe<='9')||(*pe>='a'&&*pe<='f')||(*pe>='A'&&*pe<='F')){ int c=*pe++; n=n*16+(c<='9'?c-'0':(c<='F'?c-'A'+10:c-'a'+10)); } } else while(*pe>='0'&&*pe<='9')n=n*10+*pe++-'0'; evnum=n; evtok='n'; return; }
  if(*pe=='&'&&pe[1]=='&'){pe+=2;evtok='A';return;} if(*pe=='|'&&pe[1]=='|'){pe+=2;evtok='O';return;}
  if(*pe=='='&&pe[1]=='='){pe+=2;evtok='E';return;} if(*pe=='!'&&pe[1]=='='){pe+=2;evtok='N';return;}
  if(*pe=='<'&&pe[1]=='='){pe+=2;evtok='L';return;} if(*pe=='>'&&pe[1]=='='){pe+=2;evtok='G';return;}
  evtok=*pe++;
}
long evor(void); long evand(void); long eveq(void); long evrel(void); long evadd(void); long evmul(void);
long evun(void)
{ int o=0; long a; if(evtok=='-'){evnext();o=1;} else if(evtok=='!'){evnext();o=2;} if(o){ a=evun(); return o==1?-a:!a; }
  if(evtok=='('){ evnext(); a=evor(); if(evtok!=')'){error("expected ')' in #if expression");return 0;} evnext(); return a; }
  if(evtok=='n'){ a=evnum; evnext(); return a; }
  if(evtok==100){ evnext(); return 0; }
  if(evtok==300){ evnext(); a=0; if(evtok=='('){ evnext(); if(evtok==100){ a=finddef(evname)>=0; evnext(); } if(evtok!=')'){error("expected ')' after defined");return 0;} evnext(); return a; } if(evtok==100){ a=finddef(evname)>=0; evnext(); return a; } error("expected macro name after defined"); return 0; }
  error("bad #if expression"); return 0;
}
long evmul(void)
{ long a=evun(),b; while(evtok=='*'||evtok=='/'||evtok=='%'){ int o=evtok; evnext(); b=evun(); if(o=='*')a=a*b; else if(b==0){error("division by zero in #if");a=0;} else a=o=='/'?a/b:a%b; } return a; }
long evadd(void)
{ long a=evmul(),b; while(evtok=='+'||evtok=='-'){ int o=evtok; evnext(); b=evmul(); a=o=='+'?a+b:a-b; } return a; }
long evrel(void)
{ long a=evadd(),b; while(evtok=='<'||evtok=='L'||evtok=='>'||evtok=='G'){ int o=evtok; evnext(); b=evadd(); if(o=='<')a=a<b; else if(o=='L')a=a<=b; else if(o=='>')a=a>b; else a=a>=b; } return a; }
long eveq(void)
{ long a=evrel(),b; while(evtok=='E'||evtok=='N'){ int o=evtok; evnext(); b=evrel(); a=o=='E'?a==b:a!=b; } return a; }
long evand(void)
{ long a=eveq(),b; while(evtok=='A'){ evnext(); b=eveq(); a=a&&b; } return a; }
long evor(void)
{ long a=evand(),b; while(evtok=='O'){ evnext(); b=evand(); a=a||b; } return a; }

void do_define(int i)
{ char nm[32],*v; int m,k,e;
 i=spskip(ql,i); i=rid(ql,nm,i);
 if(!nm[0]){error("expected macro name after #define");return;}
 if(ql[i]=='('){error("function-like macros are not supported");return;}
 i=spskip(ql,i); e=i; while(ql[e])e++; while(e>i&&(ql[e-1]==' '||ql[e-1]=='\t'||ql[e-1]=='\r'))e--;
 if(e>i&&ql[e-1]=='\\'){error("line continuations are not supported");return;}
 v=malloc(e-i+1); if(!v){error("out of memory defining macro");return;} for(m=i;m<e;m++)v[m-i]=ql[m]; v[e-i]=0;
 m=finddef(nm); if(m<0){ if(ndf>=MAXDEF){error("too many macros defined");return;} m=ndf++; k=0; while(nm[k]&&k<31){defname[m][k]=nm[k];k++;} defname[m][k]=0; }
 defval[m]=v;
}
void do_undef(int i)
{ char nm[32]; int j,k,m;
 i=spskip(ql,i); i=rid(ql,nm,i);
 if(!nm[0]){error("expected macro name after #undef");return;}
 j=finddef(nm); if(j<0)return;
 for(k=j;k+1<ndf;k++){ for(m=0;m<32;m++)defname[k][m]=defname[k+1][m]; defval[k]=defval[k+1]; }
 ndf--;
}
void do_if(int i)
{ long v; int n=0;
 i=spskip(ql,i); if(!ql[i]){error("expected expression after #if");return;}
 expandb(ql+i,1,ebuf,&n,MAXLINE-2); if(failed)return;
 ebuf[n]=0; pe=ebuf; evnext(); if(failed)return; v=evor(); if(failed)return;
 if(evtok){error("unexpected tokens in #if expression");return;} pushif(v!=0);
}
void do_cond(int i,int want)
{ char nm[32]; int t;
 i=spskip(ql,i); i=rid(ql,nm,i);
 if(!nm[0]){error("expected macro name after conditional");return;}
 t=(finddef(nm)>=0); if(!want)t=!t; pushif(t);
}
void do_elif(int i)
{ long v; int n=0;
 if(fltop<1){error("#elif without matching #if");return;}
 if(felse[fltop-1]){error("#elif after #else");return;}
 if(ftaken[fltop-1]){flin[fltop-1]=0;return;}
 i=spskip(ql,i); if(!ql[i]){error("expected expression after #elif");return;}
 expandb(ql+i,1,ebuf,&n,MAXLINE-2); if(failed)return;
 ebuf[n]=0; pe=ebuf; evnext(); if(failed)return; v=evor(); if(failed)return;
 if(evtok){error("unexpected tokens in #elif expression");return;}
 flin[fltop-1]=v!=0; if(v)ftaken[fltop-1]=1;
}
void do_else(void)
{ if(fltop<1){error("#else without matching #if");return;}
  if(felse[fltop-1]){error("duplicate #else");return;}
  felse[fltop-1]=1;
  if(ftaken[fltop-1])flin[fltop-1]=0; else {flin[fltop-1]=1;ftaken[fltop-1]=1;}
}
void do_endif(void)
{ if(fltop<1){error("#endif without matching #if");return;} fltop--; }
void do_error(int i)
{ char m[MAXLINE]; int k,e;
 if(!active())return; i=spskip(ql,i); e=i; while(ql[e])e++; while(e>i&&(ql[e-1]==' '||ql[e-1]=='\t'||ql[e-1]=='\r'))e--;
 for(k=i;k<e&&k-i<MAXLINE-1;k++)m[k-i]=ql[k]; m[k-i]=0;
 error(*m?m:"#error");
}
void directive(int i)
{ char d[32];
 i++; i=spskip(ql,i);
 if(!ql[i])return;
 i=rid(ql,d,i);
 if(same(d,"define")){ if(active())do_define(i); return; }
 if(same(d,"undef")){ if(active())do_undef(i); return; }
 if(same(d,"ifdef")){ do_cond(i,1); return; }
 if(same(d,"ifndef")){ do_cond(i,0); return; }
 if(same(d,"if")){ do_if(i); return; }
 if(same(d,"elif")){ do_elif(i); return; }
 if(same(d,"else")){ do_else(); return; }
 if(same(d,"endif")){ do_endif(); return; }
 if(same(d,"error")){ do_error(i); return; }
 if(same(d,"pragma")||same(d,"line"))return;
 if(same(d,"include")){ error("unexpected #include (consumed by the include pass)"); return; }
 error("unknown preprocessor directive");
}
void emitline(char *s,int *c)
{ int i;
 *c=0;
 while(*s&&*s!='\n'&&!failed){
  if(s[0]=='/'&&s[1]=='/'){ if(dused&&dst[dused-1]!=' ')putout(' '); return; }
  if(s[0]=='/'&&s[1]=='*'){ putout(' '); s+=2; while(*s&&!(s[0]=='*'&&s[1]=='/'))s++; if(*s)s+=2; else {*c=1;return;} continue; }
  if(*s=='"'){ putout(*s++); while(*s&&*s!='\n'){ putout(*s); if(*s=='\\'&&s[1]){putout(s[1]);s+=2;continue;} if(*s=='"'){s++;break;} s++; } continue; }
  if(*s=='\''){ putout(*s++); while(*s&&*s!='\n'){ putout(*s); if(*s=='\\'&&s[1]){putout(s[1]);s+=2;continue;} if(*s=='\''){s++;break;} s++; } continue; }
  if(alpha(*s)){ i=finddef(s); if(i>=0){ expand(defval[i],1); while(alnum(*s))s++; continue; } while(alnum(*s))putout(*s++); continue; }
  putout(*s++);
 }
}
void line_comment(char *b)
{ int i,c2;
 for(i=0;b[i]&&!failed;i++) if(b[i]=='*'&&b[i+1]=='/'){ incmt=0; if(active()){ emitline(b+i+2,&c2); if(c2)incmt=1; } return; }
}
int preprocess(void)
{ char *in=src; int k,j,c2;
 pline=1; dused=0; ndf=0; fltop=0; incmt=0; line=1;
 while(*in&&!failed){
  k=0; while(*in&&*in!='\n'&&k<MAXLINE-1)ql[k++]=*in++;
  if(*in&&*in!='\n'){ line=pline; error("preprocessor line is too long"); while(*in&&*in!='\n')in++; }
  ql[k]=0; if(*in=='\n')in++;
  line=pline;
  if(incmt) line_comment(ql);
  else { j=spskip(ql,0); if(ql[j]=='#'){ directive(j); putout('\n'); } else if(active()){ c2=0; emitline(ql,&c2); if(c2)incmt=1; putout('\n'); } else putout('\n'); }
  pline++;
 }
 if(fltop)error("#if without matching #endif");
 return !failed;
}

int main(int argc,char **argv)
{ int used=0,i,text,addr,fd; char hdr[256],word[4],out[256];
 if(argc<2||argc>3){printf("usage: c4cc source.c [output]\n");return 1;} src=malloc(MAXSRC);dst=malloc(MAXSRC);data=malloc(MAXDATA);code=malloc(MAXCODE*4);apatch=malloc(MAXPATCH*4);atarget=malloc(MAXPATCH*4);if(!src||!dst||!data||!code||!apatch||!atarget){printf("c4cc: out of memory\n");return 1;}
 line=1;if(!sourcefile(argv[1],0,&used))return 1;src[used]=0;if(!preprocess())return 1;src=dst;p=src;line=1;next();
 if(!(tok==Id&&named("int"))){error("expected int main definition");}else next(); if(!(tok==Id&&named("main"))){error("expected main");}else next();need('(' ,"expected '(' after main");while(tok!=')'&&tok!=Eof)next();need(')',"expected ')' after main parameters");addi(2,2,-512);addi(8,2,512);block();if(tok!=Eof)error("only one main definition is supported"); if(failed)return 1;
 li(10,0);ecall(1); /* fixed frame leaves expression pushes safely below locals */
 text=nc*4; for(i=0;i<na;i++){addr=BASE+text+atarget[i];code[apatch[i]]=(((addr+0x800)>>12)<<12)|(10<<7)|0x37;code[apatch[i]+1]=(((addr-((addr+0x800)>>12<<12))&4095)<<20)|(10<<15)|(10<<7)|0x13;}
 if(argc==3)copyname(out,argv[2]);else{copyname(out,argv[1]);for(i=0;out[i];i++);if(i>2&&out[i-2]=='.'&&out[i-1]=='c')out[i-2]=0;}fd=open(out,0x241,0755);if(fd<0){printf("c4cc: cannot create %s\n",out);return 1;}for(i=0;i<256;i++)hdr[i]=0;hdr[0]=127;hdr[1]='E';hdr[2]='L';hdr[3]='F';hdr[4]=1;hdr[5]=1;hdr[6]=1;put16(hdr,16,2);put16(hdr,18,243);put32(hdr,20,1);put32(hdr,24,BASE);put32(hdr,28,52);put16(hdr,40,52);put16(hdr,42,32);put16(hdr,44,1);put32(hdr,52,1);put32(hdr,56,256);put32(hdr,60,BASE);put32(hdr,64,BASE);put32(hdr,68,text+nd);put32(hdr,72,text+nd);put32(hdr,76,5);put32(hdr,80,4096);write(fd,hdr,256);for(i=0;i<nc;i++){put32(word,0,code[i]);write(fd,word,4);}if(nd)write(fd,data,nd);close(fd);printf("c4cc: wrote %s (%d bytes)\n",out,256+text+nd);return 0;
}
