import java.util.Vector;

public final class o {
   private int a;
   private int b;
   private int c;
   private int d;
   private int e;
   private int f;
   private int g;
   private int h;
   private int i;
   private int j;
   private int k;
   private int l;
   private int m;
   private int n;
   private int o;
   private Vector b = new Vector();
   private boolean a = false;
   public Vector a = new Vector();

   public o() {
      this.a();
   }

   private void a() {
      this.m = 0;
      this.n = 0;
      this.o = 0;
      this.a = -1;
      this.i = -1;
      this.l = -1;
      this.b = -1;
      this.c = -1;
      this.d = -1;
      this.e = -1;
      this.f = -1;
      this.g = -1;
      this.h = -1;
      this.i = -1;
      this.j = -1;
      this.k = -1;
   }

   private void b() {
      this.a();

      int var1;
      pv var2;
      for(var1 = this.b.size() - 1; var1 >= 0; --var1) {
         if ((var2 = (pv)this.b.elementAt(var1)).a == 2) {
            this.a = var1;
         } else if (var2.a == 3) {
            this.b = var1;
         } else if (var2.a == 4) {
            this.c = var1;
         } else if (var2.a == 5) {
            this.d = var1;
         } else if (var2.a == 6) {
            this.e = var1;
         } else if (var2.a == 7) {
            this.f = var1;
         } else if (var2.a == 8) {
            this.g = var1;
         } else if (var2.a == 9) {
            this.h = var1;
         } else if (var2.a == 10) {
            this.i = var1;
         } else if (var2.a == 11) {
            this.j = var1;
         } else if (var2.a == 12) {
            this.k = var1;
         } else if (var2.a == 13) {
            this.l = var1;
         }
      }

      for(var1 = 0; var1 < this.b.size(); ++var1) {
         if ((var2 = (pv)this.b.elementAt(var1)).a == 2) {
            ++this.m;
         } else if (var2.a == 10) {
            ++this.n;
         } else if (var2.a == 13) {
            ++this.o;
         }
      }

   }

   public final boolean a() {
      return this.j != -1 && this.k != 1;
   }

   public final pv[] a(String[] var1) {
      int var2;
      if ((var2 = this.b.size()) == 0) {
         return null;
      } else {
         boolean var3 = false;
         boolean var4 = false;
         String var5 = "";
         boolean var6 = !this.a();

         int var7;
         label261:
         for(var7 = 0; var7 < var1.length; ++var7) {
            boolean var8 = false;
            int var9 = 0;

            while(true) {
               while(true) {
                  int var10 = var1[var7].length();
                  byte var11 = -1;
                  String var12 = "";

                  pv var13;
                  int var14;
                  int var15;
                  for(var14 = this.g; var14 != -1 && var14 < var2 && (var13 = (pv)this.b.elementAt(var14)).a == 8; ++var14) {
                     if ((var15 = var1[var7].indexOf(var13.a, var9)) != -1 && var15 < var10) {
                        var12 = var13.a;
                        var11 = 8;
                        var10 = var15;
                     }
                  }

                  int var18;
                  pv var19;
                  for(var15 = this.d; var15 != -1 && var15 < var2 && (var19 = (pv)this.b.elementAt(var15)).a == 5; ++var15) {
                     if ((var18 = var1[var7].indexOf(var19.a, var9)) != -1 && var18 < var10) {
                        var12 = var19.a;
                        var11 = 5;
                        var10 = var18;
                     }
                  }

                  pv var20;
                  for(var18 = this.b; var18 != -1 && var18 < var2 && (var20 = (pv)this.b.elementAt(var18)).a == 3; ++var18) {
                     if ((var14 = var1[var7].indexOf(var20.a, var9)) != -1 && var14 < var10) {
                        var12 = var20.a;
                        var11 = 3;
                        var10 = var14;
                     }
                  }

                  for(var14 = this.c; var14 != -1 && var14 < var2 && (var13 = (pv)this.b.elementAt(var14)).a == 4; ++var14) {
                     if ((var15 = var1[var7].indexOf(var13.a, var9)) != -1 && var15 < var10) {
                        var12 = var13.a;
                        var11 = 4;
                        var10 = var15;
                     }
                  }

                  for(var15 = this.j; var15 != -1 && var15 < var2 && (var19 = (pv)this.b.elementAt(var15)).a == 11; ++var15) {
                     if ((var18 = var1[var7].indexOf(var19.a, var9)) != -1 && var18 < var10) {
                        var12 = var19.a;
                        var11 = 11;
                        var10 = var18;
                     }
                  }

                  for(var18 = this.k; var18 != -1 && var18 < var2 && (var20 = (pv)this.b.elementAt(var18)).a == 12; ++var18) {
                     if ((var14 = var1[var7].indexOf(var20.a, var9)) != -1 && var14 < var10) {
                        var12 = var20.a;
                        var11 = 12;
                        var10 = var14;
                     }
                  }

                  if (var10 == var1[var7].length()) {
                     continue label261;
                  }

                  var9 = var10 + var12.length();
                  if (var11 == 8 && var6 && !var3 && !var8 && (var5.equals("") || var12.equals(var5))) {
                     var5 = (var4 = !var4) ? var12 : "";
                  } else if (var11 == 5 && var6 && !var3 && !var4) {
                     var8 = true;
                  } else if (var11 == 3 && var6 && !var4 && !var8) {
                     var3 = true;
                  } else if (var11 == 4 && var6 && !var4 && !var8) {
                     var3 = false;
                  } else if (var11 == 11 && !var4 && !var8 && !var3) {
                     var6 = true;
                  } else if (var11 == 12 && var6 && !var4 && !var8 && !var3) {
                     var6 = false;
                  }
               }
            }
         }

         var7 = 0;
         if (var3) {
            ++var7;
         }

         if (var4) {
            ++var7;
         }

         if (var6) {
            ++var7;
         }

         if (var7 == 0) {
            return null;
         } else {
            int var16 = 0;
            pv[] var17 = new pv[var7];
            if (var3) {
               ++var16;
               var17[0] = new pv("", (byte)3);
            }

            if (var4) {
               var17[var16++] = new pv(var5, (byte)8);
            }

            if (var6) {
               var17[var16] = new pv("", (byte)11);
            }

            return var17;
         }
      }
   }

   private byte a(int var1, int var2, String var3) {
      if (var1 == -1) {
         return 1;
      } else {
         int var4;
         if (!this.a) {
            while(var2 - var1 > 1) {
               var4 = var1 + var2 >>> 1;
               if (((pv)this.b.elementAt(var4)).a.compareTo(var3) > 0) {
                  var2 = var4;
               } else {
                  var1 = var4;
               }
            }

            if (var1 != -1 && ((pv)this.b.elementAt(var1)).a.compareTo(var3) == 0) {
               return ((pv)this.b.elementAt(var1)).a;
            } else {
               return 1;
            }
         } else {
            var3 = var3.toLowerCase();

            while(var2 - var1 > 1) {
               var4 = var1 + var2 >>> 1;
               if (((pv)this.b.elementAt(var4)).a.toLowerCase().compareTo(var3) > 0) {
                  var2 = var4;
               } else {
                  var1 = var4;
               }
            }

            if (var1 != -1 && ((pv)this.b.elementAt(var1)).a.toLowerCase().compareTo(var3) == 0) {
               return ((pv)this.b.elementAt(var1)).a;
            } else {
               return 1;
            }
         }
      }
   }

   private byte a(String var1) {
      byte var2;
      return (var2 = this.a(this.a, this.a + this.m, var1)) == 1 && (var2 = this.a(this.i, this.i + this.n, var1)) == 1 && (var2 = this.a(this.l, this.l + this.o, var1)) == 1 ? 1 : var2;
   }

   public final void a(String var1) {
      this.b.removeAllElements();
      this.a = false;
      if (var1.trim().equals("")) {
         this.a();
      } else {
         new bs();
         String var2 = null;
         if (var1.toLowerCase().equals(".java")) {
            var2 = "IgnoreCase 0\r[CommentStart]\r/*\r[CommentEnd]\r*/\r[SingleComment]\r//\r[String]\r\"\r'\r[Escape]\r\\\r[Keyword]\rBoolean\rByte\rCharacter\rClass\rClassLoader\rCompiler\rDouble\rFloat\rInheritableThreadLocal\rInteger\rLong\rMath\rNumber\rObject\rPackage\rProcess\rRuntime\rRuntimePermission\rSecurityManager\rShort\rStackTraceElement\rStrictMath\rString\rStringBuffer\rSystem\rThread\rThreadGroup\rThreadLocal\rThrowable\rVoid\rabstract\rboolean\rbreak\rbyte\rcase\rcatch\rchar\rclass\rcontinue\rdefault\rdelegate\rdo\rdouble\relse\rextends\rfalse\rfinal\rfinally\rfloat\rfor\rif\rimplements\rimport\rinstanceof\rint\rinterface\rlong\rnative\rnew\rnull\rpackage\rprivate\rprotected\rpublic\rreturn\rshort\rstatic\rsuper\rswitch\rsynchronized\rthis\rthrow\rthrows\rtransient\rtrue\rtry\rvoid\rvolatile\rwhile\r[Preprocessor]\r#define\r#elif\r#else\r#endif\r#error\r#if\r#undef\r#warning";
            Vector var6 = new Vector();
            String var3 = "";

            for(int var4 = 0; var4 < var2.length(); ++var4) {
               char var5;
               if ((var5 = var2.charAt(var4)) == '\r') {
                  var6.addElement(var3);
                  var3 = "";
               } else {
                  var3 = var3 + var5;
               }
            }

            System.gc();
            byte var8 = 1;
            int var9 = var6.size();

            for(int var7 = 0; var7 < var9; ++var7) {
               if ((var3 = (String)var6.elementAt(var7)).toLowerCase().startsWith("ignorecase")) {
                  this.a = var3.substring(10).trim().equals("1");
               } else if (var3.toLowerCase().equals("[keyword]")) {
                  var8 = 2;
               } else if (var3.toLowerCase().equals("[commentstart]")) {
                  var8 = 3;
               } else if (var3.toLowerCase().equals("[commentend]")) {
                  var8 = 4;
               } else if (var3.toLowerCase().equals("[singlecomment]")) {
                  var8 = 5;
               } else if (var3.toLowerCase().equals("[openbrackets]")) {
                  var8 = 6;
               } else if (var3.toLowerCase().equals("[closebrackets]")) {
                  var8 = 7;
               } else if (var3.toLowerCase().equals("[string]")) {
                  var8 = 8;
               } else if (var3.toLowerCase().equals("[escape]")) {
                  var8 = 9;
               } else if (var3.toLowerCase().equals("[preprocessor]")) {
                  var8 = 10;
               } else if (var3.toLowerCase().equals("[syntaxstart]")) {
                  var8 = 11;
               } else if (var3.toLowerCase().equals("[syntaxend]")) {
                  var8 = 12;
               } else if (var3.toLowerCase().equals("[tag]")) {
                  var8 = 13;
               } else {
                  switch(var8) {
                  case 2:
                     this.b.addElement(new pv(var3, (byte)2));
                     break;
                  case 3:
                     this.b.addElement(new pv(var3, (byte)3));
                     break;
                  case 4:
                     this.b.addElement(new pv(var3, (byte)4));
                     break;
                  case 5:
                     this.b.addElement(new pv(var3, (byte)5));
                     break;
                  case 6:
                     this.b.addElement(new pv(var3, (byte)6));
                     break;
                  case 7:
                     this.b.addElement(new pv(var3, (byte)7));
                     break;
                  case 8:
                     this.b.addElement(new pv(var3, (byte)8));
                     break;
                  case 9:
                     this.b.addElement(new pv(var3, (byte)9));
                     break;
                  case 10:
                     this.b.addElement(new pv(var3, (byte)10));
                     break;
                  case 11:
                     this.b.addElement(new pv(var3, (byte)11));
                     break;
                  case 12:
                     this.b.addElement(new pv(var3, (byte)12));
                     break;
                  case 13:
                     this.b.addElement(new pv(var3, (byte)13));
                  }
               }
            }

            this.b();
            System.gc();
         }
      }
   }

   public final int a(String var1) {
      this.a.removeAllElements();
      int var2;
      if ((var2 = this.b.size()) == 0) {
         this.a.addElement(new pv(var1, (byte)1));
         return 1;
      } else {
         String var3 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_#";
         String var4 = "";
         char[] var5 = var1.toCharArray();
         int var6 = var1.length();

         for(int var7 = 0; var7 < var6; ++var7) {
            if (var3.indexOf(var5[var7]) != -1) {
               var4 = var4 + var5[var7];
            } else {
               boolean var8 = false;
               if (!var4.equals("")) {
                  this.a.addElement(new pv(var4, this.a(var4)));
                  var4 = "";
               }

               if (var5[var7] == ' ') {
                  int var12;
                  for(var12 = var7; var12 < var6 && var5[var12] == ' '; ++var12) {
                     var4 = var4 + ' ';
                  }

                  this.a.addElement(new pv(var4, (byte)1));
                  var7 += var12 - var7 - 1;
                  var4 = "";
               } else {
                  String var9 = var1.substring(var7);
                  int var10;
                  pv var11;
                  if (var7 < var6 - 1 && var3.indexOf(var5[var7 + 1]) == -1) {
                     for(var10 = this.b; var10 != -1 && var10 < var2 && (var11 = (pv)this.b.elementAt(var10)).a == 3; ++var10) {
                        if (var9.startsWith(var11.a)) {
                           this.a.addElement(new pv(var11.a, (byte)3));
                           var7 += var11.a.length() - 1;
                           var8 = true;
                           break;
                        }
                     }

                     if (var8) {
                        continue;
                     }

                     for(var10 = this.c; var10 != -1 && var10 < var2 && (var11 = (pv)this.b.elementAt(var10)).a == 4; ++var10) {
                        if (var9.startsWith(var11.a)) {
                           this.a.addElement(new pv(var11.a, (byte)4));
                           var7 += var11.a.length() - 1;
                           var8 = true;
                           break;
                        }
                     }

                     if (var8) {
                        continue;
                     }

                     for(var10 = this.d; var10 != -1 && var10 < var2 && (var11 = (pv)this.b.elementAt(var10)).a == 5; ++var10) {
                        if (var9.startsWith(var11.a)) {
                           this.a.addElement(new pv(var9, (byte)5));
                           var7 += var9.length();
                           var8 = true;
                           break;
                        }
                     }

                     if (var8) {
                        continue;
                     }
                  }

                  for(var10 = this.e; var10 != -1 && var10 < var2 && (var11 = (pv)this.b.elementAt(var10)).a == 6; ++var10) {
                     if (var5[var7] == var11.a.charAt(0)) {
                        this.a.addElement(new pv(var11.a, (byte)6));
                        var8 = true;
                        break;
                     }
                  }

                  if (!var8) {
                     for(var10 = this.f; var10 != -1 && var10 < var2 && (var11 = (pv)this.b.elementAt(var10)).a == 7; ++var10) {
                        if (var5[var7] == var11.a.charAt(0)) {
                           this.a.addElement(new pv(var11.a, (byte)7));
                           var8 = true;
                           break;
                        }
                     }

                     if (!var8) {
                        for(var10 = this.g; var10 != -1 && var10 < var2 && (var11 = (pv)this.b.elementAt(var10)).a == 8; ++var10) {
                           if (var5[var7] == var11.a.charAt(0) && (this.h == -1 || var7 > 0 && !((pv)this.b.elementAt(this.h)).a.equals("" + var5[var7 - 1]))) {
                              this.a.addElement(new pv("" + var5[var7], (byte)8));
                              var8 = true;
                              break;
                           }
                        }

                        if (!var8) {
                           for(var10 = this.j; var10 != -1 && var10 < var2 && (var11 = (pv)this.b.elementAt(var10)).a == 11; ++var10) {
                              if (var9.startsWith(var11.a)) {
                                 this.a.addElement(new pv(var11.a, (byte)11));
                                 var7 += var11.a.length() - 1;
                                 var8 = true;
                                 break;
                              }
                           }

                           if (!var8) {
                              for(var10 = this.k; var10 != -1 && var10 < var2 && (var11 = (pv)this.b.elementAt(var10)).a == 12; ++var10) {
                                 if (var9.startsWith(var11.a)) {
                                    this.a.addElement(new pv(var11.a, (byte)12));
                                    var7 += var11.a.length() - 1;
                                    var8 = true;
                                    break;
                                 }
                              }

                              if (!var8) {
                                 this.a.addElement(new pv("" + var5[var7], (byte)1));
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (!var4.equals("")) {
            this.a.addElement(new pv(var4, this.a(var4)));
         }

         return this.a.size();
      }
   }
}
