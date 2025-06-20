final class iv extends ha {
   private final nt a;

   public iv(nt var1, String var2, ha var3) {
      super(var3);
      this.a = var1;
      super.a();
   }

   protected final qd c(String var1) {
      if ((var1 = nq.c(var1)).startsWith("java.")) {
         return null;
      } else {
         int var3;
         String var2 = (var3 = var1.indexOf(36)) == -1 ? var1 : var1.substring(0, var3);

         for(var3 = 0; var3 < nt.a(this.a).size(); ++var3) {
            cx var4;
            qd var5;
            if ((var5 = (var4 = (cx)nt.a(this.a).elementAt(var3)).a(var2)) != null) {
               if (!var1.equals(var2) && (var5 = var4.a(var1)) == null) {
                  return null;
               }

               this.a(var5);
               return var5;
            }
         }

         return null;
      }
   }
}
