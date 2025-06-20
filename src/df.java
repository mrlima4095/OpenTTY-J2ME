import java.util.Vector;

public abstract class df extends ft implements dm, oe {
   private final String b;
   public String a;
   public gs[] a;
   public final Vector a = new Vector();
   qd[] a = null;

   protected df(aq var1, String var2, short var3, String var4, gs[] var5) {
      super(var1, var3);
      this.b = var2;
      this.a = var4;
      this.a = var5;

      for(int var6 = 0; var6 < var5.length; ++var6) {
         var5[var6].a((hu)(new ie(this)));
      }

   }

   public String toString() {
      return this.a;
   }

   public final void a(gb var1) {
      this.a.addElement(var1);
      var1.a((dr)this);
      if (this.a != null) {
         this.a.a = null;
      }

   }

   public final String a() {
      return this.a;
   }

   public final boolean a() {
      return this.b != null && this.b.indexOf("@deprecated") != -1;
   }
}
