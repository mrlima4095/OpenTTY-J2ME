import java.io.IOException;
import java.io.OutputStream;

public final class ly implements ep {
   private final String a;
   private final boolean a;
   private String b;
   private OutputStream a;
   private lx a;
   private pf a;

   public ly(String var1, boolean var2) {
      this.a = var1;
      this.a = var2;

      try {
         this.a = pf.a();
      } catch (IOException var3) {
      }
   }

   public final OutputStream a(od var1) {
      return this.a(var1, (lx)null);
   }

   public final OutputStream a(od var1, lx var2) {
      if (!this.a && this.b != null && !this.b.equals(this.a(var1))) {
         this.b();
      }

      if (this.a == null) {
         String var3 = this.a(var1);
         String var6 = var1.a();
         String var4 = null;

         for(int var5 = var6.indexOf(47); var5 != -1; var5 = var6.indexOf("/", var5 + 1)) {
            var4 = var6.substring(0, var5 + 1);
            this.a.a(this.a + var4, this.a.d);
            if (!this.a.a()) {
               this.a.d();
            }

            this.a.a();
            System.gc();
         }

         this.a.a(var3, this.a.d);
         if (this.a.a()) {
            this.a.b();
         }

         this.a.c();
         String var7 = var3.substring(var3.lastIndexOf(47) + 1, var3.length());
         this.a.a(var7 + "x");
         this.a.a(var7);
         this.a = this.a.a();
         this.a = var2;
         this.b = var3;
      }

      return this.a;
   }

   public final void a() {
      this.b();
   }

   private String a(od var1) {
      try {
         return this.a ? this.a : this.a + var1.a();
      } catch (Exception var2) {
         var2.printStackTrace();
         return null;
      }
   }

   private void b() {
      if (this.a != null) {
         if (this.a != null) {
            this.a = null;
         }

         this.a.close();
         this.a = null;
         this.b = null;
         this.a.a();
      }

   }
}
