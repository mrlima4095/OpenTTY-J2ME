import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class bb implements gj {
   private final dg a;

   public bb(dg var1) {
      this.a = var1;
   }

   public final void a(od var1) {
      try {
         InputStream var2 = var1.a();
         DataInputStream var6 = new DataInputStream(var2);
         aj var3;
         aj var10000 = var3 = new aj();
         jr var4 = new jr(var6);
         aj var7 = var10000;
         var4.a_(var7);
         if (var3.a() != null) {
            dg var8 = this.a;
            var8.a_(var3);
         }

         var1.a();
      } catch (Exception var5) {
         throw new IOException("Can't process class [" + var1.a() + "] (" + var5.getMessage() + ")");
      }
   }
}
