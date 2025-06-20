public final class mw {
   private String a;
   private boolean a;

   public mw(String var1, boolean var2) {
      this.a = var1;
      this.a = var2;
   }

   public final String a() {
      try {
         return this.a.endsWith("/") ? this.a.substring(this.a.lastIndexOf(47, this.a.length() - 1) + 1, this.a.length()) : this.a.substring(this.a.lastIndexOf(47) + 1, this.a.length());
      } catch (Exception var1) {
         return null;
      }
   }

   public final String b() {
      return this.a;
   }

   public final boolean a() {
      return this.a;
   }
}
