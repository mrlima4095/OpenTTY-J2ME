import java.util.Enumeration;
import java.util.Vector;

public abstract class ft implements dr {
   private final aq a;
   public final short a;
   public final Vector b = new Vector();
   public final Vector c = new Vector();
   private hu a = null;
   qd a = null;
   private int a = 0;

   public ft(aq var1, short var2) {
      this.a = var1;
      this.a = var2;
   }

   public final void a(hu var1) {
      if (this.a != null && var1 != this.a) {
         throw new RuntimeException("Enclosing scope is already set for type declaration \"" + this.toString() + "\" at " + this.a);
      } else {
         this.a = var1;
      }
   }

   public final hu a() {
      return this.a;
   }

   public final void a(kl var1) {
      this.b.addElement(var1);
      var1.a(this);
   }

   public final void a(ah var1) {
      this.c.addElement(var1);
      var1.a(this);
   }

   public final ah a(String var1) {
      Enumeration var3 = this.c.elements();

      ah var2;
      do {
         if (!var3.hasMoreElements()) {
            return null;
         }
      } while(!(var2 = (ah)var3.nextElement()).a().equals(var1));

      return var2;
   }

   public final String c() {
      return (this instanceof ow ? ((ow)this).b() : (this instanceof eo ? ((eo)this).b() : (this instanceof er ? ((er)this).b() : (this instanceof jl ? ((jl)this).b() : (this instanceof in ? ((in)this).b() : (this instanceof hq ? ((hq)this).b() : this.b())))))) + '$' + ++this.a;
   }

   public final aq a() {
      return this.a;
   }

   public final void a(String var1) {
      throw new pc(var1, this.a);
   }

   public abstract String toString();
}
