import java.io.OutputStream;

public final class ej implements ep {
   private ep a;
   private ep b;

   public ej(ep var1, ep var2) {
      this.a = var1;
      this.b = var2;
   }

   public final OutputStream a(od var1) {
      return this.a(var1, (lx)null);
   }

   public final OutputStream a(od var1, lx var2) {
      OutputStream var3;
      return (var3 = this.a.a(var1, var2)) != null ? var3 : this.b.a(var1, var2);
   }

   public final void a() {
      this.a.a();
      this.b.a();
      this.a = null;
      this.b = null;
   }
}
