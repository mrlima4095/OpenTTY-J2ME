import java.util.Vector;

public final class gf {
   public final String[] a;
   public final String a;

   public gf(String[] var1, String var2) {
      this.a = var1;
      this.a = var2;
   }

   public gf(String var1) {
      if (var1.charAt(0) != '(') {
         throw new RuntimeException();
      } else {
         int var2 = 1;

         Vector var3;
         int var4;
         for(var3 = new Vector(); var1.charAt(var2) != ')'; var2 = var4) {
            for(var4 = var2; var1.charAt(var4) == '['; ++var4) {
            }

            if ("BCDFIJSZ".indexOf(var1.charAt(var4)) != -1) {
               ++var4;
            } else {
               if (var1.charAt(var4) != 'L') {
                  throw new RuntimeException();
               }

               ++var4;

               while(var1.charAt(var4) != ';') {
                  ++var4;
               }

               ++var4;
            }

            var3.addElement(var1.substring(var2, var4));
         }

         var3.copyInto(this.a = new String[var3.size()]);
         ++var2;
         this.a = var1.substring(var2);
      }
   }

   public final String toString() {
      StringBuffer var1 = new StringBuffer("(");

      for(int var2 = 0; var2 < this.a.length; ++var2) {
         var1.append(this.a[var2]);
      }

      return var1.append(')').append(this.a).toString();
   }

   public static String a(String var0, String var1) {
      return '(' + var1 + var0.substring(1);
   }
}
