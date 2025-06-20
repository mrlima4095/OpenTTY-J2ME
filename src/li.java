import java.io.DataOutputStream;

public final class li extends ib {
   private final short a;
   private final short b;

   public li(short var1, short var2) {
      this.a = var1;
      this.b = var2;
   }

   public final short a() {
      return this.b;
   }

   public final boolean a() {
      return false;
   }

   public final void a(DataOutputStream var1) {
      var1.writeByte(12);
      var1.writeShort(this.a);
      var1.writeShort(this.b);
   }

   public final boolean equals(Object var1) {
      return var1 instanceof li && ((li)var1).a == this.a && ((li)var1).b == this.b;
   }

   public final int hashCode() {
      return this.a + (this.b << 16);
   }
}
