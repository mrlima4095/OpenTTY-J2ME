import java.util.Hashtable;
import java.util.Vector;

public abstract class kg extends ft {
   public final Vector a = new Vector();
   public final Vector d = new Vector();
   final Hashtable a = new Hashtable();

   public kg(aq var1, short var2) {
      super(var1, var2);
   }

   public final void a(jf var1) {
      this.a.addElement(var1);
      var1.a(this);
   }

   public final void a(ht var1) {
      this.d.addElement(var1);
      var1.a((dr)this);
      if (this.a != null) {
         this.a.a = null;
      }

   }

   public final void a(cm var1) {
      if (!(this instanceof kw)) {
         throw new RuntimeException();
      } else {
         cm var2;
         if ((var2 = (cm)this.a.get(var1.a())) != null) {
            if (var1.a() != var2.a()) {
               throw new RuntimeException();
            }
         } else {
            this.a.put(var1.a(), var1);
         }
      }
   }

   final jf[] a() {
      if (this.a.isEmpty()) {
         jf var2;
         (var2 = new jf(this.a(), (String)null, (short)1, new ct[0], new gs[0], (ew)null, new il(this.a()))).a(this);
         return new jf[]{var2};
      } else {
         jf[] var1 = new jf[this.a.size()];
         this.a.copyInto(var1);
         return var1;
      }
   }
}
