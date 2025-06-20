import java.util.Enumeration;
import java.util.Vector;

public final class fv implements hu {
   public String a;
   public lq a = null;
   public final Vector a = new Vector();
   public final Vector b = new Vector();

   public fv(String var1) {
      this.a = var1;
   }

   public final hu a() {
      throw new RuntimeException("A compilation unit has no enclosing scope");
   }

   public final hp a(String var1) {
      Enumeration var3 = this.b.elements();

      hp var2;
      do {
         if (!var3.hasMoreElements()) {
            return null;
         }
      } while(!(var2 = (hp)var3.nextElement()).a().equals(var1));

      return var2;
   }
}
