public final class gl extends mm implements ff, jn, jy {
   private final im a;
   private long[] a;
   private long[] b;
   private long[] c;
   private boolean a;
   private long a;

   public gl() {
      this(new im());
   }

   public gl(im var1) {
      this.a = new long[1024];
      this.b = new long[1024];
      this.c = new long[1024];
      this.a = var1;
   }

   public final boolean a(int var1, int var2) {
      return var2 >= 64 || (this.a[var1] & 1L << var2) != 0L;
   }

   private void a(int var1, int var2, boolean var3) {
      if (var2 < 64) {
         long[] var10000 = this.c;
         var10000[var1] |= 1L << var2;
      }

   }

   public final void a(aj var1, dp var2) {
   }

   public final void a(aj var1, kn var2, ac var3) {
      gl var8 = this;
      int var11 = var3.c;
      int var12;
      if (this.a.length < var11) {
         this.a = new long[var11];
         this.b = new long[var11];
         this.c = new long[var11];
      } else {
         for(var12 = 0; var12 < var11; ++var12) {
            var8.a[var12] = 0L;
            var8.b[var12] = 0L;
            var8.c[var12] = 0L;
         }
      }

      this.a.a(var1, var2, var3);
      int var4 = var3.c;
      int var5;
      if ((var5 = var3.b) > 64) {
         var5 = 64;
      }

      int var6;
      int var14;
      do {
         this.a = false;
         this.a = 0L;

         for(var6 = var4 - 1; var6 >= 0; --var6) {
            if (this.a.a(var6)) {
               ev var7;
               ev var9;
               int var10;
               if ((var7 = this.a.b(var6)) != null) {
                  var9 = var7;
                  var8 = this;
                  long var17 = 0L;
                  var14 = var7.b();

                  for(var10 = 0; var10 < var14; ++var10) {
                     var17 |= var8.a[var9.a(var10)];
                  }

                  this.a = var17;
               }

               this.a |= this.b[var6];
               this.b[var6] = this.a;
               c.a(var3.a, var6).a(var1, var2, var3, var6, this);
               this.a |= this.a[var6];
               if ((~this.a[var6] & this.a) != 0L) {
                  this.a[var6] = this.a;
                  boolean var10001 = this.a;
                  var9 = this.a.a(var6);
                  var12 = Integer.MIN_VALUE;
                  if (var9 != null) {
                     var10 = (var7 = var9.a()).b();

                     for(int var15 = 0; var15 < var10; ++var15) {
                        int var16 = var7.a(var15);
                        if (var12 < var16) {
                           var12 = var16;
                        }
                     }
                  }

                  this.a = var10001 | var6 < var12;
               }
            }
         }

         var3.a(var1, var2, (jn)this);
      } while(this.a);

      for(var6 = 0; var6 < var4; ++var6) {
         if (this.a.a(var6)) {
            for(var14 = 0; var14 < var5; ++var14) {
               it var13;
               long[] var10000;
               int var10002;
               boolean var18;
               if (this.a(var6, var14) && (var13 = this.a.a(var6).a(var14)) != null && var13.a()) {
                  this.a(var6, var14, true);
                  var10002 = var14 + 1;
                  var18 = true;
                  var11 = var10002;
                  if (var11 < 64) {
                     var10000 = this.a;
                     var10000[var6] |= 1L << var11;
                  }

                  this.a(var6, var14 + 1, true);
               }

               if ((var14 >= 64 || (this.b[var6] & 1L << var14) != 0L) && (var13 = this.a.b(var6).a(var14)) != null && var13.a()) {
                  this.a(var6, var14, true);
                  var10002 = var14 + 1;
                  var18 = true;
                  var11 = var10002;
                  if (var11 < 64) {
                     var10000 = this.b;
                     var10000[var6] |= 1L << var11;
                  }

                  this.a(var6, var14 + 1, true);
               }
            }
         }
      }

   }

   public final void a_(int var1, hh var2) {
   }

   public final void a(aj var1, kn var2, ac var3, int var4, b var5) {
      int var9;
      if ((var9 = var5.a) < 64) {
         long var7 = 1L << var9;
         if (var5.a < 54 && var5.a != -124) {
            this.a |= var7;
            return;
         }

         if (var5.a != -124) {
            this.a &= ~var7;
            long[] var10000 = this.b;
            var10000[var4] |= var7;
         }
      }

   }

   public final void a(aj var1, kn var2, ac var3, int var4, hf var5) {
      if (var4 == this.a.a()) {
         this.a |= 1L;
      }

   }

   public final void a(aj var1, kn var2, ac var3, m var4) {
      long var5;
      if ((var5 = this.a[var4.c]) != 0L) {
         int var7 = var4.a;
         int var8 = var4.b;

         for(var7 = var7; var7 < var8; ++var7) {
            if (this.a.a(var7) && (~(this.a[var7] & this.b[var7]) & var5) != 0L) {
               long[] var10000 = this.a;
               var10000[var7] |= var5;
               var10000 = this.b;
               var10000[var7] |= var5;
               this.a = true;
            }
         }
      }

   }
}
