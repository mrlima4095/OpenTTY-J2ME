import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Enumeration;
import java.util.Vector;

public final class lg extends gv {
   private final Vector a;

   lg(short var1) {
      super(var1);
      this.a = new Vector();
   }

   private lg(short var1, ln[] var2) {
      super(var1);
      this.a = new Vector(var2.length);

      for(int var3 = 0; var3 < var2.length; ++var3) {
         this.a.addElement(var2[var3]);
      }

   }

   public final Vector a() {
      return this.a;
   }

   protected final void a(DataOutputStream var1) {
      var1.writeShort(this.a.size());
      Enumeration var3 = this.a.elements();

      while(var3.hasMoreElements()) {
         ln var2 = (ln)var3.nextElement();
         var1.writeShort(var2.a);
         var1.writeShort(var2.b);
         var1.writeShort(var2.c);
         var1.writeShort(var2.d);
      }

   }

   static gv a(short var0, DataInputStream var1) {
      var1 = var1;
      ln[] var2 = new ln[var1.readShort()];

      for(short var3 = 0; var3 < var2.length; ++var3) {
         var2[var3] = new ln(var1.readShort(), var1.readShort(), var1.readShort(), var1.readShort());
      }

      return new lg(var0, var2);
   }
}
