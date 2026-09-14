/* c4cc.c - small, self-hosted C subset compiler for OpenTTY RV32IM.
 *
 * Build: ./build-elf.sh res/apps/src/c4cc.c -stdlib -o res/apps/dist/c4cc
 * Use:   c4cc source.c [output]
 *
 * This deliberately implements a useful subset, not a preprocessor or ISO C:
 * quoted #include fragments, one int main, int/char/pointer locals, expressions,
 * if/else, while, and selected OpenTTY calls.  See docs/ELF/README.md.
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
 * names are resolved next to the including file.  No macro expansion is done. */
int sourcefile(char *path,int depth,int *used)
{ int fd,n,i,j,k,bol; char *b,*full; if(depth>8){error("include nesting is too deep");return 0;} b=malloc(MAXSRC);full=malloc(256);if(!b||!full){error("out of memory reading source");return 0;} fd=open(path,0,0);if(fd<0){error("cannot open source or include");return 0;}n=read(fd,b,MAXSRC-1);close(fd);if(n<0){error("cannot read source or include");return 0;}b[n]=0;bol=1;
 for(i=0;i<n&&!failed;) { if(bol&&(b[i]==' '||b[i]=='\t')) {if(*used>=MAXSRC-1){error("combined source is too large");break;}src[(*used)++]=b[i++];continue;} if(bol&&b[i]=='#') {j=i+1;while(b[j]==' '||b[j]=='\t')j++;if(b[j]=='i'&&b[j+1]=='n'&&b[j+2]=='c'&&b[j+3]=='l'&&b[j+4]=='u'&&b[j+5]=='d'&&b[j+6]=='e'){j+=7;while(b[j]==' '||b[j]=='\t')j++;if(b[j]=='<'){error("system headers are not supported; use a quoted c4cc include fragment");break;}if(b[j]!='"'){error("expected quoted include path");break;}j++;k=0;while(b[j]&&b[j]!='"'&&k<240)full[k++]=b[j++];full[k]=0;if(b[j]!='"'){error("unterminated include path");break;} if(full[0]!='/'){int base=k,len=0;while(path[len])len++;while(len&&path[len-1]!='/')len--;if(len+base>=255){error("include path is too long");break;}{char q[256];for(j=0;j<len;j++)q[j]=path[j];for(j=0;j<base;j++)q[len+j]=full[j];q[len+j]=0;copyname(full,q);} } if(!sourcefile(full,depth+1,used))break;while(b[i]&&b[i]!='\n')i++;continue;} error("preprocessor directives are not supported");break; } if(*used>=MAXSRC-1){error("combined source is too large");break;}src[(*used)++]=b[i];bol=b[i++]=='\n'; } return !failed; }

int main(int argc,char **argv)
{ int used=0,i,text,addr,fd; char hdr[256],word[4],out[256];
 if(argc<2||argc>3){printf("usage: c4cc source.c [output]\n");return 1;} src=malloc(MAXSRC);data=malloc(MAXDATA);code=malloc(MAXCODE*4);apatch=malloc(MAXPATCH*4);atarget=malloc(MAXPATCH*4);if(!src||!data||!code||!apatch||!atarget){printf("c4cc: out of memory\n");return 1;}
 line=1;if(!sourcefile(argv[1],0,&used))return 1;src[used]=0;p=src;line=1;next();
 if(!(tok==Id&&named("int"))){error("expected int main definition");}else next(); if(!(tok==Id&&named("main"))){error("expected main");}else next();need('(' ,"expected '(' after main");while(tok!=')'&&tok!=Eof)next();need(')',"expected ')' after main parameters");addi(2,2,-512);addi(8,2,512);block();if(tok!=Eof)error("only one main definition is supported"); if(failed)return 1;
 li(10,0);ecall(1); /* fixed frame leaves expression pushes safely below locals */
 text=nc*4; for(i=0;i<na;i++){addr=BASE+text+atarget[i];code[apatch[i]]=(((addr+0x800)>>12)<<12)|(10<<7)|0x37;code[apatch[i]+1]=(((addr-((addr+0x800)>>12<<12))&4095)<<20)|(10<<15)|(10<<7)|0x13;}
 if(argc==3)copyname(out,argv[2]);else{copyname(out,argv[1]);for(i=0;out[i];i++);if(i>2&&out[i-2]=='.'&&out[i-1]=='c')out[i-2]=0;}fd=open(out,0x241,0755);if(fd<0){printf("c4cc: cannot create %s\n",out);return 1;}for(i=0;i<256;i++)hdr[i]=0;hdr[0]=127;hdr[1]='E';hdr[2]='L';hdr[3]='F';hdr[4]=1;hdr[5]=1;hdr[6]=1;put16(hdr,16,2);put16(hdr,18,243);put32(hdr,20,1);put32(hdr,24,BASE);put32(hdr,28,52);put16(hdr,40,52);put16(hdr,42,32);put16(hdr,44,1);put32(hdr,52,1);put32(hdr,56,256);put32(hdr,60,BASE);put32(hdr,64,BASE);put32(hdr,68,text+nd);put32(hdr,72,text+nd);put32(hdr,76,5);put32(hdr,80,4096);write(fd,hdr,256);for(i=0;i<nc;i++){put32(word,0,code[i]);write(fd,word,4);}if(nd)write(fd,data,nd);close(fd);printf("c4cc: wrote %s (%d bytes)\n",out,256+text+nd);return 0;
}
