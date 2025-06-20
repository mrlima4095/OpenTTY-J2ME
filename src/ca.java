public final class ca implements dg {
   private int[] a = new int[256];
   private ny[] a = new ny[256];
   private final jg a = new jg();

   public final void a_(aj var1) {
      int var5 = var1.c;
      r[] var4 = var1.a;
      aj var3 = var1;
      ca var2 = this;
      if (this.a.length < var5) {
         this.a = new int[var5];
         this.a = new ny[var5];
      }

      for(int var6 = 1; var6 < var5; ++var6) {
         r var7;
         if ((var7 = var4[var6]) == null) {
            var7 = var4[var6 - 1];
         }

         var2.a[var6] = new ny(var3, var6, var7);
      }

      r var11 = null;

      for(int var12 = 1; var12 < var5; ++var12) {
         ny var9;
         int var8 = (var9 = var2.a[var12]).a();
         var2.a[var8] = var12;
         r var10 = var9.a();
         var4[var12] = var10 != var11 ? var10 : null;
         var11 = var10;
      }

      this.a.a(this.a);
      this.a.a_(var1);
   }
}
