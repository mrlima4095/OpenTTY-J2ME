import java.util.Vector;

public final class hs extends mm implements ad, jy {
   private final boolean a;
   private final im a = new im();
   private final gl a;
   private final gy a;
   private final gu a;

   public hs() {
      this.a = new gl(this.a);
      this.a = new gy();
      this.a = new gu();
      this.a = true;
   }

   public final void a(aj var1, dp var2) {
   }

   public final void a(aj var1, kn var2, ac var3) {
      try {
         ac var5 = var3;
         hs var12 = this;
         aj var6 = (aj)var1;
         kn var7 = (kn)var2;
         this.a.a(var1, var2, var3);
         Vector var13 = new Vector();

         for(int var8 = 0; var8 < var5.c; ++var8) {
            if (var12.a.a(var8) && var12.a.b(var8)) {
               ji[] var9 = var12.a(var6, var7, var5, var8, var12.a.a(var8));
               ji[] var10 = var12.a(var6, var7, var5, var8, var12.a.a(var8));
               var13.addElement(new jo(var8, var9, var10));
            }
         }

         if (!var12.a && !var13.isEmpty()) {
            ji[] var15 = var12.a(var6, var7, var5, -1, (io)var12.a.a(0));
            if (var2.a(var6).equals("<init>")) {
               var15[0] = na.a();
            }

            var12.a(var15, var13);
         }

         String var16 = var12.a ? "StackMap" : "StackMapTable";
         int var17;
         if ((var17 = var13.size()) == 0) {
            var12.a.a(var6, var5, var16);
         } else {
            Object var18;
            if (var12.a) {
               jo[] var4 = new jo[var17];
               var13.copyInto(var4);
               var18 = new hg(var4);
            } else {
               mb[] var14 = new mb[var17];
               var13.copyInto(var14);
               var18 = new mk(var14);
            }

            ((dp)var18).f = var12.a.a(var6, var16);
            var12.a.a(var6, var5, (dp)var18);
         }
      } catch (RuntimeException var11) {
         System.err.println("Unexpected error while preverifying:");
         System.err.println("  Class       = [" + var1.a() + "]");
         System.err.println("  Method      = [" + var2.a(var1) + var2.b(var1) + "]");
         System.err.println("  Exception   = [" + var11.getClass().getName() + "] (" + var11.getMessage() + ")");
         throw var11;
      }
   }

   private ji[] a(aj var1, kn var2, ac var3, int var4, io var5) {
      int var11 = var5.a();
      int var6 = 0;
      int var7 = 0;

      for(int var8 = 0; var8 < var11; ++var8) {
         it var9 = var5.a(var8);
         ++var7;
         if (var9 != null && (var4 == -1 || this.a.a(var4, var8))) {
            var6 = var7;
            if (var9.a()) {
               ++var8;
            }
         }
      }

      ji[] var13 = new ji[var6];
      var7 = 0;

      for(int var14 = 0; var7 < var6; ++var14) {
         it var12 = var5.a(var14);
         it var10 = var5.c(var14);
         Object var15;
         if (var12 != null && (var4 == -1 || this.a.a(var4, var14))) {
            var15 = this.a(var1, var3, var4, var12, var10);
            if (var12.a()) {
               ++var14;
            }
         } else {
            var15 = na.a();
         }

         var13[var7++] = (ji)var15;
      }

      return var13;
   }

   private ji[] a(aj var1, kn var2, ac var3, int var4, eq var5) {
      int var11 = var5.a();
      int var6 = 0;

      for(int var7 = 0; var7 < var11; ++var7) {
         it var8 = var5.c(var7);
         ++var6;
         if (var8.a()) {
            ++var7;
         }
      }

      ji[] var12 = new ji[var6];
      int var13 = var6;

      for(var6 = 0; var6 < var11; ++var6) {
         it var9 = var5.c(var6);
         it var10 = var5.a(var6);
         --var13;
         var12[var13] = this.a(var1, var3, var4, var9, var10);
         if (var9.a()) {
            ++var6;
         }
      }

      return var12;
   }

   private ji a(aj var1, ac var2, int var3, it var4, it var5) {
      if (var4 == null) {
         return na.a();
      } else {
         int var6;
         switch(var6 = var4.a()) {
         case 1:
         case 6:
            return na.a();
         case 2:
            return na.a();
         case 3:
            return na.a();
         case 4:
            return na.a();
         case 5:
            nc var12;
            if ((var12 = var4.a()).b() == 1) {
               return na.a();
            } else {
               ev var14;
               if (var3 != -1 && (var14 = var5.a()).b() == 1) {
                  int var15 = var14.a(0);
                  if (this.a.a() && var3 <= this.a.a() && var15 > -1 && var2.a[var15] == 42) {
                     var15 = -1;
                  }

                  int var9;
                  if ((var9 = var15 == -1 ? this.a.a() : this.a.a(var15)) != -2 && var3 <= var9) {
                     if (var15 == -1) {
                        return na.a();
                     }

                     return na.a(var15);
                  }
               }

               gy var10000 = this.a;
               String var10002 = var12.a();
               aj var11 = var12.a();
               String var10 = var10002;
               var1 = var1;
               gy var8 = var10000;
               int var13 = var1.c;
               r[] var16 = var1.a;
               var6 = 1;

               int var18;
               while(true) {
                  if (var6 >= var13) {
                     var6 = var8.a(var1, var10);
                     var18 = gy.a(var1, (r)(new bc(var6, var11)));
                     break;
                  }

                  r var7;
                  bc var17;
                  if ((var7 = var16[var6]) != null && var7.a() == 7 && (var17 = (bc)var7).a(var1).equals(var10)) {
                     var18 = var6;
                     break;
                  }

                  ++var6;
               }

               return na.a(var18);
            }
         case 7:
            return na.a();
         default:
            throw new IllegalArgumentException("Unknown computational type [" + var6 + "]");
         }
      }
   }

   private void a(ji[] var1, Vector var2) {
      int var11 = var1.length;
      var1 = var1;
      int var3 = -1;

      for(int var4 = 0; var4 < var2.size(); ++var4) {
         jo var5;
         int var6 = (var5 = (jo)var2.elementAt(var4)).a;
         ji[] var7 = var5.a;
         int var8 = var5.b;
         ji[] var9 = var5.b;
         Object var10 = var5;
         if (var6 == var11 && a(var7, var1, var6)) {
            if (var8 == 0) {
               var10 = new op();
            } else if (var8 == 1) {
               var10 = new ap(var9[0]);
            }
         } else if (var8 == 0) {
            if ((var8 = var6 - var11) < 0 && var8 > -4 && a(var7, var1, var6)) {
               var10 = new lp((byte)(-var8));
            } else if (var8 > 0 && var8 < 4 && a(var7, var1, var11)) {
               ji[] var12 = new ji[var8];
               System.arraycopy(var7, var6 - var8, var12, 0, var8);
               var10 = new mf(var12);
            }
         }

         var8 = var5.c;
         ((mb)var10).c = var8 - var3 - 1;
         var3 = var8;
         var11 = var5.a;
         var1 = var5.a;
         var2.setElementAt(var10, var4);
      }

   }

   private static boolean a(ji[] var0, ji[] var1, int var2) {
      if (var2 > 0 && (var0.length < var2 || var1.length < var2)) {
         return false;
      } else {
         for(int var3 = 0; var3 < var2; ++var3) {
            if (!var0[var3].equals(var1[var3])) {
               return false;
            }
         }

         return true;
      }
   }
}
