import java.util.Enumeration;
import java.util.Vector;

public final class nc extends aa {
   private final String a;
   private final aj a;
   private final boolean a;

   public nc(String var1, aj var2, boolean var3) {
      this.a = var1;
      this.a = var2;
      this.a = var3;
   }

   public final String a() {
      return this.a;
   }

   public final aj a() {
      return this.a;
   }

   public final int b() {
      if (this.a == null) {
         return 1;
      } else {
         return this.a ? 0 : -1;
      }
   }

   public final int a(String var1, aj var2) {
      String var3;
      if ((var3 = this.a) == null) {
         return -1;
      } else {
         int var4 = ec.a(var3);
         int var5 = ec.a(var1);
         int var6 = Math.min(var4, var5);
         var3 = var3.substring(var6);
         var1 = var1.substring(var6);
         if (var6 > 0 && (ec.a(var3.charAt(0)) || ec.a(var1.charAt(0)))) {
            if (!var3.equals(var1)) {
               return -1;
            } else {
               return this.a ? 0 : 1;
            }
         } else {
            if (var4 == var6) {
               var3 = ec.b(var3);
            }

            if (var5 == var6) {
               var1 = ec.b(var1);
            }

            if (var4 > var5 && !ec.b(var1)) {
               return -1;
            } else if (var4 < var5 && !ec.b(var3)) {
               return -1;
            } else if (this.a) {
               return 0;
            } else if (!var3.equals(var1) && !"java/lang/Object".equals(var1)) {
               if (var4 > var5) {
                  return 1;
               } else if (var4 < var5) {
                  return 0;
               } else {
                  return this.a != null && var2 != null && this.a.a(var2) ? 1 : 0;
               }
            } else {
               return 1;
            }
         }
      }
   }

   public final it a(jj var1) {
      if (this.a == null) {
         return jj.a;
      } else {
         return (it)(!ec.a(this.a) ? jj.b : var1.a(this.a.substring(1), this.a, true));
      }
   }

   public final nc a(nc var1) {
      String var2 = this.a;
      String var3 = var1.a;
      if (var2 == null && var3 == null) {
         return jj.a;
      } else if (var2 == null) {
         return var1.a(true);
      } else if (var3 == null) {
         return this.a(true);
      } else {
         boolean var4 = this.a || var1.a;
         if (var2.equals(var3)) {
            return this.a(var4);
         } else {
            int var5 = ec.a(var2);
            int var6 = ec.a(var3);
            int var7 = Math.min(var5, var6);
            if (var5 == var6) {
               aj var13 = this.a;
               aj var14 = var1.a;
               if (var13 != null && var14 != null) {
                  if (var13.a(var14)) {
                     return var1.a(var4);
                  }

                  if (var14.a(var13)) {
                     return this.a(var4);
                  }

                  Vector var10 = new Vector();
                  var13.a(false, true, true, false, new kk(var10));
                  Vector var11 = new Vector();
                  var14.a(false, true, true, false, new kk(var11));
                  aj var12 = null;
                  var5 = -1;
                  Enumeration var15 = var10.elements();

                  while(true) {
                     aj var8;
                     int var9;
                     do {
                        if (!var15.hasMoreElements()) {
                           if (var12 == null) {
                              throw new IllegalArgumentException("Can't find common super class of [" + var2 + "] and [" + var3 + "]");
                           }

                           return new nc(var7 == 0 ? var12.a() : ec.a(var12.a(), var7), var12, var4);
                        }

                        var9 = a(var8 = (aj)var15.nextElement(), var10);
                     } while(var5 >= var9 && (var5 != var9 || var12 == null || var12.a().compareTo(var8.a()) <= 0));

                     var12 = var8;
                     var5 = var9;
                  }
               }
            } else if (var5 > var6) {
               if (ec.b(ec.b(var3))) {
                  return var1.a(var4);
               }
            } else if (var5 < var6 && ec.b(ec.b(var2))) {
               return this.a(var4);
            }

            if (var7 > 0 && ec.a(var3.charAt(var7)) || ec.a(var2.charAt(var7))) {
               --var7;
            }

            if (var7 == 0) {
               return var4 ? jj.b : jj.c;
            } else {
               return new nc(ec.a("java/lang/Object", var7), (aj)null, var4);
            }
         }
      }
   }

   private static int a(aj var0, Vector var1) {
      int var2 = 0;
      Enumeration var4 = var1.elements();

      while(var4.hasMoreElements()) {
         aj var3 = (aj)var4.nextElement();
         if (var0.a(var3)) {
            ++var2;
         }
      }

      return var2;
   }

   private nc a(boolean var1) {
      return !this.a && var1 ? new nc(this.a, this.a, true) : this;
   }

   public final nc a() {
      return this;
   }

   public final it a(it var1) {
      return this.a(var1.a());
   }

   public final int a() {
      return 5;
   }

   public final String b() {
      if (this.a == null) {
         return "Ljava/lang/Object;";
      } else {
         return ec.a(this.a) ? this.a : 'L' + this.a + ';';
      }
   }

   public final boolean equals(Object var1) {
      if (var1 != null && this.getClass() == var1.getClass()) {
         nc var2 = (nc)var1;
         if (this.a == null) {
            return var2.a == null;
         } else {
            return this.a == var2.a && this.a.equals(var2.a);
         }
      } else {
         return false;
      }
   }

   public final int hashCode() {
      return this.getClass().hashCode() ^ (this.a == null ? 0 : this.a.hashCode() ^ (this.a ? 0 : 1));
   }

   public final String toString() {
      return "a:" + (this.a == null ? "null" : this.a + (this.a == null ? "?" : "") + (this.a ? "" : "!"));
   }
}
