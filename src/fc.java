import java.util.Vector;

public final class fc {
   private final el a;
   private static final short[] a = new short[]{7};

   public fc(el var1) {
      this.a = var1;
   }

   public final fv a() {
      fv var1 = new fv(this.a.a().a());
      if (this.a("package")) {
         this.a("package");
         aq var3 = this.a.a();
         String[] var10001 = this.a();
         String var5 = ".";
         String[] var4 = var10001;
         String var15;
         if (var10001 == null) {
            var15 = "(null)";
         } else if (var4.length == 0) {
            var15 = "(zero length array)";
         } else {
            StringBuffer var6 = new StringBuffer(var4[0]);

            for(int var7 = 1; var7 < var4.length; ++var7) {
               var6.append(var5).append(var4[var7]);
            }

            var15 = var6.toString();
         }

         String var11 = var15;
         this.b(";");
         c(var11);
         lq var8 = new lq(var3, var11);
         if (var1.a != null) {
            throw new RuntimeException("Re-setting package declaration");
         }

         var1.a = var8;
      }

      while(this.a("import")) {
         this.a("import");
         fp var9 = this.a();
         this.b(";");
         var1.a.addElement(var9);
      }

      while(true) {
         while(!this.a.b().b()) {
            if (this.b(";")) {
               this.a();
            } else {
               Object var16;
               label39: {
                  String var10 = this.a.a();
                  short var13 = this.a();
                  Object var14;
                  if (this.a("class")) {
                     this.a();
                     var14 = (ow)this.a(var10, var13, fk.c);
                  } else {
                     if (!this.a("interface")) {
                        this.g("Unexpected token \"" + this.a.b() + "\" in class or interface declaration");
                        var16 = null;
                        break label39;
                     }

                     this.a();
                     var14 = (hq)this.a(var10, var13, hr.b);
                  }

                  var16 = var14;
               }

               Object var12 = var16;
               var1.b.addElement(var12);
               ((hp)var12).a(var1);
            }
         }

         return var1;
      }
   }

   private fp a() {
      aq var1 = this.a.a();
      boolean var2;
      if (this.a("static")) {
         var2 = true;
         this.a();
      } else {
         var2 = false;
      }

      Vector var3;
      (var3 = new Vector()).addElement(this.b());

      String[] var4;
      while(this.b(".")) {
         this.b(".");
         if (this.b("*")) {
            this.a();
            var4 = new String[var3.size()];
            var3.copyInto(var4);
            if (var2) {
               return new x(var1, var4);
            }

            return new bf(var1, var4);
         }

         var3.addElement(this.b());
      }

      var4 = new String[var3.size()];
      var3.copyInto(var4);
      return (fp)(var2 ? new or(var1, var4) : new my(var1, var4));
   }

   private String[] a() {
      if (!this.a.b().d()) {
         this.g("Identifier expected");
      }

      Vector var1;
      (var1 = new Vector()).addElement(this.b());

      while(this.b(".") && this.a.c().d()) {
         this.a();
         var1.addElement(this.b());
      }

      String[] var2 = new String[var1.size()];
      var1.copyInto(var2);
      return var2;
   }

   private short a() {
      short var1;
      String var2;
      int var3;
      for(var1 = 0; this.a.b().a() && (var3 = (var2 = this.a.b().a()) == "public" ? 1 : (var2 == "protected" ? 4 : (var2 == "private" ? 2 : (var2 == "static" ? 8 : (var2 == "abstract" ? 1024 : (var2 == "final" ? 16 : (var2 == "native" ? 256 : (var2 == "synchronized" ? 32 : (var2 == "transient" ? 128 : (var2 == "volatile" ? 64 : (var2 == "strictfp" ? 2048 : -1))))))))))) != -1; var1 = (short)(var1 | var3)) {
         this.a();
         if ((var1 & var3) != 0) {
            this.g("Duplicate modifier \"" + var2 + "\"");
         }

         for(int var5 = 0; var5 < a.length; ++var5) {
            short var4 = a[var5];
            if ((var3 & var4) != 0 && (var1 & var4) != 0) {
               this.g("Only one of \"" + fd.a(var4) + "\" allowed");
            }
         }
      }

      return var1;
   }

   private fe a(String var1, short var2, fk var3) {
      aq var4 = this.a.a();
      String var5;
      d(var5 = this.b());
      jb var6 = null;
      if (this.a("extends")) {
         this.a();
         var6 = this.a();
      }

      jb[] var7 = new jb[0];
      if (this.a("implements")) {
         this.a();
         var7 = this.a();
      }

      Object var8;
      if (var3 == fk.c) {
         var8 = new ow(var4, var1, var2, var5, var6, var7);
      } else if (var3 == fk.b) {
         var8 = new er(var4, var1, var2, var5, var6, var7);
      } else {
         if (var3 != fk.a) {
            throw new RuntimeException("SNO: Class declaration in unexpected context " + var3);
         }

         var8 = new jl(var4, var1, var2, var5, var6, var7);
      }

      this.a((kg)var8);
      return (fe)var8;
   }

   private void a(kg var1) {
      if (!this.b("{")) {
         this.g("\"{\" expected at start of class body");
      }

      this.a();

      while(true) {
         while(!this.b("}")) {
            if (this.b(";")) {
               this.a();
            } else {
               String var4 = this.a.a();
               short var5 = this.a();
               if (this.b("{")) {
                  if ((var5 & -9) != 0) {
                     this.g("Only modifier \"static\" allowed on initializer");
                  }

                  lh var17 = new lh(this.a.a(), (var5 & 8) != 0, this.a());
                  var1.a((ht)var17);
               } else if (this.a("void")) {
                  aq var16 = this.a.a();
                  this.a();
                  String var19 = this.b();
                  var1.a((kl)this.a(var4, var5, new om(var16, 0), var19));
               } else if (this.a("class")) {
                  this.a();
                  var1.a((ah)((ah)this.a(var4, var5, fk.b)));
               } else if (this.a("interface")) {
                  this.a();
                  var1.a((ah)((ah)this.a(var4, (short)(var5 | 8), hr.a)));
               } else if (var1 instanceof fe && this.a.b().b(((fe)var1).a) && this.a.c().c("(")) {
                  String var10002 = var4;
                  short var13 = var5;
                  String var3 = var10002;
                  this.a.a();
                  aq var14 = null;
                  this.b();
                  ct[] var15 = this.a();
                  jb[] var18;
                  if (this.a("throws")) {
                     this.a();
                     var18 = this.a();
                  } else {
                     var18 = new jb[0];
                  }

                  var14 = this.a.a();
                  this.b("{");
                  ew var20 = null;
                  il var9 = new il(var14);
                  if (this.a(new String[]{"this", "super", "new", "void", "byte", "char", "short", "int", "long", "float", "double", "boolean"}) || this.a.b().c() || this.a.b().d()) {
                     nr var10;
                     if ((var10 = this.a()) instanceof ew) {
                        this.b(";");
                        var20 = (ew)var10;
                     } else {
                        Object var21;
                        if (this.a.b().d()) {
                           gs var11 = var10.b();
                           var21 = new jv(var10.a(), (short)0, var11, this.a());
                           this.b(";");
                        } else {
                           var21 = new nh(var10.b());
                           this.b(";");
                        }

                        var9.a((ov)var21);
                     }
                  }

                  var9.a(this.a());
                  this.b("}");
                  var1.a(new jf(var14, var3, var13, var15, var18, var20, var9));
               } else {
                  gs var6 = this.a();
                  aq var7 = this.a.a();
                  String var8 = this.b();
                  if (this.b("(")) {
                     var1.a((kl)this.a(var4, var5, var6, var8));
                  } else {
                     gb var12 = new gb(var7, var4, var5, var6, this.a(var8));
                     this.b(";");
                     var1.a((ht)var12);
                  }
               }
            }
         }

         this.a();
         return;
      }
   }

   private df a(String var1, short var2, hr var3) {
      aq var4 = this.a.a();
      String var5;
      d(var5 = this.b());
      jb[] var6 = new jb[0];
      if (this.a("extends")) {
         this.a();
         var6 = this.a();
      }

      Object var7;
      if (var3 == hr.b) {
         var7 = new hq(var4, var1, var2, var5, var6);
      } else {
         if (var3 != hr.a) {
            throw new RuntimeException("SNO: Interface declaration in unexpected context " + var3);
         }

         var7 = new in(var4, var1, var2, var5, var6);
      }

      this.a((df)var7);
      return (df)var7;
   }

   private void a(df var1) {
      this.b("{");

      while(!this.b("}")) {
         if (this.b(";")) {
            this.a();
         } else {
            String var2 = this.a.a();
            short var3 = this.a();
            if (this.a("void")) {
               aq var4 = this.a.a();
               this.a();
               String var5 = this.b();
               var1.a(this.a(var2, (short)(var3 | 1024 | 1), new om(var4, 0), var5));
            } else if (this.a("class")) {
               this.a();
               var1.a((ah)this.a(var2, (short)(var3 | 8 | 1), fk.b));
            } else if (this.a("interface")) {
               this.a();
               var1.a((ah)this.a(var2, (short)(var3 | 8 | 1), hr.a));
            } else {
               gs var8 = this.a();
               if (!this.a.b().d()) {
                  this.g("Identifier expected in member declaration");
               }

               aq var9 = this.a.a();
               String var6 = this.b();
               if (this.b("(")) {
                  var1.a(this.a(var2, (short)(var3 | 1024 | 1), var8, var6));
               } else {
                  gb var7 = new gb(var9, var2, (short)(var3 | 1 | 8 | 16), var8, this.a(var6));
                  var1.a(var7);
               }
            }
         }
      }

      this.a();
   }

   private kl a(String var1, short var2, gs var3, String var4) {
      aq var5 = this.a.a();
      String var8 = var4;
      char var7;
      if (Character.isLowerCase(var4.charAt(0))) {
         for(int var6 = 0; var6 < var8.length() && (Character.isLowerCase(var7 = var8.charAt(var6)) || Character.isUpperCase(var7) || Character.isDigit(var7)); ++var6) {
         }
      }

      ct[] var10 = this.a();

      for(int var11 = this.a(); var11 > 0; --var11) {
         var3 = new nx((gs)var3);
      }

      jb[] var12;
      if (this.a("throws")) {
         this.a();
         var12 = this.a();
      } else {
         var12 = new jb[0];
      }

      il var9;
      if (this.b(";")) {
         if ((var2 & 1280) == 0) {
            this.g("Non-abstract, non-native method must have a body");
         }

         this.a();
         var9 = null;
      } else {
         if ((var2 & 1280) != 0) {
            this.g("Abstract or native method must not have a body");
         }

         var9 = this.a();
      }

      return new kl(var5, var1, var2, (gs)var3, var4, var10, var12, var9);
   }

   private ho a() {
      return (ho)(this.b("{") ? this.a() : this.a().b());
   }

   private fu a() {
      aq var1 = this.a.a();
      this.b("{");

      Vector var2;
      for(var2 = new Vector(); !this.b("}"); this.a()) {
         var2.addElement(this.a());
         if (this.b("}")) {
            break;
         }

         if (!this.b(",")) {
            this.g("\",\" or \"}\" expected");
         }
      }

      this.a();
      ho[] var3 = new ho[var2.size()];
      var2.copyInto(var3);
      return new fu(var1, var3);
   }

   private ct[] a() {
      this.b("(");
      if (this.b(")")) {
         this.a();
         return new ct[0];
      } else {
         Vector var1 = new Vector();

         while(true) {
            var1.addElement(this.a());
            if (!this.b(",")) {
               this.b(")");
               ct[] var2 = new ct[var1.size()];
               var1.copyInto(var2);
               return var2;
            }

            this.a();
         }
      }
   }

   private ct a() {
      boolean var1;
      if (var1 = this.a("final")) {
         this.a();
      }

      Object var2 = this.a();
      aq var3 = this.a.a();
      String var4;
      f(var4 = this.b());

      for(int var5 = this.a(); var5 > 0; --var5) {
         var2 = new nx((gs)var2);
      }

      return new ct(var3, var1, (gs)var2, var4);
   }

   private int a() {
      int var1;
      for(var1 = 0; this.a.b().c("[") && this.a.c().c("]"); ++var1) {
         this.a();
         this.a();
      }

      return var1;
   }

   private il a() {
      il var1 = new il(this.a.a());
      this.b("{");
      var1.a(this.a());
      this.b("}");
      return var1;
   }

   private Vector a() {
      Object var10001;
      Vector var1;
      for(var1 = new Vector(); !this.b("}") && !this.a("case") && !this.a("default"); var1.addElement(var10001)) {
         if ((!this.a.b().d() || !this.a.c().c(":")) && !this.a(new String[]{"if", "for", "while", "do", "try", "switch", "synchronized", "return", "throw", "break", "continue"}) && !this.b(new String[]{"{", ";"})) {
            if (this.a("class")) {
               String var3 = this.a.a();
               this.a();
               jl var4 = (jl)this.a(var3, (short)0, (fk)fk.a);
               var10001 = new ox(var4);
            } else {
               jv var6;
               gs var8;
               if (this.a("final")) {
                  aq var5 = this.a.a();
                  this.a();
                  var8 = this.a();
                  var6 = new jv(var5, (short)16, var8, this.a());
                  this.b(";");
                  var10001 = var6;
               } else {
                  nr var7 = this.a();
                  if (this.b(";")) {
                     this.a();
                     var10001 = new nh(var7.b());
                  } else {
                     var8 = var7.b();
                     var6 = new jv(var7.a(), (short)0, var8, this.a());
                     this.b(";");
                     var10001 = var6;
                  }
               }
            }
         } else {
            var10001 = this.a();
         }
      }

      return var1;
   }

   private cb[] a() {
      Vector var1 = new Vector();

      while(true) {
         cb var2;
         f((var2 = this.a()).a);
         var1.addElement(var2);
         if (!this.b(",")) {
            cb[] var3 = new cb[var1.size()];
            var1.copyInto(var3);
            return var3;
         }

         this.a();
      }
   }

   private cb[] a(String var1) {
      Vector var2 = new Vector();
      cb var4;
      e((var4 = this.a(var1)).a);
      var2.addElement(var4);

      while(this.b(",")) {
         this.a();
         e((var4 = this.a()).a);
         var2.addElement(var4);
      }

      cb[] var3 = new cb[var2.size()];
      var2.copyInto(var3);
      return var3;
   }

   private cb a() {
      return this.a(this.b());
   }

   private cb a(String var1) {
      aq var2 = this.a.a();
      int var3 = this.a();
      ho var4 = null;
      if (this.b("=")) {
         this.a();
         var4 = this.a();
      }

      return new cb(var2, var1, var3, var4);
   }

   private ic a() {
      if (this.a.b().d() && this.a.c().c(":")) {
         String var15 = this.b();
         this.b(":");
         return new p(this.a.a(), var15, this.a());
      } else {
         ms var1;
         Object var10000;
         if ((var1 = this.a.b()).c("{")) {
            var10000 = this.a();
         } else {
            mu var2;
            aq var6;
            if (var1.a("if")) {
               var6 = this.a.a();
               this.a("if");
               this.b("(");
               var2 = this.a().b();
               this.b(")");
               ic var3 = this.a();
               ic var4 = null;
               if (this.a("else")) {
                  this.a();
                  var4 = this.a();
               }

               var10000 = new hx(var6, var2, var3, var4);
            } else {
               mu var12;
               if (var1.a("for")) {
                  var6 = this.a.a();
                  this.a("for");
                  this.b("(");
                  Object var7 = null;
                  if (!this.b(";")) {
                     fc var8 = this;
                     gs var16;
                     if (this.a(new String[]{"final", "byte", "short", "char", "int", "long", "float", "double", "boolean"})) {
                        short var10 = this.a();
                        var16 = this.a();
                        var10000 = new jv(this.a.a(), var10, var16, this.a());
                     } else {
                        nr var11 = this.a();
                        if (this.a.b().d()) {
                           var16 = var11.b();
                           var10000 = new jv(var11.a(), (short)0, var16, this.a());
                        } else if (!this.b(",")) {
                           var10000 = new nh(var11.b());
                        } else {
                           this.a();
                           Vector var17;
                           (var17 = new Vector()).addElement(new nh(var11.b()));

                           while(true) {
                              var17.addElement(new nh(var8.a().b()));
                              if (!var8.b(",")) {
                                 il var9;
                                 (var9 = new il(var11.a())).a(var17);
                                 var10000 = var9;
                                 break;
                              }

                              var8.a();
                           }
                        }
                     }

                     var7 = var10000;
                  }

                  this.b(";");
                  var12 = null;
                  if (!this.b(";")) {
                     var12 = this.a().b();
                  }

                  this.b(";");
                  mu[] var19 = null;
                  if (!this.b(")")) {
                     var19 = this.a();
                  }

                  this.b(")");
                  var10000 = new os(var6, (ov)var7, var12, var19, this.a());
               } else if (var1.a("while")) {
                  var6 = this.a.a();
                  this.a("while");
                  this.b("(");
                  var2 = this.a().b();
                  this.b(")");
                  var10000 = new bh(var6, var2, this.a());
               } else if (var1.a("do")) {
                  var6 = this.a.a();
                  this.a("do");
                  ic var13 = this.a();
                  this.a("while");
                  this.b("(");
                  var12 = this.a().b();
                  this.b(")");
                  this.b(";");
                  var10000 = new eg(var6, var13, var12);
               } else if (var1.a("try")) {
                  var10000 = this.b();
               } else if (var1.a("switch")) {
                  var10000 = this.c();
               } else if (var1.a("synchronized")) {
                  var6 = this.a.a();
                  this.a("synchronized");
                  this.b("(");
                  var2 = this.a().b();
                  this.b(")");
                  var10000 = new pk(var6, var2, this.a());
               } else if (var1.a("return")) {
                  var6 = this.a.a();
                  this.a("return");
                  var2 = this.b(";") ? null : this.a().b();
                  this.b(";");
                  var10000 = new y(var6, var2);
               } else if (var1.a("throw")) {
                  var6 = this.a.a();
                  this.a("throw");
                  var2 = this.a().b();
                  this.b(";");
                  var10000 = new fi(var6, var2);
               } else {
                  String var18;
                  if (var1.a("break")) {
                     var6 = this.a.a();
                     this.a("break");
                     var18 = null;
                     if (this.a.b().d()) {
                        var18 = this.b();
                     }

                     this.b(";");
                     var10000 = new ki(var6, var18);
                  } else if (var1.a("continue")) {
                     var6 = this.a.a();
                     this.a("continue");
                     var18 = null;
                     if (this.a.b().d()) {
                        var18 = this.b();
                     }

                     this.b(";");
                     var10000 = new af(var6, var18);
                  } else if (var1.c(";")) {
                     var6 = this.a.a();
                     this.b(";");
                     var10000 = new g(var6);
                  } else {
                     mu var14 = this.a().b();
                     this.b(";");
                     var10000 = new nh(var14);
                  }
               }
            }
         }

         return (ic)var10000;
      }
   }

   private ic b() {
      aq var1 = this.a.a();
      this.a("try");
      il var2 = this.a();
      Vector var3 = new Vector();

      while(this.a("catch")) {
         aq var4 = this.a.a();
         this.a();
         this.b("(");
         ct var5 = this.a();
         this.b(")");
         var3.addElement(new iq(var4, var5, this.a()));
      }

      il var6 = null;
      if (this.a("finally")) {
         this.a();
         var6 = this.a();
      }

      if (var3.size() == 0 && var6 == null) {
         this.g("\"try\" statement must have at least one \"catch\" clause or a \"finally\" clause");
      }

      return new ga(var1, var2, var3, var6);
   }

   private ic c() {
      aq var1 = this.a.a();
      this.a("switch");
      this.b("(");
      mu var2 = this.a().b();
      this.b(")");
      this.b("{");
      Vector var3 = new Vector();

      while(!this.b("}")) {
         aq var4 = this.a.a();
         boolean var5 = false;
         Vector var6 = new Vector();

         do {
            if (this.a("case")) {
               this.a();
               var6.addElement(this.a().b());
            } else if (this.a("default")) {
               this.a();
               if (var5) {
                  this.g("Duplicate \"default\" label");
               }

               var5 = true;
            } else {
               this.g("\"case\" or \"default\" expected");
            }

            this.b(":");
         } while(this.a(new String[]{"case", "default"}));

         jk var8 = new jk(var4, var6, var5, this.a());
         var3.addElement(var8);
      }

      this.a();
      return new bq(var1, var2, var3);
   }

   private mu[] a() {
      Vector var1 = new Vector();

      while(true) {
         var1.addElement(this.a().b());
         if (!this.b(",")) {
            mu[] var3 = new mu[var1.size()];
            var1.copyInto(var3);
            return var3;
         }

         this.a();
      }
   }

   private gs a() {
      ms var1 = this.a.b();
      byte var2 = -1;
      if (var1.a("byte")) {
         var2 = 1;
      } else if (var1.a("short")) {
         var2 = 2;
      } else if (var1.a("char")) {
         var2 = 3;
      } else if (var1.a("int")) {
         var2 = 4;
      } else if (var1.a("long")) {
         var2 = 5;
      } else if (var1.a("float")) {
         var2 = 6;
      } else if (var1.a("double")) {
         var2 = 7;
      } else if (var1.a("boolean")) {
         var2 = 8;
      }

      Object var4;
      if (var2 != -1) {
         var4 = new om(var1.a(), var2);
         this.a();
      } else {
         var4 = this.a();
      }

      for(int var3 = this.a(); var3 > 0; --var3) {
         var4 = new nx((gs)var4);
      }

      return (gs)var4;
   }

   private jb a() {
      return new jb(this.a.a(), this.a());
   }

   private jb[] a() {
      Vector var1;
      (var1 = new Vector()).addElement(this.a());

      while(this.b(",")) {
         this.a();
         var1.addElement(this.a());
      }

      jb[] var2 = new jb[var1.size()];
      var1.copyInto(var2);
      return var2;
   }

   private nr a() {
      nr var1 = this.b();
      if (this.b(new String[]{"=", "+=", "-=", "*=", "/=", "&=", "|=", "^=", "%=", "<<=", ">>=", ">>>="})) {
         aq var2 = this.a.a();
         String var3 = this.a();
         jt var5 = var1.b();
         mu var4 = this.a().b();
         return new pq(var2, var5, var3, var4);
      } else {
         return var1;
      }
   }

   private nr b() {
      fc var1 = this;

      Object var2;
      aq var3;
      for(var2 = this.c(); var1.b("||"); var2 = new qa(var3, ((nr)var2).b(), "||", var1.c().b())) {
         var3 = var1.a.a();
         var1.a();
      }

      Object var5 = var2;
      if (!this.b("?")) {
         return (nr)var2;
      } else {
         aq var7 = this.a.a();
         this.a();
         mu var6 = ((nr)var5).b();
         mu var8 = this.a().b();
         this.b(":");
         mu var4 = this.b().b();
         return new da(var7, var6, var8, var4);
      }
   }

   private nr c() {
      Object var1;
      aq var2;
      for(var1 = this.d(); this.b("&&"); var1 = new qa(var2, ((nr)var1).b(), "&&", this.d().b())) {
         var2 = this.a.a();
         this.a();
      }

      return (nr)var1;
   }

   private nr d() {
      Object var1;
      aq var2;
      for(var1 = this.e(); this.b("|"); var1 = new qa(var2, ((nr)var1).b(), "|", this.e().b())) {
         var2 = this.a.a();
         this.a();
      }

      return (nr)var1;
   }

   private nr e() {
      Object var1;
      aq var2;
      for(var1 = this.f(); this.b("^"); var1 = new qa(var2, ((nr)var1).b(), "^", this.f().b())) {
         var2 = this.a.a();
         this.a();
      }

      return (nr)var1;
   }

   private nr f() {
      Object var1;
      aq var2;
      for(var1 = this.g(); this.b("&"); var1 = new qa(var2, ((nr)var1).b(), "&", this.g().b())) {
         var2 = this.a.a();
         this.a();
      }

      return (nr)var1;
   }

   private nr g() {
      Object var1;
      for(var1 = this.h(); this.b(new String[]{"==", "!="}); var1 = new qa(this.a.a(), ((nr)var1).b(), this.a(), this.h().b())) {
      }

      return (nr)var1;
   }

   private nr h() {
      Object var1 = this.i();

      while(true) {
         while(!this.a("instanceof")) {
            if (!this.b(new String[]{"<", ">", "<=", ">="})) {
               return (nr)var1;
            }

            var1 = new qa(this.a.a(), ((nr)var1).b(), this.a(), this.i().b());
         }

         aq var2 = this.a.a();
         this.a();
         var1 = new nl(var2, ((nr)var1).b(), this.a());
      }
   }

   private nr i() {
      Object var1;
      for(var1 = this.j(); this.b(new String[]{"<<", ">>", ">>>"}); var1 = new qa(this.a.a(), ((nr)var1).b(), this.a(), this.j().b())) {
      }

      return (nr)var1;
   }

   private nr j() {
      Object var1;
      for(var1 = this.k(); this.b(new String[]{"+", "-"}); var1 = new qa(this.a.a(), ((nr)var1).b(), this.a(), this.k().b())) {
      }

      return (nr)var1;
   }

   private nr k() {
      Object var1;
      for(var1 = this.l(); this.b(new String[]{"*", "/", "%"}); var1 = new qa(this.a.a(), ((nr)var1).b(), this.a(), this.l().b())) {
      }

      return (nr)var1;
   }

   private nr l() {
      if (this.b(new String[]{"++", "--"})) {
         return new gz(this.a.a(), this.a(), this.l().b());
      } else if (this.b(new String[]{"+", "-", "~", "!"})) {
         return new kh(this.a.a(), this.a(), this.l().b());
      } else {
         Object var2;
         int var3;
         int var4;
         Object var10000;
         aq var22;
         if (this.b("(")) {
            this.a();
            if (this.a(new String[]{"boolean", "char", "byte", "short", "int", "long", "float", "double"})) {
               var2 = this.a();
               var3 = this.a();
               this.b(")");

               for(var4 = 0; var4 < var3; ++var4) {
                  var2 = new nx((gs)var2);
               }

               var10000 = new nj(this.a.a(), (gs)var2, this.l().b());
            } else {
               nr var7 = this.a();
               this.b(")");
               var10000 = !this.a.b().c() && !this.a.b().d() && !this.b(new String[]{"(", "~", "!"}) && !this.a(new String[]{"this", "super", "new"}) ? new ke(var7.a(), var7.b()) : new nj(this.a.a(), var7.b(), this.l().b());
            }
         } else if (this.a.b().c()) {
            ms var8;
            if (!(var8 = this.a.a()).c()) {
               this.g("Literal expected");
            }

            var10000 = new ne(var8.a(), var8.a());
         } else {
            aq var9;
            if (this.a.b().d()) {
               var9 = this.a.a();
               String[] var10 = this.a();
               if (this.b("(")) {
                  var10000 = new gt(this.a.a(), var10.length == 1 ? null : new gp(var9, var10, var10.length - 1), var10[var10.length - 1], this.c());
               } else if (this.b("[") && this.a.c().c("]")) {
                  Object var14 = new jb(var9, var10);
                  int var5 = this.a();

                  for(int var11 = 0; var11 < var5; ++var11) {
                     var14 = new nx((gs)var14);
                  }

                  if (this.b(".") && this.a.c().a("class")) {
                     this.a();
                     var9 = this.a.a();
                     this.a();
                     var10000 = new pb(var9, (gs)var14);
                  } else {
                     var10000 = var14;
                  }
               } else {
                  var10000 = new gp(this.a.a(), var10);
               }
            } else if (this.a("this")) {
               var9 = this.a.a();
               this.a();
               var10000 = this.b("(") ? new dh(var9, this.c()) : new cc(var9);
            } else if (this.a("super")) {
               this.a();
               if (this.b("(")) {
                  var10000 = new nd(this.a.a(), (mu)null, this.c());
               } else {
                  this.b(".");
                  String var15 = this.b();
                  var10000 = this.b("(") ? new u(this.a.a(), var15, this.c()) : new cu(this.a.a(), (gs)null, var15);
               }
            } else if (this.a("new")) {
               var9 = this.a.a();
               this.a();
               gs var12;
               if ((var12 = this.a()) instanceof nx) {
                  var10000 = new ey(var9, (nx)var12, this.a());
               } else if (var12 instanceof jb && this.b("(")) {
                  mu[] var16 = this.c();
                  if (this.b("{")) {
                     eo var17 = new eo(this.a.a(), var12);
                     this.a((kg)var17);
                     var10000 = new mr(var9, (mu)null, var17, var16);
                  } else {
                     var10000 = new ok(var9, (mu)null, var12, var16);
                  }
               } else {
                  var10000 = new d(var9, var12, this.b(), this.a());
               }
            } else if (this.a(new String[]{"boolean", "char", "byte", "short", "int", "long", "float", "double"})) {
               var2 = this.a();
               var3 = this.a();

               for(var4 = 0; var4 < var3; ++var4) {
                  var2 = new nx((gs)var2);
               }

               if (this.b(".") && this.a.c().a("class")) {
                  this.a();
                  var22 = this.a.a();
                  this.a();
                  var10000 = new pb(var22, (gs)var2);
               } else {
                  var10000 = var2;
               }
            } else {
               label146: {
                  if (this.a("void")) {
                     this.a();
                     if (this.b(".") && this.a.c().a("class")) {
                        this.a();
                        var9 = this.a.a();
                        this.a();
                        var10000 = new pb(var9, new om(var9, 0));
                        break label146;
                     }

                     this.g("\"void\" encountered in wrong context");
                  }

                  this.g("Unexpected token \"" + this.a.b() + "\" in primary");
                  var10000 = null;
               }
            }
         }

         Object var1;
         for(var1 = var10000; this.b(new String[]{".", "["}); var1 = var10000) {
            aq var13;
            if (this.b(".")) {
               this.a();
               if (this.a.b().d()) {
                  String var20 = this.b();
                  var10000 = this.b("(") ? new gt(this.a.a(), ((nr)var1).b(), var20, this.c()) : new mz(this.a.a(), ((nr)var1).b(), var20);
                  continue;
               }

               if (this.a("this")) {
                  var13 = this.a.a();
                  this.a();
                  var10000 = new py(var13, ((nr)var1).b());
                  continue;
               }

               if (this.a("super")) {
                  var13 = this.a.a();
                  this.a();
                  if (this.b("(")) {
                     var10000 = new nd(var13, ((nr)var1).b(), this.c());
                     continue;
                  }

                  this.b(".");
                  String var23 = this.b();
                  if (!this.b("(")) {
                     var10000 = new cu(var13, ((nr)var1).b(), var23);
                     continue;
                  }

                  this.g("Qualified superclass method invocation NYI");
               }

               if (this.a("new")) {
                  mu var18 = ((nr)var1).a();
                  var22 = this.a.a();
                  this.a();
                  String var19 = this.b();
                  kv var24 = new kv(var22, var18, var19);
                  mu[] var21 = this.c();
                  if (this.b("{")) {
                     eo var26 = new eo(this.a.a(), var24);
                     this.a((kg)var26);
                     var10000 = new mr(var22, var18, var26, var21);
                  } else {
                     var10000 = new ok(var22, var18, var24, var21);
                  }
                  continue;
               }

               if (this.a("class")) {
                  var13 = this.a.a();
                  this.a();
                  var10000 = new pb(var13, ((nr)var1).b());
                  continue;
               }

               this.g("Unexpected selector \"" + this.a.b() + "\" after \".\"");
            }

            if (this.b("[")) {
               var13 = this.a.a();
               this.a();
               mu var25 = this.a().b();
               this.b("]");
               var10000 = new kr(var13, ((nr)var1).b(), var25);
            } else {
               this.g("Unexpected token \"" + this.a.b() + "\" in selector");
               var10000 = null;
            }
         }

         while(this.b(new String[]{"++", "--"})) {
            var1 = new gz(this.a.a(), ((nr)var1).b(), this.a());
         }

         return (nr)var1;
      }
   }

   private mu[] b() {
      Vector var1;
      (var1 = new Vector()).addElement(this.a());

      while(this.b("[") && !this.a.c().c("]")) {
         var1.addElement(this.a());
      }

      mu[] var2 = new mu[var1.size()];
      var1.copyInto(var2);
      return var2;
   }

   private mu a() {
      this.b("[");
      mu var1 = this.a().b();
      this.b("]");
      return var1;
   }

   private mu[] c() {
      this.b("(");
      if (this.b(")")) {
         this.a();
         return new mu[0];
      } else {
         fc var1 = this;
         Vector var2 = new Vector();

         while(true) {
            var2.addElement(var1.a().b());
            if (!var1.b(",")) {
               mu[] var4 = new mu[var2.size()];
               var2.copyInto(var4);
               this.b(")");
               return var4;
            }

            var1.a();
         }
      }
   }

   private void a() {
      this.a.a();
   }

   private boolean a(String var1) {
      return this.a.b().a(var1);
   }

   private boolean a(String[] var1) {
      return this.a.b().a(var1);
   }

   private void a(String var1) {
      if (!this.a.a().a(var1)) {
         this.g("\"" + var1 + "\" expected");
      }

   }

   private boolean b(String var1) {
      return this.a.b().c(var1);
   }

   private boolean b(String[] var1) {
      return this.a.b().b(var1);
   }

   private String a() {
      ms var1;
      if (!(var1 = this.a.a()).e()) {
         this.g("Operator expected");
      }

      return var1.c();
   }

   private void b(String var1) {
      if (!this.a.a().c(var1)) {
         this.g("Operator \"" + var1 + "\" expected");
      }

   }

   private String b() {
      ms var1;
      if (!(var1 = this.a.a()).d()) {
         this.g("Identifier expected");
      }

      return var1.b();
   }

   private static void c(String var0) {
      if (Character.isLowerCase(var0.charAt(0))) {
         for(int var1 = 0; var1 < var0.length(); ++var1) {
            char var2;
            if (!Character.isLowerCase(var2 = var0.charAt(var1)) && var2 != '_' && var2 != '.') {
               return;
            }
         }

      }
   }

   private static void d(String var0) {
      if (Character.isUpperCase(var0.charAt(0))) {
         for(int var1 = 0; var1 < var0.length(); ++var1) {
            char var2;
            if (!Character.isLowerCase(var2 = var0.charAt(var1)) && !Character.isUpperCase(var2) && !Character.isDigit(var2)) {
               return;
            }
         }

      }
   }

   private static void e(String var0) {
      int var1;
      char var2;
      if (Character.isUpperCase(var0.charAt(0))) {
         for(var1 = 0; var1 < var0.length(); ++var1) {
            if (!Character.isUpperCase(var2 = var0.charAt(var1)) && !Character.isDigit(var2) && var2 != '_') {
               return;
            }
         }

      } else {
         if (Character.isLowerCase(var0.charAt(0))) {
            for(var1 = 0; var1 < var0.length(); ++var1) {
               if (!Character.isLowerCase(var2 = var0.charAt(var1)) && !Character.isUpperCase(var2) && !Character.isDigit(var2)) {
                  return;
               }
            }
         }

      }
   }

   private static void f(String var0) {
      if (Character.isLowerCase(var0.charAt(0))) {
         for(int var1 = 0; var1 < var0.length(); ++var1) {
            char var2;
            if (!Character.isLowerCase(var2 = var0.charAt(var1)) && !Character.isUpperCase(var2) && !Character.isDigit(var2)) {
               return;
            }
         }

      }
   }

   private void g(String var1) {
      throw new pc(var1, this.a.a());
   }
}
