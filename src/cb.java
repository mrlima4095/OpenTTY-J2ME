public final class cb extends et {
   public final String a;
   public final int a;
   public final ho a;
   public po a = null;

   public cb(aq var1, String var2, int var3, ho var4) {
      super(var1);
      this.a = var2;
      this.a = var3;
      this.a = var4;
   }

   public final String toString() {
      StringBuffer var1;
      (var1 = new StringBuffer()).append(this.a);

      for(int var2 = 0; var2 < this.a; ++var2) {
         var1.append("[]");
      }

      if (this.a != null) {
         var1.append(" = ").append(this.a);
      }

      return var1.toString();
   }
}
