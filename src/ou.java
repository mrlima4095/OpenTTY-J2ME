import java.io.DataOutputStream;

public final class ou extends ib {
   private final short a;
   private final short b;

   public ou(short var1, short var2) {
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
      var1.writeByte(9);
      var1.writeShort(this.a);
      var1.writeShort(this.b);
   }

   public final boolean equals(Object var1) {
      return var1 instanceof ou && ((ou)var1).a == this.a && ((ou)var1).b == this.b;
   }

   public final int hashCode() {
      return this.a + (this.b << 16);
   }
}
