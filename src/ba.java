public final class ba extends ms {
   private final String a;

   private ba(el var1, String var2) {
      super(var1, (fx)null);
      this.a = var2;
   }

   public final boolean a() {
      return true;
   }

   public final boolean a(String var1) {
      return this.a == var1;
   }

   public final boolean a(String[] var1) {
      for(int var2 = 0; var2 < var1.length; ++var2) {
         if (this.a == var1[var2]) {
            return true;
         }
      }

      return false;
   }

   public final String a() {
      return this.a;
   }

   public final String toString() {
      return this.a;
   }

   ba(el var1, String var2, fx var3) {
      this(var1, var2);
   }
}
