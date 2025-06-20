import java.io.IOException;
import javax.microedition.lcdui.Form;
import sdk.SDK;

public final class nz extends Form implements Runnable {
   private iy a;
   private mn a = new mn();
   private String a = "";
   private String b = "";
   public static pf a;
   private SDK a;
   private int a = 0;

   public nz(SDK var1) {
      super("");
      this.a = var1;
      (new Thread(this)).start();
   }

   private void a(String var1, iy var2, int var3, int var4, gj var5) {
      for(var3 = var3; var3 < var4; ++var3) {
         mw var6;
         if (!(var6 = var2.a(var3)).a()) {
            gj var8 = var5;
            mw var7 = var6;
            nz var11 = this;

            try {
               var8 = iu.a(var7, var8);
               ky var9;
               (var9 = new ky(var7.b())).a(var8);
               var11.a = var9.a;
            } catch (IOException var10) {
               throw new IOException("Can't read [" + var6.a() + "] (" + var10.getMessage() + ")");
            }
         }
      }

   }

   private void a() {
      iy var1;
      mw var2;
      if ((var2 = (var1 = this.a).a(0)).a()) {
         throw new IOException("The output ClassFile [" + var2.a() + "] must be specified after an input ClassFile, or it will be empty.");
      } else {
         int var4;
         int var13;
         for(var13 = 0; var13 < var1.a() - 1; ++var13) {
            mw var3;
            if ((var3 = var1.a(var13)).a()) {
               if (var1.a(var13 + 1).a()) {
                  throw new IOException("The output ClassFile [" + var3.a() + "] must have a filter, or all subsequent ClassFile will be empty.");
               }

               for(var4 = 0; var4 < var1.a(); ++var4) {
                  mw var5;
                  if (!(var5 = var1.a(var4)).a() && var3.b().equals(var5.b())) {
                     throw new IOException("The output ClassFile [" + var3.a() + "] must be different from all input ClassFile.");
                  }
               }
            }
         }

         var13 = 0;
         int var14 = 0;

         for(var4 = 0; var4 < var1.a(); ++var4) {
            if (!var1.a(var4).a()) {
               var14 = var4;
            } else {
               int var16;
               if ((var16 = var4 + 1) == var1.a() || !var1.a(var16).a()) {
                  int var10004 = var14 + 1;
                  int var10 = var16;
                  int var9 = var10004;
                  int var8 = var13;
                  iy var7 = var1;
                  mn var6 = this.a;
                  nz var15 = this;

                  try {
                     ep var19 = jm.a(var7, var9, var10);
                     hl var17 = new hl(var6, var19);
                     oa var11 = new oa(var19);
                     go var18 = new go(var17, var11);
                     var15.a("  Copying resources from program ", var7, var8, var9, var18);
                     var19.a();
                  } catch (IOException var12) {
                     throw new IOException("Can't write [" + var1.a(var9).a() + "] (" + var12.getMessage() + ")");
                  }

                  var13 = var16;
               }
            }
         }

      }
   }

   public final void run() {
      this.append("pre-preverify:\n");

      try {
         a = pf.a();
         this.a = this.a.projectFolder + "/build/compiled/";
         this.b = this.a.projectFolder + "/build/preverified/";
         this.append("do-preverify:\n");
         this.a = a(this.a, this.a, false);
         this.a = a(this.a, this.b, true);
         System.gc();
         iy var10002 = this.a;
         go var5 = new go(new bb(new ko(this.a, (dg)null, new q(this.a))));
         iy var4 = var10002;
         String var2 = "Reading program ";
         this.a(var2, var4, 0, var4.a(), var5);
         this.append("Preverifying " + this.a + " file(s) into " + this.b + " directory.\n");
         nz var8;
         (var8 = this).a.a((dg)(new fa()));
         hc var3 = new hc(new pl(new kb()));
         var8.a.a((dg)var3);
         (var8 = this).a.a((dg)(new fa()));
         var3 = new hc(new pl(new hs()));
         var8.a.a((dg)var3);
         this.a.a((dg)(new px()));
         if (this.a.a()) {
            this.a();
         }

         this.append("post-preverify:\n");
         this.append("preverify:\n");
         System.gc();
         SDK var10000 = this.a;
         this.a.getClass();
         var10000.autorun((byte)3);
      } catch (Error var6) {
         this.append(var6.toString() + "\n");
         this.a.buildFailed(this);
      } catch (Exception var7) {
         this.append(var7.getMessage() + "\n");
         this.a.buildFailed(this);
      }
   }

   private static iy a(iy var0, String var1, boolean var2) {
      if (var0 == null) {
         var0 = new iy();
      }

      mw var3 = new mw(var1, var2);
      var0.a(var3);
      return var0;
   }
}
