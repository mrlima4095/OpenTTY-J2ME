import java.io.DataOutputStream;

final class dc extends kx {
   private final long a;

   public dc(long var1) {
      this.a = var1;
   }

   public final Object a(nb var1) {
      return new Long(this.a);
   }

   public final boolean a() {
      return true;
   }

   public final void a(DataOutputStream var1) {
      var1.writeByte(5);
      var1.writeLong(this.a);
   }

   public final boolean equals(Object var1) {
      return var1 instanceof dc && ((dc)var1).a == this.a;
   }

   public final int hashCode() {
      return (int)this.a ^ (int)(this.a >> 32);
   }
}
