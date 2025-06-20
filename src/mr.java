public final class mr extends mu {
   public final mu a;
   public final eo a;
   public final mu[] a;

   public mr(aq var1, mu var2, eo var3, mu[] var4) {
      super(var1);
      this.a = var2;
      this.a = var3;
      this.a = var4;
   }

   public final String toString() {
      StringBuffer var1 = new StringBuffer();
      if (this.a != null) {
         var1.append(this.a.toString()).append('.');
      }

      var1.append("new ").append(this.a.a.toString()).append("() { ... }");
      return var1.toString();
   }

   public final void a(gm var1) {
      var1.a(this);
   }

   public final void a(bn var1) {
      var1.a(this);
   }
}
