final class fj implements pd {
   private int b;
   private short[] a;
   private short[] b;
   private int c;
   private int d;
   private boolean a;
   private int e;
   private int f;
   private int g;
   private byte[] a;
   private int h;
   private int i;
   private int j;
   private int k;
   private int l;
   private int m;
   private byte[] b;
   private long a;
   private int n;
   private int o;
   private dl a;
   private dy a;
   private oz a;

   fj(dl var1) {
      this.a = var1;
      this.a = new dy(var1);
      this.a = new oz();
      this.a = new byte[65536];
      this.a = new short['耀'];
      this.b = new short['耀'];
      this.e = this.f = 1;
   }

   public final void a() {
      this.a.a();
      this.a.a();
      this.e = this.f = 1;
      this.g = 0;
      this.a = 0L;
      this.a = false;
      this.d = 2;

      int var1;
      for(var1 = 0; var1 < 32768; ++var1) {
         this.a[var1] = 0;
      }

      for(var1 = 0; var1 < 32768; ++var1) {
         this.b[var1] = 0;
      }

   }

   public final void b() {
      this.a.a();
   }

   public final int a() {
      return (int)this.a.a();
   }

   public final void a(int var1) {
      this.h = var1;
   }

   public final void b(int var1) {
      this.l = pd.a[var1];
      this.j = pd.b[var1];
      this.k = pd.c[var1];
      this.i = pd.d[var1];
      if (pd.e[var1] != this.m) {
         switch(this.m) {
         case 0:
            if (this.f > this.e) {
               this.a.a(this.a, this.e, this.f - this.e, false);
               this.e = this.f;
            }

            this.c();
            break;
         case 1:
            if (this.f > this.e) {
               this.a.b(this.a, this.e, this.f - this.e, false);
               this.e = this.f;
            }
            break;
         case 2:
            if (this.a) {
               this.a.a(this.a[this.f - 1] & 255);
            }

            if (this.f > this.e) {
               this.a.b(this.a, this.e, this.f - this.e, false);
               this.e = this.f;
            }

            this.a = false;
            this.d = 2;
         }

         this.m = pd.e[var1];
      }

   }

   private void c() {
      this.b = this.a[this.f] << 5 ^ this.a[this.f + 1];
   }

   private int b() {
      int var2 = (this.b << 5 ^ this.a[this.f + 2]) & 32767;
      short var1;
      this.b[this.f & 32767] = var1 = this.a[var2];
      this.a[var2] = (short)this.f;
      this.b = var2;
      return var1 & '\uffff';
   }

   private void d() {
      System.arraycopy(this.a, 32768, this.a, 0, 32768);
      this.c -= 32768;
      this.f -= 32768;
      this.e -= 32768;

      int var1;
      int var2;
      for(var1 = 0; var1 < 32768; ++var1) {
         var2 = this.a[var1] & '\uffff';
         this.a[var1] = var2 >= 32768 ? (short)(var2 - '耀') : 0;
      }

      for(var1 = 0; var1 < 32768; ++var1) {
         var2 = this.b[var1] & '\uffff';
         this.b[var1] = var2 >= 32768 ? (short)(var2 - '耀') : 0;
      }

   }

   private boolean a(int var1) {
      int var2 = this.i;
      int var3 = this.k;
      short[] var4 = this.b;
      int var5 = this.f;
      int var7 = this.f + this.d;
      int var8 = Math.max(this.d, 2);
      int var9 = Math.max(this.f - 32506, 0);
      int var10 = var5 + 258 - 1;
      byte var11 = this.a[var7 - 1];
      byte var12 = this.a[var7];
      if (var8 >= this.l) {
         var2 >>= 2;
      }

      if (var3 > this.g) {
         var3 = this.g;
      }

      do {
         if (this.a[var1 + var8] == var12 && this.a[var1 + var8 - 1] == var11 && this.a[var1] == this.a[var5] && this.a[var1 + 1] == this.a[var5 + 1]) {
            int var6 = var1 + 2;
            var5 += 2;

            byte var10000;
            do {
               ++var5;
               var10000 = this.a[var5];
               ++var6;
               if (var10000 != this.a[var6]) {
                  break;
               }

               ++var5;
               var10000 = this.a[var5];
               ++var6;
               if (var10000 != this.a[var6]) {
                  break;
               }

               ++var5;
               var10000 = this.a[var5];
               ++var6;
               if (var10000 != this.a[var6]) {
                  break;
               }

               ++var5;
               var10000 = this.a[var5];
               ++var6;
               if (var10000 != this.a[var6]) {
                  break;
               }

               ++var5;
               var10000 = this.a[var5];
               ++var6;
               if (var10000 != this.a[var6]) {
                  break;
               }

               ++var5;
               var10000 = this.a[var5];
               ++var6;
               if (var10000 != this.a[var6]) {
                  break;
               }

               ++var5;
               var10000 = this.a[var5];
               ++var6;
               if (var10000 != this.a[var6]) {
                  break;
               }

               ++var5;
               var10000 = this.a[var5];
               ++var6;
            } while(var10000 == this.a[var6] && var5 < var10);

            if (var5 > var7) {
               this.c = var1;
               var7 = var5;
               if ((var8 = var5 - this.f) >= var3) {
                  break;
               }

               var11 = this.a[var5 - 1];
               var12 = this.a[var5];
            }

            var5 = this.f;
         }

         if ((var1 = var4[var1 & 32767] & '\uffff') <= var9) {
            break;
         }

         --var2;
      } while(var2 != 0);

      this.d = Math.min(var8, this.g);
      return this.d >= 3;
   }

   public final boolean a(boolean var1, boolean var2) {
      boolean var9;
      do {
         fj var3 = this;
         if (this.f >= 65274) {
            this.d();
         }

         while(var3.g < 262 && var3.n < var3.o) {
            int var4;
            if ((var4 = 65536 - var3.g - var3.f) > var3.o - var3.n) {
               var4 = var3.o - var3.n;
            }

            System.arraycopy(var3.b, var3.n, var3.a, var3.f + var3.g, var4);
            var3.a.a(var3.b, var3.n, var4);
            var3.n += var4;
            var3.a += (long)var4;
            var3.g += var4;
         }

         if (var3.g >= 3) {
            var3.c();
         }

         var9 = var1 && this.n == this.o;
         boolean var10000;
         int var6;
         boolean var10;
         boolean var11;
         switch(this.m) {
         case 0:
            if (!var9 && this.g == 0) {
               var10000 = false;
            } else {
               this.f += this.g;
               this.g = 0;
               if ((var6 = this.f - this.e) < pd.a && (this.e >= 32768 || var6 < 32506) && !var9) {
                  var10000 = true;
               } else {
                  var11 = var2;
                  if (var6 > pd.a) {
                     var6 = pd.a;
                     var11 = false;
                  }

                  this.a.a(this.a, this.e, var6, var11);
                  this.e += var6;
                  var10000 = !var11;
               }
            }

            var9 = var10000;
            break;
         case 1:
            var10 = var9;
            var3 = this;
            if (this.g < 262 && !var9) {
               var10000 = false;
            } else {
               while(true) {
                  if (var3.g < 262 && !var10) {
                     var10000 = true;
                     break;
                  }

                  if (var3.g == 0) {
                     var3.a.b(var3.a, var3.e, var3.f - var3.e, var2);
                     var3.e = var3.f;
                     var10000 = false;
                     break;
                  }

                  if (var3.f > 65274) {
                     var3.d();
                  }

                  if (var3.g >= 3 && (var6 = var3.b()) != 0 && var3.h != 2 && var3.f - var6 <= 32506 && var3.a(var6)) {
                     var11 = var3.a.a(var3.f - var3.c, var3.d);
                     var3.g -= var3.d;
                     if (var3.d <= var3.j && var3.g >= 3) {
                        while(--var3.d > 0) {
                           ++var3.f;
                           var3.b();
                        }

                        ++var3.f;
                     } else {
                        var3.f += var3.d;
                        if (var3.g >= 2) {
                           var3.c();
                        }
                     }

                     var3.d = 2;
                     if (!var11) {
                        continue;
                     }
                  } else {
                     var3.a.a(var3.a[var3.f] & 255);
                     ++var3.f;
                     --var3.g;
                  }

                  if (var3.a.a()) {
                     var11 = var2 && var3.g == 0;
                     var3.a.b(var3.a, var3.e, var3.f - var3.e, var11);
                     var3.e = var3.f;
                     var10000 = !var11;
                     break;
                  }
               }
            }

            var9 = var10000;
            break;
         case 2:
            var10 = var9;
            var3 = this;
            if (this.g < 262 && !var9) {
               var10000 = false;
            } else {
               while(true) {
                  if (var3.g < 262 && !var10) {
                     var10000 = true;
                     break;
                  }

                  if (var3.g == 0) {
                     if (var3.a) {
                        var3.a.a(var3.a[var3.f - 1] & 255);
                     }

                     var3.a = false;
                     var3.a.b(var3.a, var3.e, var3.f - var3.e, var2);
                     var3.e = var3.f;
                     var10000 = false;
                     break;
                  }

                  if (var3.f >= 65274) {
                     var3.d();
                  }

                  var6 = var3.c;
                  int var7 = var3.d;
                  int var8;
                  if (var3.g >= 3) {
                     var8 = var3.b();
                     if (var3.h != 2 && var8 != 0 && var3.f - var8 <= 32506 && var3.a(var8) && var3.d <= 5 && (var3.h == 1 || var3.d == 3 && var3.f - var3.c > 4096)) {
                        var3.d = 2;
                     }
                  }

                  if (var7 >= 3 && var3.d <= var7) {
                     var3.a.a(var3.f - 1 - var6, var7);
                     var7 -= 2;

                     do {
                        ++var3.f;
                        --var3.g;
                        if (var3.g >= 3) {
                           var3.b();
                        }

                        --var7;
                     } while(var7 > 0);

                     ++var3.f;
                     --var3.g;
                     var3.a = false;
                     var3.d = 2;
                  } else {
                     if (var3.a) {
                        var3.a.a(var3.a[var3.f - 1] & 255);
                     }

                     var3.a = true;
                     ++var3.f;
                     --var3.g;
                  }

                  if (var3.a.a()) {
                     var8 = var3.f - var3.e;
                     if (var3.a) {
                        --var8;
                     }

                     var10 = var2 && var3.g == 0 && !var3.a;
                     var3.a.b(var3.a, var3.e, var8, var10);
                     var3.e += var8;
                     var10000 = !var10;
                     break;
                  }
               }
            }

            var9 = var10000;
            break;
         default:
            throw new Error();
         }
      } while(this.a.a() && var9);

      return var9;
   }

   public final void a(byte[] var1, int var2, int var3) {
      if (this.n < this.o) {
         throw new IllegalStateException("Old input was not completely processed");
      } else {
         var3 += var2;
         if (0 <= var2 && var2 <= var3 && var3 <= var1.length) {
            this.b = var1;
            this.n = var2;
            this.o = var3;
         } else {
            throw new ArrayIndexOutOfBoundsException();
         }
      }
   }

   public final boolean a() {
      return this.o == this.n;
   }
}
