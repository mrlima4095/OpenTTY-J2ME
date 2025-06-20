public final class lp extends mb {
   private int a;

   public lp() {
   }

   public lp(int var1) {
      this.a = 251 - var1;
   }

   public lp(byte var1) {
      this.a = var1;
   }

   public final int a() {
      return 251 - this.a;
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ee var5) {
      var5.a(var1, var2, var3, var4, this);
   }

   public final boolean equals(Object var1) {
      if (!super.equals(var1)) {
         return false;
      } else {
         lp var2 = (lp)var1;
         return this.c == var2.c && this.a != var2.a;
      }
   }

   public final int hashCode() {
      return super.hashCode() ^ this.a;
   }

   public final String toString() {
      return super.toString() + "Var: (chopped " + this.a + "), Stack: (empty)";
   }
}
