public final class ow extends fe implements hp {
   public ow(aq var1, String var2, short var3, String var4, gs var5, gs[] var6) {
      super(var1, var2, var3, var4, var5, var6);
      if ((var3 & 14) != 0) {
         this.a((String)"Modifiers \"protected\", \"private\" and \"static\" not allowed in package member class declaration");
      }

   }

   public final void a(fv var1) {
      this.a((hu)var1);
   }

   public final fv a() {
      return (fv)this.a();
   }

   public final String b() {
      String var1 = super.a;
      fv var2;
      if ((var2 = (fv)this.a()).a != null) {
         var1 = var2.a.a + '.' + var1;
      }

      return var1;
   }

   public final void a(oy var1) {
      var1.a(this);
   }
}
