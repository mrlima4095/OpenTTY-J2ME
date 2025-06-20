import java.io.DataOutputStream;

final class pu extends gv {
   private final byte[] a;

   pu(nb var1, short var2, byte[] var3) {
      super(var2);
      this.a = var3;
   }

   protected final void a(DataOutputStream var1) {
      var1.write(this.a);
   }
}
