public final class ls {
   private ls() {
   }

   private static void b(ho var0, ov var1) {
      if (var0 instanceof mu) {
         ((mu)var0).a(var1);
      } else if (!(var0 instanceof fu)) {
         throw new RuntimeException("Unexpected array or initializer class " + var0.getClass().getName());
      } else {
         ho[] var3 = ((fu)var0).a;

         for(int var2 = 0; var2 < var3.length; ++var2) {
            b(var3[var2], var1);
         }

      }
   }

   public static String a(Object[] var0, String var1) {
      return a(var0, var1, 0, var0.length);
   }

   public static String a(Object[] var0, String var1, int var2, int var3) {
      if (var0 == null) {
         return "(null)";
      } else if (0 >= var3) {
         return "";
      } else {
         StringBuffer var4 = new StringBuffer(var0[0].toString());
         ++var2;

         while(var2 < var3) {
            var4.append(var1);
            var4.append(var0[var2]);
            ++var2;
         }

         return var4.toString();
      }
   }

   static void a(ho var0, ov var1) {
      b(var0, var1);
   }
}
