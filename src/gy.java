public final class gy {
   public final int a(aj var1, String var2) {
      int var7 = var1.c;
      r[] var3 = var1.a;

      for(int var4 = 1; var4 < var7; ++var4) {
         r var5;
         ci var6;
         if ((var5 = var3[var4]) != null && var5.a() == 1 && (var6 = (ci)var5).a().equals(var2)) {
            return var4;
         }
      }

      return a(var1, (r)(new ci(var2)));
   }

   public static int a(aj var0, r var1) {
      int var2 = var0.c;
      r[] var3;
      if ((var3 = var0.a).length < var2 + 2) {
         var0.a = new r[var2 + 2];
         System.arraycopy(var3, 0, var0.a, 0, var2);
         var3 = var0.a;
      }

      var3[var0.c++] = var1;
      int var4;
      if ((var4 = var1.a()) == 5 || var4 == 6) {
         var3[var0.c++] = null;
      }

      return var2;
   }
}
