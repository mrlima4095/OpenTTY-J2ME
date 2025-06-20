public final class ni extends mm implements dq, ee, ff, fw, ir, jn, jy, nn {
   private int a;
   private int b;
   private int c;
   private int d = -1;
   private byte[] a = new byte[1024];
   private int[] a = new int[1024];
   private final int[] b = new int[32];
   private final int[] c = new int[32];
   private final int[][] a = new int[32][1024];
   private m[] a = new m[16];
   private int e;
   private final je a = new je();
   private final dn a = new dn();

   public final void a() {
      this.a = 0;
      this.b = 0;
      this.c = 0;
      this.d = -1;
   }

   public final void a(int var1) {
      ++this.d;
      if (this.d >= 32) {
         throw new IllegalArgumentException("Maximum number of code fragment levels exceeded [" + this.d + "]");
      } else {
         this.a += var1;
         if (this.a.length < this.a) {
            byte[] var2 = new byte[this.a];
            System.arraycopy(this.a, 0, var2, 0, this.b);
            this.a = var2;
            int[] var3 = new int[this.a];
            System.arraycopy(this.a, 0, var3, 0, this.b);
            this.a = var3;
         }

         if (this.a[this.d].length <= var1) {
            this.a[this.d] = new int[var1 + 1];
         }

         this.b[this.d] = this.b;
         this.c[this.d] = var1;
      }
   }

   public final void b(int var1, hh var2) {
      this.a[this.b] = var1;
      var2.c(this.a, this.b);
      this.a[this.d][var1] = this.b;
      this.b += var2.a(this.b);
   }

   public final void b(int var1) {
      this.a[this.d][var1] = this.b;
   }

   public final void a(m var1) {
      this.a((aj)null, (kn)null, (ac)null, (m)var1);
      if (var1.a != var1.b) {
         if (this.a.length <= this.c) {
            m[] var2 = new m[this.c + 1];
            System.arraycopy(this.a, 0, var2, 0, this.c);
            this.a = var2;
         }

         this.a[this.c++] = var1;
      }
   }

   public final void b() {
      if (this.d < 0) {
         throw new IllegalArgumentException("Code fragment not begun [" + this.d + "]");
      } else {
         hh var2;
         for(int var1 = this.b[this.d]; var1 < this.b; var1 += var2.a(var1)) {
            var2 = c.a(this.a, var1);
            if (this.a[var1] >= 0) {
               var2.a((aj)null, (kn)null, (ac)null, var1, this);
               var2.c(this.a, var1);
               this.a[var1] = -1;
            }
         }

         this.a += this.b - this.b[this.d] - this.c[this.d];
         --this.d;
      }
   }

   public final void a(aj var1, dp var2) {
   }

   public final void a(aj var1, kn var2, ac var3) {
      if (this.d != -1) {
         throw new IllegalArgumentException("Code fragment not ended [" + this.d + "]");
      } else {
         ++this.d;
         if (var3.c < this.b) {
            var3.a = new byte[this.b];
         }

         var3.c = this.b;
         System.arraycopy(this.a, 0, var3.a, 0, this.b);
         if (var3.a.length < this.c) {
            var3.a = new m[this.c];
         }

         var3.d = this.c;
         System.arraycopy(this.a, 0, var3.a, 0, this.c);
         this.a.a(var1, var2, var3);
         this.a.a(var1, var2, var3);
         var3.b(var1, var2, this);
         --this.d;
      }
   }

   public final void a(aj var1, kn var2, ac var3, hg var4) {
      this.e = -1;
      var4.a(var1, var2, var3, (ee)this);
   }

   public final void a(aj var1, kn var2, ac var3, mk var4) {
      this.e = 0;
      var4.a(var1, var2, var3, (ee)this);
   }

   public final void a(aj var1, kn var2, ac var3, as var4) {
      var4.a(var1, var2, var3, (fw)this);
      int var9 = var3.c;
      int var8 = var4.a;
      ol[] var7 = var4.a;
      int var10 = 0;

      int var11;
      for(var11 = 0; var11 < var8; ++var11) {
         ol var5;
         int var6;
         if ((var6 = (var5 = var7[var11]).a) < var9 && (var11 == 0 || var6 > var7[var11 - 1].a)) {
            var7[var10++] = var5;
         }
      }

      for(var11 = var10; var11 < var8; ++var11) {
         var7[var11] = null;
      }

      var4.a = var10;
   }

   public final void a(aj var1, kn var2, ac var3, pt var4) {
      var4.a(var1, var2, var3, (nn)this);
      int var8 = var3.b;
      int var7 = var4.a;
      ak[] var6 = var4.a;
      int var9 = 0;

      int var10;
      for(var10 = 0; var10 < var7; ++var10) {
         ak var5;
         if ((var5 = var6[var10]).b > 0 && var5.e < var8) {
            var6[var9++] = var5;
         }
      }

      for(var10 = var9; var10 < var7; ++var10) {
         var6[var10] = null;
      }

      var4.a = var9;
   }

   public final void a(aj var1, kn var2, ac var3, hy var4) {
      var4.a(var1, var2, var3, (dq)this);
      int var8 = var3.b;
      int var7 = var4.a;
      hk[] var6 = var4.a;
      int var9 = 0;

      int var10;
      for(var10 = 0; var10 < var7; ++var10) {
         hk var5;
         if ((var5 = var6[var10]).b > 0 && var5.e < var8) {
            var6[var9++] = var5;
         }
      }

      for(var10 = var9; var10 < var7; ++var10) {
         var6[var10] = null;
      }

      var4.a = var9;
   }

   public final void a_(int var1, hh var2) {
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ae var5) {
      var5.a = this.a(var4, var5.a);
   }

   public final void a(aj var1, kn var2, ac var3, int var4, do var5) {
      var5.a = this.a(var4, var5.a);
      int[] var8 = var5.a;
      int var7 = var4;
      ni var6 = this;

      for(int var9 = 0; var9 < var8.length; ++var9) {
         var8[var9] = var6.a(var7, var8[var9]);
      }

   }

   public final void a(aj var1, kn var2, ac var3, m var4) {
      var4.a = this.a(var4.a);
      var4.b = this.a(var4.b);
      var4.c = this.a(var4.c);
   }

   public final void a(aj var1, kn var2, ac var3, int var4, mb var5) {
      int var6;
      int var7 = var6 = this.a(var4);
      if (this.e >= 0) {
         var7 -= this.e;
         this.e = var6 + 1;
      }

      var5.c = var7;
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ap var5) {
      this.a(var1, var2, var3, var4, (mb)var5);
      var5.a(var1, var2, var3, var4, (ir)this);
   }

   public final void a(aj var1, kn var2, ac var3, int var4, mf var5) {
      this.a(var1, var2, var3, var4, (mb)var5);
      var5.a(var1, var2, var3, var4, (ir)this);
   }

   public final void a(aj var1, kn var2, ac var3, int var4, jo var5) {
      this.a(var1, var2, var3, var4, (mb)var5);
      var5.a(var1, var2, var3, var4, (ir)this);
      var5.b(var1, var2, var3, var4, this);
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ji var5) {
   }

   public final void a(aj var1, kn var2, ac var3, int var4, js var5) {
      var5.a = this.a(var5.a);
   }

   public final void a(ol var1) {
      var1.a = this.a(var1.a);
   }

   public final void a(ak var1) {
      int var2 = this.a(var1.a);
      int var3 = this.a(var1.a + var1.b);
      var1.a = var2;
      var1.b = var3 - var2;
   }

   public final void a(hk var1) {
      int var2 = this.a(var1.a);
      int var3 = this.a(var1.a + var1.b);
      var1.a = var2;
      var1.b = var3 - var2;
   }

   private int a(int var1, int var2) {
      if (var1 >= 0 && var1 <= this.b) {
         var1 = this.a[var1];
         return this.a(var1 + var2) - this.a(var1);
      } else {
         throw new IllegalArgumentException("Invalid instruction offset [" + var1 + "] in code with length [" + this.b + "]");
      }
   }

   private int a(int var1) {
      if (var1 >= 0 && var1 <= this.c[this.d]) {
         return this.a[this.d][var1];
      } else {
         throw new IllegalArgumentException("Invalid instruction offset [" + var1 + "] in code fragment with length [" + this.c[this.d] + "]");
      }
   }
}
