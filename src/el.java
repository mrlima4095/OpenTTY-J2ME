import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public final class el {
   public static final Integer a = new Integer(Integer.MIN_VALUE);
   public static final Long a = new Long(Long.MIN_VALUE);
   private String a;
   private Reader a;
   private int a;
   private boolean a;
   private short a;
   private short b;
   private ms a;
   private ms b;
   private short c;
   private short d;
   private String b;
   private final String[] a;
   private final String[] b;

   public el(String var1, InputStream var2, String var3) {
      this(var1, (Reader)(var3.equals("windows-1251") ? new ll(var2) : new InputStreamReader(var2, var3)), (short)1, (short)0);
   }

   private el(String var1, Reader var2, short var3, short var4) {
      this.a = -1;
      this.a = false;
      this.b = null;
      this.a = new String[]{"abstract", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "extends", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while"};
      this.b = new String[]{"(", ")", "{", "}", "[", "]", ";", ",", ".", "=", ">", "<", "!", "~", "?", ":", "==", "<=", ">=", "!=", "&&", "||", "++", "--", "+", "-", "*", "/", "&", "|", "^", "%", "<<", ">>", ">>>", "+=", "-=", "*=", "/=", "&=", "|=", "^=", "%=", "<<=", ">>=", ">>>="};
      this.a = var1;
      this.a = new cv(var2);
      this.a = 1;
      this.b = 0;
      this.a();
      this.a = this.d();
      this.b = null;
   }

   public final ms a() {
      ms var1 = this.a;
      if (this.b != null) {
         this.a = this.b;
         this.b = null;
      } else {
         this.a = this.d();
      }

      return var1;
   }

   public final ms b() {
      return this.a;
   }

   public final ms c() {
      if (this.b == null) {
         this.b = this.d();
      }

      return this.b;
   }

   public final String a() {
      String var1 = this.b;
      this.b = null;
      return var1;
   }

   public final aq a() {
      return this.a.a();
   }

   private static boolean a(char var0) {
      if (var0 >= 'a' && var0 <= 'z') {
         return true;
      } else if (var0 >= 'A' && var0 <= 'Z') {
         return true;
      } else {
         return var0 == '$' || var0 == '_';
      }
   }

   public static String a(Object var0) {
      if (var0 instanceof String) {
         StringBuffer var6;
         (var6 = new StringBuffer()).append('"');
         String var5 = (String)var0;

         for(int var2 = 0; var2 < var5.length(); ++var2) {
            char var3;
            if ((var3 = var5.charAt(var2)) == '"') {
               var6.append("\\\"");
            } else {
               a(var3, var6);
            }
         }

         var6.append('"');
         return var6.toString();
      } else if (var0 instanceof Character) {
         char var1;
         if ((var1 = (Character)var0) == '\'') {
            return "'\\''";
         } else {
            StringBuffer var4 = new StringBuffer("'");
            a(var1, var4);
            return var4.append('\'').toString();
         }
      } else if (var0 instanceof Integer) {
         return var0 == a ? "2147483648" : var0.toString();
      } else if (var0 instanceof Long) {
         return var0 == a ? "9223372036854775808L" : var0.toString() + 'L';
      } else if (var0 instanceof Float) {
         return var0.toString() + 'F';
      } else if (var0 instanceof Double) {
         return var0.toString() + 'D';
      } else if (var0 instanceof Boolean) {
         return var0.toString();
      } else if (var0 == null) {
         return "null";
      } else {
         throw new RuntimeException("Unexpected value type \"" + var0.getClass().getName() + "\"");
      }
   }

   private static void a(char var0, StringBuffer var1) {
      int var2;
      if ((var2 = "\b\t\n\f\r\\".indexOf(var0)) != -1) {
         var1.append('\\').append("btnfr\\".charAt(var2));
      } else if (var0 >= ' ' && var0 < 255 && var0 != 127) {
         var1.append(var0);
      } else {
         var1.append("\\u");

         String var3;
         for(var2 = (var3 = Integer.toHexString('\uffff' & var0)).length(); var2 < 4; ++var2) {
            var1.append('0');
         }

         var1.append(var3);
      }
   }

   private ms d() {
      if (this.b != null) {
         this.b = null;
      }

      int var1 = 0;
      StringBuffer var2 = null;

      while(true) {
         switch(var1) {
         case 0:
            if (this.a == -1) {
               return new di(this);
            }

            if (" \t\n\r\f".indexOf((char)this.a) == -1) {
               if (this.a != 47) {
                  this.c = this.a;
                  this.d = this.b;
                  String var5;
                  String var7;
                  StringBuffer var12;
                  if (a((char)this.a)) {
                     (var12 = new StringBuffer()).append((char)this.a);

                     while(true) {
                        this.a();
                        if (this.a == -1) {
                           break;
                        }

                        char var9 = (char)this.a;
                        if (!(a(var9) ? true : (var9 >= '0' && var9 <= '9' ? true : (var9 >= 0 && var9 <= '\b' ? true : var9 >= 14 && var9 <= 27)))) {
                           break;
                        }

                        var12.append((char)this.a);
                     }

                     if ((var5 = var12.toString()).equals("true")) {
                        return new hm(this, Boolean.TRUE);
                     }

                     if (var5.equals("false")) {
                        return new hm(this, Boolean.FALSE);
                     }

                     if (var5.equals("null")) {
                        return new hm(this, (Object)null);
                     }

                     var7 = null;

                     for(int var8 = 0; var8 < this.a.length; ++var8) {
                        if (this.a[var8].equals(var5)) {
                           var7 = this.a[var8];
                           break;
                        }
                     }

                     if (var7 != null) {
                        return new ba(this, var7, (fx)null);
                     }

                     return new ik(this, var5, (fx)null);
                  }

                  if (Character.isDigit((char)this.a)) {
                     return this.a((int)0);
                  }

                  if (this.a == 46) {
                     this.a();
                     if (Character.isDigit((char)this.a)) {
                        return this.a((int)2);
                     }

                     return new nm(this, ".", (fx)null);
                  }

                  if (this.a == 34) {
                     var12 = new StringBuffer("");
                     this.a();
                     if (this.a == -1) {
                        throw new pw(this, "EOF in string literal");
                     }

                     if (this.a != 13 && this.a != 10) {
                        while(this.a != 34) {
                           var12.append(this.a());
                        }

                        this.a();
                        return new hm(this, var12.toString());
                     }

                     throw new pw(this, "Line break in string literal");
                  }

                  if (this.a == 39) {
                     this.a();
                     if (this.a == 39) {
                        throw new pw(this, "Single quote must be backslash-escaped in character literal");
                     }

                     char var11 = this.a();
                     if (this.a != 39) {
                        throw new pw(this, "Closing single quote missing");
                     }

                     this.a();
                     return new hm(this, new Character(var11));
                  }

                  String var10 = null;
                  var5 = new String(new char[]{(char)this.a});

                  for(int var3 = 0; var3 < this.b.length; ++var3) {
                     if (this.b[var3].equals(var5)) {
                        var10 = this.b[var3];
                        break;
                     }
                  }

                  if (var10 == null) {
                     throw new pw(this, "Invalid character input \"" + (char)this.a + "\" (character code " + this.a + ")");
                  }

                  while(true) {
                     this.a();
                     var7 = null;
                     String var4 = var10 + (char)this.a;

                     for(int var6 = 0; var6 < this.b.length; ++var6) {
                        if (this.b[var6].equals(var4)) {
                           var7 = this.b[var6];
                           break;
                        }
                     }

                     if (var7 == null) {
                        return new nm(this, var10, (fx)null);
                     }

                     var10 = var7;
                  }
               }

               var1 = 1;
            }
            break;
         case 1:
            if (this.a == -1) {
               return new nm(this, "/", (fx)null);
            }

            if (this.a == 61) {
               this.a();
               return new nm(this, "/=", (fx)null);
            }

            if (this.a == 47) {
               var1 = 2;
            } else {
               if (this.a != 42) {
                  return new nm(this, "/", (fx)null);
               }

               var1 = 3;
            }
            break;
         case 2:
            if (this.a == -1) {
               return new di(this);
            }

            if (this.a == 13 || this.a == 10) {
               var1 = 0;
            }
            break;
         case 3:
            if (this.a == -1) {
               throw new pw(this, "EOF in traditional comment");
            }

            if (this.a == 42) {
               var1 = 4;
            } else {
               var1 = 9;
            }
            break;
         case 4:
            if (this.a == -1) {
               throw new pw(this, "EOF in doc comment");
            }

            if (this.a == 47) {
               var1 = 0;
               break;
            }

            (var2 = new StringBuffer()).append((char)this.a);
            var1 = this.a != 13 && this.a != 10 ? (this.a == 42 ? 8 : 5) : 6;
            break;
         case 5:
            if (this.a == -1) {
               throw new pw(this, "EOF in doc comment");
            }

            if (this.a == 42) {
               var1 = 8;
            } else {
               if (this.a != 13 && this.a != 10) {
                  var2.append((char)this.a);
                  break;
               }

               var2.append((char)this.a);
               var1 = 6;
            }
            break;
         case 6:
            if (this.a == -1) {
               throw new pw(this, "EOF in doc comment");
            }

            if (this.a == 42) {
               var1 = 7;
            } else {
               if (this.a != 13 && this.a != 10) {
                  if (this.a != 32 && this.a != 9) {
                     var2.append((char)this.a);
                     var1 = 5;
                  }
                  break;
               }

               var2.append((char)this.a);
            }
            break;
         case 7:
            if (this.a == -1) {
               throw new pw(this, "EOF in doc comment");
            }

            if (this.a != 42) {
               if (this.a == 47) {
                  this.b = var2.toString();
                  var1 = 0;
               } else {
                  var2.append((char)this.a);
                  var1 = 5;
               }
            }
            break;
         case 8:
            if (this.a == -1) {
               throw new pw(this, "EOF in doc comment");
            }

            if (this.a == 47) {
               this.b = var2.toString();
               var1 = 0;
            } else if (this.a == 42) {
               var2.append('*');
            } else {
               var2.append('*');
               var2.append((char)this.a);
               var1 = 5;
            }
            break;
         case 9:
            if (this.a == -1) {
               throw new pw(this, "EOF in traditional comment");
            }

            if (this.a == 42) {
               var1 = 10;
            }
            break;
         case 10:
            if (this.a == -1) {
               throw new pw(this, "EOF in traditional comment");
            }

            if (this.a == 47) {
               var1 = 0;
            } else if (this.a != 42) {
               var1 = 9;
            }
         }

         this.a();
      }
   }

   private ms a(int var1) {
      StringBuffer var2 = var1 == 2 ? new StringBuffer("0.") : new StringBuffer();
      var1 = var1;

      while(true) {
         switch(var1) {
         case 0:
            if (this.a == 48) {
               var1 = 6;
            } else {
               var2.append((char)this.a);
               var1 = 1;
            }
            break;
         case 1:
            if (Character.isDigit((char)this.a)) {
               var2.append((char)this.a);
               break;
            } else {
               if (this.a != 108 && this.a != 76) {
                  if (this.a == 102 || this.a == 70) {
                     this.a();
                     return this.a(var2.toString());
                  }

                  if (this.a == 100 || this.a == 68) {
                     this.a();
                     return this.b(var2.toString());
                  }

                  if (this.a == 46) {
                     var2.append('.');
                     var1 = 2;
                  } else {
                     if (this.a != 69 && this.a != 101) {
                        return this.a(var2.toString(), 10);
                     }

                     var2.append('E');
                     var1 = 3;
                  }
                  break;
               }

               this.a();
               return this.b(var2.toString(), 10);
            }
         case 2:
            if (Character.isDigit((char)this.a)) {
               var2.append((char)this.a);
            } else {
               if (this.a != 101 && this.a != 69) {
                  if (this.a != 102 && this.a != 70) {
                     if (this.a != 100 && this.a != 68) {
                        return this.b(var2.toString());
                     }

                     this.a();
                     return this.b(var2.toString());
                  }

                  this.a();
                  return this.a(var2.toString());
               }

               var2.append('E');
               var1 = 3;
            }
            break;
         case 3:
            if (Character.isDigit((char)this.a)) {
               var2.append((char)this.a);
               var1 = 5;
            } else {
               if (this.a != 45 && this.a != 43) {
                  throw new pw(this, "Exponent missing after \"E\"");
               }

               var2.append((char)this.a);
               var1 = 4;
            }
            break;
         case 4:
            if (!Character.isDigit((char)this.a)) {
               throw new pw(this, "Exponent missing after \"E\" and sign");
            }

            var2.append((char)this.a);
            var1 = 5;
            break;
         case 5:
            if (!Character.isDigit((char)this.a)) {
               if (this.a != 102 && this.a != 70) {
                  if (this.a != 100 && this.a != 68) {
                     return this.b(var2.toString());
                  }

                  this.a();
                  return this.b(var2.toString());
               }

               this.a();
               return this.a(var2.toString());
            }

            var2.append((char)this.a);
            break;
         case 6:
            if ("01234567".indexOf(this.a) != -1) {
               var2.append((char)this.a);
               var1 = 7;
               break;
            } else {
               if (this.a != 108 && this.a != 76) {
                  if (this.a != 102 && this.a != 70) {
                     if (this.a == 100 || this.a == 68) {
                        this.a();
                        return this.b("0");
                     }

                     if (this.a == 46) {
                        var2.append("0.");
                        var1 = 2;
                     } else if (this.a != 69 && this.a != 101) {
                        if (this.a != 120 && this.a != 88) {
                           return this.a("0", 10);
                        }

                        var1 = 8;
                     } else {
                        var2.append('E');
                        var1 = 3;
                     }
                     break;
                  }

                  this.a();
                  return this.a("0");
               }

               this.a();
               return this.b("0", 10);
            }
         case 7:
            if ("01234567".indexOf(this.a) == -1) {
               if (this.a != 108 && this.a != 76) {
                  return this.a(var2.toString(), 8);
               }

               this.a();
               return this.b(var2.toString(), 8);
            }

            var2.append((char)this.a);
            break;
         case 8:
            if (Character.digit((char)this.a, 16) == -1) {
               throw new pw(this, "Hex digit expected after \"0x\"");
            }

            var2.append((char)this.a);
            var1 = 9;
            break;
         case 9:
            if (Character.digit((char)this.a, 16) == -1) {
               if (this.a != 108 && this.a != 76) {
                  return this.a(var2.toString(), 16);
               }

               this.a();
               return this.b(var2.toString(), 16);
            }

            var2.append((char)this.a);
         }

         this.a();
      }
   }

   private hm a(String var1, int var2) {
      int var3;
      switch(var2) {
      case 8:
         var2 = 0;

         for(var3 = 0; var3 < var1.length(); ++var3) {
            if ((var2 & -536870912) != 0) {
               throw new pw(this, "Value of octal integer literal \"" + var1 + "\" is out of range");
            }

            var2 = (var2 << 3) + Character.digit(var1.charAt(var3), 8);
         }

         return new hm(this, new Integer(var2));
      case 10:
         if (var1.equals("2147483648")) {
            return new hm(this, a);
         } else {
            try {
               var2 = Integer.parseInt(var1);
               return new hm(this, new Integer(var2));
            } catch (NumberFormatException var4) {
               throw new pw(this, "Value of decimal integer literal \"" + var1 + "\" is out of range");
            }
         }
      case 16:
         var2 = 0;

         for(var3 = 0; var3 < var1.length(); ++var3) {
            if ((var2 & -268435456) != 0) {
               throw new pw(this, "Value of hexadecimal integer literal \"" + var1 + "\" is out of range");
            }

            var2 = (var2 << 4) + Character.digit(var1.charAt(var3), 16);
         }

         return new hm(this, new Integer(var2));
      default:
         throw new RuntimeException("Illegal radix " + var2);
      }
   }

   private hm b(String var1, int var2) {
      long var3;
      switch(var2) {
      case 8:
         var3 = 0L;

         for(var2 = 0; var2 < var1.length(); ++var2) {
            if ((var3 & -2305843009213693952L) != 0L) {
               throw new pw(this, "Value of octal long literal \"" + var1 + "\" is out of range");
            }

            var3 = (var3 << 3) + (long)Character.digit(var1.charAt(var2), 8);
         }

         return new hm(this, new Long(var3));
      case 10:
         if (var1.equals("9223372036854775808")) {
            return new hm(this, a);
         } else {
            try {
               var3 = Long.parseLong(var1);
               return new hm(this, new Long(var3));
            } catch (NumberFormatException var5) {
               throw new pw(this, "Value of decimal long literal \"" + var1 + "\" is out of range");
            }
         }
      case 16:
         var3 = 0L;

         for(var2 = 0; var2 < var1.length(); ++var2) {
            if ((var3 & -1152921504606846976L) != 0L) {
               throw new pw(this, "Value of hexadecimal long literal \"" + var1 + "\" is out of range");
            }

            var3 = (var3 << 4) + (long)Character.digit(var1.charAt(var2), 16);
         }

         return new hm(this, new Long(var3));
      default:
         throw new RuntimeException("Illegal radix " + var2);
      }
   }

   private hm a(String var1) {
      float var2;
      try {
         var2 = Float.parseFloat(var1);
      } catch (NumberFormatException var5) {
         throw new RuntimeException("SNO: parsing float literal \"" + var1 + "\" throws a \"NumberFormatException\"");
      }

      if (Float.isInfinite(var2)) {
         throw new pw(this, "Value of float literal \"" + var1 + "\" is out of range");
      } else if (Float.isNaN(var2)) {
         throw new RuntimeException("SNO: parsing float literal \"" + var1 + "\" results is NaN");
      } else {
         if (var2 == 0.0F) {
            for(int var3 = 0; var3 < var1.length(); ++var3) {
               char var4 = var1.charAt(var3);
               if ("123456789".indexOf(var4) != -1) {
                  throw new pw(this, "Literal \"" + var1 + "\" is too small to be represented as a float");
               }

               if ("0.".indexOf(var4) == -1) {
                  break;
               }
            }
         }

         return new hm(this, new Float(var2));
      }
   }

   private hm b(String var1) {
      double var2;
      try {
         var2 = Double.parseDouble(var1);
      } catch (NumberFormatException var6) {
         throw new RuntimeException("SNO: parsing double literal \"" + var1 + "\" throws a \"NumberFormatException\"");
      }

      if (Double.isInfinite(var2)) {
         throw new pw(this, "Value of double literal \"" + var1 + "\" is out of range");
      } else if (Double.isNaN(var2)) {
         throw new RuntimeException("SNO: parsing double literal \"" + var1 + "\" results is NaN");
      } else {
         if (var2 == 0.0D) {
            for(int var4 = 0; var4 < var1.length(); ++var4) {
               char var5 = var1.charAt(var4);
               if ("123456789".indexOf(var5) != -1) {
                  throw new pw(this, "Literal \"" + var1 + "\" is too small to be represented as a double");
               }

               if ("0.".indexOf(var5) == -1) {
                  break;
               }
            }
         }

         return new hm(this, new Double(var2));
      }
   }

   private char a() {
      if (this.a == -1) {
         throw new pw(this, "EOF in character literal");
      } else if (this.a != 13 && this.a != 10) {
         if (this.a != 92) {
            char var3 = (char)this.a;
            this.a();
            return var3;
         } else {
            this.a();
            int var1;
            char var2;
            if ((var1 = "btnfr".indexOf(this.a)) != -1) {
               var2 = "\b\t\n\f\r".charAt(var1);
               this.a();
               return var2;
            } else if ((var1 = "01234567".indexOf(this.a)) != -1) {
               int var4 = var1;
               this.a();
               if ((var1 = "01234567".indexOf(this.a)) == -1) {
                  return (char)var4;
               } else {
                  var4 = var4 * 8 + var1;
                  this.a();
                  if ((var1 = "01234567".indexOf(this.a)) == -1) {
                     return (char)var4;
                  } else if ((var4 = var4 * 8 + var1) > 255) {
                     throw new pw(this, "Invalid octal escape");
                  } else {
                     this.a();
                     return (char)var4;
                  }
               }
            } else {
               var2 = (char)this.a;
               this.a();
               return var2;
            }
         }
      } else {
         throw new pw(this, "Line break in literal not allowed");
      }
   }

   private void a() {
      try {
         this.a = this.a.read();
      } catch (RuntimeException var2) {
         throw new pw(this, var2.getMessage(), var2);
      }

      if (this.a == 13) {
         ++this.a;
         this.b = 0;
         this.a = true;
      } else if (this.a == 10) {
         if (this.a) {
            this.a = false;
         } else {
            ++this.a;
            this.b = 0;
         }
      } else {
         ++this.b;
      }
   }

   static String a(el var0) {
      return var0.a;
   }

   static short a(el var0) {
      return var0.c;
   }

   static short b(el var0) {
      return var0.d;
   }

   static short c(el var0) {
      return var0.a;
   }

   static short d(el var0) {
      return var0.b;
   }
}
