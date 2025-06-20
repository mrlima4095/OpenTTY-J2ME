import java.io.DataInputStream;
import java.io.DataOutputStream;

public final class jw extends gv {
   private final short a;

   public jw(short var1, short var2) {
      super(var1);
      this.a = var2;
   }

   protected final void a(DataOutputStream var1) {
      var1.writeShort(this.a);
   }

   static gv a(short var0, DataInputStream var1) {
      return new jw(var0, var1.readShort());
   }
}
