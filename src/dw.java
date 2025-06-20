public final class dw extends do {
   public int b;
   private int c;

   public final hh a() {
      return this;
   }

   protected final void a(byte[] var1, int var2) {
      var2 += -var2 & 3;
      this.a = a(var1, var2);
      var2 += 4;
      this.b = a(var1, var2);
      var2 += 4;
      this.c = a(var1, var2);
      var2 += 4;
      this.a = new int[this.c - this.b + 1];

      for(int var3 = 0; var3 < this.a.length; ++var3) {
         this.a[var3] = a(var1, var2);
         var2 += 4;
      }

   }

   protected final void b(byte[] var1, int var2) {
      while((var2 & 3) != 0) {
         a(var1, var2++, 0);
      }

      b(var1, var2, this.a);
      var2 += 4;
      b(var1, var2, this.b);
      var2 += 4;
      b(var1, var2, this.c);
      var2 += 4;
      int var3 = this.c - this.b + 1;

      for(int var4 = 0; var4 < var3; ++var4) {
         b(var1, var2, this.a[var4]);
         var2 += 4;
      }

   }

   public final int a(int var1) {
      return 1 + (-(var1 + 1) & 3) + 12 + (this.c - this.b + 1 << 2);
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ff var5) {
      var5.a(var1, var2, var3, var4, this);
   }
}
