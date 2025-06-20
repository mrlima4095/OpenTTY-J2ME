import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public abstract class gv {
   private final short a;

   public gv(short var1) {
      this.a = var1;
   }

   public final void b(DataOutputStream var1) {
      ByteArrayOutputStream var2 = new ByteArrayOutputStream();
      this.a(new DataOutputStream(var2));
      var1.writeShort(this.a);
      var1.writeInt(var2.size());
      byte[] var3 = var2.toByteArray();
      var1.write(var3, 0, var3.length);
   }

   protected abstract void a(DataOutputStream var1);

   static short a(gv var0) {
      return var0.a;
   }
}
