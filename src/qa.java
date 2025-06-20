import java.util.Enumeration;
import java.util.Vector;

public final class qa extends ml {
   public final mu a;
   public final String a;
   public final mu b;

   public qa(aq var1, mu var2, String var3, mu var4) {
      super(var1);
      this.a = var2;
      this.a = var3;
      this.b = var4;
   }

   public final String toString() {
      return this.a.toString() + ' ' + this.a + ' ' + this.b.toString();
   }

   public final Enumeration a() {
      Vector var1 = new Vector();
      qa var2 = this;

      while(true) {
         var1.addElement(var2.b);
         mu var4;
         if (!((var4 = var2.a) instanceof qa) || ((qa)var4).a != this.a) {
            var1.addElement(var4);
            var1.trimToSize();
            Vector var5 = new Vector(var1.size());
            int var3 = var1.size();

            while(var3 > 0) {
               --var3;
               var5.addElement(var1.elementAt(var3));
            }

            System.gc();
            return var5.elements();
         }

         var2 = (qa)var4;
      }
   }

   public final void a(gm var1) {
      var1.a(this);
   }

   public final void a(bn var1) {
      var1.a(this);
   }
}
