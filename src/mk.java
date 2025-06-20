public final class mk extends dp {
   public int a;
   public mb[] a;

   public mk() {
   }

   public mk(mb[] var1) {
      this(var1.length, var1);
   }

   private mk(int var1, mb[] var2) {
      this.a = var1;
      this.a = var2;
   }

   public final void a(aj var1, kn var2, ac var3, jy var4) {
      var4.a(var1, var2, var3, this);
   }

   public final void a(aj var1, kn var2, ac var3, ee var4) {
      int var5 = 0;

      for(int var6 = 0; var6 < this.a; ++var6) {
         mb var7 = this.a[var6];
         var5 += var7.c + (var6 == 0 ? 0 : 1);
         var7.a(var1, var2, var3, var5, var4);
      }

   }
}
