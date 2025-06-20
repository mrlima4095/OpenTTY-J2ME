import java.io.DataInputStream;
import java.io.DataOutputStream;

final class lu extends gv {
   private final short a;
   private final short b;
   private final byte[] a;
   private final br[] a;
   private final gv[] a;

   private lu(short var1, short var2, short var3, byte[] var4, br[] var5, gv[] var6) {
      super(var1);
      this.a = var2;
      this.b = var3;
      this.a = var4;
      this.a = var5;
      this.a = var6;
   }

   public static gv a(short var0, nb var1, DataInputStream var2) {
      short var3 = var2.readShort();
      short var4 = var2.readShort();
      byte[] var5 = nb.a(var2);
      br[] var6 = new br[var2.readShort()];

      for(int var7 = 0; var7 < var6.length; ++var7) {
         var6[var7] = new br(var2.readShort(), var2.readShort(), var2.readShort(), var2.readShort());
      }

      gv[] var9 = new gv[var2.readShort()];

      for(int var8 = 0; var8 < var9.length; ++var8) {
         var9[var8] = nb.a(var1, var2);
      }

      return new lu(var0, var3, var4, var5, var6, var9);
   }

   protected final void a(DataOutputStream var1) {
      var1.writeShort(this.a);
      var1.writeShort(this.b);
      var1.writeInt(this.a.length);
      var1.write(this.a);
      var1.writeShort(this.a.length);

      int var2;
      for(var2 = 0; var2 < this.a.length; ++var2) {
         br var3 = this.a[var2];
         var1.writeShort(br.a(var3));
         var1.writeShort(br.b(var3));
         var1.writeShort(br.c(var3));
         var1.writeShort(br.d(var3));
      }

      var1.writeShort(this.a.length);

      for(var2 = 0; var2 < this.a.length; ++var2) {
         this.a[var2].b(var1);
      }

   }
}
