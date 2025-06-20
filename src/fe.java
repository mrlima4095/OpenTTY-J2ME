public abstract class fe extends kg implements dm, oe {
   private final String b;
   public final String a;
   public final gs a;
   public final gs[] a;

   public fe(aq var1, String var2, short var3, String var4, gs var5, gs[] var6) {
      super(var1, var3);
      this.b = var2;
      this.a = var4;
      this.a = var5;
      if (var5 != null) {
         var5.a((hu)(new ie(this)));
      }

      this.a = var6;

      for(int var7 = 0; var7 < var6.length; ++var7) {
         var6[var7].a((hu)(new ie(this)));
      }

   }

   public String toString() {
      return this.a;
   }

   public final String a() {
      return this.a;
   }

   public final boolean a() {
      return this.b != null && this.b.indexOf("@deprecated") != -1;
   }
}
