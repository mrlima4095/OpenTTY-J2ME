import java.io.DataOutputStream;
import java.io.UTFDataFormatException;

public final class cg extends ib {
   private final String a;

   public cg(String var1) {
      if (var1 == null) {
         throw new RuntimeException();
      } else {
         this.a = var1;
      }
   }

   public final String a() {
      return this.a;
   }

   public final boolean a() {
      return false;
   }

   public final void a(DataOutputStream var1) {
      var1.writeByte(1);

      try {
         var1.writeUTF(this.a);
      } catch (UTFDataFormatException var2) {
         throw new NoClassDefFoundError("String constant too long to store in class file");
      }
   }

   public final boolean equals(Object var1) {
      return var1 instanceof cg && ((cg)var1).a.equals(this.a);
   }

   public final int hashCode() {
      return this.a.hashCode();
   }

   static String a(cg var0) {
      return var0.a;
   }
}
