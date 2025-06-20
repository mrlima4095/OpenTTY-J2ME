import java.io.IOException;
import java.util.Enumeration;

public final class ky {
   private final String a;
   public int a;

   public ky(String var1) {
      this.a = var1;
      this.a = 0;
   }

   public final void a(gj var1) {
      nz.a.a(this.a, nz.a.b);
      if (!nz.a.a()) {
         nz.a.a();
         throw new IOException("No such file or directory");
      } else {
         this.a(this.a, var1);
      }
   }

   private void a(String var1, gj var2) {
      if (!var1.endsWith("/")) {
         var2.a(new od(this.a, var1));
         ++this.a;
      } else {
         nz.a.a(var1, nz.a.b);
         Enumeration var3 = nz.a.a("*", true);
         nz.a.a();

         while(var3.hasMoreElements()) {
            this.a(var1 + (String)var3.nextElement(), var2);
         }

      }
   }
}
