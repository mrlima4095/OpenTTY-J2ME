public final class os extends nu {
   public final ov a;
   public final mu a;
   public final mu[] a;
   public final ov b;

   public os(aq var1, ov var2, mu var3, mu[] var4, ov var5) {
      super(var1);
      this.a = var2;
      if (var2 != null) {
         var2.a((hu)this);
      }

      this.a = var3;
      if (var3 != null) {
         var3.a((ov)this);
      }

      this.a = var4;
      if (var4 != null) {
         for(int var6 = 0; var6 < var4.length; ++var6) {
            var4[var6].a((ov)this);
         }
      }

      (this.b = var5).a((hu)this);
   }

   public final void a(lr var1) {
      var1.a(this);
   }
}
