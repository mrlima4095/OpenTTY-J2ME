import java.util.Enumeration;
import java.util.Vector;

public final class il extends ic {
   public final Vector a = new Vector();

   public il(aq var1) {
      super(var1);
   }

   public final void a(ov var1) {
      this.a.addElement(var1);
      var1.a((hu)this);
   }

   public final void a(Vector var1) {
      Enumeration var2 = var1.elements();

      while(var2.hasMoreElements()) {
         this.a.addElement(var2.nextElement());
      }

      var2 = var1.elements();

      while(var2.hasMoreElements()) {
         ((ov)var2.nextElement()).a((hu)this);
      }

   }

   public final void b(Vector var1) {
      Enumeration var2 = var1.elements();

      while(var2.hasMoreElements()) {
         this.a.addElement(var2.nextElement());
      }

   }

   public final void a(lr var1) {
      var1.a(this);
   }
}
