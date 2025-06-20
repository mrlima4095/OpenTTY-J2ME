import java.io.DataOutputStream;
import java.io.OutputStream;

public final class hl implements gj {
   private final mn a;
   private final ep a;

   public hl(mn var1, ep var2) {
      this.a = var1;
      this.a = var2;
   }

   public final void a(od var1) {
      String var2 = (var2 = var1.a()).substring(0, var2.length() - ".class".length());
      aj var3;
      if ((var3 = (aj)this.a.a(var2)) != null) {
         String var4 = var3.a();
         var2.equals(var4);
         OutputStream var5;
         if ((var5 = this.a.a(var1)) != null) {
            DataOutputStream var6 = new DataOutputStream(var5);
            (new ek(var6)).a_(var3);
            var6.flush();
         }
      }

   }
}
