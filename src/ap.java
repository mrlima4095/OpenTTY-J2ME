public final class ap extends mb {
   public ji a;

   public ap() {
   }

   public ap(int var1) {
      this.c = var1 - 64;
   }

   public ap(ji var1) {
      this.a = var1;
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ir var5) {
      this.a.a(var1, var2, var3, var4, var5);
   }

   public final int a() {
      return this.c < 64 ? 64 + this.c : 247;
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ee var5) {
      var5.a(var1, var2, var3, var4, this);
   }

   public final boolean equals(Object var1) {
      if (!super.equals(var1)) {
         return false;
      } else {
         ap var2 = (ap)var1;
         return this.c == var2.c && this.a.equals(var2.a);
      }
   }

   public final int hashCode() {
      return super.hashCode() ^ this.a.hashCode();
   }

   public final String toString() {
      return super.toString() + "Var: ..., Stack: [" + this.a.toString() + "]";
   }
}
