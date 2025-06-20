public final class jd {
   public static String[] a(String var0) {
      int var1 = 1;

      for(int var2 = 0; var2 < var0.length(); ++var2) {
         if (var0.charAt(var2) == ',') {
            ++var1;
         }
      }

      String[] var4 = new String[var1];

      int var3;
      for(var3 = 0; (var1 = var0.indexOf(44)) != -1; ++var3) {
         var4[var3] = new String(var0.substring(0, var1));
         var0 = var0.substring(var1 + 1);
      }

      var4[var3] = new String(var0);
      return var4;
   }

   public static String a(String var0, String var1) {
      String var2 = "<" + var0.toLowerCase() + ">";
      var0 = "</" + var0.toLowerCase() + ">";
      String var3 = var1.toLowerCase();
      int var4 = var1.length();

      for(int var5 = 0; var5 < var4; ++var5) {
         if (var3.startsWith(var2, var5)) {
            for(int var6 = var4 - 1; var6 >= 0; --var6) {
               if (var3.startsWith(var0, var6)) {
                  return var1.substring(var5 + var2.length(), var6);
               }
            }
         }
      }

      return "";
   }
}
