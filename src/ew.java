public abstract class ew extends nr implements ov {
   public final mu[] a;
   private hu a = null;

   protected ew(aq var1, mu[] var2) {
      super(var1);
      this.a = var2;

      for(int var3 = 0; var3 < var2.length; ++var3) {
         var2[var3].a((ov)this);
      }

   }

   public final void a(hu var1) {
      if (this.a != null && var1 != null) {
         throw new RuntimeException("Enclosing scope is already set for statement \"" + this.toString() + "\" at " + this.a());
      } else {
         this.a = var1;
      }
   }

   public final hu a() {
      return this.a;
   }
}
