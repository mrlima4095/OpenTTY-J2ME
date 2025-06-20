public abstract class bi extends em implements oe {
   private final String b;
   public final short a;
   public final gs a;
   public final String a;
   public final ct[] a;
   public final gs[] a;
   public final il a;
   qd a = null;

   public bi(aq var1, String var2, short var3, gs var4, String var5, ct[] var6, gs[] var7, il var8) {
      super(var1, (var3 & 8) != 0);
      this.b = var2;
      this.a = var3;
      (this.a = var4).a((hu)this);
      this.a = var5;
      this.a = var6;

      int var9;
      for(var9 = 0; var9 < var6.length; ++var9) {
         var6[var9].a.a((hu)this);
      }

      this.a = var7;

      for(var9 = 0; var9 < var7.length; ++var9) {
         var7[var9].a((hu)this);
      }

      this.a = var8;
      if (var8 != null) {
         var8.a((hu)this);
      }

   }

   public final hu a() {
      return this.a();
   }

   public final boolean a() {
      return this.b != null && this.b.indexOf("@deprecated") != -1;
   }
}
