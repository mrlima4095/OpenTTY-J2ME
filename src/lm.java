import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Gauge;

final class lm implements Runnable {
   private final String[] a;
   private final String a;
   private final String b;
   private final Gauge a;
   private final qe a;
   private final qe b;

   lm(qe var1, String[] var2, String var3, String var4, Gauge var5, qe var6) {
      this.b = var1;
      this.a = var2;
      this.a = var3;
      this.b = var4;
      this.a = var5;
      this.a = var6;
   }

   public final void run() {
      for(int var1 = 0; var1 < this.a.length; ++var1) {
         pf var2 = null;
         InputStream var3 = null;
         OutputStream var4 = null;

         try {
            this.b.a.a(this.a + this.a[var1], this.b.a.b);
            if (!this.b.a.a()) {
               this.b.a.a();
               continue;
            }

            int var5 = (int)this.b.a.a();
            var3 = this.b.a.a();
            (var2 = pf.a()).a(this.b + this.a[var1], var2.d);
            if (var2.a()) {
               var2.b();
            }

            var2.c();
            var4 = var2.a();
            byte[] var6 = new byte[1024];
            this.a.setMaxValue(var5);
            int var8 = 0;

            int var7;
            while((var7 = var3.read(var6)) != -1) {
               var4.write(var6, 0, var7);
               var4.flush();
               var8 += var7;
               this.a.setValue(var8);
               this.a.setLabel(this.a[var1] + ": " + var8 * 100 / var5 + "%");
            }
         } catch (IOException var10) {
         }

         try {
            if (var4 != null) {
               var4.close();
            }

            if (var2 != null) {
               var2.a();
            }

            if (var3 != null) {
               var3.close();
            }

            if (this.b.a != null) {
               this.b.a.a();
            }
         } catch (Exception var9) {
         }
      }

      qe.a(this.b, qe.a(this.b).a("Projects", this.a));
      Display.getDisplay(qe.a(this.b)).setCurrent(qe.a(this.b));
      System.gc();
   }
}
