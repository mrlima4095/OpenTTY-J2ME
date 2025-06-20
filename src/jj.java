public final class jj {
   private static ps a = new ps();
   static final dt a = new dt();
   private static eb a = new eb();
   private static oh a = new oh();
   static final nc a = new nc((String)null, (aj)null, true);
   static final nc b = new nc("java/lang/Object", (aj)null, true);
   static final nc c = new nc("java/lang/Object", (aj)null, false);

   public final it a(String var1, aj var2, boolean var3) {
      switch(var1.charAt(0)) {
      case 'B':
      case 'C':
      case 'I':
      case 'S':
      case 'Z':
         return a;
      case 'D':
         return a;
      case 'E':
      case 'G':
      case 'H':
      case 'K':
      case 'L':
      case 'M':
      case 'N':
      case 'O':
      case 'P':
      case 'Q':
      case 'R':
      case 'T':
      case 'U':
      case 'W':
      case 'X':
      case 'Y':
      default:
         return a(ec.a(var1) ? var1 : ec.b(var1), var2, var3);
      case 'F':
         return a;
      case 'J':
         return a;
      case 'V':
         return null;
      }
   }

   public static ps a() {
      return a;
   }

   public final ps b() {
      return a;
   }

   public static dt a() {
      return a;
   }

   public final dt b() {
      return a;
   }

   public static eb a() {
      return a;
   }

   public final eb b() {
      return a;
   }

   public static oh a() {
      return a;
   }

   public final oh b() {
      return a;
   }

   public static nc a() {
      return a;
   }

   public static nc a(String var0, aj var1, boolean var2) {
      if (var0 == null) {
         return a;
      } else if (!var0.equals("java/lang/Object")) {
         return new nc(var0, var1, var2);
      } else {
         return var2 ? b : c;
      }
   }

   public final nc a(String var1, aj var2, ps var3) {
      this.a(var1, var2, false);
      return a('[' + var1, var2, false);
   }
}
