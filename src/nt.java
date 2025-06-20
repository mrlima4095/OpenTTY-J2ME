import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.lcdui.Form;
import sdk.SDK;

public final class nt extends Form implements Runnable {
   private String a;
   private ha a;
   private Vector a;
   private String b;
   public static pf a;
   private SDK a;

   public nt(SDK var1) {
      super("");
      this.a = var1;
      (new Thread(this)).start();
   }

   public final void run() {
      this.append("pre-compile:\n");
      String var1 = null;
      this.a = null;
      String var2 = "";
      var1 = "";
      this.a = this.a.encoding;
      this.b = this.a.projectFolder + "/build/compiled/";
      this.append("do-compile:\n");

      try {
         a = pf.a();
         String[] var3 = this.a();
         hi var6 = new hi(var1, (ha)null);
         if (!var2.equals("")) {
            var6 = new hi(var2, var6);
         }

         this.a = new iv(this, var2, var6);
         System.gc();
         this.append("Compiling " + var3.length + " source files to " + this.b + "\n");
         this.a(var3);
         this.append("post-compile:\n");
         System.gc();
         this.append("compile:\n");
         SDK var10000 = this.a;
         this.a.getClass();
         var10000.autorun((byte)2);
      } catch (Error var4) {
         this.append(var4.toString() + "\n");
         this.a.buildFailed(this);
      } catch (Exception var5) {
         this.append(var5.getMessage() + "\n");
         this.a.buildFailed(this);
      }
   }

   private String[] a() {
      String var1 = null;
      Vector var2 = new Vector();
      var1 = this.a.projectFolder + "/src/";

      try {
         this.a(var1, var2);
         String[] var5 = new String[var2.size()];

         for(int var4 = 0; var4 < var2.size(); ++var4) {
            var5[var4] = (String)var2.elementAt(var4);
         }

         if (var5.length == 0) {
            throw new Exception("No source files.");
         } else {
            System.gc();
            return var5;
         }
      } catch (IOException var3) {
         throw new IOException(var3.toString());
      }
   }

   private void a(String var1, Vector var2) {
      a.a(var1, a.b);
      Enumeration var3 = a.a("*", true);
      a.a();

      while(var3.hasMoreElements()) {
         String var4;
         if ((var4 = (String)var3.nextElement()).endsWith("/")) {
            this.a(var1 + var4, var2);
         } else if (var4.endsWith(".java")) {
            var2.addElement(var1 + var4);
         }
      }

   }

   private boolean a(String[] var1) {
      System.gc();
      this.a = new Vector(var1.length);
      this.a.removeAllElements();

      int var2;
      for(var2 = 0; var2 < var1.length; ++var2) {
         System.gc();
         a.a(var1[var2], a.b);
         this.a.addElement(new cx(a(var1[var2].substring(var1[var2].lastIndexOf(47) + 1), a.a(), this.a), this.a));
      }

      for(var2 = 0; var2 < this.a.size(); ++var2) {
         System.gc();
         cx var4;
         if ((var4 = (cx)this.a.elementAt(var2)).a.a == null) {
            throw new RuntimeException();
         }

         nb[] var5 = var4.a();
         System.gc();

         for(int var3 = 0; var3 < var5.length; ++var3) {
            this.a(var5[var3]);
         }
      }

      return true;
   }

   private static fv a(String var0, InputStream var1, String var2) {
      fv var6;
      try {
         el var5 = new el(var0, var1, var2);
         var6 = (new fc(var5)).a();
      } finally {
         var1.close();
         a.a();
         System.gc();
      }

      return var6;
   }

   private void a(nb var1) {
      String var2 = nb.a(var1.a());
      String var3 = "";

      for(int var4 = var2.indexOf(47); var4 != -1; var4 = var2.indexOf("/", var4 + 1)) {
         var3 = var2.substring(0, var4 + 1);
         a.a(this.b + var3, a.d);
         if (!a.a()) {
            a.d();
         }

         a.a();
         System.gc();
      }

      String var15 = var2.substring(var2.lastIndexOf(47) + 1);
      OutputStream var14 = null;
      a.a(this.b + var3 + var15, a.d);
      if (a.a()) {
         a.b();
      }

      a.c();
      a.a(var15 + "x");
      a.a(var15);

      try {
         var14 = a.a();
         var1.a(var14);
      } catch (IOException var12) {
         try {
            var14.close();
            a.a();
         } catch (IOException var11) {
         }

         var14 = null;
         throw new IOException(var12.toString());
      } finally {
         if (var14 != null) {
            try {
               var14.flush();
               var14.close();
               a.a();
               System.gc();
            } catch (IOException var10) {
            }
         }

      }

   }

   static Vector a(nt var0) {
      return var0.a;
   }
}
