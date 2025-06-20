import java.io.Reader;

public final class cv extends Reader {
   private Reader a;
   private int a = -1;
   private boolean a = false;

   public cv(Reader var1) {
      super(var1);
      this.a = var1;
   }

   public final int read() {
      int var1;
      if (this.a == -1) {
         var1 = this.a.read();
      } else {
         var1 = this.a;
         this.a = -1;
      }

      if (var1 == 92 && !this.a) {
         if ((var1 = this.a.read()) != 117) {
            this.a = var1;
            this.a = true;
            return 92;
         } else {
            while((var1 = this.a.read()) != -1) {
               if (var1 != 117) {
                  char[] var2;
                  (var2 = new char[4])[0] = (char)var1;
                  if (this.a.read(var2, 1, 3) != 3) {
                     throw new RuntimeException("UnicodeUnescapeException: Incomplete escape sequence");
                  }

                  try {
                     return '\uffff' & Integer.parseInt(new String(var2), 16);
                  } catch (NumberFormatException var3) {
                     throw new RuntimeException("UnicodeUnescapeException: Invalid escape sequence \"\\u" + new String(var2) + "\"");
                  }
               }
            }

            throw new RuntimeException("UnicodeUnescapeException: Incomplete escape sequence");
         }
      } else {
         this.a = false;
         return var1;
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
