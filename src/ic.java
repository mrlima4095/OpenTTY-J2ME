public abstract class ic extends et implements ov {
   private hu a = null;

   protected ic(aq var1) {
      super(var1);
   }

   public final void a(hu var1) {
      if (this.a != null && var1 != this.a) {
         throw new RuntimeException("Enclosing scope is already set for statement \"" + this.toString() + "\" at " + this.a());
      } else {
         this.a = var1;
      }
   }

   public final hu a() {
      return this.a;
   }
}
