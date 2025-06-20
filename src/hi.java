import java.io.IOException;
import java.io.InputStream;

public final class hi extends ha {
   private final String a;

   public hi(String var1, ha var2) {
      super(var2);
      this.a = var1;
      this.a();
   }

   protected final qd c(String var1) {
      var1 = nb.a(nq.c(var1));
      pf var3 = null;

      InputStream var2;
      try {
         if (!this.a.equals("")) {
            (var3 = nt.a).a(this.a + "/" + var1, var3.b);
            if (!var3.a()) {
               var3.a();
               return null;
            }

            var2 = var3.a();
         } else if ((var2 = this.getClass().getResourceAsStream("/api/" + var1.substring(0, var1.length() - 2) + "zz")) == null) {
            return null;
         }
      } catch (IOException var13) {
         throw new ClassNotFoundException("Opening resource \"" + this.a + "/" + var1 + "\"" + "\n[" + var13.toString() + "]");
      }

      nb var4;
      try {
         var4 = new nb(var2);
      } catch (IOException var11) {
         throw new ClassNotFoundException("Reading resource \"" + this.a + "/" + var1 + "\"" + "\n[" + var11.toString() + "]");
      } finally {
         try {
            var2.close();
            if (var3 != null) {
               var3.a();
            }
         } catch (IOException var10) {
         }

      }

      ai var14 = new ai(var4, this);
      this.a(var14);
      return var14;
   }
}
