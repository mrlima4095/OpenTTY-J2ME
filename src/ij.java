public final class ij extends mm implements ff, fs, jn, jy {
   private short[] a = new short[1025];
   private int[] a = new int[1024];
   private int[] b = new int[1024];
   private int[] c = new int[1024];
   private int[] d = new int[1024];
   private int a;
   private int b;
   private int c;
   private final int[] e = new int[32];
   private int d;
   private boolean a;

   public final boolean a(int var1) {
      return (this.a[var1] & 1) != 0;
   }

   public final boolean b(int var1) {
      return (this.a[var1] & 4) != 0;
   }

   public final boolean c(int var1) {
      return (this.a[var1] & 64) != 0;
   }

   public final boolean d(int var1) {
      return (this.a[var1] & 128) != 0;
   }

   public final boolean e(int var1) {
      return this.a[var1] == var1;
   }

   public final boolean f(int var1) {
      return this.a[var1] != -2;
   }

   public final boolean g(int var1) {
      return (this.a[var1] & 256) != 0;
   }

   public final int a(int var1) {
      return this.b[var1];
   }

   public final int b(int var1) {
      return this.d[var1];
   }

   public final boolean a() {
      return this.a != -2;
   }

   public final int a() {
      return this.a;
   }

   public final void a(aj var1, dp var2) {
   }

   public final void a(aj var1, kn var2, ac var3) {
      int var4 = var3.c;
      int var5;
      if (this.a.length < var4) {
         this.a = new short[var4 + 1];
         this.a = new int[var4];
         this.b = new int[var4];
         this.c = new int[var4];
         this.d = new int[var4];

         for(var5 = 0; var5 < var4; ++var5) {
            this.a[var5] = -2;
            this.b[var5] = -2;
            this.c[var5] = -2;
            this.d[var5] = -2;
         }
      } else {
         for(var5 = 0; var5 < var4; ++var5) {
            this.a[var5] = 0;
            this.a[var5] = -2;
            this.b[var5] = -2;
            this.c[var5] = -2;
            this.d[var5] = -2;
         }

         this.a[var4] = 0;
      }

      this.a = -2;
      this.b = -2;
      this.c = -2;
      this.d = 0;
      if (var2.a(var1).equals("<init>")) {
         this.e[this.d++] = -1;
      }

      this.a[var4] = 4;
      var3.a(var1, var2, (ff)this);
      var3.a(var1, var2, (jn)this);
      var5 = -2;
      int var6 = var4;
      boolean var7 = false;

      for(int var8 = var4 - 1; var8 >= 0; --var8) {
         if (this.a(var8)) {
            if (this.a[var8] != -2) {
               var5 = this.a[var8];
            } else if (var5 != -2) {
               this.a[var8] = var5;
            }

            if (this.e(var8)) {
               var5 = -2;
            }

            if (this.f(var8)) {
               this.b[var8] = var6;
               if (this.g(var8)) {
                  var7 = true;
               } else if (var7) {
                  short[] var10000 = this.a;
                  var10000[var8] = (short)(var10000[var8] | 256);
               }
            } else {
               var6 = var8;
               var7 = false;
            }
         }
      }

   }

   public final void a(aj var1, kn var2, ac var3, int var4, du var5) {
      short[] var10000 = this.a;
      var10000[var4] = (short)(var10000[var4] | 1);
      this.c(var4);
      byte var6;
      if ((var6 = var5.a) == -84 || var6 == -83 || var6 == -82 || var6 == -81 || var6 == -80 || var6 == -65) {
         this.a(var4);
         this.b(var4 + var5.a(var4));
      }

   }

   public final void a(aj var1, kn var2, ac var3, int var4, hf var5) {
      short[] var10000 = this.a;
      var10000[var4] = (short)(var10000[var4] | 1);
      this.c(var4);
      if (var5.a == -69) {
         this.e[this.d++] = var4;
      } else {
         this.a = false;
         var1.a(var5.a, this);
         if (this.a) {
            int var6 = this.e[--this.d];
            this.c[var4] = var6;
            if (var6 == -1) {
               this.a = var4;
               return;
            }

            this.d[var6] = var4;
         }

      }
   }

   public final void a(aj var1, kn var2, ac var3, int var4, b var5) {
      short[] var10000 = this.a;
      var10000[var4] = (short)(var10000[var4] | 1);
      this.c(var4);
      if (var5.a == -87) {
         this.a(var4);
         var10000 = this.a;
         var10000[var4] = (short)(var10000[var4] | 256);
         this.b(var4 + var5.a(var4));
      }

   }

   public final void a(aj var1, kn var2, ac var3, int var4, ae var5) {
      this.a(var4);
      this.c(var4);
      this.a(var4, var5.a);
      byte var6;
      if ((var6 = var5.a) != -88 && var6 != -55) {
         if (var6 == -89 || var6 == -56) {
            this.b(var4 + var5.a(var4));
         }

      } else {
         short[] var10000 = this.a;
         var10000[var4] = (short)(var10000[var4] | 128);
         int var7 = var4 + var5.a;
         this.a[var7] = var7;
      }
   }

   public final void a(aj var1, kn var2, ac var3, int var4, do var5) {
      this.a(var4);
      this.c(var4);
      this.a(var4, var5.a);
      int[] var9 = var5.a;
      int var8 = var4;
      ij var7 = this;

      for(int var6 = 0; var6 < var9.length; ++var6) {
         var7.a(var8, var9[var6]);
      }

      this.b(var4 + var5.a(var4));
   }

   public final void a(aj var1, r var2) {
   }

   public final void a(aj var1, jp var2) {
      this.a = var2.b(var1).equals("<init>");
   }

   public final void a(aj var1, kn var2, ac var3, m var4) {
      short[] var10000 = this.a;
      int var10001 = var4.a;
      var10000[var10001] = (short)(var10000[var10001] | 16);
      var10000 = this.a;
      var10001 = var4.b;
      var10000[var10001] = (short)(var10000[var10001] | 32);
      var10000 = this.a;
      var10001 = var4.c;
      var10000[var10001] = (short)(var10000[var10001] | 64);
   }

   private void a(int var1) {
      short[] var10000 = this.a;
      var10000[var1] = (short)(var10000[var1] | 3);
   }

   private void a(int var1, int var2) {
      var2 += var1;
      short[] var10000 = this.a;
      var10000[var2] = (short)(var10000[var2] | 4);
      if (this.f(var1)) {
         this.a[var2] = this.b;
         if (this.c < var2) {
            this.c = var2;
         }
      }

   }

   private void b(int var1) {
      short[] var10000 = this.a;
      var10000[var1] = (short)(var10000[var1] | 8);
      if (this.c <= var1) {
         this.b = -2;
      }

   }

   private void c(int var1) {
      if (this.f(var1)) {
         this.b = this.a[var1];
      } else {
         this.a[var1] = this.b;
      }
   }
}
