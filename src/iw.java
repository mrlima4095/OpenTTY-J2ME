import java.io.DataInputStream;
import java.io.DataOutputStream;

public final class iw extends gv {
   private final pz[] a;

   private iw(short var1, pz[] var2) {
      super(var1);
      this.a = var2;
   }

   protected final void a(DataOutputStream var1) {
      var1.writeShort(this.a.length);

      for(int var2 = 0; var2 < this.a.length; ++var2) {
         pz var3 = this.a[var2];
         var1.writeShort(var3.a);
         var1.writeShort(var3.b);
         var1.writeShort(var3.c);
         var1.writeShort(var3.d);
         var1.writeShort(var3.e);
      }

   }

   static gv a(short var0, DataInputStream var1) {
      var1 = var1;
      short var2;
      pz[] var3 = new pz[var2 = var1.readShort()];

      for(short var4 = 0; var4 < var2; ++var4) {
         var3[var4] = new pz(var1.readShort(), var1.readShort(), var1.readShort(), var1.readShort(), var1.readShort());
      }

      return new iw(var0, var3);
   }
}
