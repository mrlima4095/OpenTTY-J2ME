import java.util.Enumeration;
import java.util.Hashtable;

public final class es {
   private Hashtable a = new Hashtable();

   public final void a(String var1) {
      this.a.clear();
      String var2 = "";
      String var3 = null;
      int var4 = 0;
      int var5 = 0;
      int var6 = var1.trim().length();

      for(int var7 = 0; var7 < var6; ++var7) {
         if (var1.startsWith("<name>", var7)) {
            var4 = var7 + 6;
         } else if (var1.startsWith("</name>", var7)) {
            var2 = var1.substring(var4, var7);
         } else if (var1.startsWith("<code>", var7)) {
            var5 = var7 + 6;
         } else if (var1.startsWith("</code>", var7)) {
            var3 = var1.substring(var5, var7);
            this.a.put(var2, var3);
         }
      }

   }

   public final String a() {
      String var1 = "";

      String var3;
      for(Enumeration var2 = this.a.keys(); var2.hasMoreElements(); var1 = var1 + "<code>" + (String)this.a.get(var3) + "</code>") {
         var3 = (String)var2.nextElement();
         var1 = var1 + "<name>" + var3 + "</name>";
      }

      return var1;
   }

   public final String[] a() {
      String[] var1 = new String[this.a.size() + 2];
      int var2 = 2;

      for(Enumeration var3 = this.a.keys(); var3.hasMoreElements(); ++var2) {
         var1[var2] = (String)var3.nextElement();
      }

      return var1;
   }

   public final String a(String var1) {
      return var1.equals("") ? "" : (String)this.a.get(var1);
   }

   public final void a(String var1, String var2) {
      if (this.a.containsKey(var1)) {
         this.a.remove(var1);
      }

      this.a.put(var1, var2);
   }

   public final void b(String var1) {
      this.a.remove(var1);
   }
}
