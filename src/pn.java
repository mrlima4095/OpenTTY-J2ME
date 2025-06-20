import java.io.DataInputStream;
import java.io.DataOutputStream;

public final class pn extends gv {
   private final ka[] a;

   public pn(short var1, ka[] var2) {
      super(var1);
      this.a = var2;
   }

   protected final void a(DataOutputStream var1) {
      var1.writeShort(this.a.length);

      for(int var2 = 0; var2 < this.a.length; ++var2) {
         var1.writeShort(this.a[var2].a);
         var1.writeShort(this.a[var2].b);
      }

   }

   static gv a(short var0, DataInputStream var1) {
      var1 = var1;
      ka[] var2 = new ka[var1.readShort()];

      for(short var3 = 0; var3 < var2.length; ++var3) {
         var2[var3] = new ka(var1.readShort(), var1.readShort());
      }

      return new pn(var0, var2);
   }
}
