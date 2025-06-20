import java.util.Hashtable;

public abstract class dz {
   private String a;
   private static final Hashtable a = new Hashtable();

   protected dz(String var1) {
      if (var1 == null) {
         throw new NullPointerException();
      } else {
         this.a = var1;
         Class var2 = this.getClass();
         Hashtable var10000;
         Hashtable var3;
         if ((var3 = (Hashtable)a.get(var2)) != null) {
            var10000 = var3;
         } else {
            var3 = new Hashtable();
            a.put(var2, var3);
            var10000 = var3;
         }

         var10000.put(var1, this);
      }
   }

   public final boolean equals(Object var1) {
      return this == var1;
   }

   public final int hashCode() {
      return super.hashCode();
   }

   public String toString() {
      return this.a;
   }
}
