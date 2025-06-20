public final class gt extends h {
   public final nr a;
   public final String a;
   gw a;

   public gt(aq var1, nr var2, String var3, mu[] var4) {
      super(var1, var4);
      this.a = var2;
      this.a = var3;
   }

   public final String toString() {
      StringBuffer var1 = new StringBuffer();
      if (this.a != null) {
         var1.append(this.a.toString()).append('.');
      }

      var1.append(this.a).append('(');

      for(int var2 = 0; var2 < this.a.length; ++var2) {
         if (var2 > 0) {
            var1.append(", ");
         }

         var1.append(this.a[var2].toString());
      }

      var1.append(')');
      return var1.toString();
   }

   public final void a(gm var1) {
      var1.a(this);
   }

   public final void a(bn var1) {
      var1.a(this);
   }
}
