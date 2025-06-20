import java.io.DataInputStream;
import java.io.DataOutputStream;

public final class dj extends gv {
   private final short a;

   dj(short var1, short var2) {
      super(var1);
      this.a = var2;
   }

   public final short a() {
      return this.a;
   }

   protected final void a(DataOutputStream var1) {
      var1.writeShort(this.a);
   }

   static gv a(short var0, DataInputStream var1) {
      return new dj(var0, var1.readShort());
   }
}
