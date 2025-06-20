final class dy {
   private static final int[] a = new int[]{16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15};
   dl a;
   private ia a;
   private ia b;
   private ia c;
   private short[] a;
   private byte[] a;
   private int a;
   private int b;
   private static short[] b = new short[286];
   private static byte[] b = new byte[286];
   private static short[] c;
   private static byte[] c;

   static short a(int var0) {
      return (short)("\u0000\b\u0004\f\u0002\n\u0006\u000e\u0001\t\u0005\r\u0003\u000b\u0007\u000f".charAt(var0 & 15) << 12 | "\u0000\b\u0004\f\u0002\n\u0006\u000e\u0001\t\u0005\r\u0003\u000b\u0007\u000f".charAt(var0 >> 4 & 15) << 8 | "\u0000\b\u0004\f\u0002\n\u0006\u000e\u0001\t\u0005\r\u0003\u000b\u0007\u000f".charAt(var0 >> 8 & 15) << 4 | "\u0000\b\u0004\f\u0002\n\u0006\u000e\u0001\t\u0005\r\u0003\u000b\u0007\u000f".charAt(var0 >> 12));
   }

   public dy(dl var1) {
      this.a = var1;
      this.a = new ia(this, 286, 257, 15);
      this.b = new ia(this, 30, 1, 15);
      this.c = new ia(this, 19, 4, 7);
      this.a = new short[16384];
      this.a = new byte[16384];
   }

   public final void a() {
      this.a = 0;
      this.b = 0;
      this.a.a();
      this.b.a();
      this.c.a();
   }

   private static int a(int var0) {
      if (var0 == 255) {
         return 285;
      } else {
         int var1;
         for(var1 = 257; var0 >= 8; var0 >>= 1) {
            var1 += 4;
         }

         return var1 + var0;
      }
   }

   private static int b(int var0) {
      int var1;
      for(var1 = 0; var0 >= 4; var0 >>= 1) {
         var1 += 2;
      }

      return var1 + var0;
   }

   private void b() {
      for(int var1 = 0; var1 < this.a; ++var1) {
         int var2 = this.a[var1] & 255;
         short var3;
         short var10000 = var3 = this.a[var1];
         int var5 = var3 - 1;
         if (var10000 != 0) {
            int var4 = a(var2);
            this.a.a(var4);
            if ((var4 = (var4 - 261) / 4) > 0 && var4 <= 5) {
               this.a.a(var2 & (1 << var4) - 1, var4);
            }

            var2 = b(var5);
            this.b.a(var2);
            if ((var4 = var2 / 2 - 1) > 0) {
               this.a.a(var5 & (1 << var4) - 1, var4);
            }
         } else {
            this.a.a(var2);
         }
      }

      this.a.a(256);
   }

   public final void a(byte[] var1, int var2, int var3, boolean var4) {
      this.a.a(0 + (var4 ? 1 : 0), 3);
      this.a.a();
      this.a.a(var3);
      this.a.a(~var3);
      this.a.a(var1, var2, var3);
      this.a();
   }

   public final void b(byte[] var1, int var2, int var3, boolean var4) {
      ++this.a.a[256];
      this.a.c();
      this.b.c();
      this.a.a(this.c);
      this.b.a(this.c);
      this.c.c();
      int var5 = 4;

      int var6;
      for(var6 = 18; var6 > var5; --var6) {
         if (this.c.a[a[var6]] > 0) {
            var5 = var6 + 1;
         }
      }

      var6 = 14 + var5 * 3 + this.c.a() + this.a.a() + this.b.a() + this.b;
      int var7 = this.b;

      int var8;
      for(var8 = 0; var8 < 286; ++var8) {
         var7 += this.a.a[var8] * b[var8];
      }

      for(var8 = 0; var8 < 30; ++var8) {
         var7 += this.b.a[var8] * c[var8];
      }

      if (var6 >= var7) {
         var6 = var7;
      }

      if (var2 >= 0 && var3 + 4 < var6 >> 3) {
         this.a(var1, var2, var3, var4);
      } else if (var6 == var7) {
         this.a.a(2 + (var4 ? 1 : 0), 3);
         this.a.a(b, b);
         this.b.a(c, c);
         this.b();
         this.a();
      } else {
         this.a.a(4 + (var4 ? 1 : 0), 3);
         var2 = var5;
         dy var9 = this;
         this.c.b();
         this.a.b();
         this.b.b();
         this.a.a(this.a.a - 257, 5);
         this.a.a(this.b.a - 1, 5);
         this.a.a(var5 - 4, 4);

         for(var3 = 0; var3 < var2; ++var3) {
            var9.a.a(var9.c.a[a[var3]], 3);
         }

         var9.a.b(var9.c);
         var9.b.b(var9.c);
         this.b();
         this.a();
      }
   }

   public final boolean a() {
      return this.a == 16384;
   }

   public final boolean a(int var1) {
      this.a[this.a] = 0;
      this.a[this.a++] = (byte)var1;
      ++this.a.a[var1];
      return this.a == 16384;
   }

   public final boolean a(int var1, int var2) {
      this.a[this.a] = (short)var1;
      this.a[this.a++] = (byte)(var2 - 3);
      var2 = a(var2 - 3);
      ++this.a.a[var2];
      if (var2 >= 265 && var2 < 285) {
         this.b += (var2 - 261) / 4;
      }

      var1 = b(var1 - 1);
      ++this.b.a[var1];
      if (var1 >= 4) {
         this.b += var1 / 2 - 1;
      }

      return this.a == 16384;
   }

   static {
      int var0;
      for(var0 = 0; var0 < 144; b[var0++] = 8) {
         b[var0] = a(var0 + 48 << 8);
      }

      while(var0 < 256) {
         b[var0] = a(var0 + 256 << 7);
         b[var0++] = 9;
      }

      while(var0 < 280) {
         b[var0] = a(var0 + -256 << 9);
         b[var0++] = 7;
      }

      while(var0 < 286) {
         b[var0] = a(var0 + -88 << 8);
         b[var0++] = 8;
      }

      c = new short[30];
      c = new byte[30];

      for(var0 = 0; var0 < 30; ++var0) {
         c[var0] = a(var0 << 11);
         c[var0] = 5;
      }

   }
}
