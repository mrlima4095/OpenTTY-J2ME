public final class gb extends ic implements ht, oe {
   private final String a;
   public final short a;
   public final gs a;
   public final cb[] a;

   public gb(aq var1, String var2, short var3, gs var4, cb[] var5) {
      super(var1);
      this.a = var2;
      this.a = var3;
      (this.a = var4).a((hu)this);
      this.a = var5;

      for(int var6 = 0; var6 < var5.length; ++var6) {
         cb var7;
         if ((var7 = var5[var6]).a != null) {
            ls.a((ho)var7.a, (ov)this);
         }
      }

   }

   public final void a(dr var1) {
      this.a((hu)var1);
   }

   public final dr a() {
      return (dr)this.a();
   }

   public final boolean b() {
      return (this.a & 8) != 0;
   }

   public final String toString() {
      StringBuffer var1;
      (var1 = new StringBuffer()).append(fd.a(this.a)).append(' ').append(this.a).append(' ').append(this.a[0]);

      for(int var2 = 1; var2 < this.a.length; ++var2) {
         var1.append(", ").append(this.a[var2]);
      }

      return var1.toString();
   }

   public final void a(mq var1) {
      var1.a(this);
   }

   public final void a(lr var1) {
      var1.a(this);
   }

   public final boolean a() {
      return this.a != null && this.a.indexOf("@deprecated") != -1;
   }
}
