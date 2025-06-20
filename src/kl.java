public final class kl extends bi {
   gw a = null;

   public kl(aq var1, String var2, short var3, gs var4, String var5, ct[] var6, gs[] var7, il var8) {
      super(var1, var2, var3, var4, var5, var6, var7, var8);
   }

   public final String toString() {
      StringBuffer var1;
      (var1 = new StringBuffer(this.a)).append('(');
      ct[] var3 = this.a;

      for(int var2 = 0; var2 < var3.length; ++var2) {
         if (var2 > 0) {
            var1.append(", ");
         }

         var1.append(var3[var2].toString());
      }

      var1.append(')');
      return var1.toString();
   }

   public final void a(mq var1) {
      var1.a(this);
   }
}
