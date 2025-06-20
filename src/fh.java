class fh {
   private byte[] a;
   int a;
   int b;
   private int d;
   int c;

   public fh() {
      this(4096);
   }

   public fh(int var1) {
      this.a = new byte[var1];
   }

   public final void a(int var1) {
      this.a[this.b++] = (byte)var1;
      this.a[this.b++] = (byte)(var1 >> 8);
   }

   public final void a(byte[] var1, int var2, int var3) {
      System.arraycopy(var1, var2, this.a, this.b, var3);
      this.b += var3;
   }

   public final void a() {
      if (this.c > 0) {
         this.a[this.b++] = (byte)this.d;
         if (this.c > 8) {
            this.a[this.b++] = (byte)(this.d >>> 8);
         }
      }

      this.d = 0;
      this.c = 0;
   }

   public final void a(int var1, int var2) {
      this.d |= var1 << this.c;
      this.c += var2;
      if (this.c >= 16) {
         this.a[this.b++] = (byte)this.d;
         this.a[this.b++] = (byte)(this.d >>> 8);
         this.d >>>= 16;
         this.c -= 16;
      }

   }

   public final void b(int var1) {
      this.a[this.b++] = (byte)(var1 >> 8);
      this.a[this.b++] = (byte)var1;
   }

   public final boolean a() {
      return this.b == 0;
   }

   public final int a(byte[] var1, int var2, int var3) {
      if (this.c >= 8) {
         this.a[this.b++] = (byte)this.d;
         this.d >>>= 8;
         this.c -= 8;
      }

      if (var3 > this.b - this.a) {
         var3 = this.b - this.a;
         System.arraycopy(this.a, this.a, var1, var2, var3);
         this.a = 0;
         this.b = 0;
      } else {
         System.arraycopy(this.a, this.a, var1, var2, var3);
         this.a += var3;
      }

      return var3;
   }
}
