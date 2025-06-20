import java.io.DataOutputStream;

public final class mv extends ib {
   private final short a;

   public mv(short var1) {
      this.a = var1;
   }

   public final boolean a() {
      return false;
   }

   public final void a(DataOutputStream var1) {
      var1.writeByte(7);
      var1.writeShort(this.a);
   }

   public final boolean equals(Object var1) {
      return var1 instanceof mv && ((mv)var1).a == this.a;
   }

   public final int hashCode() {
      return this.a;
   }

   static short a(mv var0) {
      return var0.a;
   }
}
