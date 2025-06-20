import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Form;
import sdk.SDK;

public final class hj extends Form implements Runnable {
   private pf a;
   private a a = null;
   private SDK a;
   private String a = "";
   private String b = "";
   private String c = "";
   private boolean a = true;
   private byte[] a;
   private String d = "";

   public hj(SDK var1) {
      super("");
      this.a = var1;
      (new Thread(this)).start();
   }

   public final void run() {
      this.append("pre-jar:\n");

      try {
         this.a = this.a.projectFolder;
         this.b = this.a.jarName;
         this.c = this.a.jadName;
         this.a = this.a.compressedFlag;
         this.a = pf.a();
         String[] var1 = this.a();
         this.append("do-jar:\n");
         String[] var2 = var1;
         hj var8 = this;
         OutputStream var3 = null;
         int var4 = this.a ? 6 : 0;

         try {
            var8.a.a(var8.a + "/dist/" + var8.b, var8.a.d);
            if (var8.a.a()) {
               var8.a.b();
            }

            var8.a.c();
            var8.a.a(var8.b + "x");
            var8.a.a(var8.b);
            var3 = var8.a.a();
            var8.a = new a(var3);
            var8.a.a(var4);

            for(var4 = 0; var4 < var2.length; ++var4) {
               var8.a(var2[var4]);
               var8.a.flush();
            }

            if (var8.a != null) {
               var8.a.flush();
               var8.a.close();
            }

            if (var3 != null) {
               var3.close();
            }

            if (var8.a != null) {
               var8.d = Integer.toString((int)var8.a.a());
               var8.a.a();
            }

            System.gc();
         } catch (IOException var6) {
            throw new IOException("JARing:" + var6.toString());
         }

         this.append("Building jar: " + this.a + "/dist/" + this.b + "\n");
         this.append("create-jad:\n");
         var8 = this;
         String var9 = "MIDlet-Jar-URL: " + this.b + "\r\nMIDlet-Jar-Size: " + this.d + "\r\n";

         try {
            byte[] var11 = var9.getBytes("UTF-8");
            var8.a.a(var8.a + "/dist/" + var8.c, var8.a.d);
            if (var8.a.a()) {
               var8.a.b();
            }

            var8.a.c();
            var8.a.a(var8.c + "x");
            var8.a.a(var8.c);
            OutputStream var12;
            (var12 = var8.a.a()).write(var8.a);
            var12.write(var11);
            var12.close();
            var8.a.a();
         } catch (IOException var5) {
            throw new IOException("CreateJAD: " + var5.toString());
         }

         this.append("Creating application descriptor: " + this.a + "/dist/" + this.c + "\n");
         this.append("post-jar:\n");
         this.append("jar:\n");
         this.append("pre-build:\npost-build:\nbuild:\n");
         this.removeCommand(this.a.cancelCommand);
         this.addCommand(new Command("OK", 4, 1));
         long var10 = (System.currentTimeMillis() - this.a.startBuildTime) / 1000L;
         this.append("BUILD SUCCESSFUL (total time: " + var10 + " seconds)");
         Display.getDisplay(this.a).setCurrentItem(this.get(this.size() - 1));
         System.gc();
      } catch (IOException var7) {
         this.append(var7.getMessage() + "\n");
         this.a.buildFailed(this);
      }
   }

   private String[] a() {
      String var1 = null;
      var1 = null;
      Vector var2;
      (var2 = new Vector()).addElement(this.a + "/build/manifest.mf");

      try {
         var1 = this.a + "/build/preverified/";
         this.a(var1, var2);
         var1 = this.a + "/src/";
         this.a(var1, var2);
         String[] var5 = new String[var2.size()];

         for(int var4 = 0; var4 < var2.size(); ++var4) {
            var5[var4] = (String)var2.elementAt(var4);
         }

         System.gc();
         return var5;
      } catch (IOException var3) {
         throw new IOException(var3.toString());
      }
   }

   private void a(String var1, Vector var2) {
      this.a.a(var1, this.a.b);
      Enumeration var3 = this.a.a("*", true);
      this.a.a();

      while(var3.hasMoreElements()) {
         String var4;
         if ((var4 = (String)var3.nextElement()).endsWith("/")) {
            this.a(var1 + var4, var2);
         } else if (!var4.endsWith(".java")) {
            var2.addElement(var1 + var4);
         }
      }

   }

   private void a(String var1) {
      pf var2 = null;
      String var3 = null;
      var3 = this.a + "/build/preverified/";
      String var4 = this.a + "/src/";
      String var5 = "";
      boolean var6 = false;
      (var2 = pf.a()).a(var1, var2.b);
      if (var1.startsWith(var3)) {
         var5 = var1.substring(var3.length());
      } else if (var1.startsWith(var4)) {
         var5 = var1.substring(var4.length());
      } else if (var1.endsWith("manifest.mf")) {
         var5 = "META-INF/MANIFEST.MF";
         var6 = true;
      }

      StringBuffer var8 = new StringBuffer(var5.length());

      int var11;
      for(var11 = 0; var11 < var5.length(); ++var11) {
         char var7;
         if ((var7 = var5.charAt(var11)) > 127) {
            if ((var7 = (char)(var7 - 848)) >= 240) {
               var7 = (char)(var7 - 16);
            } else {
               var7 = (char)(var7 - 64);
            }
         }

         var8.append(var7);
      }

      InputStream var10 = var2.a();
      this.a.a(new en(var8.toString()));
      if (var6) {
         byte[] var12 = "Manifest-Version: 1.0\r\nCreated-By: 1.1.0_00 (J2ME SDK Mobile)\r\n".getBytes("UTF-8");
         this.a = new byte[(int)var2.a()];
         this.a.write(var12, 0, var12.length);
      }

      int var13 = 0;
      byte[] var9 = new byte[1024];

      while((var11 = var10.read(var9)) != -1) {
         this.a.write(var9, 0, var11);
         if (var6) {
            System.arraycopy(var9, 0, this.a, var13, var11);
            ++var13;
         }
      }

      if (var10 != null) {
         var10.close();
      }

      var2.a();
      System.gc();
   }
}
