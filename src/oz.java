public final class oz {
   private int a = 1;

   public final void a() {
      this.a = 1;
   }

   public final void a(byte[] var1, int var2, int var3) {
      int var4 = this.a & '\uffff';

      int var5;
      for(var5 = this.a >>> 16; var3 > 0; var5 %= 65521) {
         int var6 = 3800;
         if (3800 > var3) {
            var6 = var3;
         }

         var3 -= var6;

         while(true) {
            --var6;
            if (var6 < 0) {
               var4 %= 65521;
               break;
            }

            var4 += var1[var2++] & 255;
            var5 += var4;
         }
      }

      this.a = var5 << 16 | var4;
   }

   public final long a() {
      return (long)this.a & 4294967295L;
   }
}
