import java.io.InputStream;
import java.io.Reader;

public final class ll extends Reader {
   private InputStream a;
   private char[] a = new char[]{'А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ж', 'З', 'И', 'Й', 'К', 'Л', 'М', 'Н', 'О', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Щ', 'Ъ', 'Ы', 'Ь', 'Э', 'Ю', 'Я', 'а', 'б', 'в', 'г', 'д', 'е', 'ж', 'з', 'и', 'й', 'к', 'л', 'м', 'н', 'о', 'п', 'р', 'с', 'т', 'у', 'ф', 'х', 'ц', 'ч', 'ш', 'щ', 'Ъ', 'ы', 'ь', 'э', 'ю', 'я'};

   public ll(InputStream var1) {
      this.a = var1;
   }

   public final int read() {
      int var1;
      if ((var1 = this.a.read()) == 184) {
         return 1105;
      } else if (var1 == 168) {
         return 1025;
      } else {
         return var1 >= 192 && var1 <= 255 ? this.a[var1 - 192] : var1;
      }
   }

   public final int read(char[] var1, int var2, int var3) {
      if (var3 == 0) {
         return 0;
      } else {
         int var4 = 0;

         int var5;
         while((var5 = this.read()) != -1) {
            var1[var2++] = (char)var5;
            ++var4;
            if (var4 >= var3) {
               break;
            }
         }

         return var4 == 0 ? -1 : var4;
      }
   }

   public final void close() {
   }
}
