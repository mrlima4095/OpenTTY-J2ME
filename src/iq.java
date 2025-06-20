public final class iq extends et implements hu {
   public final ct a;
   public final il a;
   private ga a = null;

   public iq(aq var1, ct var2, il var3) {
      super(var1);
      (this.a = var2).a.a((hu)this);
      (this.a = var3).a((hu)this);
   }

   public final void a(ga var1) {
      if (this.a != null && var1 != this.a) {
         throw new RuntimeException("Enclosing TYR statement already set for catch clause " + this.toString() + " at " + this.a());
      } else {
         this.a = var1;
      }
   }

   public final hu a() {
      return this.a;
   }
}
