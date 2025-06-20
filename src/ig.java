import java.io.DataOutputStream;
import java.util.Vector;

public final class ig {
   private final short a;
   private final short b;
   private final short c;
   private final Vector a;
   private final nb a;

   public ig(nb var1, short var2, short var3, short var4, Vector var5) {
      this.a = var1;
      this.a = var2;
      this.b = var3;
      this.c = var4;
      this.a = var5;
   }

   public final nb a() {
      return this.a;
   }

   public final short a() {
      return this.a;
   }

   public final short b() {
      return this.b;
   }

   public final short c() {
      return this.c;
   }

   public final gv[] a() {
      gv[] var1 = new gv[this.a.size()];
      this.a.copyInto(var1);
      return var1;
   }

   public final void a(gv var1) {
      this.a.addElement(var1);
   }

   public final void a(DataOutputStream var1) {
      var1.writeShort(this.a);
      var1.writeShort(this.b);
      var1.writeShort(this.c);
      nb.a(var1, this.a);
   }
}
