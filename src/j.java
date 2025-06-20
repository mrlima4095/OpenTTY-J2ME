public final class j {
   private final String a;

   public j(String var1) {
      this.a = var1;
   }

   public final boolean a(String var1) {
      String var10000 = var1;
      var1 = this.a;
      String var4 = var10000;
      int var2 = var10000.length();
      int var3 = var1.length();
      return var4.regionMatches(true, var2 - var3, var1, 0, var3);
   }
}
