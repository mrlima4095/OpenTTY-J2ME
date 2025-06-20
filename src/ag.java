import java.io.DataOutputStream;

final class ag extends kx {
   private final float a;

   public ag(float var1) {
      this.a = var1;
   }

   public final Object a(nb var1) {
      return new Float(this.a);
   }

   public final boolean a() {
      return false;
   }

   public final void a(DataOutputStream var1) {
      var1.writeByte(4);
      var1.writeFloat(this.a);
   }

   public final boolean equals(Object var1) {
      return var1 instanceof ag && ((ag)var1).a == this.a;
   }

   public final int hashCode() {
      return Float.floatToIntBits(this.a);
   }
}
