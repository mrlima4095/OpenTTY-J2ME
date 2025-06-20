public final class fd {
   private static final Object[] a = new Object[]{"public", new Short((short)1), "private", new Short((short)2), "protected", new Short((short)4), "static", new Short((short)8), "final", new Short((short)16), "super", new Short((short)32), "synchronized", new Short((short)32), "volatile", new Short((short)64), "transient", new Short((short)128), "native", new Short((short)256), "interface", new Short((short)512), "abstract", new Short((short)1024), "strictfp", new Short((short)2048)};

   private fd() {
   }

   public static boolean a(short var0) {
      return (var0 & 7) == 2;
   }

   public static short a(short var0, short var1) {
      return (short)(var0 & -8);
   }

   public static String a(short var0) {
      String var1 = "";

      for(int var2 = 0; var2 < a.length; var2 += 2) {
         if ((var0 & (Short)a[var2 + 1]) != 0) {
            if (var1.length() > 0) {
               var1 = var1 + ' ';
            }

            var1 = var1 + (String)a[var2];
         }
      }

      return var1;
   }
}
