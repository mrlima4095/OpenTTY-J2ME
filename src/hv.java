public class hv extends no {
   private final aq a;

   public hv(String var1, aq var2) {
      super(var1);
      this.a = var2;
   }

   public hv(String var1, aq var2, Throwable var3) {
      super(var1, var3);
      this.a = var2;
   }

   public String getMessage() {
      return this.a == null ? super.getMessage() : this.a.toString() + ": " + super.getMessage();
   }
}
