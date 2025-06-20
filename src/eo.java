public final class eo extends kg implements kw {
   public final gs a;
   private String a = null;

   public eo(aq var1, gs var2) {
      super(var1, (short)18);
      (this.a = var2).a((hu)(new ie(this)));
   }

   public final void a(oy var1) {
      var1.a(this);
   }

   public final String b() {
      if (this.a == null) {
         hu var1;
         for(var1 = this.a(); !(var1 instanceof dr); var1 = var1.a()) {
         }

         this.a = ((dr)var1).c();
      }

      return this.a;
   }

   public final String toString() {
      return this.b();
   }
}
