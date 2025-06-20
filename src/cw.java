import java.util.Vector;

public final class cw {
   private Vector a = new Vector();
   private Vector b = new Vector();
   private Vector c = new Vector();
   private int c;
   private int d;
   private boolean a;
   private boolean b;
   private int e;
   private int f;
   private int g;
   public int a;
   public int b;
   private int h;
   private int i;
   private boolean c;
   private int j;
   private boolean d;
   private int k;
   private int l = 20;

   public cw() {
      this.a.addElement(new String());
      this.a(0, 0);
   }

   public final boolean a() {
      return this.d;
   }

   final void a() {
      this.d = false;
   }

   public final void a(int var1) {
      this.k = var1;

      for(int var2 = 0; var2 < this.a.size(); ++var2) {
         String var3 = (String)this.a.elementAt(var2);
         if (var1 > 0) {
            if (var3.length() > 0 && var3.charAt(var3.length() - 1) != '\n') {
               this.a.setElementAt(var3 + "\n", var2);
            }
         } else {
            int var4;
            while((var4 = var3.indexOf(10)) != -1) {
               var3 = var3.substring(0, var4) + var3.substring(var4 + 1);
            }

            this.a.setElementAt(var3, var2);
         }
      }

      this.j();
   }

   public final int a() {
      return this.a.size();
   }

   public final int b() {
      return this.c;
   }

   public final int c() {
      return this.d;
   }

   public final void a(boolean var1) {
      this.c = var1;
   }

   public final boolean b() {
      return this.a;
   }

   public final void b(boolean var1) {
      this.a = var1;
   }

   public final int d() {
      return this.g;
   }

   public final boolean c() {
      return this.b;
   }

   public final void b() {
      this.a(0, 0);
      this.c.removeAllElements();
      this.a.removeAllElements();
      this.a.addElement(new String());
      this.c(false);
      this.a = false;
      this.j = 0;
      this.e = 0;
      this.f = 0;
      this.h = 0;
      this.i = 0;
      this.d = false;
      this.k = 0;
      System.gc();
   }

   public final void a(String var1) {
      int var2 = 0;

      try {
         int var3;
         String var4;
         while((var3 = var1.indexOf(10, var2)) != -1) {
            var4 = (String)this.a.elementAt(this.a.size() - 1);
            String var6;
            if (var3 > 0 && var1.charAt(var3 - 1) == '\r') {
               var6 = var1.substring(var2, var3 - 1);
               if (this.k > 0) {
                  var6 = var6 + "\n";
               }
            } else if (this.k > 0) {
               var6 = var1.substring(var2, var3 + 1);
            } else {
               var6 = var1.substring(var2, var3);
            }

            this.a.setElementAt(var4 + var6, this.a.size() - 1);
            this.a.addElement(new String());
            var2 = var3 + 1;
         }

         var4 = (String)this.a.elementAt(this.a.size() - 1);
         this.a.setElementAt(var4 + var1.substring(var2), this.a.size() - 1);
      } catch (Throwable var5) {
         throw new lo("TextContainer::appendData()", var5);
      }

      if (this.k > 0) {
         this.j();
      }

   }

   public final String toString() {
      String var1 = "";

      try {
         for(int var2 = 0; var2 < this.a.size(); ++var2) {
            String var3 = (String)this.a.elementAt(var2);
            if (this.k == 0) {
               var1 = var1 + var3 + "\r\n";
            } else {
               int var4;
               while((var4 = var3.indexOf(10)) != -1 && var4 - 1 >= 0 && var3.charAt(var4 - 1) != '\r') {
                  var3 = var3.substring(0, var4) + "\r\n" + var3.substring(var4 + 1);
               }

               var1 = var1 + var3;
            }
         }
      } catch (Throwable var5) {
         throw new lo("TextContainer::toString()", var5);
      }

      this.d = false;
      return var1;
   }

   public final String[] a(int var1, int var2) {
      if (var1 < 0) {
         var1 = 0;
      }

      int var3 = this.a.size();
      String[] var4 = null;
      if (var1 + var2 > var3) {
         var4 = new String[var3 - var1];
      } else {
         var4 = new String[var2];
      }

      var2 = 0;

      for(var1 = var1; var2 < var4.length; ++var1) {
         var4[var2] = (String)this.a.elementAt(var1);
         ++var2;
      }

      return var4;
   }

   public final void a(int var1, String var2) {
      if (var1 >= 0 && var1 < this.a.size()) {
         if (var2.indexOf(10) == -1) {
            this.a(new co((byte)3, (String)this.a.elementAt(var1), var1, 0));
            this.a.setElementAt(var2, var1);
         } else {
            var2 = var2 + "\n";
            int var3 = 0;

            int var5;
            for(boolean var4 = false; (var5 = var2.indexOf(10)) != -1; ++var3) {
               if (var3 == 0) {
                  this.a(new co((byte)3, (String)this.a.elementAt(var1), var1, 0));
                  this.a.setElementAt(var2.substring(0, var5) + (this.k <= 0 ? "" : "\n"), var1);
               } else {
                  this.a(new co((byte)5, "", var1 + var3, 0));
                  this.a.insertElementAt(var2.substring(0, var5) + (this.k <= 0 ? "" : "\n"), var1 + var3);
               }

               var2 = var2.substring(var5 + 1);
            }
         }

         if (((String)this.a.elementAt(this.c)).length() < this.d) {
            this.a(this.c, ((String)this.a.elementAt(this.c)).length());
         }

         if (this.k > 0) {
            this.j();
         }

         this.d = true;
      }

   }

   public final void a(int var1, int var2) {
      if (var1 >= 0 && var1 < this.a.size()) {
         String var3 = (String)this.a.elementAt(var1);
         if (var2 > var3.length()) {
            var2 = var3.length();
         } else if (var2 < 0) {
            var2 = 0;
         }

         this.c = var1;
         this.d = var2;
         if (this.b) {
            this.g = 0;
            boolean var8 = false;
            boolean var9 = false;
            boolean var10 = false;
            boolean var4 = false;
            int var11;
            int var12;
            if (this.c < this.f) {
               var1 = this.d;
               var2 = this.c;
               var11 = this.e;
               var12 = this.f;
            } else if (this.c > this.f) {
               var1 = this.e;
               var2 = this.f;
               var11 = this.d;
               var12 = this.c;
            } else {
               if (this.d < this.e) {
                  var1 = this.d;
                  var11 = this.e;
               } else {
                  var1 = this.e;
                  var11 = this.d;
               }

               var2 = this.c;
               var12 = this.c;
            }

            for(int var5 = var2; var5 <= var12 && var5 < this.a.size(); ++var5) {
               String var6 = (String)this.a.elementAt(var5);
               int var7;
               if (var5 == var2) {
                  var7 = var1;
               } else {
                  var7 = 0;
               }

               int var13;
               if (var5 == var12) {
                  var13 = var11;
               } else {
                  var13 = var6.length();
               }

               this.g += var13 - var7;
            }

            this.a = var1;
            this.b = var2;
         }
      }

   }

   public final void b(int var1) {
      String var4;
      switch(var1) {
      case 1:
         if (this.c > 0) {
            this.a(this.c - 1, this.j);
            return;
         }
         break;
      case 2:
         if (this.c < this.a.size() - 1) {
            this.a(this.c + 1, this.j);
            return;
         }
         break;
      case 3:
         if (this.d == 0 && this.c > 0) {
            var4 = (String)this.a.elementAt(this.c - 1);
            this.a(this.c - 1, var4.length());
         } else if (this.d > 0) {
            this.a(this.c, this.d - 1);
         }

         this.j = this.d;
         return;
      case 4:
         var4 = (String)this.a.elementAt(this.c);
         if (this.d == var4.length() && this.c + 1 < this.a.size()) {
            this.a(this.c + 1, 0);
         } else if (this.d < var4.length()) {
            this.a(this.c, this.d + 1);
         }

         this.j = this.d;
         return;
      case 5:
         var4 = (String)this.a.elementAt(this.c);
         int var2 = 0;

         for(int var3 = 0; var3 < var4.length(); ++var3) {
            if (var4.charAt(var3) != ' ') {
               var2 = var3;
               break;
            }
         }

         this.j = this.d;
         this.a(this.c, this.d == var2 ? 0 : var2);
         return;
      case 6:
         this.j = this.d;
         this.a(this.c, ((String)this.a.elementAt(this.c)).length());
         break;
      default:
         return;
      }

   }

   public final void b(String var1) {
      if (this.b) {
         this.i();
      }

      String var2 = (String)this.a.elementAt(this.c);
      if (this.a) {
         if (this.d + var1.length() > var2.length()) {
            var2 = var2.substring(0, this.d) + var1;
         } else {
            var2 = var2.substring(0, this.d) + var1 + var2.substring(this.d + var1.length());
         }
      }

      this.a.setElementAt(var2.substring(0, this.d) + var1 + var2.substring(this.d), this.c);
      this.a(this.c, this.d + var1.length());
      this.a(new co((byte)1, var1.length() != 0 ? var1 : " ", this.c, this.d));
      if (this.k > 0) {
         this.j();
      }

      this.d = true;
   }

   public final void c(int var1) {
      if (this.b) {
         this.i();
      } else {
         String var2 = (String)this.a.elementAt(this.c);
         String var3;
         if (var1 < 0) {
            if (this.d == 0 && this.c > 0) {
               var3 = (String)this.a.elementAt(this.c - 1);
               this.a(new co((byte)4, var2, this.c, 0));
               this.a(new co((byte)3, var3, this.c - 1, 0));
               if (this.k > 0 && var3.length() > 0) {
                  this.a.setElementAt(var3.substring(0, var3.length() - 1), this.c - 1);
                  var3 = (String)this.a.elementAt(this.c - 1);
               }

               this.a.removeElementAt(this.c);
               this.a(this.c - 1, var3.length());
               this.a.setElementAt(var3 + var2, this.c);
            } else if (this.d > 0) {
               this.a(new co((byte)2, var2.substring(this.d + var1, this.d), this.c, this.d + var1));
               this.a.setElementAt(var2.substring(0, this.d + var1) + var2.substring(this.d), this.c);
               this.d += var1;
            }
         } else if (var1 > 0) {
            if (this.d == var2.length() && this.c + 1 < this.a.size()) {
               var3 = (String)this.a.elementAt(this.c + 1);
               this.a.setElementAt(var2 + var3, this.c);
               this.a.removeElementAt(this.c + 1);
            } else if (this.d < var2.length()) {
               this.a.setElementAt(var2.substring(0, this.d) + var2.substring(this.d + var1), this.c);
            }
         }

         if (this.k > 0) {
            this.j();
         }

         this.d = true;
      }
   }

   public final void c() {
      if (this.b) {
         this.i();
      }

      String var1 = (String)this.a.elementAt(this.c);
      this.a(new co((byte)3, var1, this.c, this.d));
      this.a(new co((byte)5, "", this.c + 1, 0));
      String var2 = var1.substring(this.d);
      this.a.setElementAt(var1.substring(0, this.d) + (this.k <= 0 ? "" : "\n"), this.c);
      var1 = "";
      if (this.c) {
         String var3 = (String)this.a.elementAt(this.c);

         label30:
         for(int var4 = 0; var4 < var3.length(); ++var4) {
            if (var3.charAt(var4) != ' ') {
               int var5 = 0;

               while(true) {
                  if (var5 >= var4) {
                     break label30;
                  }

                  var1 = var1 + " ";
                  ++var5;
               }
            }
         }
      }

      this.a.insertElementAt(var1 + var2, this.c + 1);
      this.a(this.c + 1, var1.length());
      this.d = true;
   }

   public final void d() {
      if (this.d != 0) {
         this.c(false);
         boolean var1 = false;
         String var2 = "abcdefghijklmnopqrstuvwxyz0123456789";
         int var3 = 0;
         String var4 = (String)this.a.elementAt(this.c);
         if (var2.indexOf(Character.toLowerCase(var4.charAt(this.d - 1))) != -1) {
            int var5;
            for(var5 = this.d - 1; var5 >= 0; --var5) {
               if (var2.indexOf(Character.toLowerCase(var4.charAt(var5))) == -1) {
                  var3 = var5 + 1;
                  var4.substring(var5 + 1, this.d);
                  var1 = true;
                  break;
               }
            }

            String var11;
            if (var1) {
               var11 = var4.substring(var3, this.d);
            } else {
               var11 = var4.substring(0, this.d);
            }

            var5 = this.a.size();
            var4 = "";

            for(int var6 = 0; var6 < var5 && var4.equals(""); ++var6) {
               String var7 = (String)this.a.elementAt(var6);

               int var10;
               for(int var8 = 0; (var8 = var7.indexOf(var11, var8)) != -1; var8 = var10 + 1) {
                  int var9;
                  for(var9 = var8; var9 >= 0 && var2.indexOf(Character.toLowerCase(var7.charAt(var9))) != -1; --var9) {
                  }

                  for(var10 = var8; var10 < var7.length() && var2.indexOf(Character.toLowerCase(var7.charAt(var10))) != -1; ++var10) {
                  }

                  if (var7.substring(var9 + 1, var10).startsWith(var11) && (var6 != this.c || var8 != var3)) {
                     var4 = var7.substring(var9 + 1, var10);
                     break;
                  }
               }
            }

            if (!var4.equals("")) {
               this.b(var4.substring(this.d - var3));
            }
         }

      }
   }

   public final void c(boolean var1) {
      if (var1) {
         this.e = this.d;
         this.f = this.c;
      } else {
         this.g = 0;
         this.a = 0;
         this.b = 0;
      }

      this.b = var1;
   }

   public final void e() {
      if (this.b) {
         this.b.removeAllElements();
         int var1 = this.g;

         for(int var2 = this.b; var1 > 0; ++var2) {
            int var3 = 0;
            String var4 = (String)this.a.elementAt(var2);
            int var5;
            if (var2 == this.b) {
               var3 = this.a;
               if (var1 < var4.length() - this.a) {
                  var5 = var1;
               } else {
                  var5 = var4.length() - this.a;
               }
            } else if (var1 < var4.length()) {
               var5 = var1;
            } else {
               var5 = var4.length();
            }

            if (var5 > 0) {
               this.b.addElement(var4.substring(var3, var3 + var5));
               var1 -= var5;
            }
         }

      }
   }

   public final void f() {
      if (this.b) {
         this.i();
      }

      int var1;
      if ((var1 = this.b.size()) == 1) {
         this.b((String)this.b.elementAt(0));
      } else {
         String var2 = "";

         for(int var3 = 0; var3 < var1; ++var3) {
            String var4 = (String)this.b.elementAt(var3);
            if (var3 == 0) {
               String var5 = (String)this.a.elementAt(this.c);
               this.a(new co((byte)3, var5, this.c, this.d));
               var2 = var5.substring(this.d);
               this.a.setElementAt(var5.substring(0, this.d) + var4, this.c);
            } else {
               this.a.insertElementAt(var4, this.c + var3);
               this.a(new co((byte)5, var4, this.c + var3, 0));
            }
         }

         String var6 = (String)this.a.elementAt(this.c + this.b.size() - 1);
         this.a.setElementAt(var6 + var2, this.c + this.b.size() - 1);
         this.a(this.c + this.b.size() - 1, var6.length());
      }
   }

   public final void g() {
      if (this.b) {
         this.e();
         this.c(1);
      }
   }

   private void i() {
      if (this.g != 0) {
         String var1 = "";
         String var2 = "";
         Vector var3 = new Vector();
         Vector var4 = new Vector();
         int var5 = this.g;

         int var6;
         for(var6 = this.b; var5 > 0; ++var6) {
            int var7 = 0;
            String var8 = (String)this.a.elementAt(var6);
            int var9;
            if (var6 == this.b) {
               var7 = this.a;
               var1 = var8.substring(0, var7);
               if (var5 < var8.length() - this.a) {
                  var9 = var5;
               } else {
                  var9 = var8.length() - this.a;
               }

               if (var9 > 0) {
                  var4.addElement(new co((byte)3, var8, var6, var7));
               }
            } else {
               if (var5 < var8.length()) {
                  var9 = var5;
               } else {
                  var9 = var8.length();
               }

               if (var9 > 0) {
                  var4.addElement(new co((byte)4, var8, var6, 0));
               }
            }

            if (var9 > 0) {
               var3.addElement(new Integer(var6));
               var2 = var8.substring(var7 + var9);
               var5 -= var9;
            }
         }

         for(var6 = var3.size() - 1; var6 >= 0; --var6) {
            this.a.removeElementAt((Integer)var3.elementAt(var6));
         }

         if (var3.size() > 0) {
            this.a.insertElementAt(var1 + var2, this.b);
            this.d = true;
         }

         for(var6 = var4.size() - 1; var6 >= 0; --var6) {
            this.a((co)var4.elementAt(var6));
         }

         this.a(this.b, this.a);
         this.c(false);
      }
   }

   public final void d(int var1) {
      if (this.b) {
         Vector var2 = new Vector();
         int var3 = this.g;
         int var4 = 0;

         int var7;
         for(int var5 = this.b; var3 > 0; ++var5) {
            String var6 = (String)this.a.elementAt(var5);
            if (var5 == this.b) {
               if (var3 < var6.length() - this.a) {
                  var7 = var3;
               } else {
                  var7 = var6.length() - this.a;
               }
            } else if (var3 < var6.length()) {
               var7 = var3;
            } else {
               var7 = var6.length();
            }

            if (var7 > 0) {
               var2.addElement(new Integer(var5));
               var3 -= var7;
               var4 += var6.length();
            }
         }

         this.g = var4;
         this.a = 0;
         String var9 = var1 >= 0 ? "L," : "R,";

         for(int var10 = 0; var10 < var2.size(); ++var10) {
            var7 = (Integer)var2.elementAt(var10);
            String var8 = (String)this.a.elementAt(var7);
            var9 = var9 + var7 + ",";
            if (var1 > 0) {
               this.a.setElementAt(" " + var8, var7);
               ++this.g;
               this.d = true;
            } else if (var1 < 0 && var8.charAt(0) == ' ') {
               this.a.setElementAt(var8.substring(1), var7);
               --this.g;
               this.d = true;
            }
         }

         var9 = var9.substring(0, var9.length() - 1);
         this.a(new co((byte)6, var9, this.c, 0));
      }
   }

   public final boolean a(String var1, boolean var2) {
      for(int var3 = this.c; var3 < this.a.size(); ++var3) {
         String var4 = (String)this.a.elementAt(var3);
         int var5;
         if (var2) {
            var5 = var4.indexOf(var1, var3 != this.i ? 0 : this.h);
         } else {
            var5 = var4.toLowerCase().indexOf(var1.toLowerCase(), var3 != this.i ? 0 : this.h);
         }

         this.i = var3;
         if (var5 != -1) {
            this.a(var3, var5);
            this.c(true);
            this.a(var3, var5 + var1.length());
            this.h = var5 + var1.length();
            return true;
         }
      }

      return false;
   }

   public final void a(String var1, String var2, boolean var3) {
      for(int var4 = this.c; var4 < this.a.size(); ++var4) {
         int var5 = 0;

         while(var5 != -1) {
            String var6 = (String)this.a.elementAt(var4);
            if ((var5 = var3 ? var6.indexOf(var1) : var6.toLowerCase().indexOf(var1.toLowerCase())) != -1) {
               this.a(new co((byte)3, var6, var4, var5));
               this.a.setElementAt(var6.substring(0, var5) + var2 + var6.substring(var5 + var1.length()), var4);
               this.d = true;
            }
         }
      }

      if (this.k > 0) {
         this.j();
      }

   }

   private void j() {
      for(int var1 = 0; var1 < this.a.size(); ++var1) {
         String var2;
         int var3;
         if ((var2 = (String)this.a.elementAt(var1)).length() > this.k && (var3 = var2.lastIndexOf(32, this.k - 1)) != -1) {
            String var4 = var2.substring(var3 + 1);
            this.a.setElementAt(var2.substring(0, var3 + 1), var1);
            if (var1 + 1 < this.a.size() && var2.indexOf(10) == -1) {
               var2 = (String)this.a.elementAt(var1 + 1);
               this.a.setElementAt(var4 + var2, var1 + 1);
            } else {
               this.a.insertElementAt(var4, var1 + 1);
            }

            if (this.c == var1 && this.d > var3) {
               this.a(this.c + 1, this.d - var3 - 1);
            }
         }
      }

   }

   public final void e(int var1) {
      this.l = var1;
      if (var1 == 0) {
         this.c.removeAllElements();
      }

   }

   private void a(co var1) {
      if (this.l > 0) {
         this.c.addElement(var1);
         if (this.c.size() > this.l) {
            this.c.removeElementAt(0);
         }
      }

   }

   public final void h() {
      if (this.c.size() > 0) {
         co var1 = (co)this.c.lastElement();
         this.a(var1.a, var1.b);
         int var2 = this.l;
         this.l = 0;
         if (var1.a == 2) {
            this.b(var1.a);
         } else if (var1.a == 1) {
            this.c(-var1.a.length());
         } else if (var1.a == 3) {
            this.a.setElementAt(var1.a, var1.a);
         } else if (var1.a == 4) {
            this.a.insertElementAt(var1.a, var1.a);
         } else if (var1.a == 5) {
            this.a.removeElementAt(var1.a);
         } else if (var1.a == 6) {
            String[] var7;
            byte var3 = (byte)((var7 = jd.a(var1.a))[0].equals("L") ? -1 : 1);

            for(int var4 = 1; var4 < var7.length; ++var4) {
               int var5 = Integer.parseInt(var7[var4]);
               String var6 = (String)this.a.elementAt(var5);
               if (var3 > 0) {
                  this.a.setElementAt(" " + var6, var5);
               } else if (var3 < 0 && var6.charAt(0) == ' ') {
                  this.a.setElementAt(var6.substring(1), var5);
               }
            }
         }

         this.l = var2;
         this.c.removeElementAt(this.c.size() - 1);
      }

   }
}
