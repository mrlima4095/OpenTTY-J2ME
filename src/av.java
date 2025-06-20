final class av extends cm {
   private final gb a;
   private final cb a;
   private final cx a;

   av(cx var1, qd var2, gb var3, cb var4) {
      var2.getClass();
      super(var2);
      this.a = var1;
      this.a = var3;
      this.a = var4;
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

   public final boolean a() {
      return (this.a.a & 8) != 0;
   }

   public final qd a() {
      return cx.a((cx)this.a, (nr)this.a.a).a(this.a.a, cx.a(this.a).a);
   }

   public final String a() {
      return this.a.a;
   }

   public final Object a() {
      Object var1;
      return (this.a.a & 16) != 0 && this.a.a instanceof mu && (var1 = this.a.a((mu)this.a.a)) != null ? cx.a(this.a, (t)this.a.a, var1, this.a()) : null;
   }
}
