public final class jv extends ic {
   public final short a;
   public final gs a;
   public final cb[] a;

   public jv(aq var1, short var2, gs var3, cb[] var4) {
      super(var1);
      this.a = var2;
      (this.a = var3).a((hu)this);
      this.a = var4;

      for(int var5 = 0; var5 < var4.length; ++var5) {
         cb var6;
         if ((var6 = var4[var5]).a != null) {
            ls.a((ho)var6.a, (ov)this);
         }
      }

   }

   public final void a(lr var1) {
      var1.a(this);
   }

   public final String toString() {
      StringBuffer var1 = new StringBuffer();
      if (this.a != 0) {
         var1.append(fd.a(this.a)).append(' ');
      }

      var1.append(this.a).append(' ').append(this.a[0].toString());

      for(int var2 = 1; var2 < this.a.length; ++var2) {
         var1.append(", ").append(this.a[var2].toString());
      }

      return var1.append(';').toString();
   }
}
