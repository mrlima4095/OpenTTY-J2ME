import java.util.Vector;

public final class iy {
   private final Vector a = new Vector();

   public final boolean a() {
      for(int var1 = 0; var1 < this.a.size(); ++var1) {
         if (((mw)this.a.elementAt(var1)).a()) {
            return true;
         }
      }

      return false;
   }

   public final boolean a(mw var1) {
      this.a.addElement(var1);
      return true;
   }

   public final mw a(int var1) {
      return (mw)this.a.elementAt(var1);
   }

   public final int a() {
      return this.a.size();
   }
}
