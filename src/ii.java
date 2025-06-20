import java.io.DataOutputStream;

final class ii extends kx {
   private final int a;

   public ii(int var1) {
      this.a = var1;
   }

   public final Object a(nb var1) {
      return new Integer(this.a);
   }

   public final boolean a() {
      return false;
   }

   public final void a(DataOutputStream var1) {
      var1.writeByte(3);
      var1.writeInt(this.a);
   }

   public final boolean equals(Object var1) {
      return var1 instanceof ii && ((ii)var1).a == this.a;
   }

   public final int hashCode() {
      return this.a;
   }
}
