public abstract class pm extends cn {
   private final qd a;

   public pm(qd var1) {
      super(var1);
      this.a = var1;
   }

   public abstract qd[] a();

   public String a() {
      qd[] var1 = this.a();
      qd var3;
      if ((var3 = this.a.f()) != null) {
         qd[] var2;
         (var2 = new qd[var1.length + 1])[0] = var3;
         System.arraycopy(var1, 0, var2, 1, var1.length);
         var1 = var2;
      }

      return (new gf(qd.a(var1), "V")).toString();
   }

   public String toString() {
      StringBuffer var1;
      (var1 = new StringBuffer(this.b().toString())).append('(');

      try {
         qd[] var4 = this.a();

         for(int var2 = 0; var2 < var4.length; ++var2) {
            if (var2 > 0) {
               var1.append(", ");
            }

            var1.append(var4[var2].toString());
         }
      } catch (ng var3) {
         var1.append("<invalid type>");
      }

      var1.append(')');
      return var1.toString();
   }
}
