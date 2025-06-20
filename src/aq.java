public final class aq {
   private String a;
   private short a;
   private short b;

   public aq(String var1, short var2, short var3) {
      this.a = var1;
      this.a = var2;
      this.b = var3;
   }

   public final String a() {
      return this.a;
   }

   public final short a() {
      return this.a;
   }

   public final String toString() {
      StringBuffer var1 = new StringBuffer();
      if (this.a != null) {
         var1.append("File ").append(this.a).append(", ");
      }

      var1.append("Line ").append(this.a).append(", ");
      var1.append("Column ").append(this.b);
      return var1.toString();
   }
}
