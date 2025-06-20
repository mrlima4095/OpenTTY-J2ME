import java.io.DataOutputStream;

final class iz extends kx {
   private final double a;

   public iz(double var1) {
      this.a = var1;
   }

   public final Object a(nb var1) {
      return new Double(this.a);
   }

   public final boolean a() {
      return true;
   }

   public final void a(DataOutputStream var1) {
      var1.writeByte(6);
      var1.writeDouble(this.a);
   }

   public final boolean equals(Object var1) {
      return var1 instanceof iz && ((iz)var1).a == this.a;
   }

   public final int hashCode() {
      long var1;
      return (int)(var1 = Double.doubleToLongBits(this.a)) ^ (int)(var1 >> 32);
   }
}
