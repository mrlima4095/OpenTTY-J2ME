import java.util.Enumeration;
import java.util.Vector;

public final class bq extends gg {
   public final mu a;
   public final Vector a;

   public bq(aq var1, mu var2, Vector var3) {
      super(var1);
      (this.a = var2).a((ov)this);
      this.a = var3;
      Enumeration var4 = var3.elements();

      while(var4.hasMoreElements()) {
         jk var5;
         Enumeration var6 = (var5 = (jk)var4.nextElement()).a.elements();

         while(var6.hasMoreElements()) {
            ((mu)((mu)var6.nextElement())).a((ov)this);
         }

         var6 = var5.b.elements();

         while(var6.hasMoreElements()) {
            ((ov)((ov)var6.nextElement())).a((hu)this);
         }
      }

   }

   public final void a(lr var1) {
      var1.a(this);
   }
}
