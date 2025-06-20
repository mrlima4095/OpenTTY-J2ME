public final class jm {
   public static ep a(iy var0, int var1, int var2) {
      Object var3 = null;
      --var2;

      while(var2 >= var1) {
         String var5;
         mw var10;
         boolean var6 = a(var5 = (var10 = var0.a(var2)).a(), ".jar");
         boolean var7 = a(var5, ".war");
         boolean var8 = a(var5, ".ear");
         boolean var12 = a(var5, ".zip");
         Object var9 = null;
         ep var11 = a(a(a(a((ep)(new ly(var10.b(), var6 || var7 || var8 || var12)), ".zip"), ".ear"), ".war"), ".jar");
         var11 = var9 != null ? null : var11;
         var3 = var3 != null ? new ej(var11, (ep)var3) : var11;
         --var2;
      }

      return (ep)var3;
   }

   private static ep a(ep var0, String var1) {
      return new pa(new pj(new gn(new j(var1))), (ep)null, var0);
   }

   private static boolean a(String var0, String var1) {
      int var2 = var0.length();
      int var3 = var1.length();
      return var0.regionMatches(true, var2 - var3, var1, 0, var3);
   }
}
