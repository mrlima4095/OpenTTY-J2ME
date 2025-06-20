public final class ec {
   private static final cy a = new cy();

   public static int a(int var0, int var1) {
      return var0 << 16 | var1;
   }

   public static int a(int var0) {
      return var0 >>> 16;
   }

   public static int b(int var0) {
      return var0 & '\uffff';
   }

   public static boolean a(String var0) {
      return var0.length() > 1 && var0.charAt(0) == '[';
   }

   public static int a(String var0) {
      int var1;
      for(var1 = 0; var0.charAt(var1) == '['; ++var1) {
      }

      return var1;
   }

   public static boolean b(String var0) {
      return "java/lang/Object".equals(var0) || "java/lang/Cloneable".equals(var0) || "java/io/Serializable".equals(var0);
   }

   public static boolean a(char var0) {
      return var0 == 'Z' || var0 == 'B' || var0 == 'C' || var0 == 'S' || var0 == 'I' || var0 == 'F' || var0 == 'J' || var0 == 'D';
   }

   public static String a(String var0) {
      return a(var0, 0);
   }

   public static String a(String var0, int var1) {
      StringBuffer var2 = new StringBuffer(var0.length() + var1 + 2);

      for(int var3 = 0; var3 < var1; ++var3) {
         var2.append('[');
      }

      return var2.append('L').append(var0).append(';').toString();
   }

   public static String b(String var0) {
      int var2;
      return (var2 = var0.length()) > 1 && var0.charAt(var2 - 1) == ';' ? var0.substring(var0.indexOf(76) + 1, var0.length() - 1) : var0;
   }

   public static String c(String var0) {
      int var1 = var0.indexOf(41);
      return var0.substring(var1 + 1);
   }

   public static int b(String var0) {
      a.a(var0);

      int var1;
      for(var1 = 0; a.a(); ++var1) {
         a.a();
      }

      return var1;
   }

   public static int c(String var0) {
      a.a(var0);

      String var1;
      int var2;
      for(var2 = 0; a.a(); var2 += d(var1)) {
         var1 = a.a();
      }

      return var2;
   }

   public static int d(String var0) {
      if (var0.length() == 1) {
         char var1;
         if ((var1 = var0.charAt(0)) == 'J' || var1 == 'D') {
            return 2;
         }

         if (var1 == 'V') {
            return 0;
         }
      }

      return 1;
   }

   static {
      new hd();
   }
}
