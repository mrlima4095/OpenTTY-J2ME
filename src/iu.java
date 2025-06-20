public final class iu {
   public static gj a(mw var0, gj var1) {
      String var2;
      boolean var3 = a(var2 = var0.a(), ".jar");
      boolean var4 = a(var2, ".war");
      boolean var5 = a(var2, ".ear");
      boolean var10 = a(var2, ".zip");
      Object var6 = null;
      Object var7 = null;
      Object var8 = null;
      var0 = null;
      var1 = a(var1, var3, (String)var6, ".jar");
      if (!var3) {
         var1 = a(var1, var4, (String)var7, ".war");
         if (!var4) {
            var1 = a(var1, var5, (String)var8, ".ear");
            if (!var5) {
               var1 = a(var1, var10, var0, ".zip");
            }
         }
      }

      return var1;
   }

   private static gj a(gj var0, boolean var1, String var2, String var3) {
      return var1 ? null : new gd(new gn(new j(var3)), (gj)null, var0);
   }

   private static boolean a(String var0, String var1) {
      int var2 = var0.length();
      int var3 = var1.length();
      return var0.regionMatches(true, var2 - var3, var1, 0, var3);
   }
}
