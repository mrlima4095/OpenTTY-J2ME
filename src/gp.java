public final class gp extends jt {
   public final String[] a;
   public final int a;
   private gs a;
   nr a;

   public gp(aq var1, String[] var2) {
      this(var1, var2, var2.length);
   }

   public gp(aq var1, String[] var2, int var3) {
      super(var1);
      this.a = null;
      this.a = null;
      this.a = var2;
      this.a = var3;
   }

   public final gs a() {
      if (this.a == null) {
         String[] var1 = new String[this.a];
         System.arraycopy(this.a, 0, var1, 0, this.a);
         this.a = new jb(this.a(), var1);
         this.a.a((hu)this.a());
      }

      return this.a;
   }

   public final String toString() {
      return ls.a(this.a, ".", 0, this.a);
   }

   public final void a(gm var1) {
      var1.a(this);
   }

   public final void a(bn var1) {
      var1.a((gp)this);
   }

   public final void a(kj var1) {
      var1.a(this);
   }
}
