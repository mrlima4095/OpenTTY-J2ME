final class pi extends pm {
   private final qd[] a;
   private final qd[] b;
   private final pe a;
   private final ai a;

   pi(ai var1, qd[] var2, qd[] var3, pe var4) {
      super(var1);
      this.a = var1;
      this.a = var2;
      this.b = var3;
      this.a = var4;
   }

   public final qd[] a() {
      qd var1;
      if ((var1 = this.a.f()) != null) {
         if (this.a.length < 1) {
            throw new RuntimeException("Inner class constructor lacks magic first parameter");
         } else if (this.a[0] != var1) {
            throw new RuntimeException("Magic first parameter of inner class constructor has type \"" + this.a[0].toString() + "\" instead of that of its enclosing instance (\"" + var1.toString() + "\")");
         } else {
            qd[] var2 = new qd[this.a.length - 1];
            System.arraycopy(this.a, 1, var2, 0, var2.length);
            return var2;
         }
      } else {
         return this.a;
      }
   }

   public final qd[] b() {
      return this.b;
   }

   public final pe a() {
      return this.a;
   }
}
