public final class gr extends do {
   public int[] b;

   public final hh a() {
      return this;
   }

   protected final void a(byte[] var1, int var2) {
      var2 += -var2 & 3;
      this.a = a(var1, var2);
      var2 += 4;
      int var3 = a(var1, var2);
      var2 += 4;
      this.b = new int[var3];
      this.a = new int[var3];

      for(int var4 = 0; var4 < var3; ++var4) {
         this.b[var4] = a(var1, var2);
         var2 += 4;
         this.a[var4] = a(var1, var2);
         var2 += 4;
      }

   }

   protected final void b(byte[] var1, int var2) {
      while((var2 & 3) != 0) {
         a(var1, var2++, 0);
      }

      b(var1, var2, this.a);
      var2 += 4;
      b(var1, var2, this.b.length);
      var2 += 4;

      for(int var3 = 0; var3 < this.b.length; ++var3) {
         b(var1, var2, this.b[var3]);
         var2 += 4;
         b(var1, var2, this.a[var3]);
         var2 += 4;
      }

   }

   public final int a(int var1) {
      return 1 + (-(var1 + 1) & 3) + 8 + (this.b.length << 3);
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ff var5) {
      var5.a(var1, var2, var3, var4, this);
   }
}
