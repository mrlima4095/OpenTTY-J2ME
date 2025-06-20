final class z extends gw {
   private final kl a;
   private final cx a;

   z(cx var1, qd var2, kl var3) {
      var2.getClass();
      super(var2);
      this.a = var1;
      this.a = var3;
   }

   public final pe a() {
      switch(this.a.a & 7) {
      case 0:
         return pe.c;
      case 1:
         return pe.d;
      case 2:
         return pe.a;
      case 3:
      default:
         throw new RuntimeException("Invalid access");
      case 4:
         return pe.b;
      }
   }

   public final qd[] a() {
      ct[] var1;
      qd[] var2 = new qd[(var1 = this.a.a).length];

      for(int var3 = 0; var3 < var1.length; ++var3) {
         var2[var3] = cx.a((cx)this.a, (nr)var1[var3].a);
      }

      return var2;
   }

   public final qd[] b() {
      qd[] var1 = new qd[this.a.a.length];

      for(int var2 = 0; var2 < var1.length; ++var2) {
         var1[var2] = cx.a((cx)this.a, (nr)this.a.a[var2]);
      }

      return var1;
   }

   public final boolean a() {
      return (this.a.a & 8) != 0;
   }

   public final boolean b() {
      return this.a.a() instanceof df || (this.a.a & 1024) != 0;
   }

   public final qd a() {
      return cx.a((cx)this.a, (bi)this.a);
   }

   public final String a_() {
      return this.a.a;
   }
}
