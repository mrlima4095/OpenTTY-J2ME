public final class js extends ji {
   public int a;

   public js() {
   }

   public js(int var1) {
      this.a = var1;
   }

   public final int a() {
      return 8;
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ir var5) {
      var5.a(var1, var2, var3, var4, this);
   }

   public final void a(aj var1, kn var2, ac var3, int var4, int var5, ir var6) {
      var6.b(var1, var2, var3, var4, this);
   }

   public final void b(aj var1, kn var2, ac var3, int var4, int var5, ir var6) {
      var6.c(var1, var2, var3, var4, this);
   }

   public final boolean equals(Object var1) {
      if (!super.equals(var1)) {
         return false;
      } else {
         js var2 = (js)var1;
         return this.a == var2.a;
      }
   }

   public final int hashCode() {
      return super.hashCode() ^ this.a;
   }

   public final String toString() {
      return "u:" + this.a;
   }
}
