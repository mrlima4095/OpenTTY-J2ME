public abstract class gs extends nr {
   private hu a = null;

   protected gs(aq var1) {
      super(var1);
   }

   public void a(hu var1) {
      if (this.a != null && var1 != this.a) {
         throw new RuntimeException("Enclosing scope already set for type \"" + this.toString() + "\" at " + this.a());
      } else {
         this.a = var1;
      }
   }

   public final hu a() {
      return this.a;
   }

   public final gs a() {
      return this;
   }

   public abstract void a(cd var1);

   static hu a(gs var0) {
      return var0.a;
   }

   static hu a(gs var0, hu var1) {
      return var0.a = var1;
   }
}
