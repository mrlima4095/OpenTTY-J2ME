import java.io.DataInputStream;
import java.io.DataOutputStream;

public final class dx extends gv {
   private final short[] a;

   public dx(short var1, short[] var2) {
      super(var1);
      this.a = var2;
   }

   public final short[] a() {
      short[] var1 = new short[this.a.length];
      System.arraycopy(this.a, 0, var1, 0, var1.length);
      return var1;
   }

   protected final void a(DataOutputStream var1) {
      nb.a(var1, this.a);
   }

   static gv a(short var0, DataInputStream var1) {
      return new dx(var0, nb.a(var1));
   }
}
