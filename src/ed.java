final class ed extends nf {
   private final ov a;

   ed(mu var1, ov var2) {
      this.a = var2;
   }

   public final void a(mu var1) {
      if (mu.a(var1) != null && this.a != mu.a(var1)) {
         throw new RuntimeException("Enclosing block statement for rvalue \"" + var1 + "\" at " + var1.a() + " is already set");
      } else {
         mu.a(var1, this.a);
         super.a(var1);
      }
   }

   public final void a(eo var1) {
      var1.a(this.a);
   }

   public final void a(gs var1) {
      if (gs.a(var1) != null && this.a != gs.a(var1)) {
         throw new RuntimeException("Enclosing scope already set for type \"" + this.toString() + "\" at " + var1.a());
      } else {
         gs.a(var1, this.a);
         super.a(var1);
      }
   }
}
