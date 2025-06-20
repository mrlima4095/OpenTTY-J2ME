public final class gu {
   public final void a(aj var1, ac var2, dp var3) {
      dp[] var4 = var2.a;
      int var10 = var2.e;
      aj var8 = var1;
      String var6 = var3.a(var1);
      int var7 = 0;

      boolean var12;
      while(true) {
         if (var7 >= var10) {
            var12 = false;
            break;
         }

         if (var4[var7].a(var8).equals(var6)) {
            var4[var7] = var3;
            var12 = true;
            break;
         }

         ++var7;
      }

      if (!var12) {
         dp[] var11 = var2.a;
         int var9 = var2.e;
         if (var11.length <= var9) {
            dp[] var5 = new dp[var9 + 1];
            System.arraycopy(var11, 0, var5, 0, var9);
            var11 = var5;
         }

         var11[var9] = var3;
         var2.a = var11;
         ++var2.e;
      }

   }

   public final void a(aj var1, ac var2, String var3) {
      var3 = var3;
      dp[] var8 = var2.a;
      int var7 = var2.e;
      aj var6 = var1;
      int var4 = 0;

      int var5;
      for(var5 = 0; var5 < var7; ++var5) {
         if (!var8[var5].a(var6).equals(var3)) {
            var8[var4++] = var8[var5];
         }
      }

      for(var5 = var4; var5 < var7; ++var5) {
         var8[var5] = null;
      }

      var2.e = var4;
   }
}
