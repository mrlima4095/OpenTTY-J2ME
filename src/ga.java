import java.util.Enumeration;
import java.util.Vector;

public final class ga extends ic {
   public final ov a;
   public final Vector a;
   public final il a;
   gx a = null;

   public ga(aq var1, ov var2, Vector var3, il var4) {
      super(var1);
      (this.a = var2).a((hu)this);
      this.a = var3;
      Enumeration var5 = var3.elements();

      while(var5.hasMoreElements()) {
         ((iq)var5.nextElement()).a(this);
      }

      this.a = var4;
      if (var4 != null) {
         var4.a((hu)this);
      }

   }

   public final void a(lr var1) {
      var1.a(this);
   }
}
