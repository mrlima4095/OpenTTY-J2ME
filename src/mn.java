import java.util.Enumeration;
import java.util.Hashtable;

public final class mn {
   private final Hashtable a = new Hashtable();

   public final void a(aj var1) {
      this.a.put(var1.a(), var1);
   }

   public final aj a(String var1) {
      return (aj)this.a.get(ec.b(var1));
   }

   public final void a(dg var1) {
      Enumeration var4 = this.a.elements();

      while(var4.hasMoreElements()) {
         aj var2 = (aj)var4.nextElement();
         var1.a_(var2);
      }

   }
}
