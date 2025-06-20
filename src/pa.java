import java.io.OutputStream;

public final class pa implements ep {
   private final bz a;
   private ep a;
   private ep b;

   public pa(bz var1, ep var2, ep var3) {
      this.a = var1;
      this.a = null;
      this.b = var3;
   }

   public final OutputStream a(od var1) {
      return this.a(var1, (lx)null);
   }

   public final OutputStream a(od var1, lx var2) {
      ep var3;
      return (var3 = this.a.a(var1) ? this.a : this.b) != null ? var3.a(var1, var2) : null;
   }

   public final void a() {
      if (this.a != null) {
         this.a.a();
         this.a = null;
      }

      if (this.b != null) {
         this.b.a();
         this.b = null;
      }

   }
}
