import java.util.Vector;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.midlet.MIDlet;

public final class by extends Canvas {
   private int a;
   private final char[] a = new char[]{'ý', 'ü', 'û', 'ú', 'ù', 'ø', '÷', 'ö', 'õ', 'ô', 'ó', 'ò', ' ', 'ñ', 'ð', 'æ', 'ï', 'î', 'ì', 'í', 'ë', 'ê', 'é'};
   private final String[] a = new String[]{"Up", "Down", "Left", "Right", "Home", "End", "PgUp", "PgDown", "BackSpace", "Delete", "Insert", "Enter", "Space", "Edit Line", "Select", "Cut", "Copy", "Paste", "Undo", "Find", "Shift Left", "Shift Right", "Complete Word"};
   private Vector a;
   private Image a;
   private MIDlet a;
   private Displayable a;

   public by(MIDlet var1) {
      this.a = var1;
      this.a = new Vector();
   }

   public final void paint(Graphics var1) {
      if (this.a != null) {
         var1.drawImage(this.a, 0, 0, 0);
      }

   }

   public final void keyPressed(int var1) {
      int var3 = var1;
      int var2 = this.a;
      by var6 = this;
      int var4 = 0;

      while(true) {
         if (var4 >= var6.a.size()) {
            var6.a.addElement(new ez(var2, var3));
            var4 = var6.a.size();

            for(var2 = 0; var2 < var4; ++var2) {
               for(var3 = var2 + 1; var3 < var4; ++var3) {
                  if (((ez)var6.a.elementAt(var3)).b < ((ez)var6.a.elementAt(var2)).b) {
                     ez var5 = new ez(((ez)var6.a.elementAt(var2)).a, ((ez)var6.a.elementAt(var2)).b);
                     var6.a.setElementAt(new ez(((ez)var6.a.elementAt(var3)).a, ((ez)var6.a.elementAt(var3)).b), var2);
                     var6.a.setElementAt(var5, var3);
                  }
               }
            }

            System.gc();
            break;
         }

         if (((ez)var6.a.elementAt(var4)).b == var3) {
            ((ez)var6.a.elementAt(var4)).a = var2;
            break;
         }

         ++var4;
      }

      Display.getDisplay(this.a).setCurrent(this.a);
      System.gc();
   }

   public final void keyReleased(int var1) {
   }

   public final String[] a() {
      String[] var1 = new String[this.a.length];

      for(int var2 = 0; var2 < var1.length; ++var2) {
         String var3 = "";

         try {
            var3 = " (" + this.getKeyName(this.a(this.a[var2])) + ")";
         } catch (Exception var4) {
         }

         var1[var2] = this.a[var2] + var3;
      }

      return var1;
   }

   public final char a(int var1) {
      int var2 = this.a.size();
      int var3 = -1;

      while(var2 - var3 > 1) {
         int var4 = var3 + var2 >>> 1;
         if (((ez)this.a.elementAt(var4)).b > var1) {
            var2 = var4;
         } else {
            var3 = var4;
         }
      }

      if (var3 != -1 && ((ez)this.a.elementAt(var3)).b == var1) {
         return this.a[((ez)this.a.elementAt(var3)).a];
      } else {
         return 'þ';
      }
   }

   private int a(char var1) {
      for(int var2 = 0; var2 < this.a.length; ++var2) {
         if (this.a[var2] == var1) {
            for(int var3 = 0; var3 < this.a.size(); ++var3) {
               ez var4;
               if ((var4 = (ez)this.a.elementAt(var3)).a == var2) {
                  return var4.b;
               }
            }
         }
      }

      return 0;
   }

   public final void a(int var1, Displayable var2, int var3, int var4) {
      Display.getDisplay(this.a).setCurrent(this);
      this.a = var2;
      this.a = var1;
      this.a = Image.createImage(var3, var4);
      Graphics var5;
      (var5 = this.a.getGraphics()).setColor(255, 255, 255);
      var5.fillRect(0, 0, this.a.getWidth(), this.a.getHeight());
      var5.setColor(0, 0, 0);
      var5.drawString("PRESS KEY FOR", this.a.getWidth() / 2, this.a.getHeight() / 2, 33);
      var5.drawString(this.a[var1], this.a.getWidth() / 2, this.a.getHeight() / 2, 17);
      this.repaint(0, 0, this.a.getWidth(), this.a.getHeight());
      this.serviceRepaints();
   }

   public final void a(String var1) {
      this.a.removeAllElements();
      String var2 = "";
      int var3 = 0;
      int var4 = 0;
      int var5 = var1.trim().length();

      for(int var6 = 0; var6 < var5; ++var6) {
         if (var1.startsWith("<keyindex>", var6)) {
            var3 = var6 + 10;
         } else if (var1.startsWith("</keyindex>", var6)) {
            var2 = var1.substring(var3, var6);
         } else if (var1.startsWith("<value>", var6)) {
            var4 = var6 + 7;
         } else if (var1.startsWith("</value>", var6)) {
            this.a.addElement(new ez(Integer.parseInt(var2), Integer.parseInt(var1.substring(var4, var6))));
         }
      }

   }

   public final String a() {
      String var1 = "";

      for(int var2 = 0; var2 < this.a.size(); ++var2) {
         ez var3 = (ez)this.a.elementAt(var2);
         var1 = var1 + "<keyindex>" + var3.a + "</keyindex>";
         var1 = var1 + "<value>" + var3.b + "</value>";
      }

      return var1;
   }
}
