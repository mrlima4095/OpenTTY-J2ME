import java.util.Hashtable;
import java.util.Vector;

public abstract class ha {
   public qd a;
   public qd b;
   public qd c;
   private final ha a;
   private final Hashtable a = new Hashtable();
   private final Vector a = new Vector();

   public ha(ha var1) {
      this.a = var1;
   }

   protected final void a() {
      try {
         this.a = this.b("Ljava/lang/Object;");
         this.b = this.b("Ljava/lang/String;");
         this.c = this.b("Ljava/lang/Class;");
      } catch (ClassNotFoundException var1) {
         throw new RuntimeException("Cannot load simple types");
      }
   }

   public final qd a(String var1) {
      try {
         return this.b(var1);
      } catch (ClassNotFoundException var2) {
         throw new RuntimeException("Cannot load simple types");
      }
   }

   public final qd b(String var1) {
      if (var1.length() == 1 && "VBCDFIJSZ".indexOf(var1.charAt(0)) != -1) {
         if (var1.equals("V")) {
            return qd.a;
         } else if (var1.equals("B")) {
            return qd.b;
         } else if (var1.equals("C")) {
            return qd.c;
         } else if (var1.equals("D")) {
            return qd.d;
         } else if (var1.equals("F")) {
            return qd.e;
         } else if (var1.equals("I")) {
            return qd.f;
         } else if (var1.equals("J")) {
            return qd.g;
         } else if (var1.equals("S")) {
            return qd.h;
         } else {
            return var1.equals("Z") ? qd.i : null;
         }
      } else {
         qd var2;
         if (this.a != null && (var2 = this.a.b(var1)) != null) {
            return var2;
         } else {
            synchronized(this) {
               if (this.a.contains(var1)) {
                  return null;
               }

               if ((var2 = (qd)this.a.get(var1)) != null) {
                  return var2;
               }

               if (nq.a(var1)) {
                  if (var1.charAt(0) != '[') {
                     throw new RuntimeException("Cannot determine component descriptor from non-array descriptor \"" + var1 + "\"");
                  }

                  if ((var2 = this.b(var1.substring(1))) == null) {
                     return null;
                  }

                  var2 = var2.a(this.a);
                  this.a.put(var1, var2);
                  return var2;
               }

               if ((var2 = this.c(var1)) == null) {
                  if (!this.a.contains(var1)) {
                     this.a.addElement(var1);
                  }

                  return null;
               }
            }

            if (!var2.b().equalsIgnoreCase(var1)) {
               throw new RuntimeException("\"findIClass()\" returned \"" + var2.b() + "\" instead of \"" + var1 + "\"");
            } else {
               return var2;
            }
         }
      }
   }

   protected abstract qd c(String var1);

   protected final void a(qd var1) {
      String var2 = var1.b();
      qd var3;
      if ((var3 = (qd)this.a.get(var2)) != null) {
         if (var3 != var1) {
            throw new RuntimeException("Non-identical definition of IClass \"" + var2 + "\"");
         }
      } else {
         this.a.put(var2, var1);
      }
   }
}
