public abstract class gw extends cn {
   public gw(qd var1) {
      super(var1);
   }

   public abstract boolean a();

   public abstract boolean b();

   public abstract qd a();

   public abstract String a_();

   public final String a() {
      return (new gf(qd.a(this.a()), this.a().b())).toString();
   }

   public String toString() {
      StringBuffer var1 = new StringBuffer();

      try {
         var1.append(this.a().toString());
      } catch (ng var4) {
         var1.append("<invalid type>");
      }

      var1.append(' ');
      var1.append(this.b().toString());
      var1.append('.');
      var1.append(this.a_());
      var1.append('(');

      qd[] var2;
      int var3;
      try {
         var2 = this.a();

         for(var3 = 0; var3 < var2.length; ++var3) {
            if (var3 > 0) {
               var1.append(", ");
            }

            var1.append(var2[var3].toString());
         }
      } catch (ng var6) {
         var1.append("<invalid type>");
      }

      var1.append(')');

      try {
         if ((var2 = this.b()).length > 0) {
            var1.append(" throws ").append(var2[0]);

            for(var3 = 1; var3 < var2.length; ++var3) {
               var1.append(", ").append(var2[var3]);
            }
         }
      } catch (ng var5) {
         var1.append("<invalid thrown exception type>");
      }

      return var1.toString();
   }
}
