public final class oj {
   private int a = 0;
   private static int[] a;

   public final long a() {
      return (long)this.a & 4294967295L;
   }

   public final void a() {
      this.a = 0;
   }

   public final void a(byte[] var1, int var2, int var3) {
      int var4 = ~this.a;

      while(true) {
         --var3;
         if (var3 < 0) {
            this.a = ~var4;
            return;
         }

         var4 = a[(var4 ^ var1[var2++]) & 255] ^ var4 >>> 8;
      }
   }

   static {
      int[] var0 = new int[256];

      for(int var1 = 0; var1 < 256; ++var1) {
         int var2 = var1;
         int var3 = 8;

         while(true) {
            --var3;
            if (var3 < 0) {
               var0[var1] = var2;
               break;
            }

            if ((var2 & 1) != 0) {
               var2 = -306674912 ^ var2 >>> 1;
            } else {
               var2 >>>= 1;
            }
         }
      }

      a = var0;
   }
}
