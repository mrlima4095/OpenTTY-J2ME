public final class ac extends dp {
   public int a;
   public int b;
   public int c;
   public byte[] a;
   public int d;
   public m[] a;
   public int e;
   public dp[] a;

   public final void a(aj var1, kn var2, jy var3) {
      var3.a(var1, var2, this);
   }

   public final void a(aj var1, kn var2, ff var3) {
      this.a(var1, var2, 0, this.c, (ff)var3);
   }

   public final void a(aj var1, kn var2, int var3, int var4, ff var5) {
      int var7;
      for(var3 = var3; var3 < var4; var3 += var7) {
         hh var6;
         var7 = (var6 = c.a(this.a, var3)).a(var3);
         var6.a(var1, var2, this, var3, var5);
      }

   }

   public final void a(aj var1, kn var2, jn var3) {
      for(int var4 = 0; var4 < this.d; ++var4) {
         var3.a(var1, var2, this, this.a[var4]);
      }

   }

   public final void a(aj var1, kn var2, int var3, int var4, jn var5) {
      for(int var6 = 0; var6 < this.d; ++var6) {
         m var7;
         m var8;
         if ((var8 = var7 = this.a[var6]).a < var4 && var8.b > var3) {
            var5.a(var1, var2, this, var7);
         }
      }

   }

   public final void b(aj var1, kn var2, jy var3) {
      for(int var4 = 0; var4 < this.e; ++var4) {
         this.a[var4].a(var1, var2, this, var3);
      }

   }
}
