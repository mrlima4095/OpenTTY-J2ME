public abstract class em extends et implements ht {
   private dr a;
   public final boolean a;

   protected em(aq var1, boolean var2) {
      super(var1);
      this.a = var2;
   }

   public final void a(dr var1) {
      if (this.a != null && var1 != null) {
         throw new RuntimeException("Declaring type for type body declaration \"" + this.toString() + "\"at " + this.a() + " is already set");
      } else {
         this.a = var1;
      }
   }

   public final dr a() {
      return this.a;
   }

   public final boolean b() {
      return this.a;
   }

   public final void a(hu var1) {
      this.a = (dr)var1;
   }

   public hu a() {
      return this.a;
   }
}
