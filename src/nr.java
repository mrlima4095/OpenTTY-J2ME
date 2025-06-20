public abstract class nr extends et {
   public nr(aq var1) {
      super(var1);
   }

   public gs a() {
      return null;
   }

   public mu a() {
      return null;
   }

   public jt a() {
      return null;
   }

   public abstract String toString();

   public final gs b() {
      gs var1;
      if ((var1 = this.a()) == null) {
         this.a("Expression \"" + this.toString() + "\" is not a type");
      }

      return var1;
   }

   public final mu b() {
      mu var1;
      if ((var1 = this.a()) == null) {
         this.a("Expression \"" + this.toString() + "\" is not an rvalue");
      }

      return var1;
   }

   public final jt b() {
      jt var1;
      if ((var1 = this.a()) == null) {
         this.a("Expression \"" + this.toString() + "\" is not an lvalue");
      }

      return var1;
   }

   public abstract void a(gm var1);
}
