public abstract class cn implements fb {
   private final qd a;

   public cn(qd var1) {
      this.a = var1;
   }

   public abstract pe a();

   public final qd b() {
      return this.a;
   }

   public abstract qd[] a();

   public abstract String a();

   public abstract qd[] b();

   public final boolean a(cn var1) {
      qd[] var3 = this.a();
      qd[] var4 = var1.a();

      for(int var2 = 0; var2 < var3.length; ++var2) {
         if (!var4[var2].a(var3[var2])) {
            return false;
         }
      }

      if (!qd.a((Object[])var3, (Object[])var4)) {
         return true;
      } else {
         return false;
      }
   }

   public abstract String toString();
}
