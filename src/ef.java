import java.io.DataOutputStream;

final class ef extends kx {
   private final short a;

   public ef(short var1) {
      this.a = var1;
   }

   public final Object a(nb var1) {
      return var1.b(this.a);
   }

   public final boolean a() {
      return false;
   }

   public final void a(DataOutputStream var1) {
      var1.writeByte(8);
      var1.writeShort(this.a);
   }

   public final boolean equals(Object var1) {
      return var1 instanceof ef && ((ef)var1).a == this.a;
   }

   public final int hashCode() {
      return this.a;
   }
}
