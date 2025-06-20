public final class om extends gs {
   public int a;

   public om(aq var1, int var2) {
      super(var1);
      this.a = var2;
   }

   public final String toString() {
      switch(this.a) {
      case 0:
         return "void";
      case 1:
         return "byte";
      case 2:
         return "short";
      case 3:
         return "char";
      case 4:
         return "int";
      case 5:
         return "long";
      case 6:
         return "float";
      case 7:
         return "double";
      case 8:
         return "boolean";
      default:
         throw new RuntimeException("Invalid index " + this.a);
      }
   }

   public final void a(cd var1) {
      var1.a(this);
   }

   public final void a(gm var1) {
      var1.a(this);
   }
}
