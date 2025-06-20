import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import sdk.SDK;

public final class qe extends Canvas implements Runnable, CommandListener, ItemCommandListener, ItemStateListener {
   private SDK a;
   private boolean a;
   private Image a;
   private int[] a = new int[]{0, 16711680, 9868950, 230, 28672, 13531904};
   private int a;
   private int b;
   private int c;
   private int d;
   private int e;
   private int f;
   private int g;
   private cw a;
   private o a;
   private int h;
   private int i;
   private int j;
   private int k;
   private boolean b;
   private boolean c;
   private boolean d;
   private boolean e;
   private String a;
   private fz a;
   private eu a;
   private int l;
   private by a;
   private ix a;
   private Displayable a;
   private long a;
   private boolean f;
   private boolean g;
   private TextBox a;
   private String b;
   private Displayable b;
   private Displayable c;
   private Displayable d;
   private Displayable e;
   private Displayable f;
   private Displayable g;
   private Displayable h;
   private Displayable i;
   private Displayable j;
   private Displayable k;
   private Displayable l;
   private Displayable m;
   private Displayable n;
   private Displayable o;
   private Image b;
   private Image c;
   private Image d;
   private boolean h;
   private int m;
   private int n;
   private int o;
   private final String[][] a = new String[][]{{" New Project... ", " Open Project... ", " Save All ", " Projects ", " Build ", " Options ", " Help ", " Exit "}, {" Edit Line ", " Undo ", " Select ", " Cut ", " Copy ", " Paste ", " Find... ", " Insert Code... "}};
   private boolean i = true;
   private Font a;
   private Displayable p;
   private lv a;
   private es a;
   private Displayable q;
   pf a;
   private Displayable r;
   private sdk.a a;
   private Displayable s;
   private Displayable t;
   private boolean j = false;
   private Displayable u;
   private Displayable v;
   private Displayable w;
   private Displayable x;
   private Displayable y;
   private Displayable z;
   private Displayable A;
   private Displayable B;
   private Displayable C;
   private Displayable D;
   private Displayable E;
   private Displayable F;
   private boolean k = false;

   public qe(SDK var1) {
      this.setFullScreenMode(true);

      try {
         this.a = pf.a();
      } catch (IOException var6) {
         var6.printStackTrace();
      }

      this.c = false;
      this.d = false;
      this.e = false;
      this.a = "";
      this.f = true;
      this.b = "";
      this.a = false;
      this.a = var1;

      try {
         this.b = Image.createImage("/img/mainbar.png");
         this.c = Image.createImage("/img/editbar.png");
         this.d = Image.createImage("/img/arrow.png");
      } catch (IOException var5) {
      }

      this.h = true;
      this.n = 0;
      this.m = 0;
      this.f();
      this.a = new o();
      this.a = new cw();
      this.a = new ix();
      this.a = new fz();
      this.a = new eu();
      this.a = new by(var1);
      this.a = new es();
      this.a = new sdk.a(this.a);
      this.a.a = this.a;
      this.a.a = this.a;
      this.a.a = this.a;
      this.a.a = this.a;
      this.a.a = this.a;
      this.a.a = this.a;
      this.a.a = this.a;
      if (!mx.a("sdkset") && !mx.b("sdkset")) {
         throw new lo("EditCanvas()", new Exception("Could not create initial user settings"));
      } else if (!mx.a("hotkeys") && !mx.b("hotkeys")) {
         throw new lo("EditCanvas()", new Exception("Could not create initial hotkeys store"));
      } else if (!mx.a("templates") && !mx.b("templates")) {
         throw new lo("EditCanvas()", new Exception("Could not create initial templates store"));
      } else {
         int[] var2;
         if ((var2 = mx.a("sdkset")) == null) {
            throw new lo("EditCanvas()", new Exception("Settings store found no records"));
         } else if (var2.length != 1) {
            throw new lo("EditCanvas()", new Exception("Settings store found " + var2.length + " records"));
         } else {
            String var7 = mx.a("sdkset", var2[0]);
            this.a.a = jd.a("menubar", var7).equals("1");
            this.a.b = jd.a("rememberIndent", var7).equals("1");
            this.a.s = Integer.parseInt(jd.a("undolevels", var7));
            this.a.a(this.a.b);
            this.a.e(this.a.s);
            String[] var10 = jd.a(jd.a("fontset", var7).trim());
            this.a(Integer.parseInt(var10[0]));
            if ((var10 = jd.a(jd.a("fontcolors", var7).trim())).length != 7) {
               throw new lo("EditCanvas()", new Exception("Font color record returned " + var10.length + " values."));
            } else {
               this.a.j = Integer.parseInt(var10[0]);
               this.a.k = Integer.parseInt(var10[1]);
               this.a.l = Integer.parseInt(var10[2]);
               this.a.m = Integer.parseInt(var10[3]);
               this.a.n = Integer.parseInt(var10[4]);
               this.a.o = Integer.parseInt(var10[5]);
               this.a.p = Integer.parseInt(var10[6]);
               this.a = this.a[this.a.j];
               this.b = this.a[this.a.k];
               this.c = this.a[this.a.l];
               this.d = this.a[this.a.m];
               this.e = this.a[this.a.n];
               this.f = this.a[this.a.o];
               this.g = this.a[this.a.p];
               String[] var3;
               if (!jd.a("bgcolor", var7).trim().equals("")) {
                  var3 = jd.a(jd.a("bgcolor", var7).trim());
                  this.a.a = Integer.parseInt(var3[0]);
                  this.a.b = Integer.parseInt(var3[1]);
                  this.a.c = Integer.parseInt(var3[2]);
               }

               if (!jd.a("caretcolor", var7).trim().equals("")) {
                  var3 = jd.a(jd.a("caretcolor", var7).trim());
                  this.a.d = Integer.parseInt(var3[0]);
                  this.a.e = Integer.parseInt(var3[1]);
                  this.a.f = Integer.parseInt(var3[2]);
               }

               boolean var13 = false;
               boolean var11 = false;
               int var12;
               int var14;
               if (!jd.a("screensize", var7).trim().equals("")) {
                  String[] var8;
                  var14 = Integer.parseInt((var8 = jd.a(jd.a("screensize", var7).trim()))[0]);
                  var12 = Integer.parseInt(var8[1]);
               } else {
                  var14 = this.getWidth();
                  var12 = this.getHeight();
               }

               this.a(this.a.a);
               this.a(var14, var12, this.a.h, this.a.i);
               int[] var9 = mx.a("hotkeys");
               String var15 = "";
               if (var9 != null && var9.length == 1) {
                  var15 = mx.a("hotkeys", var9[0]);
               } else if (var9 != null && var9.length > 1) {
                  throw new lo("EditCanvas()", new Exception("Keymaps store found " + var9.length + " records"));
               }

               this.a.a(var15);
               int[] var16 = mx.a("templates");
               if (var9 != null && var9.length == 1) {
                  var15 = mx.a("templates", var16[0]);
               } else if (var9 != null && var9.length > 1) {
                  throw new lo("EditCanvas()", new Exception("Templates store found " + var9.length + " records"));
               }

               this.a.a(var15);
               if (!this.a.projectFolder.equals("")) {
                  var15 = this.a.projectFolder.substring(this.a.projectFolder.lastIndexOf(47) + 1, this.a.projectFolder.length());
                  var7 = this.a.projectFolder.substring(0, this.a.projectFolder.lastIndexOf(47) + 1);

                  try {
                     this.a.a(var15, var7);
                  } catch (IOException var4) {
                     this.a((String)var4.getMessage(), (Displayable)this);
                     return;
                  }

                  this.m = 3;
                  if (!this.a.activeFile.equals("")) {
                     this.a.g = this.a.activeFile.substring(0, this.a.activeFile.lastIndexOf(47) + 1);
                     var7 = this.a.activeFile.substring(this.a.g.length());
                     this.a(this.a.g, var7);
                  }
               }

               this.a = true;
               this.d();
               System.gc();
            }
         }
      }
   }

   public final void paint(Graphics var1) {
      if (this.a != null) {
         var1.drawImage(this.a, 0, 0, 0);
      }

   }

   private void a(String var1, Displayable var2) {
      Form var3;
      (var3 = new Form("Alert")).append(var1);
      var3.addCommand(new Command("OK", 4, 1));
      var3.setCommandListener(this);
      Display.getDisplay(this.a).setCurrent(var3);
      this.a = var2;
   }

   private void a(String var1, String var2) {
      System.gc();

      try {
         this.a.a(var1 + var2, this.a.b);
         int var3 = (int)this.a.a();
         InputStream var4 = this.a.a();
         StringBuffer var5 = new StringBuffer();
         int var8;
         if (this.a.f.equals("windows-1251")) {
            char[] var10 = new char[]{'А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ж', 'З', 'И', 'Й', 'К', 'Л', 'М', 'Н', 'О', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Щ', 'Ъ', 'Ы', 'Ь', 'Э', 'Ю', 'Я', 'а', 'б', 'в', 'г', 'д', 'е', 'ж', 'з', 'и', 'й', 'к', 'л', 'м', 'н', 'о', 'п', 'р', 'с', 'т', 'у', 'ф', 'х', 'ц', 'ч', 'ш', 'щ', 'Ъ', 'ы', 'ь', 'э', 'ю', 'я'};

            for(int var11 = 0; var11 < var3; ++var11) {
               if ((var8 = var4.read()) == 184) {
                  var5.append('ё');
               } else if (var8 == 168) {
                  var5.append('Ё');
               } else if (var8 >= 192 && var8 <= 255) {
                  var5.append(var10[var8 - 192]);
               } else {
                  var5.append((char)var8);
               }
            }
         } else {
            InputStreamReader var6 = new InputStreamReader(var4, this.a.f);
            char[] var7 = new char[1024];

            while((var8 = var6.read(var7)) != -1) {
               var5.append(var7, 0, var8);
            }

            var6.close();
         }

         this.a.a = var2.substring(0, var2.length() - 5);
         this.a.b = var1;
         this.a.a = false;
         this.a.c = var2.substring(var2.lastIndexOf(46));
         this.a.b();
         this.a.a(0);
         this.a.a(var5.toString());
         this.k = 0;
         this.j = 0;
         this.b = true;
         this.a.a(this.a.c);
         var4.close();
         this.a.a();
      } catch (IOException var9) {
         this.a((String)("loadActiveFile(): " + var9.toString()), (Displayable)this);
      }

      System.gc();
   }

   private void b(String var1, String var2) {
      System.gc();

      try {
         this.a.a(var2 + var1 + this.a.c, this.a.d);
         if (this.a.a()) {
            this.a.b();
         }

         this.a.c();
         this.a.a(var1 + ".tmp");
         this.a.a(var1 + this.a.c);
         OutputStream var3 = this.a.a();
         String var4 = this.a.toString();
         if (!this.a.f.equals("windows-1251")) {
            OutputStreamWriter var9;
            (var9 = new OutputStreamWriter(var3, this.a.f)).write(var4);
            var9.close();
         } else {
            byte[] var5 = new byte[var4.length()];
            int var6 = 0;

            while(true) {
               if (var6 >= var4.length()) {
                  var3.write(var5, 0, var5.length);
                  break;
               }

               char var7;
               if ((var7 = var4.charAt(var6)) > 0 && var7 < 128) {
                  var5[var6] = (byte)var7;
               } else if (var7 == 1025) {
                  var5[var6] = -88;
               } else if (var7 == 1105) {
                  var5[var6] = -72;
               } else {
                  var5[var6] = (byte)((byte)var7 + 176);
               }

               ++var6;
            }
         }

         var3.close();
         this.a.a();
         this.a.a = var1;
         this.a.b = var2;
      } catch (IOException var8) {
         this.a((String)("saveActiveFile(): " + var8.toString()), (Displayable)this);
      }

      System.gc();
   }

   private void a() {
      this.a.a = "";
      this.a.b = "";
      this.a.a = false;
      this.a.c = "";
      this.a.b();
      this.k = 0;
      this.j = 0;
      this.b = false;
   }

   private void a(String[] var1) {
      System.gc();
      Form var2 = new Form("Copying Resources...");
      Gauge var3;
      (var3 = new Gauge("", false, -1, 0)).setLayout(51);
      var2.append(var3);
      Display.getDisplay(this.a).setCurrent(var2);
      String var6 = this.a.b;
      String var4 = this.a.g;
      (new Thread(new lm(this, var1, var6, var4, var3, this))).start();
   }

   private void a(String var1) {
      this.a.a(var1, this.a.b);
      Enumeration var2 = this.a.a("*", true);
      this.a.a();

      for(; var2.hasMoreElements(); this.a.a()) {
         String var3;
         if ((var3 = var1 + (String)var2.nextElement()).endsWith("/")) {
            this.a(var3);
         }

         this.a.a(var3, this.a.d);
         if (this.a.a()) {
            this.a.b();
         }
      }

   }

   private void b() {
      int[] var1;
      if ((var1 = mx.a("templates")) == null) {
         mx.a("templates", -1, this.a.a());
      } else if (var1.length == 1) {
         if (mx.a("templates", var1[0], this.a.a()) == -1) {
            this.a((String)"Default templates store was not found for overwritting", (Displayable)this);
         }
      } else {
         throw new lo("Overwrite default templates", new Exception("Found " + var1.length + " mapping entries"));
      }
   }

   private void a(boolean var1) {
      if (var1) {
         this.l = this.a.i + 3;
      } else {
         this.l = 0;
      }
   }

   private void c() {
      String[] var1;
      if ((var1 = this.a.a(this.a.b(), 1)).length == 1) {
         this.a = new TextBox("Editing Line " + (this.a.b() + 1), var1[0], 1000, 0);
         this.a.addCommand(new Command("Save", 4, 1));
         this.a.addCommand(new Command("Back", 2, 1));
         this.a.setCommandListener(this);
         Display.getDisplay(this.a).setCurrent(this.a);
      }

   }

   public final void run() {
      if (this.k) {
         this.k = false;
         qe var8;
         Form var1 = (Form)(var8 = this).F;

         try {
            if (var8.a.a() && !var8.a.a.equals("") && !var8.a.b.equals("")) {
               var8.b(var8.a.a, var8.a.b);
            }

            var1.append("create-manifest:\n");
            var8.a.b();
            var1.append("increment-app-version:\n");
            sdk.a var2;
            if ((var2 = var8.a).a) {
               int var3;
               int var4 = (var3 = var2.a % 1000000) / 10000;
               int var5 = (var3 %= 10000) / 100;
               var3 %= 100;
               var2.c = Integer.toString(var4) + "." + Integer.toString(var5) + "." + Integer.toString(var3);
               ++var2.a;
            }

            var1.append("Updating property file: " + var8.a.a + var8.a.b + "/nbproject/project.properties\n");
            var8.a.a();
            var1.append("post-init:\n");
            var8.a.activeFile = var8.a.b + var8.a.a + var8.a.c;
            var8.a.c();
            var1.append("init:\n");
            SDK var10000 = var8.a;
            var8.a.getClass();
            var10000.autorun((byte)1);
         } catch (IOException var6) {
            var1.append(var6.getMessage() + "\n");
            var8.a.buildFailed(var1);
         }
      } else {
         synchronized(this) {
            this.f = false;
            this.d();
            this.f = true;
         }
      }
   }

   private synchronized void d() {
      if (this.a) {
         long var1 = System.currentTimeMillis();
         if (this.a.b() >= this.j + this.h) {
            this.j = this.a.b() - this.h + 1;
            this.b = true;
         } else if (this.a.b() < this.j) {
            this.j = this.a.b();
            this.b = true;
         }

         label279: {
            qe var10000;
            int var10001;
            if (this.a.c() > this.k + this.i) {
               var10000 = this;
               var10001 = this.a.c() - this.i + 15;
            } else {
               if (this.a.c() >= this.k) {
                  break label279;
               }

               var10000 = this;
               var10001 = this.a.c() - 15 >= 1 ? this.a.c() - 15 : 0;
            }

            var10000.k = var10001;
         }

         int var3 = this.a.b();
         int var4 = this.a.c();
         Graphics var5;
         (var5 = this.a.getGraphics()).setFont(this.a);
         var5.setColor(this.a.a, this.a.b, this.a.c);
         var5.fillRect(0, 0, this.a.q, this.a.r);
         int var6 = this.a.h * (var4 - this.k);
         int var7 = this.a.a.equals("") ? 35 : 54;
         int var8 = (this.h ? var7 : 0) + this.a.i * (var3 - this.j);
         var5.setColor(233, 239, 248);
         var5.fillRect(0, var8, this.a.q, this.a.i);
         var5.setColor(this.a.d, this.a.e, this.a.f);
         var5.fillRect(var6 - 1, var8 - 1, 2, this.a.i);
         boolean var9;
         boolean var10;
         int var12;
         if (this.b) {
            pv[] var31 = this.a.a(this.a.a(0, this.j));
            boolean var33 = false;
            var9 = false;
            var10 = false;
            String var11 = "";
            if (var31 != null) {
               for(var12 = 0; var12 < var31.length; ++var12) {
                  if (var31[var12].a == 8) {
                     var33 = true;
                     var11 = var31[var12].a;
                  } else if (var31[var12].a == 3) {
                     var10 = true;
                  } else if (var31[var12].a == 11) {
                     var9 = true;
                  }
               }
            }

            this.e = var33;
            this.a = var11;
            this.d = var10;
            this.c = var9;
            this.b = false;
         }

         boolean var32 = this.e;
         String var34 = this.a;
         var9 = this.d;
         var10 = this.c || !this.a.a();
         String[] var35 = this.a.a(this.j, this.h);
         var12 = 0;
         int var13 = 0;
         int var14 = 0;
         boolean var15;
         int var16;
         if (var15 = this.a.c()) {
            if (this.j > this.a.b && this.a.d() > 0) {
               var13 = 0;
               var14 = 0;

               for(var16 = 0; var16 <= var3 - this.j; ++var16) {
                  if (var16 == var3 - this.j) {
                     var12 += var4;
                  } else {
                     var12 += var35[var16].length();
                  }
               }
            } else {
               var13 = this.a.a;
               var14 = this.a.b - this.j;
               var12 = this.a.d();
            }
         }

         var16 = 0;

         int var18;
         int var36;
         String var38;
         for(var7 = this.h ? var7 : 0; var16 < var35.length; ++var16) {
            if (var15 && var16 >= var14 && var12 > 0) {
               boolean var17 = false;
               var18 = 0;
               if (var16 == var14) {
                  var18 = var13;
               }

               if (var12 > var35[var16].length() - var18) {
                  var36 = var35[var16].length() - var18;
               } else {
                  var36 = var12;
               }

               var5.setColor(176, 197, 227);
               var5.fillRect((var18 - this.k) * this.a.h, var7 - 1, var36 * this.a.h, this.a.i);
               var12 -= var36;
            }

            var36 = this.a.a(var35[var16]);
            var38 = "";
            int var19 = 0;

            for(int var20 = 0; var20 < var36; ++var20) {
               var5.setColor(this.a);
               pv var21;
               if ((var21 = (pv)this.a.a.elementAt(var20)).a == 11) {
                  var10 = true;
                  var5.setColor(this.d);
               } else if (var21.a == 12) {
                  var10 = false;
                  var5.setColor(this.d);
               }

               if (var10) {
                  switch(var21.a) {
                  case 2:
                     var5.setColor(this.e);
                     break;
                  case 3:
                     if (!var32) {
                        var9 = true;
                        var5.setColor(this.c);
                     }
                     break;
                  case 4:
                     var9 = false;
                     var5.setColor(this.c);
                     break;
                  case 5:
                     var5.setColor(this.c);
                     break;
                  case 6:
                  case 7:
                     var5.setColor(this.d);
                     break;
                  case 8:
                     if (!var9 && (var34.equals("") || var21.a.equals(var34))) {
                        var34 = (var32 = !var32) ? var21.a : "";
                        var5.setColor(this.b);
                     }
                  case 9:
                  case 11:
                  case 12:
                  default:
                     break;
                  case 10:
                     var5.setColor(this.g);
                     break;
                  case 13:
                     var5.setColor(this.f);
                  }
               }

               int var22 = var38.length();
               int var23 = var21.a.length();
               if (var22 + var23 > this.k && var22 < this.k + this.i) {
                  String var40;
                  if (var22 < this.k) {
                     var40 = var21.a.substring(this.k - var22);
                  } else if (var22 + var23 > this.k + this.i) {
                     var40 = var21.a.substring(0, this.k + this.i - var22);
                  } else {
                     var40 = var21.a;
                  }

                  if (var32) {
                     var5.setColor(this.b);
                  } else if (var9) {
                     var5.setColor(this.c);
                  }

                  int var27 = var7;
                  String var25 = var40;
                  Graphics var24 = var5;
                  qe var41 = this;
                  int var28 = var40.length();
                  int var29 = 0;

                  for(int var26 = var19; var29 < var28; var26 += var41.a.h) {
                     var24.drawChar(var25.charAt(var29), var26 + var41.a.h / 2, var27, 17);
                     ++var29;
                  }

                  var19 += var40.length() * this.a.h;
               }

               var38 = var38 + var21.a;
            }

            var7 += this.a.i;
         }

         if (this.g) {
            this.g = false;
            this.d();
         } else {
            if (this.a.a.equals("")) {
               Image var37 = null;

               try {
                  var37 = Image.createImage("/img/logo.png");
               } catch (IOException var30) {
               }

               var5.setColor(5798291);
               var5.fillRect(0, 0, this.a.q, this.a.r);
               var5.drawImage(var37, this.a.q / 2, this.a.r / 2, 3);
            }

            if (this.h) {
               var5.setColor(1, 101, 246);
               var5.fillRect(0, 0, this.a.q, 16);
               var5.setColor(236, 233, 216);
               var5.fillRect(0, 16, this.a.q, 19);
               var5.setColor(255, 255, 255);
               var5.drawLine(0, 17, this.a.q, 17);
               var5.setColor(172, 168, 153);
               var5.drawLine(0, 34, this.a.q, 34);
               if (!this.a.a.equals("")) {
                  var5.setColor(236, 233, 216);
                  var5.fillRect(0, 35, this.a.q, 19);
                  var5.setColor(172, 168, 153);
                  var5.drawLine(0, 53, this.a.q, 53);
                  var5.setColor(255, 255, 255);
                  var5.drawLine(0, 35, this.a.q, 35);
                  var5.drawLine(0, 52, this.a.q, 52);
               }

               var36 = this.m << 4;
               var18 = this.n == 0 ? 18 : 36;
               var5.setColor(255, 255, 255);
               var5.fillRect(var36, var18, 15, 15);
               var5.drawImage(this.b, 0, 0, 0);
               if (!this.a.a.equals("")) {
                  var5.drawImage(this.c, 0, 35, 0);
               }

               var5.setColor(166, 166, 166);
               var5.drawRect(var36, var18, 15, 15);
               var5.drawImage(this.d, var36 + 8, var18 + 14, 20);
               if (this.i) {
                  var5.setFont(Font.getFont(0, 0, 8));
                  var5.setColor(16777185);
                  var5.fillRect(var36 + 8, var18 + 28, var5.getFont().stringWidth(this.a[this.n][this.m]), var5.getFont().getHeight());
                  var5.setColor(0);
                  var5.drawString(this.a[this.n][this.m], var36 + 8, var18 + 29, 20);
                  var5.drawRect(var36 + 8, var18 + 28, var5.getFont().stringWidth(this.a[this.n][this.m]), var5.getFont().getHeight());
                  var5.setFont(this.a);
               }

               var5.setColor(255, 255, 255);
               var5.drawString((this.a.b.equals("") ? "" : this.a.b + " - ") + "J2ME SDK Mobile 1.0", 20, (16 - var5.getFont().getHeight()) / 2, 20);
            }

            if (this.a.a) {
               var5.setColor(236, 233, 216);
               var5.fillRect(0, this.getHeight() - this.l, this.a.q, this.a.i + 1);
               var5.setColor(172, 168, 153);
               var5.drawRect(0, this.getHeight() - this.l, this.a.q, this.a.i + 1);
               String var39 = " " + Integer.toString(var3 + 1) + ":" + Integer.toString(var4 + 1) + " ";
               var5.drawRect(0, this.getHeight() - this.l, this.a.stringWidth(var39), this.a.i + 1);
               var5.setColor(0, 0, 0);
               var5.drawString(var39 + (var15 ? " (SEL)" : "") + " " + (this.h ? this.a[this.n][this.m] : this.a.a + this.a.c + (this.a.a() ? "*" : "")), 1, this.getHeight() - this.l + 2, 20);
               if (!this.a.a.equals("")) {
                  var38 = this.h ? " Edit " : " Menu ";
                  var5.setColor(236, 233, 216);
                  var5.fillRect(this.a.q - this.a.stringWidth(var38), this.getHeight() - this.l, this.a.stringWidth(var38), this.a.i + 1);
                  var5.setColor(172, 168, 153);
                  var5.drawRect(this.a.q - this.a.stringWidth(var38), this.getHeight() - this.l, this.a.stringWidth(var38), this.a.i + 1);
                  var5.setColor(0, 0, 0);
                  var5.drawString(var38, this.a.q - this.a.stringWidth(var38), this.getHeight() - this.l + 2, 20);
               }
            }

            this.repaint(0, 0, this.a.q, this.a.r);
            this.serviceRepaints();
            this.a = System.currentTimeMillis() - var1;
         }
      }
   }

   private boolean a(int var1) {
      System.gc();
      this.a = Font.getFont(32, 0, var1);
      this.a.g = var1;
      this.a.h = this.a.charWidth('W');
      this.a.i = this.a.getHeight();
      System.gc();
      return true;
   }

   private void a(int var1, int var2, int var3, int var4) {
      if (var1 < 64 || var2 < 64) {
         var1 = this.getWidth();
         var2 = this.getHeight();
      }

      this.a = Image.createImage(var1, var2);
      this.i = var1 / var3;
      this.h = (var2 - this.l) / var4;
      this.a.q = var1;
      this.a.r = var2;
   }

   public final void keyPressed(int var1) {
      char var2 = this.a.a(var1);
      if (var1 == this.o && !this.a.a.equals("")) {
         this.h = !this.h;
      } else if (this.h) {
         switch(var2) {
         case 'ò':
            this.e();
         case 'ó':
         case 'ô':
         case 'õ':
         case 'ö':
         case '÷':
         case 'ø':
         case 'ù':
         default:
            break;
         case 'ú':
            this.m = this.m < 7 ? this.m + 1 : 0;
            break;
         case 'û':
            this.m = this.m > 0 ? this.m - 1 : 7;
            break;
         case 'ü':
            this.n = !this.a.a.equals("") ? 1 : 0;
            break;
         case 'ý':
            this.n = 0;
         }
      } else {
         switch(var2) {
         case 'æ':
            this.a.g();
            break;
         case 'ç':
         case 'è':
         default:
            this.a.b("" + var2);
            break;
         case 'é':
            this.a.d();
            break;
         case 'ê':
            this.a.d(1);
            break;
         case 'ë':
            this.a.d(-1);
            break;
         case 'ì':
            this.a.h();
            break;
         case 'í':
            this.f = this.a.a("FindReplace", this);
            Display.getDisplay(this.a).setCurrent(this.f);
            break;
         case 'î':
            this.a.f();
            break;
         case 'ï':
            this.a.e();
            break;
         case 'ð':
            this.a.c(!this.a.c());
            break;
         case 'ñ':
            this.c();
            break;
         case 'ò':
            this.a.c();
            break;
         case 'ó':
            this.a.b(!this.a.b());
            break;
         case 'ô':
            this.a.c(1);
            break;
         case 'õ':
            this.a.c(-1);
            break;
         case 'ö':
            var1 = this.a.a();
            if (this.j + (this.h << 1) <= var1) {
               this.j += this.h;
               this.a.a(this.a.b() + this.h, this.a.c());
            } else {
               this.j = var1 - this.h < 0 ? 0 : var1 - this.h;
               this.a.a(var1 - 1, this.a.c());
            }

            this.b = true;
            break;
         case '÷':
            var1 = this.a.b() - this.j;
            if (this.j - this.h < 0) {
               this.j = 0;
               this.a.a(0, this.a.c());
            } else {
               this.j -= this.h;
               this.a.a(this.j + var1, this.a.c());
            }

            this.b = true;
            break;
         case 'ø':
            this.a.b(6);
            break;
         case 'ù':
            this.a.b(5);
            break;
         case 'ú':
            this.a.b(4);
            break;
         case 'û':
            this.a.b(3);
            break;
         case 'ü':
            this.a.b(2);
            break;
         case 'ý':
            this.a.b(1);
            break;
         case 'þ':
            try {
               this.a.b(this.getKeyName(var1));
            } catch (Exception var3) {
            }
         case 'ÿ':
         }
      }

      if (this.a > 100L) {
         if (this.f) {
            (new Thread(this)).start();
         } else {
            this.g = true;
         }
      } else {
         this.d();
      }
   }

   public final void keyReleased(int var1) {
   }

   public final void keyRepeated(int var1) {
      if (var1 == this.o) {
         this.n = 0;
         this.m = 5;
         this.e();
      } else {
         this.keyPressed(var1);
      }
   }

   public final void itemStateChanged(Item var1) {
      TextField var10000;
      StringBuffer var10001;
      String var10002;
      TextField var4;
      label56: {
         String var2;
         String var5;
         TextField var6;
         if (!(var2 = var1.getLabel().toLowerCase()).equals("project name:") && !var2.equals("project location:")) {
            if (!var2.equals("class name:") && !var2.equals("package name:")) {
               return;
            }

            Form var7 = (Form)this.b;
            var6 = (TextField)var1;
            var4 = null;

            for(int var8 = 0; var8 < var7.size(); ++var8) {
               if (var7.get(var8).getLabel().toLowerCase().equals("created file:")) {
                  var4 = (TextField)var7.get(var8);
               }

               if (var7.get(var8).getLabel().toLowerCase().equals("created folder:")) {
                  var4 = (TextField)var7.get(var8);
               }
            }

            if (var4 == null) {
               this.a("TextField item not found", this.b);
               return;
            }

            var4.setConstraints(0);
            var5 = var6.getString();
            var10000 = var4;
            var10001 = (new StringBuffer()).append(this.a.g).append(var5);
            if (var2.equals("class name:")) {
               var10002 = ".java";
               break label56;
            }
         } else {
            TextField var3 = (TextField)((Form)this.r).get(0);
            var6 = (TextField)((Form)this.r).get(1);
            (var4 = (TextField)((Form)this.r).get(3)).setConstraints(0);
            if (!(var5 = var6.getString()).endsWith("/")) {
               var5 = var5 + "/";
            }

            var10000 = var4;
            var10001 = (new StringBuffer()).append(var5).append(var3.getString());
         }

         var10002 = "/";
      }

      var10000.setString(var10001.append(var10002).toString());
      var4.setConstraints(131072);
   }

   public final void commandAction(Command var1, Item var2) {
      if (((StringItem)var2).getText().toLowerCase().equals("browse...")) {
         this.b = "browse";
         this.a.b = "";
         this.c = this.a.a("Browser", this);
         Display.getDisplay(this.a).setCurrent(this.c);
      }

   }

   public final void commandAction(Command var1, Displayable var2) {
      String var3 = var1.getLabel().toLowerCase();
      if (var2 == this.a) {
         if (var3.equals("back")) {
            Display.getDisplay(this.a).setCurrent(this);
            return;
         }

         if (var3.equals("save")) {
            this.a.a(this.a.b(), this.a.getString());
            this.h = false;
            Display.getDisplay(this.a).setCurrent(this);
            this.d();
            return;
         }
      } else {
         TextField var4;
         Form var5;
         sdk.a var10000;
         String var10001;
         TextField var19;
         sdk.a var20;
         String var22;
         int var26;
         if (var2 == this.r) {
            if (var3.equals("cancel")) {
               Display.getDisplay(this.a).setCurrent(this);
               this.r = null;
               return;
            }

            if (var3.equals("create")) {
               var19 = null;
               var4 = null;
               var5 = (Form)var2;

               for(var26 = 0; var26 < var5.size(); ++var26) {
                  if (var5.get(var26).getLabel().toLowerCase().equals("project name:")) {
                     var19 = (TextField)var5.get(var26);
                  } else if (var5.get(var26).getLabel().toLowerCase().equals("project location:")) {
                     var4 = (TextField)var5.get(var26);
                  }
               }

               if (var19 != null && var4 != null) {
                  if (!var19.getString().trim().equals("") && !var4.getString().trim().equals("")) {
                     try {
                        var10000 = this.a;
                        var10001 = var19.getString();
                        var3 = var4.getString();
                        var22 = var10001;
                        var20 = var10000;
                        if (!var3.endsWith("/")) {
                           var3 = var3 + "/";
                        }

                        try {
                           var20.a.a(var3, var20.a.d);
                           if (!var20.a.a()) {
                              var20.a.d();
                           }

                           var20.a.a();
                        } catch (IOException var15) {
                           throw new IOException("CreateProjectLocationError: " + var15.getMessage());
                        }

                        try {
                           var20.a.a(var3 + var22 + "/", var20.a.d);
                           if (var20.a.a()) {
                              var20.a.a();
                              throw new IOException("Project Folder already exists.");
                           }

                           var20.a.d();
                           var20.a.a();
                           var20.a.a(var3 + var22 + "/build/", var20.a.c);
                           var20.a.d();
                           var20.a.a();
                           var20.a.a(var3 + var22 + "/build/compiled/", var20.a.c);
                           var20.a.d();
                           var20.a.a();
                           var20.a.a(var3 + var22 + "/build/preverified/", var20.a.c);
                           var20.a.d();
                           var20.a.a();
                           var20.a.a(var3 + var22 + "/dist/", var20.a.c);
                           var20.a.d();
                           var20.a.a();
                           var20.a.a(var3 + var22 + "/src/", var20.a.c);
                           var20.a.d();
                           var20.a.a();
                           var20.a.a(var3 + var22 + "/nbproject/", var20.a.c);
                           var20.a.d();
                           var20.a.a();
                        } catch (IOException var16) {
                           throw new IOException("CreateProjectFoldersError: " + var16.getMessage());
                        }

                        var20.a = var3;
                        var20.b = var22;
                        var20.a.removeAllElements();
                        var20.b.removeAllElements();
                        var20.b.addElement("MIDlet-Name: " + var20.b);
                        var20.b.addElement("MIDlet-Vendor: Vendor");
                        var20.b.addElement("MIDlet-Version: 1.0");
                        var20.b.addElement("MicroEdition-Configuration: CLDC-1.0");
                        var20.b.addElement("MicroEdition-Profile: MIDP-2.0");
                        var20.c = "0.0.1";
                        var20.a = true;
                        var20.a = 2;
                        var20.d = var20.b + ".jad";
                        var20.e = var20.b + ".jar";
                        var20.b = true;
                        var20.f = "windows-1251";
                        var20.c = false;
                        var20.d = false;
                        var20.a();
                        System.gc();
                     } catch (IOException var17) {
                        this.a(var17.getMessage(), this.r);
                        return;
                     }

                     this.a();
                     this.t = this.a.a("Projects", this);
                     Display.getDisplay(this.a).setCurrent(this.t);
                     this.r = null;
                     this.m = 3;
                     return;
                  }

                  this.a("Both fields must be filled out", this.r);
                  return;
               }

               this.a("NewProject item not found", this.r);
               return;
            }
         } else {
            ix var10002;
            List var24;
            int var31;
            String var33;
            StringBuffer var53;
            if (var2 == this.s) {
               if (var3.equals("cancel")) {
                  Display.getDisplay(this.a).setCurrent(this);
                  this.s = null;
                  return;
               }

               if (var3.equals("open project")) {
                  if ((var31 = (var24 = (List)var2).getSelectedIndex()) != -1) {
                     if ((var33 = var24.getString(var31)).equals("..")) {
                        var26 = (var33 = this.a.b.substring(0, this.a.b.length() - 1)).lastIndexOf(47) + 1;
                        this.a.b = var33.substring(0, var26);
                        this.s = this.a.a("Open Project", this);
                        Display.getDisplay(this.a).setCurrent(this.s);
                        return;
                     }

                     if (!this.a.a(this.a.b + var33 + "/")) {
                        var53 = new StringBuffer();
                        var10002 = this.a;
                        var10002.b = var53.append(var10002.b).append(var33).append("/").toString();
                        this.s = this.a.a("Open Project", this);
                        Display.getDisplay(this.a).setCurrent(this.s);
                        return;
                     }

                     try {
                        this.a.a(var33, this.a.b);
                     } catch (IOException var9) {
                        this.a(var9.getMessage(), this.s);
                        return;
                     }

                     this.a();
                     this.t = this.a.a("Projects", this);
                     Display.getDisplay(this.a).setCurrent(this.t);
                     this.s = null;
                     this.m = 3;
                     return;
                  }

                  return;
               }

               return;
            }

            Form var21;
            ChoiceGroup var36;
            int var41;
            if (var2 == this.D) {
               if (var3.equals("cancel")) {
                  Display.getDisplay(this.a).setCurrent(this.t);
                  this.D = null;
                  return;
               }

               if (var3.equals("next")) {
                  var21 = (Form)var2;
                  var36 = null;

                  for(var41 = 0; var41 < var21.size(); ++var41) {
                     if (var21.get(var41).getLabel().toLowerCase().equals("file types:")) {
                        var36 = (ChoiceGroup)var21.get(var41);
                     }
                  }

                  if (var36 == null) {
                     this.a("File Types item not found", this.D);
                     return;
                  }

                  if (var36.getSelectedIndex() == 0) {
                     this.a.e = "Midlet";
                  } else if (var36.getSelectedIndex() == 1) {
                     this.a.e = "MIDPCanvas";
                  } else if (var36.getSelectedIndex() == 2) {
                     this.a.e = "NewClass";
                  } else if (var36.getSelectedIndex() == 3) {
                     this.a.e = "NewInterface";
                  } else if (var36.getSelectedIndex() == 4) {
                     this.a.e = "newpackage";
                  }

                  this.b = this.a.a("New File", this);
                  Display.getDisplay(this.a).setCurrent(this.b);
                  return;
               }

               return;
            }

            String var7;
            int var8;
            String var23;
            TextField var29;
            TextField var30;
            String var40;
            if (var2 == this.b) {
               if (var3.equals("back")) {
                  this.a.e = "";
                  Display.getDisplay(this.a).setCurrent(this.D);
                  this.b = null;
                  return;
               }

               if (var3.equals("cancel")) {
                  this.a.e = "";
                  Display.getDisplay(this.a).setCurrent(this.t);
                  this.b = null;
                  this.D = null;
                  return;
               }

               if (var3.equals("finish")) {
                  var21 = (Form)var2;
                  var4 = null;
                  var30 = null;
                  var29 = null;
                  TextField var6 = null;
                  var7 = "";

                  for(var8 = 0; var8 < var21.size(); ++var8) {
                     if (var21.get(var8).getLabel().toLowerCase().equals("midlet name:")) {
                        var4 = (TextField)var21.get(var8);
                     } else if (var21.get(var8).getLabel().toLowerCase().equals("class name:")) {
                        var30 = (TextField)var21.get(var8);
                     } else if (var21.get(var8).getLabel().toLowerCase().equals("package name:")) {
                        var30 = (TextField)var21.get(var8);
                     } else if (var21.get(var8).getLabel().toLowerCase().equals("package:")) {
                        var6 = (TextField)var21.get(var8);
                     } else if (var21.get(var8).getLabel().toLowerCase().equals("midlet icon:")) {
                        var29 = (TextField)var21.get(var8);
                     }
                  }

                  if (var30 == null) {
                     this.a("Class Name item not found", this.b);
                     return;
                  }

                  if ((var40 = var30.getString().trim()).equals("")) {
                     this.a("File Name field can not be empty", this.b);
                     return;
                  }

                  if (var6 != null && (var7 = var6.getString().trim()).equals("<default package>")) {
                     var7 = "";
                  }

                  if (this.a.e.equals("Midlet")) {
                     if (var4 == null || var29 == null) {
                        this.a("A form item was not found", this.b);
                        return;
                     }

                     var23 = var4.getString().trim();
                     var22 = var29.getString().trim();
                     if (var23.equals("")) {
                        this.a("Midlet Name field can not be empty", this.b);
                        return;
                     }

                     var3 = (var7.equals("") ? "" : var7 + ".") + var40;
                     this.a.a.addElement(var23 + "," + var22 + "," + var3);

                     try {
                        this.a.a();
                     } catch (IOException var11) {
                        this.a(var11.getMessage(), this.b);
                        return;
                     }
                  }

                  if (this.a.e.equals("newpackage")) {
                     this.a.e = "";
                     this.b = null;
                     this.D = null;

                     try {
                        this.a.a(this.a.g + var40, this.a.d);
                        this.a.d();
                        this.a.a();
                     } catch (IOException var10) {
                        this.a("createPackage(): " + var10.toString(), this.t);
                        return;
                     }

                     this.t = this.a.a("Projects", this);
                     Display.getDisplay(this.a).setCurrent(this.t);
                     System.gc();
                     return;
                  }

                  this.a.b = "";
                  this.a.a(0);
                  this.a.b();
                  if (!var7.equals("")) {
                     var7 = "package " + var7 + ";\r\n";
                  }

                  if (this.a.e.equals("Midlet")) {
                     this.a.a(var7 + "import javax.microedition.midlet.*;\r\nimport javax.microedition.lcdui.*;\r\n\r\npublic class " + var40 + " extends MIDlet {\r\n\r\n public void startApp() {\r\n }\r\n\r\n public void pauseApp() {\r\n }\r\n\r\n public void destroyApp(boolean unconditional) {\r\n }\r\n}\r\n");
                  } else if (this.a.e.equals("MIDPCanvas")) {
                     this.a.a(var7 + "import javax.microedition.lcdui.*;\r\n\r\npublic class " + var40 + " extends Canvas {\r\n\r\n /**\r\n * constructor\r\n */\r\n public " + var40 + "() {\r\n }\r\n\r\n /**\r\n * paint\r\n */\r\n public void paint(Graphics g) {\r\n }\r\n\r\n /**\r\n * Called when a key is pressed.\r\n */\r\n protected  void keyPressed(int keyCode) {\r\n }\r\n\r\n}\r\n");
                  } else if (this.a.e.equals("NewClass")) {
                     this.a.a(var7 + "\r\npublic class " + var40 + " {\r\n\r\n /**\r\n * constructor\r\n */\r\n public " + var40 + "() {\r\n }\r\n    \r\n}\r\n");
                  } else if (this.a.e.equals("NewInterface")) {
                     this.a.a(var7 + "\r\npublic interface " + var40 + " {\r\n\r\n}\r\n");
                  }

                  this.a.c = ".java";
                  this.j = 0;
                  this.k = 0;
                  this.b = true;
                  this.a.a(this.a.c);
                  this.h = false;
                  this.n = 1;
                  this.m = 0;
                  this.a.e = "";
                  this.b = null;
                  this.D = null;
                  this.t = null;
                  this.b(var40, this.a.g);
                  Display.getDisplay(this.a).setCurrent(this);
                  this.d();
                  System.gc();
                  return;
               }
            } else {
               String var32;
               int var34;
               if (var2 == this.c) {
                  if (this.b.equals("browse")) {
                     if (var3.equals("cancel")) {
                        Display.getDisplay(this.a).setCurrent(this.r);
                        return;
                     }

                     if (var1 == List.SELECT_COMMAND && (var31 = (var24 = (List)var2).getSelectedIndex()) != -1) {
                        if ((var33 = var24.getString(var31).toUpperCase()).equals("..")) {
                           var26 = (var33 = this.a.b.substring(0, this.a.b.length() - 1)).lastIndexOf(47) + 1;
                           this.a.b = var33.substring(0, var26);
                           this.c = this.a.a("Browser", this);
                           Display.getDisplay(this.a).setCurrent(this.c);
                           return;
                        }

                        if (var33.endsWith("/")) {
                           var53 = new StringBuffer();
                           var10002 = this.a;
                           var10002.b = var53.append(var10002.b).append(var24.getString(var31)).toString();
                           this.c = this.a.a("Browser", this);
                           Display.getDisplay(this.a).setCurrent(this.c);
                        }

                        return;
                     }

                     if (var3.equals("open")) {
                        if ((var24 = (List)var2).getSelectedIndex() == -1) {
                           return;
                        }

                        if ((var32 = var24.getString(var24.getSelectedIndex())).equals("..") || var32.equals("j2mesdkprojects/")) {
                           var32 = "";
                        }

                        (var30 = (TextField)((Form)this.r).get(1)).setString(this.a.b + var32 + "j2mesdkprojects/");
                        this.itemStateChanged(var30);
                        Display.getDisplay(this.a).setCurrent(this.r);
                        return;
                     }
                  } else {
                     if (!this.b.equals("addresources")) {
                        return;
                     }

                     if (var3.equals("back")) {
                        this.j = false;
                        Display.getDisplay(this.a).setCurrent(this.t);
                        return;
                     }

                     if (var3.equals("mark several")) {
                        this.j = true;
                        this.c = this.a.a("Add Resources", this);
                        Display.getDisplay(this.a).setCurrent(this.c);
                        return;
                     }

                     if (var3.equals("add")) {
                        String[] var35;
                        if ((var31 = (var24 = (List)var2).getSelectedIndex()) != -1) {
                           if ((var33 = var24.getString(var31).toUpperCase()).equals("..")) {
                              var26 = (var33 = this.a.b.substring(0, this.a.b.length() - 1)).lastIndexOf(47) + 1;
                              this.a.b = var33.substring(0, var26);
                              this.c = this.a.a("Add Resource", this);
                              Display.getDisplay(this.a).setCurrent(this.c);
                              return;
                           }

                           if (var33.endsWith("/")) {
                              var53 = new StringBuffer();
                              var10002 = this.a;
                              var10002.b = var53.append(var10002.b).append(var24.getString(var31)).toString();
                              this.c = this.a.a("Add Resource", this);
                              Display.getDisplay(this.a).setCurrent(this.c);
                              return;
                           }

                           var35 = new String[]{var24.getString(var31)};
                           this.a(var35);
                           return;
                        }

                        if (!this.j) {
                           return;
                        }

                        this.j = false;
                        Vector var37 = new Vector();

                        for(var26 = 0; var26 < var24.size(); ++var26) {
                           if (var24.isSelected(var26)) {
                              var37.addElement(var24.getString(var26));
                           }
                        }

                        var35 = new String[var37.size()];

                        for(var34 = 0; var34 < var37.size(); ++var34) {
                           var35[var34] = (String)var37.elementAt(var34);
                        }

                        this.a(var35);
                        return;
                     }
                  }
               } else {
                  if (var2 == this.t) {
                     if (var3.equals("menu")) {
                        Display.getDisplay(this.a).setCurrent(this);
                        this.d();
                        this.t = null;
                        return;
                     }

                     if (var3.equals("new")) {
                        this.D = this.a.a("Choose File Type", this);
                        Display.getDisplay(this.a).setCurrent(this.D);
                        return;
                     }

                     if (var3.equals("add resources...")) {
                        this.a.b = "";
                        this.b = "addresources";
                        this.c = this.a.a("Add Resource", this);
                        Display.getDisplay(this.a).setCurrent(this.c);
                        return;
                     }

                     if (var3.equals("project properties")) {
                        this.u = this.a.a("Properties", this);
                        Display.getDisplay(this.a).setCurrent(this.u);
                        return;
                     }

                     if ((var31 = (var24 = (List)var2).getSelectedIndex()) == -1) {
                        return;
                     }

                     var33 = var24.getString(var31);
                     if (var3.equals("delete")) {
                        if (!var33.equals("..")) {
                           if (var33.endsWith(".")) {
                              var33 = var33.substring(0, var33.length() - 1) + "/";
                           }

                           this.a.e = var33;
                           this.E = this.a.a("Safe Delete", this);
                           Display.getDisplay(this.a).setCurrent(this.E);
                        }

                        return;
                     }

                     if (var3.equals("open")) {
                        if (var33.equals("Source Packages")) {
                           this.a.g = this.a.a + this.a.b + "/src/";
                           this.t = this.a.a("Projects", this);
                           Display.getDisplay(this.a).setCurrent(this.t);
                           return;
                        }

                        if (!var33.equals("Resources")) {
                           if (var33.equals("..")) {
                              var26 = (var33 = this.a.g.substring(0, this.a.g.length() - 1)).lastIndexOf(47) + 1;
                              this.a.g = var33.substring(0, var26);
                              this.t = this.a.a("Projects", this);
                              Display.getDisplay(this.a).setCurrent(this.t);
                              return;
                           }

                           if (var33.endsWith(".")) {
                              var53 = new StringBuffer();
                              sdk.a var54 = this.a;
                              var54.g = var53.append(var54.g).append(var33.substring(0, var33.length() - 1)).append("/").toString();
                              this.t = this.a.a("Projects", this);
                              Display.getDisplay(this.a).setCurrent(this.t);
                              return;
                           }

                           if (var33.endsWith(".java")) {
                              this.a(this.a.g, var33);
                              this.h = false;
                              this.n = 1;
                              this.m = 0;
                              Display.getDisplay(this.a).setCurrent(this);
                              this.d();
                           }
                        }

                        return;
                     }

                     return;
                  }

                  if (var2 == this.E) {
                     if (var3.equals("cancel")) {
                        Display.getDisplay(this.a).setCurrent(this.t);
                        this.E = null;
                        return;
                     }

                     if (!var3.equals("ok")) {
                        return;
                     }

                     var22 = this.a.g + this.a.e;
                     qe var50 = this;
                     if (var22.equals(this.a.b + this.a.a + this.a.c)) {
                        this.a();
                     }

                     label1239: {
                        try {
                           if (var22.endsWith("/")) {
                              var50.a(var22);
                           }

                           var50.a.a(var22, var50.a.d);
                           if (var50.a.a()) {
                              var50.a.b();
                           }

                           var50.a.a();
                        } catch (IOException var18) {
                           this.a("deleteFile(): " + var18.getMessage(), this.t);
                           break label1239;
                        }

                        this.t = this.a.a("Projects", this);
                        Display.getDisplay(this.a).setCurrent(this.t);
                     }

                     Display.getDisplay(this.a).setCurrent(this.t);
                     this.E = null;
                     return;
                  }

                  if (var2 == this.u) {
                     if (var3.equals("cancel")) {
                        Display.getDisplay(this.a).setCurrent(this.t);
                        return;
                     }

                     if (var1 == List.SELECT_COMMAND) {
                        if ((var24 = (List)var2).getSelectedIndex() == -1) {
                           return;
                        }

                        if ((var32 = var24.getString(var24.getSelectedIndex()).toLowerCase()).equals("general")) {
                           this.v = this.a.a(var24.getString(var24.getSelectedIndex()), this);
                           Display.getDisplay(this.a).setCurrent(this.v);
                           return;
                        }

                        if (var32.equals("application descriptor")) {
                           this.w = this.a.a(var24.getString(var24.getSelectedIndex()), this);
                           Display.getDisplay(this.a).setCurrent(this.w);
                           return;
                        }

                        if (var32.equals("build")) {
                           this.w = this.a.a(var24.getString(var24.getSelectedIndex()), this);
                           Display.getDisplay(this.a).setCurrent(this.w);
                           return;
                        }
                     }

                     if (var3.equals("ok")) {
                        try {
                           this.a.a();
                        } catch (IOException var12) {
                           this.a(var12.getMessage(), this.u);
                           return;
                        }

                        Display.getDisplay(this.a).setCurrent(this.t);
                        return;
                     }

                     return;
                  }

                  ChoiceGroup var49;
                  if (var2 == this.v) {
                     if (var3.equals("cancel")) {
                        Display.getDisplay(this.a).setCurrent(this.u);
                        return;
                     } else if (var3.equals("ok")) {
                        var21 = (Form)var2;
                        var4 = null;
                        var49 = null;
                        if (var21.get(4).getLabel().toLowerCase().equals("application version counter:")) {
                           var4 = (TextField)var21.get(4);
                        }

                        if (var21.get(5).getLabel().toLowerCase().equals("")) {
                           var49 = (ChoiceGroup)var21.get(5);
                        }

                        if (var4 != null && var49 != null) {
                           if (var4.getString().trim().equals("")) {
                              this.a("Counter field can not be empty", this.v);
                              return;
                           } else {
                              try {
                                 var10000 = this.a;
                                 var10001 = var4.getString();
                                 boolean var51 = var49.isSelected(0);
                                 var22 = var10001;
                                 var20 = var10000;
                                 boolean var52 = false;

                                 try {
                                    var31 = Integer.parseInt(var22.trim());
                                 } catch (NumberFormatException var13) {
                                    throw new Exception("Application Version Counter: " + var13.getMessage());
                                 }

                                 var20.a = var31;
                                 var20.a = var51;
                              } catch (Exception var14) {
                                 this.a(var14.getMessage(), this.v);
                                 return;
                              }

                              Display.getDisplay(this.a).setCurrent(this.u);
                              return;
                           }
                        } else {
                           this.a("A form item was not found", this.v);
                           return;
                        }
                     } else {
                        return;
                     }
                  }

                  if (var2 == this.w) {
                     if (var3.equals("back")) {
                        Display.getDisplay(this.a).setCurrent(this.u);
                        return;
                     }

                     if (var3.equals("select")) {
                        if ((var24 = (List)var2).getSelectedIndex() == -1) {
                           return;
                        }

                        if ((var32 = var24.getString(var24.getSelectedIndex()).toLowerCase()).equals("attributes")) {
                           this.x = this.a.a(var24.getString(var24.getSelectedIndex()), this);
                           Display.getDisplay(this.a).setCurrent(this.x);
                           return;
                        }

                        if (var32.equals("midlets")) {
                           this.z = this.a.a(var24.getString(var24.getSelectedIndex()), this);
                           Display.getDisplay(this.a).setCurrent(this.z);
                           return;
                        }

                        if (var32.equals("compiling")) {
                           this.C = this.a.a(var24.getString(var24.getSelectedIndex()), this);
                           Display.getDisplay(this.a).setCurrent(this.C);
                           return;
                        }

                        if (var32.equals("creating jar")) {
                           this.B = this.a.a(var24.getString(var24.getSelectedIndex()), this);
                           Display.getDisplay(this.a).setCurrent(this.B);
                        }

                        return;
                     }

                     return;
                  }

                  if (var2 == this.x) {
                     if (var3.equals("back")) {
                        Display.getDisplay(this.a).setCurrent(this.w);
                        return;
                     }

                     if (var3.equals("add...")) {
                        this.y = this.a.a("Add Attribute", this);
                        Display.getDisplay(this.a).setCurrent(this.y);
                        return;
                     }

                     if ((var24 = (List)var2).getSelectedIndex() == -1) {
                        return;
                     }

                     var32 = var24.getString(var24.getSelectedIndex());
                     if (var3.equals("edit...")) {
                        this.a.a = var32.substring(0, var32.indexOf(58));
                        this.a.c = var32.substring(var32.indexOf(58) + 2, var32.length());
                        this.y = this.a.a("Edit Attribute", this);
                        Display.getDisplay(this.a).setCurrent(this.y);
                        return;
                     }

                     if (var3.equals("remove")) {
                        this.a.b.removeElementAt(var24.getSelectedIndex());
                        this.x = this.a.a("Attributes", this);
                        Display.getDisplay(this.a).setCurrent(this.x);
                        return;
                     }

                     return;
                  }

                  String var39;
                  if (var2 == this.y) {
                     if (var3.equals("cancel")) {
                        Display.getDisplay(this.a).setCurrent(this.x);
                        return;
                     }

                     if (var3.equals("ok")) {
                        var21 = (Form)var2;
                        var4 = null;
                        var30 = null;
                        if (var21.get(0).getLabel().toLowerCase().equals("name:")) {
                           var4 = (TextField)var21.get(0);
                        }

                        if (var21.get(1).getLabel().toLowerCase().equals("value:")) {
                           var30 = (TextField)var21.get(1);
                        }

                        if (var4 != null && var30 != null) {
                           var3 = var4.getString().trim();
                           var39 = var30.getString().trim();
                           if (!var3.equals("") && !var39.equals("")) {
                              var7 = var3 + ": " + var39;
                              if (var2.getTitle().equals("Add Attribute")) {
                                 this.a.b.addElement(var7);
                              } else {
                                 var8 = ((List)this.x).getSelectedIndex();
                                 this.a.b.setElementAt(var7, var8);
                              }

                              this.x = this.a.a("Attributes", this);
                              Display.getDisplay(this.a).setCurrent(this.x);
                              return;
                           }

                           this.a("Both fields must be filled out", this.y);
                           return;
                        }

                        this.a("A form item was not found", this.y);
                        return;
                     }

                     return;
                  }

                  if (var2 == this.z) {
                     if (var3.equals("back")) {
                        Display.getDisplay(this.a).setCurrent(this.w);
                        return;
                     }

                     if (var3.equals("add...")) {
                        this.A = this.a.a("Add MIDlet", this);
                        Display.getDisplay(this.a).setCurrent(this.A);
                        return;
                     }

                     if ((var24 = (List)var2).getSelectedIndex() == -1) {
                        return;
                     }

                     var32 = var24.getString(var24.getSelectedIndex());
                     if (var3.equals("edit...")) {
                        this.a.a = var32.substring(var32.indexOf(58) + 2, var32.indexOf(","));
                        this.a.c = var32.substring(var32.lastIndexOf(44) + 1, var32.length());
                        this.a.e = var32.substring(var32.indexOf(",") + 1, var32.lastIndexOf(44));
                        this.A = this.a.a("Edit MIDlet", this);
                        Display.getDisplay(this.a).setCurrent(this.A);
                        return;
                     }

                     if (var3.equals("remove")) {
                        this.a.a.removeElementAt(var24.getSelectedIndex());
                        this.z = this.a.a("MIDlets", this);
                        Display.getDisplay(this.a).setCurrent(this.z);
                        return;
                     }

                     return;
                  }

                  if (var2 == this.A) {
                     if (var3.equals("cancel")) {
                        Display.getDisplay(this.a).setCurrent(this.z);
                        return;
                     }

                     if (var3.equals("ok")) {
                        var21 = (Form)var2;
                        var4 = null;
                        var30 = null;
                        var29 = null;
                        if (var21.get(0).getLabel().toLowerCase().equals("midlet name:")) {
                           var4 = (TextField)var21.get(0);
                        }

                        if (var21.get(1).getLabel().toLowerCase().equals("midlet class:")) {
                           var30 = (TextField)var21.get(1);
                        }

                        if (var21.get(2).getLabel().toLowerCase().equals("midlet icon:")) {
                           var29 = (TextField)var21.get(2);
                        }

                        if (var4 != null && var30 != null && var29 != null) {
                           var39 = var4.getString().trim();
                           var7 = var30.getString().trim();
                           var40 = var29.getString().trim();
                           if (!var39.equals("") && !var7.equals("")) {
                              var23 = var39 + "," + var40 + "," + var7;
                              if (var2.getTitle().equals("Add MIDlet")) {
                                 this.a.a.addElement(var23);
                              } else {
                                 int var27 = ((List)this.z).getSelectedIndex();
                                 this.a.a.setElementAt(var23, var27);
                              }

                              this.z = this.a.a("MIDlets", this);
                              Display.getDisplay(this.a).setCurrent(this.z);
                              return;
                           }

                           this.a("Both fields must be filled out", this.A);
                           return;
                        }

                        this.a("A form item was not found", this.A);
                        return;
                     }

                     return;
                  }

                  if (var2 == this.B) {
                     if (var3.equals("cancel")) {
                        Display.getDisplay(this.a).setCurrent(this.w);
                        return;
                     }

                     if (var3.equals("ok")) {
                        var21 = (Form)var2;
                        var4 = null;
                        var30 = null;
                        ChoiceGroup var45 = null;
                        if (var21.get(0).getLabel().toLowerCase().equals("jad file name:")) {
                           var4 = (TextField)var21.get(0);
                        }

                        if (var21.get(1).getLabel().toLowerCase().equals("jar file name:")) {
                           var30 = (TextField)var21.get(1);
                        }

                        if (var21.get(2).getLabel().toLowerCase().equals("")) {
                           var45 = (ChoiceGroup)var21.get(2);
                        }

                        if (var4 != null && var30 != null && var45 != null) {
                           if (!var4.getString().trim().equals("") && !var30.getString().trim().equals("")) {
                              this.a.d = var4.getString().trim();
                              this.a.e = var30.getString().trim();
                              this.a.b = var45.isSelected(0);
                              Display.getDisplay(this.a).setCurrent(this.w);
                              return;
                           }

                           this.a("Both fields must be filled out", this.B);
                           return;
                        }

                        this.a("A form item was not found", this.B);
                        return;
                     }

                     return;
                  }

                  if (var2 == this.C) {
                     if (var3.equals("cancel")) {
                        Display.getDisplay(this.a).setCurrent(this.w);
                        return;
                     }

                     if (var3.equals("ok")) {
                        var21 = (Form)var2;
                        var36 = null;
                        var49 = null;
                        if (var21.get(0).getLabel().toLowerCase().equals("javac options:")) {
                           var36 = (ChoiceGroup)var21.get(0);
                        }

                        if (var21.get(1).getLabel().toLowerCase().equals("encoding:")) {
                           var49 = (ChoiceGroup)var21.get(1);
                        }

                        if (var36 != null && var49 != null) {
                           this.a.c = var36.isSelected(0);
                           this.a.d = var36.isSelected(1);
                           this.a.f = var49.getString(var49.isSelected(0) ? 0 : (var49.isSelected(1) ? 1 : 2));
                           Display.getDisplay(this.a).setCurrent(this.w);
                           return;
                        }

                        this.a("A form item was not found", this.C);
                        return;
                     }

                     return;
                  }

                  if (var2 == this.d) {
                     if (var3.equals("close")) {
                        Display.getDisplay(this.a).setCurrent(this);
                        return;
                     }
                  } else if (var2 == this.e) {
                     if (var3.equals("cancel")) {
                        Display.getDisplay(this.a).setCurrent(this);
                        this.e = null;
                        return;
                     }

                     if (var3.equals("discard") || var3.equals("save")) {
                        if (var3.equals("save")) {
                           this.b(this.a.a, this.a.b);
                        } else {
                           this.a.a();
                        }

                        Display.getDisplay(this.a).setCurrent(this);
                        this.e = null;
                        this.d();
                        this.e();
                        return;
                     }
                  } else {
                     int var43;
                     if (var2 == this.f) {
                        if (var3.equals("back")) {
                           Display.getDisplay(this.a).setCurrent(this);
                           return;
                        }

                        if (var3.equals("go")) {
                           ChoiceGroup var28 = null;
                           var36 = null;
                           var30 = null;
                           var29 = null;
                           Form var38 = (Form)var2;

                           for(var43 = 0; var43 < var38.size(); ++var43) {
                              if (var38.get(var43).getLabel().toLowerCase().equals("find")) {
                                 var30 = (TextField)var38.get(var43);
                              } else if (var38.get(var43).getLabel().toLowerCase().equals("replace")) {
                                 var28 = (ChoiceGroup)var38.get(var43);
                              } else if (var38.get(var43).getLabel().toLowerCase().equals("replace with")) {
                                 var29 = (TextField)var38.get(var43);
                              } else if (var38.get(var43).getLabel().toLowerCase().equals("case sensitive")) {
                                 var36 = (ChoiceGroup)var38.get(var43);
                              }
                           }

                           if (var30 != null && var28 != null && var29 != null && var36 != null) {
                              if (var30.getString().trim().equals("")) {
                                 this.a("Find field can not be empty", this.f);
                                 return;
                              }

                              if (var28.isSelected(0)) {
                                 this.a.a(var30.getString(), var29.getString(), var36.isSelected(0));
                              } else {
                                 this.a.a(var30.getString(), var36.isSelected(0));
                              }

                              this.a.a = var30.getString();
                              this.h = false;
                              this.n = 1;
                              this.m = 0;
                              Display.getDisplay(this.a).setCurrent(this);
                              this.d();
                              return;
                           }

                           this.a("A form item was not found", this.f);
                           return;
                        }
                     } else {
                        if (var2 == this.o) {
                           if (var3.equals("back")) {
                              Display.getDisplay(this.a).setCurrent(this);
                              return;
                           }

                           if ((var24 = (List)var2).getSelectedIndex() == -1) {
                              return;
                           }

                           if (var3.equals("select")) {
                              String[] var48 = this.a.a(this.a.b(), 1);
                              var33 = "";
                              var3 = null;
                              if (var48.length == 1) {
                                 var33 = var48[0];
                              }

                              if ((var39 = var24.getString(var24.getSelectedIndex())).equals("Add Code")) {
                                 this.a.d = "";
                                 this.q = this.a.a("EditCode", this);
                                 Display.getDisplay(this.a).setCurrent(this.q);
                                 return;
                              }

                              if (var39.equals("import ...")) {
                                 this.p = this.a.a("PackageBrowser", this);
                                 this.a = new lv(this.p);
                                 Display.getDisplay(this.a).setCurrent(this.p);
                                 return;
                              }

                              if (var39 != null && !var39.equals("")) {
                                 var3 = var33 + this.a.a(var39);
                                 this.a.a(this.a.b(), var3);
                                 this.h = false;
                                 this.n = 1;
                                 this.m = 0;
                                 Display.getDisplay(this.a).setCurrent(this);
                                 this.d();
                                 return;
                              }

                              return;
                           }

                           if (!var3.equals("edit") && !var3.equals("delete")) {
                              return;
                           }

                           if (!(var32 = var24.getString(var24.getSelectedIndex())).equals("Add Code") && !var32.equals("import ...")) {
                              if (var3.equals("edit")) {
                                 this.a.d = var32;
                                 this.q = this.a.a("EditCode", this);
                                 Display.getDisplay(this.a).setCurrent(this.q);
                                 return;
                              }

                              if (var3.equals("delete")) {
                                 this.a.b(var32);
                                 this.o = this.a.a("InsertCode", this);
                                 Display.getDisplay(this.a).setCurrent(this.o);
                                 this.b();
                              }

                              return;
                           }

                           return;
                        }

                        if (var2 == this.q) {
                           if (var3.equals("back")) {
                              this.o = this.a.a("InsertCode", this);
                              Display.getDisplay(this.a).setCurrent(this.o);
                              return;
                           }

                           if (var3.equals("save")) {
                              var19 = null;
                              var4 = null;
                              var5 = (Form)var2;

                              for(var26 = 0; var26 < var5.size(); ++var26) {
                                 if (var5.get(var26).getLabel().toLowerCase().equals("name")) {
                                    var19 = (TextField)var5.get(var26);
                                 } else if (var5.get(var26).getLabel().toLowerCase().equals("code")) {
                                    var4 = (TextField)var5.get(var26);
                                 }
                              }

                              if (var19 != null && var4 != null) {
                                 if (!var19.getString().trim().equals("") && !var4.getString().trim().equals("")) {
                                    this.a.a(var19.getString(), var4.getString());
                                    this.o = this.a.a("InsertCode", this);
                                    Display.getDisplay(this.a).setCurrent(this.o);
                                    this.b();
                                    return;
                                 }

                                 this.a("Both fields must be filled out", this.q);
                                 return;
                              }

                              this.a("EditCode item not found", this.q);
                              return;
                           }
                        } else if (var2 == this.p) {
                           if (var3.equals("back")) {
                              this.a = null;
                              System.gc();
                              this.o = this.a.a("InsertCode", this);
                              Display.getDisplay(this.a).setCurrent(this.o);
                              return;
                           }

                           if (var3.equals("select")) {
                              if ((var24 = (List)var2).getSelectedIndex() == -1) {
                                 return;
                              }

                              if ((var32 = var24.getString(var24.getSelectedIndex())).equals("..")) {
                                 var41 = this.a.a.lastIndexOf(46);
                                 if (-1 == var41) {
                                    this.a.a = "";
                                 } else {
                                    this.a.a = this.a.a.substring(0, var41);
                                 }

                                 this.a.a();
                                 return;
                              }

                              if (null != var32 && !var32.equals("*") && Character.isLowerCase(var32.charAt(0))) {
                                 if (null != this.a.a && 0 != this.a.a.length()) {
                                    this.a.a = this.a.a + "." + var32;
                                 } else {
                                    this.a.a = var32;
                                 }

                                 this.a.a();
                                 return;
                              }

                              if (null != var32 && !var32.equals("")) {
                                 this.a.a = this.a.a + "." + var32;
                                 this.a.a(this.a.b(), "import " + this.a.a + ";");
                                 this.h = false;
                                 this.m = 0;
                                 this.a = null;
                                 System.gc();
                                 Display.getDisplay(this.a).setCurrent(this);
                                 this.d();
                                 return;
                              }

                              return;
                           }
                        } else if (var2 == this.g) {
                           if (var3.equals("back")) {
                              if (!mx.a("sdkset")) {
                                 throw new lo("saveUserSettings()", new Exception("Settings store not found"));
                              }

                              int[] var25;
                              if ((var25 = mx.a("sdkset")) != null && var25.length == 1) {
                                 var3 = "<bgcolor>" + this.a.a + "," + this.a.b + "," + this.a.c + "</bgcolor>" + "<caretcolor>" + this.a.d + "," + this.a.e + "," + this.a.f + "</caretcolor>" + "<menubar>" + (this.a.a ? "1" : "0") + "</menubar>" + "<screensize>" + this.a.q + "," + this.a.r + "</screensize>" + "<fontset>" + this.a.g + "</fontset>" + "<fontcolors>" + this.a.j + "," + this.a.k + "," + this.a.l + "," + this.a.m + "," + this.a.n + "," + this.a.o + "," + this.a.p + "</fontcolors>" + "<rememberIndent>" + (this.a.b ? "1" : "0") + "</rememberIndent>" + "<undolevels>" + this.a.s + "</undolevels>";
                                 if (mx.a("sdkset", var25[0], var3) == -1) {
                                    this.a((String)"Existing user settings were not found", (Displayable)this);
                                 }

                                 Display.getDisplay(this.a).setCurrent(this);
                                 this.d();
                                 return;
                              }

                              throw new lo("saveUserSettings()", new Exception("Found " + var25.length + " user settings"));
                           }

                           if (var3.equals("select")) {
                              if ((var24 = (List)var2).getSelectedIndex() == -1) {
                                 return;
                              }

                              if ((var32 = var24.getString(var24.getSelectedIndex()).toLowerCase()).equals("text color")) {
                                 this.j = this.a.a("Font Text", this);
                                 Display.getDisplay(this.a).setCurrent(this.j);
                              } else if (var32.equals("hot keys")) {
                                 this.m = this.a.a("KB Map", this);
                                 Display.getDisplay(this.a).setCurrent(this.m);
                              } else if (var32.equals("font")) {
                                 this.i = this.a.a("Font Choose", this);
                                 Display.getDisplay(this.a).setCurrent(this.i);
                              } else if (var32.startsWith("word wrap")) {
                                 this.a.a = !this.a.a;
                                 this.a.a(this.a.a ? this.a.q / this.a.h : 0);
                                 this.g = this.a.a("Options", this);
                                 Display.getDisplay(this.a).setCurrent(this.g);
                                 ((List)this.g).setSelectedIndex(((List)var2).getSelectedIndex(), true);
                              } else if (var32.startsWith("keep indent")) {
                                 this.a.b = !this.a.b;
                                 this.g = this.a.a("Options", this);
                                 Display.getDisplay(this.a).setCurrent(this.g);
                                 this.a.a(this.a.b);
                                 ((List)this.g).setSelectedIndex(((List)var2).getSelectedIndex(), true);
                              } else if (var32.equals("background color")) {
                                 this.b = "backgroundcolor";
                                 this.l = this.a.a("Background Color", this);
                                 Display.getDisplay(this.a).setCurrent(this.l);
                              } else if (var32.equals("caret color")) {
                                 this.b = "caretcolor";
                                 this.l = this.a.a("Caret Color", this);
                                 Display.getDisplay(this.a).setCurrent(this.l);
                              } else if (var32.equals("screen size")) {
                                 this.h = this.a.a("Screen Size", this);
                                 Display.getDisplay(this.a).setCurrent(this.h);
                              } else if (var32.startsWith("status bar")) {
                                 this.a.a = !this.a.a;
                                 this.a(this.a.a);
                                 this.a(this.a.q, this.a.r, this.a.h, this.a.i);
                                 this.g = this.a.a("Options", this);
                                 Display.getDisplay(this.a).setCurrent(this.g);
                                 ((List)this.g).setSelectedIndex(((List)var2).getSelectedIndex(), true);
                              } else {
                                 if (!var32.equals("undo levels")) {
                                    return;
                                 }

                                 this.n = this.a.a("Undo Levels", this);
                                 Display.getDisplay(this.a).setCurrent(this.n);
                              }
                           }
                        } else if (var2 == this.h) {
                           if (var3.equals("back")) {
                              this.g = this.a.a("Options", this);
                              Display.getDisplay(this.a).setCurrent(this.g);
                              return;
                           }

                           if (var3.equals("save")) {
                              var19 = null;
                              var4 = null;
                              var5 = (Form)var2;

                              for(var26 = 0; var26 < var5.size(); ++var26) {
                                 if (var5.get(var26).getLabel().toLowerCase().equals("screen width")) {
                                    var19 = (TextField)var5.get(var26);
                                 } else if (var5.get(var26).getLabel().toLowerCase().equals("screen height")) {
                                    var4 = (TextField)var5.get(var26);
                                 }
                              }

                              if (var19 != null && var4 != null) {
                                 if (!var19.getString().trim().equals("") && !var4.getString().trim().equals("")) {
                                    this.a(Integer.parseInt(var19.getString()), Integer.parseInt(var4.getString()), this.a.h, this.a.i);
                                    this.g = this.a.a("Options", this);
                                    Display.getDisplay(this.a).setCurrent(this.g);
                                    return;
                                 }

                                 this.a("Both fields must be filled out", this.h);
                                 return;
                              }

                              this.a("Screen size item not found", this.h);
                              return;
                           }
                        } else {
                           boolean var42;
                           if (var2 == this.i) {
                              if (var3.equals("back")) {
                                 this.g = this.a.a("Options", this);
                                 Display.getDisplay(this.a).setCurrent(this.g);
                                 return;
                              }

                              if (var3.equals("save")) {
                                 var21 = (Form)var2;
                                 var31 = 0;

                                 for(var42 = false; var31 < var21.size(); ++var31) {
                                    if (var21.get(var31) instanceof ChoiceGroup) {
                                       var26 = ((ChoiceGroup)var21.get(var31)).getSelectedIndex();
                                       if ((var39 = ((ChoiceGroup)var21.get(var31)).getString(var26).toLowerCase()).equals("small")) {
                                          this.a(8);
                                       } else if (var39.startsWith("medium")) {
                                          this.a(0);
                                       } else if (var39.startsWith("large")) {
                                          this.a(16);
                                       }

                                       this.a = this.a[this.a.j];
                                       this.b = this.a[this.a.k];
                                       this.c = this.a[this.a.l];
                                       this.d = this.a[this.a.m];
                                       this.e = this.a[this.a.n];
                                       this.f = this.a[this.a.o];
                                       this.g = this.a[this.a.p];
                                       this.a(this.a.a);
                                       this.a(this.a.q, this.a.r, this.a.h, this.a.i);
                                       System.gc();
                                       this.g = this.a.a("Options", this);
                                       Display.getDisplay(this.a).setCurrent(this.g);
                                       this.d();
                                       return;
                                    }
                                 }

                                 return;
                              }
                           } else {
                              if (var2 == this.j) {
                                 if (var3.equals("back")) {
                                    this.g = this.a.a("Options", this);
                                    Display.getDisplay(this.a).setCurrent(this.g);
                                    return;
                                 }

                                 if (var3.equals("select")) {
                                    if ((var24 = (List)var2).getSelectedIndex() == -1) {
                                       return;
                                    }

                                    this.a.c = var24.getString(var24.getSelectedIndex());
                                    this.k = this.a.a("Font Color", this);
                                    Display.getDisplay(this.a).setCurrent(this.k);
                                    return;
                                 }

                                 return;
                              }

                              if (var2 == this.k) {
                                 if (var3.equals("back")) {
                                    this.j = this.a.a("Font Text", this);
                                    Display.getDisplay(this.a).setCurrent(this.j);
                                    return;
                                 }

                                 if (var3.equals("save")) {
                                    var21 = (Form)var2;
                                    var31 = 0;

                                    for(var42 = false; var31 < var21.size(); ++var31) {
                                       if (var21.get(var31) instanceof ChoiceGroup) {
                                          if (this.a.c.toLowerCase().equals("regular text")) {
                                             this.a.j = ((ChoiceGroup)var21.get(var31)).getSelectedIndex();
                                          } else if (this.a.c.toLowerCase().equals("keywords")) {
                                             this.a.n = ((ChoiceGroup)var21.get(var31)).getSelectedIndex();
                                          } else if (this.a.c.toLowerCase().equals("strings")) {
                                             this.a.k = ((ChoiceGroup)var21.get(var31)).getSelectedIndex();
                                          } else if (this.a.c.toLowerCase().equals("comments")) {
                                             this.a.l = ((ChoiceGroup)var21.get(var31)).getSelectedIndex();
                                          } else if (this.a.c.toLowerCase().equals("braces")) {
                                             this.a.m = ((ChoiceGroup)var21.get(var31)).getSelectedIndex();
                                          } else if (this.a.c.toLowerCase().equals("tags")) {
                                             this.a.o = ((ChoiceGroup)var21.get(var31)).getSelectedIndex();
                                          } else if (this.a.c.toLowerCase().equals("directives")) {
                                             this.a.p = ((ChoiceGroup)var21.get(var31)).getSelectedIndex();
                                          }
                                          break;
                                       }
                                    }

                                    this.a = this.a[this.a.j];
                                    this.b = this.a[this.a.k];
                                    this.c = this.a[this.a.l];
                                    this.d = this.a[this.a.m];
                                    this.e = this.a[this.a.n];
                                    this.f = this.a[this.a.o];
                                    this.g = this.a[this.a.p];
                                    this.j = this.a.a("Font Text", this);
                                    Display.getDisplay(this.a).setCurrent(this.j);
                                    return;
                                 }
                              } else if (var2 == this.l) {
                                 if (var3.equals("back")) {
                                    this.g = this.a.a("Options", this);
                                    Display.getDisplay(this.a).setCurrent(this.g);
                                    return;
                                 }

                                 if (var3.equals("save")) {
                                    var19 = null;
                                    var4 = null;
                                    var30 = null;
                                    Form var44 = (Form)var2;

                                    for(var34 = 0; var34 < var44.size(); ++var34) {
                                       if (var44.get(var34).getLabel().toLowerCase().equals("red")) {
                                          var19 = (TextField)var44.get(var34);
                                       } else if (var44.get(var34).getLabel().toLowerCase().equals("green")) {
                                          var4 = (TextField)var44.get(var34);
                                       } else if (var44.get(var34).getLabel().toLowerCase().equals("blue")) {
                                          var30 = (TextField)var44.get(var34);
                                       }
                                    }

                                    if (var19 != null && var4 != null && var30 != null) {
                                       if (!var19.getString().trim().equals("") && !var4.getString().trim().equals("") && !var30.getString().trim().equals("")) {
                                          var34 = Integer.parseInt(var19.getString());
                                          var43 = Integer.parseInt(var4.getString());
                                          var8 = Integer.parseInt(var30.getString());
                                          if (var34 < 0) {
                                             var34 = 0;
                                          }

                                          if (var34 > 255) {
                                             var34 = 255;
                                          }

                                          if (var43 < 0) {
                                             var43 = 0;
                                          }

                                          if (var43 > 255) {
                                             var43 = 255;
                                          }

                                          if (var8 < 0) {
                                             var8 = 0;
                                          }

                                          if (var8 > 255) {
                                             var8 = 255;
                                          }

                                          if (this.b.equals("backgroundcolor")) {
                                             this.a.a = var34;
                                             this.a.b = var43;
                                             this.a.c = var8;
                                          } else if (this.b.equals("caretcolor")) {
                                             this.a.d = var34;
                                             this.a.e = var43;
                                             this.a.f = var8;
                                          }

                                          this.g = this.a.a("Options", this);
                                          Display.getDisplay(this.a).setCurrent(this.g);
                                          return;
                                       }

                                       this.a("All fields must be filled out", this.l);
                                       return;
                                    }

                                    this.a("Color items not found", this.l);
                                    return;
                                 }
                              } else if (var2 == this.m) {
                                 if (var3.equals("back")) {
                                    this.g = this.a.a("Options", this);
                                    Display.getDisplay(this.a).setCurrent(this.g);
                                    return;
                                 }

                                 if (var3.equals("select")) {
                                    if ((var24 = (List)var2).getSelectedIndex() == -1) {
                                       return;
                                    }

                                    this.a.a(var24.getSelectedIndex(), this.m, this.a.q, this.a.r);
                                    return;
                                 }

                                 if (var3.equals("save")) {
                                    int[] var46;
                                    if ((var46 = mx.a("hotkeys")) == null) {
                                       mx.a("hotkeys", -1, this.a.a());
                                    } else {
                                       if (var46.length != 1) {
                                          throw new lo("Overwrite default hotkeys", new Exception("Found " + var46.length + " mapping entries"));
                                       }

                                       if (mx.a("hotkeys", var46[0], this.a.a()) == -1) {
                                          this.a("Default hotkeys store was not found for overwritting", this.m);
                                       }
                                    }

                                    this.g = this.a.a("Options", this);
                                    Display.getDisplay(this.a).setCurrent(this.g);
                                    return;
                                 }
                              } else if (var2 == this.n) {
                                 if (var3.equals("back")) {
                                    this.g = this.a.a("Options", this);
                                    Display.getDisplay(this.a).setCurrent(this.g);
                                    return;
                                 }

                                 if (var3.equals("save")) {
                                    var23 = "";
                                    Form var47 = (Form)var2;

                                    for(var41 = 0; var41 < var47.size(); ++var41) {
                                       if (var47.get(var41).getLabel().toLowerCase().equals("levels")) {
                                          var23 = ((TextField)var47.get(var41)).getString();
                                       }
                                    }

                                    if (var23.equals("")) {
                                       this.a("Please enter the number of undo levels.", this.n);
                                       return;
                                    }

                                    if (Integer.parseInt(var23) < 0) {
                                       this.a("Please specify a number between 0 and 999.", this.n);
                                       return;
                                    }

                                    this.a.s = Integer.parseInt(var23);
                                    this.a.e(this.a.s);
                                    this.g = this.a.a("Options", this);
                                    Display.getDisplay(this.a).setCurrent(this.g);
                                    return;
                                 }
                              } else {
                                 Display.getDisplay(this.a).setCurrent(this.a);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

   }

   private void e() {
      if (this.n == 0) {
         switch(this.m) {
         case 0:
            if (!this.a.a()) {
               this.r = this.a.a("New Project", this);
               Display.getDisplay(this.a).setCurrent(this.r);
            } else {
               this.e = this.a.a("Question", this);
               Display.getDisplay(this.a).setCurrent(this.e);
            }

            return;
         case 1:
            if (!this.a.a()) {
               this.a.b = this.a.a;
               this.s = this.a.a("Open Project", this);
               Display.getDisplay(this.a).setCurrent(this.s);
            } else {
               this.e = this.a.a("Question", this);
               Display.getDisplay(this.a).setCurrent(this.e);
            }

            return;
         case 2:
            if (!this.a.b.equals("")) {
               this.b(this.a.a, this.a.b);
               this.h = false;
               Display.getDisplay(this.a).setCurrent(this);
               this.d();
               return;
            }
            break;
         case 3:
            if (!this.a.b.equals("")) {
               this.t = this.a.a("Projects", this);
               Display.getDisplay(this.a).setCurrent(this.t);
               return;
            }
            break;
         case 4:
            if (!this.a.b.equals("")) {
               this.a.startBuildTime = System.currentTimeMillis();
               this.F = this.a.a("Output", this);
               this.F.addCommand(this.a.cancelCommand);
               this.F.setCommandListener(this.a);
               Display.getDisplay(this.a).setCurrent(this.F);
               this.k = true;
               (new Thread(this)).start();
               return;
            }
            break;
         case 5:
            this.g = this.a.a("Options", this);
            Display.getDisplay(this.a).setCurrent(this.g);
            return;
         case 6:
            this.d = this.a.a("About", this);
            Display.getDisplay(this.a).setCurrent(this.d);
            return;
         case 7:
            if (!this.a.a()) {
               this.a.projectFolder = this.a.b.equals("") ? "" : this.a.a + this.a.b;
               this.a.activeFile = this.a.b + this.a.a + this.a.c;

               try {
                  SDK var10000 = this.a;
                  this.a.getClass();
                  var10000.writeProjectData((byte)0);
               } catch (IOException var1) {
               }

               this.a.destroyApp(true);
               return;
            }

            this.e = this.a.a("Question", this);
            Display.getDisplay(this.a).setCurrent(this.e);
         }

         return;
      } else {
         switch(this.m) {
         case 0:
            this.c();
            return;
         case 1:
            this.a.h();
            this.h = false;
            this.m = 0;
            Display.getDisplay(this.a).setCurrent(this);
            this.d();
            return;
         case 2:
            this.a.c(!this.a.c());
            this.h = false;
            this.m = this.a.c() ? 3 : 0;
            Display.getDisplay(this.a).setCurrent(this);
            this.d();
            return;
         case 3:
            this.a.g();
            this.h = false;
            this.m = 5;
            Display.getDisplay(this.a).setCurrent(this);
            this.d();
            return;
         case 4:
            this.a.e();
            this.a.c(false);
            this.h = false;
            this.m = 5;
            Display.getDisplay(this.a).setCurrent(this);
            return;
         case 5:
            this.a.f();
            this.h = false;
            this.m = 0;
            Display.getDisplay(this.a).setCurrent(this);
            this.d();
            return;
         case 6:
            this.f = this.a.a("FindReplace", this);
            Display.getDisplay(this.a).setCurrent(this.f);
            return;
         case 7:
            this.o = this.a.a("InsertCode", this);
            Display.getDisplay(this.a).setCurrent(this.o);
         }
      }

   }

   private void f() {
      this.o = 0;

      try {
         Class.forName("com.siemens.mp.game.Light");
         this.o = -4;
      } catch (ClassNotFoundException var5) {
         try {
            Class.forName("com.motorola.phonebook.PhoneBookRecord");
            if (this.getKeyName(-21).toUpperCase().indexOf("SOFT") < 0) {
               this.o = 22;
               return;
            }

            this.o = -22;
         } catch (ClassNotFoundException var4) {
            try {
               boolean var1;
               if (this.getKeyName(21).toUpperCase().indexOf("SOFT") >= 0) {
                  this.o = 22;
                  var1 = false;
               }

               if (this.getKeyName(-6).toUpperCase().indexOf("SOFT") >= 0) {
                  this.o = -7;
                  var1 = false;
               }
            } catch (Exception var3) {
            }

            for(int var6 = -127; var6 < 127; ++var6) {
               try {
                  if (this.getKeyName(var6).toUpperCase().indexOf("SOFT") >= 0 && this.getKeyName(var6).indexOf("2") >= 0) {
                     this.o = var6;
                  }
               } catch (Exception var2) {
                  this.o = -7;
               }
            }
         }

      }
   }

   static Displayable a(qe var0, Displayable var1) {
      return var0.t = var1;
   }

   static ix a(qe var0) {
      return var0.a;
   }

   static Displayable a(qe var0) {
      return var0.t;
   }

   static SDK a(qe var0) {
      return var0.a;
   }
}
