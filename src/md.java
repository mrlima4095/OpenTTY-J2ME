import java.io.DataOutputStream;

public final class md extends ib {
   private final short a;
   private final short b;

   public md(short var1, short var2) {
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
      var1.writeByte(11);
      var1.writeShort(this.a);
      var1.writeShort(this.b);
   }

   public final boolean equals(Object var1) {
      return var1 instanceof md && ((md)var1).a == this.a && ((md)var1).b == this.b;
   }

   public final int hashCode() {
      return this.a + (this.b << 16);
   }
}
