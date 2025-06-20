import java.io.InputStream;

public final class od {
   private final String a;
   private final String b;
   private InputStream a;

   public od(String var1, String var2) {
      this.a = var1;
      this.b = var2;
   }

   public final String a() {
      return this.b.equals(this.a) ? this.b.substring(this.b.lastIndexOf(47) + 1, this.b.length()) : this.b.substring(this.a.length());
   }

   public final InputStream a() {
      if (this.a == null) {
         nz.a.a(this.b, nz.a.b);
         this.a = nz.a.a();
      }

      return this.a;
   }

   public final void a() {
      this.a.close();
      this.a = null;
      nz.a.a();
   }

   public final String toString() {
      return this.a();
   }
}
