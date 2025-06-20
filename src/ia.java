final class ia {
   short[] a;
   private short[] b;
   byte[] a;
   private int[] a;
   private int b;
   int a;
   private int c;
   private final dy a;

   ia(dy var1, int var2, int var3, int var4) {
      this.a = var1;
      this.b = var3;
      this.c = var4;
      this.a = new short[var2];
      this.a = new int[var4];
   }

   final void a() {
      for(int var1 = 0; var1 < this.a.length; ++var1) {
         this.a[var1] = 0;
      }

      this.b = null;
      this.a = null;
   }

   final void a(int var1) {
      this.a.a.a(this.b[var1] & '\uffff', this.a[var1]);
   }

   final void a(short[] var1, byte[] var2) {
      this.b = var1;
      this.a = var2;
   }

   public final void b() {
      int[] var1 = new int[this.c];
      int var2 = 0;
      this.b = new short[this.a.length];

      int var3;
      for(var3 = 0; var3 < this.c; ++var3) {
         var1[var3] = var2;
         var2 += this.a[var3] << 15 - var3;
      }

      for(var3 = 0; var3 < this.a; ++var3) {
         byte var4;
         if ((var4 = this.a[var3]) > 0) {
            this.b[var3] = dy.a(var1[var4 - 1]);
            var1[var4 - 1] += 1 << 16 - var4;
         }
      }

   }

   private void a(int[] var1) {
      this.a = new byte[this.a.length];
      int var2;
      int var3 = ((var2 = var1.length / 2) + 1) / 2;
      int var4 = 0;

      for(int var5 = 0; var5 < this.c; ++var5) {
         this.a[var5] = 0;
      }

      int[] var7;
      (var7 = new int[var2])[var2 - 1] = 0;
      --var2;

      int var10002;
      int var6;
      for(; var2 >= 0; --var2) {
         if (var1[var2 * 2 + 1] != -1) {
            if ((var6 = var7[var2] + 1) > this.c) {
               var6 = this.c;
               ++var4;
            }

            var7[var1[var2 * 2]] = var7[var1[var2 * 2 + 1]] = var6;
         } else {
            var6 = var7[var2];
            var10002 = this.a[var6 - 1]++;
            this.a[var1[var2 * 2]] = (byte)var7[var2];
         }
      }

      if (var4 != 0) {
         var2 = this.c - 1;

         do {
            do {
               --var2;
            } while(this.a[var2] == 0);

            do {
               var10002 = this.a[var2]--;
               ++var2;
               var10002 = this.a[var2]++;
            } while((var4 -= 1 << this.c - 1 - var2) > 0 && var2 < this.c - 1);
         } while(var4 > 0);

         int[] var10000 = this.a;
         int var10001 = this.c - 1;
         var10000[var10001] += var4;
         var10000 = this.a;
         var10001 = this.c - 2;
         var10000[var10001] -= var4;
         var6 = var3 * 2;

         for(var2 = this.c; var2 != 0; --var2) {
            var3 = this.a[var2 - 1];

            while(var3 > 0) {
               var4 = 2 * var1[var6++];
               if (var1[var4 + 1] == -1) {
                  this.a[var1[var4]] = (byte)var2;
                  --var3;
               }
            }
         }

      }
   }

   final void c() {
      int var1;
      int[] var2 = new int[var1 = this.a.length];
      int var3 = 0;
      int var4 = 0;

      int var5;
      int var7;
      for(var5 = 0; var5 < var1; ++var5) {
         short var6;
         if ((var6 = this.a[var5]) != 0) {
            for(var4 = var3++; var4 > 0 && this.a[var2[var7 = (var4 - 1) / 2]] > var6; var4 = var7) {
               var2[var4] = var2[var7];
            }

            var2[var4] = var5;
            var4 = var5;
         }
      }

      while(var3 < 2) {
         int var10000;
         if (var4 < 2) {
            ++var4;
            var10000 = var4;
         } else {
            var10000 = 0;
         }

         var5 = var10000;
         var2[var3++] = var5;
      }

      this.a = Math.max(var4 + 1, this.b);
      int[] var12 = new int[var3 * 4 - 2];
      int[] var11 = new int[var3 * 2 - 1];
      var7 = var3;

      for(var1 = 0; var1 < var3; var2[var1] = var1++) {
         var5 = var2[var1];
         var12[var1 * 2] = var5;
         var12[var1 * 2 + 1] = -1;
         var11[var1] = this.a[var5] << 8;
      }

      label82:
      do {
         var1 = var2[0];
         --var3;
         var5 = var2[var3];
         int var8 = 0;

         int var9;
         for(var9 = 1; var9 < var3; var9 = (var9 << 1) + 1) {
            if (var9 + 1 < var3 && var11[var2[var9]] > var11[var2[var9 + 1]]) {
               ++var9;
            }

            var2[var8] = var2[var9];
            var8 = var9;
         }

         int var10 = var11[var5];

         while(true) {
            var9 = var8;
            if (var8 <= 0 || var11[var2[var8 = (var8 - 1) / 2]] <= var10) {
               var2[var8] = var5;
               var8 = var2[0];
               var5 = var7++;
               var12[var5 * 2] = var1;
               var12[var5 * 2 + 1] = var8;
               var9 = Math.min(var11[var1] & 255, var11[var8] & 255);
               var11[var5] = var10 = var11[var1] + var11[var8] - var9 + 1;
               var8 = 0;

               for(var9 = 1; var9 < var3; var9 = (var9 << 1) + 1) {
                  if (var9 + 1 < var3 && var11[var2[var9]] > var11[var2[var9 + 1]]) {
                     ++var9;
                  }

                  var2[var8] = var2[var9];
                  var8 = var9;
               }

               while(true) {
                  var9 = var8;
                  if (var8 <= 0 || var11[var2[var8 = (var8 - 1) / 2]] <= var10) {
                     var2[var8] = var5;
                     continue label82;
                  }

                  var2[var9] = var2[var8];
               }
            }

            var2[var9] = var2[var8];
         }
      } while(var3 > 1);

      if (var2[0] != var12.length / 2 - 1) {
         throw new RuntimeException("Weird!");
      } else {
         this.a(var12);
      }
   }

   final int a() {
      int var1 = 0;

      for(int var2 = 0; var2 < this.a.length; ++var2) {
         var1 += this.a[var2] * this.a[var2];
      }

      return var1;
   }

   final void a(ia var1) {
      byte var4 = -1;
      int var5 = 0;

      while(var5 < this.a) {
         int var3 = 1;
         short var2;
         byte var6;
         if ((var6 = this.a[var5]) == 0) {
            var2 = 138;
         } else {
            var2 = 6;
            if (var4 != var6) {
               ++var1.a[var6];
               var3 = 0;
            }
         }

         var4 = var6;
         ++var5;

         while(var5 < this.a && var4 == this.a[var5]) {
            ++var5;
            ++var3;
            if (var3 >= var2) {
               break;
            }
         }

         if (var3 < 3) {
            short[] var10000 = var1.a;
            var10000[var4] = (short)(var10000[var4] + var3);
         } else if (var4 != 0) {
            ++var1.a[16];
         } else if (var3 <= 10) {
            ++var1.a[17];
         } else {
            ++var1.a[18];
         }
      }

   }

   final void b(ia var1) {
      byte var4 = -1;
      int var5 = 0;

      while(true) {
         while(var5 < this.a) {
            int var3 = 1;
            short var2;
            byte var6;
            if ((var6 = this.a[var5]) == 0) {
               var2 = 138;
            } else {
               var2 = 6;
               if (var4 != var6) {
                  var1.a(var6);
                  var3 = 0;
               }
            }

            var4 = var6;
            ++var5;

            while(var5 < this.a && var4 == this.a[var5]) {
               ++var5;
               ++var3;
               if (var3 >= var2) {
                  break;
               }
            }

            if (var3 < 3) {
               while(var3-- > 0) {
                  var1.a(var4);
               }
            } else if (var4 != 0) {
               var1.a(16);
               this.a.a.a(var3 - 3, 2);
            } else if (var3 <= 10) {
               var1.a(17);
               this.a.a.a(var3 - 3, 3);
            } else {
               var1.a(18);
               this.a.a.a(var3 - 11, 7);
            }
         }

         return;
      }
   }
}
