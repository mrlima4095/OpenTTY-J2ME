import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public abstract class qd {
   public static final qd a = new oi("V");
   public static final qd b = new oi("B");
   public static final qd c = new oi("C");
   public static final qd d = new oi("D");
   public static final qd e = new oi("F");
   public static final qd f = new oi("I");
   public static final qd g = new oi("J");
   public static final qd h = new oi("S");
   public static final qd i = new oi("Z");
   private pm[] a = null;
   protected gw[] a = null;
   Hashtable a = null;
   private gw[] b = null;
   private static gw[] c = new gw[0];
   protected cm[] a = null;
   private qd[] a = null;
   private boolean a = false;
   private qd j = null;
   private boolean b = false;
   private qd k = null;
   private boolean c = false;
   private qd l = null;
   private qd[] b = null;
   private String a = null;
   private boolean d = false;
   private qd m = null;
   private static final Vector a = new Vector();
   private qd n = null;
   private final Hashtable b = new Hashtable();
   private static final qd[] c;

   public final pm[] b() {
      if (this.a == null) {
         this.a = this.a();
      }

      return this.a;
   }

   protected abstract pm[] a();

   private gw[] c() {
      if (this.a == null) {
         this.a = this.a();
      }

      return this.a;
   }

   protected abstract gw[] a();

   public final gw[] a(String var1) {
      if (this.a == null) {
         Hashtable var2 = new Hashtable();
         gw[] var3 = this.c();

         for(int var4 = 0; var4 < var3.length; ++var4) {
            gw var5;
            String var6 = (var5 = var3[var4]).a_();
            Object var7;
            if ((var7 = var2.get(var6)) == null) {
               var2.put(var6, var5);
            } else if (var7 instanceof gw) {
               Vector var8;
               (var8 = new Vector()).addElement(var7);
               var8.addElement(var5);
               var2.put(var6, var8);
            } else {
               ((Vector)var7).addElement(var5);
            }
         }

         Enumeration var10 = var2.keys();

         while(var10.hasMoreElements()) {
            Object var11 = var10.nextElement();
            Object var12;
            if ((var12 = var2.get(var11)) instanceof gw) {
               var2.put(var11, new gw[]{(gw)var12});
            } else {
               Vector var13;
               gw[] var14 = new gw[(var13 = (Vector)var12).size()];
               var13.copyInto(var14);
               var2.put(var11, var14);
            }
         }

         this.a = var2;
      }

      gw[] var9;
      return (var9 = (gw[])((gw[])this.a.get(var1))) == null ? c : var9;
   }

   public final gw[] b() {
      if (this.b == null) {
         Hashtable var1 = new Hashtable();
         this.a(var1);
         gw[] var2 = new gw[var1.size()];
         int var3 = 0;

         for(Enumeration var4 = var1.elements(); var4.hasMoreElements(); ++var3) {
            var2[var3] = (gw)var4.nextElement();
         }

         this.b = var2;
      }

      return this.b;
   }

   public static boolean a(Object[] var0, Object[] var1) {
      if (var0 == var1) {
         return true;
      } else if (var0 != null && var1 != null) {
         int var2 = var0.length;
         if (var1.length != var2) {
            return false;
         } else {
            int var3 = 0;

            while(true) {
               if (var3 >= var2) {
                  return true;
               }

               Object var4 = var0[var3];
               Object var5 = var1[var3];
               if (var4 == null) {
                  if (var5 != null) {
                     break;
                  }
               } else if (!var4.equals(var5)) {
                  break;
               }

               ++var3;
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private void a(Hashtable var1) {
      gw[] var2 = this.c();

      for(int var3 = 0; var3 < var2.length; ++var3) {
         gw var4 = var2[var3];
         String var5 = var4.a_() + var4.a();
         gw var6;
         if ((var6 = (gw)var1.get(var5)) == null || var6.b()) {
            var1.put(var5, var4);
         }
      }

      qd var7;
      if ((var7 = this.g()) != null) {
         var7.a(var1);
      }

      qd[] var8 = this.d();

      for(int var9 = 0; var9 < var8.length; ++var9) {
         var8[var9].a(var1);
      }

   }

   public final boolean a(String var1, qd[] var2) {
      gw[] var4 = this.a(var1);

      for(int var5 = 0; var5 < var4.length; ++var5) {
         if (a((Object[])var4[var5].a(), (Object[])var2)) {
            return true;
         }
      }

      return false;
   }

   public final cm[] c() {
      if (this.a == null) {
         this.a = this.a();
      }

      return this.a;
   }

   protected abstract cm[] a();

   public cm[] b() {
      return new cm[0];
   }

   public final qd[] c() {
      if (this.a == null) {
         this.a = this.a();
      }

      return this.a;
   }

   protected abstract qd[] a();

   public final qd e() {
      if (!this.a) {
         this.j = this.a();
         this.a = true;
      }

      return this.j;
   }

   protected abstract qd a();

   public final qd f() {
      if (!this.b) {
         this.k = this.b();
         this.b = true;
      }

      return this.k;
   }

   protected abstract qd b();

   public final qd g() {
      if (!this.c) {
         this.l = this.c();
         this.c = true;
         if (this.l != null && this.l.b(this)) {
            throw new ng("Class circularity detected for \"" + nq.c(this.b()) + "\"", (aq)null);
         }
      }

      return this.l;
   }

   protected abstract qd c();

   public abstract pe a();

   public abstract boolean a();

   public final qd[] d() {
      if (this.b == null) {
         this.b = this.b();

         for(int var1 = 0; var1 < this.b.length; ++var1) {
            if (this.b[var1].c(this)) {
               throw new ng("Interface circularity detected for \"" + nq.c(this.b()) + "\"", (aq)null);
            }
         }
      }

      return this.b;
   }

   protected abstract qd[] b();

   public abstract boolean c();

   public final String b() {
      if (this.a == null) {
         this.a = this.a();
      }

      return this.a;
   }

   protected abstract String a();

   public static String[] a(qd[] var0) {
      String[] var1 = new String[var0.length];

      for(int var2 = 0; var2 < var0.length; ++var2) {
         var1[var2] = var0[var2].b();
      }

      return var1;
   }

   public abstract boolean b();

   public abstract boolean d();

   public abstract boolean e();

   public abstract boolean f();

   public final qd h() {
      if (!this.d) {
         this.m = this.d();
         this.d = true;
      }

      return this.m;
   }

   protected abstract qd d();

   public String toString() {
      return nq.c(this.b());
   }

   public final boolean a(qd var1) {
      if (this == var1) {
         return true;
      } else {
         String var2;
         if ((var2 = var1.b() + this.b()).length() == 2 && a.contains(var2)) {
            return true;
         } else if (var1.b(this)) {
            return true;
         } else if (var1.c(this)) {
            return true;
         } else if (var1 == a && !this.e()) {
            return true;
         } else if (var1.b() && this.b().equals("Ljava/lang/Object;")) {
            return true;
         } else {
            if (var1.d()) {
               if (this.b().equals("Ljava/lang/Object;")) {
                  return true;
               }

               if (this.d()) {
                  qd var4 = this.h();
                  qd var3 = var1.h();
                  if (!var4.e() && var4.a(var3)) {
                     return true;
                  }
               }
            }

            return false;
         }
      }
   }

   private boolean b(qd var1) {
      for(qd var2 = this.g(); var2 != null; var2 = var2.g()) {
         if (var2 == var1) {
            return true;
         }
      }

      return false;
   }

   private boolean c(qd var1) {
      for(qd var5 = this; var5 != null; var5 = var5.g()) {
         qd[] var2 = var5.d();

         for(int var3 = 0; var3 < var2.length; ++var3) {
            qd var4;
            if ((var4 = var2[var3]) == var1 || var4.c(var1)) {
               return true;
            }
         }
      }

      return false;
   }

   public final qd a(int var1, qd var2) {
      qd var4 = this;

      for(int var3 = 0; var3 < var1; ++var3) {
         var4 = var4.a(var2);
      }

      return var4;
   }

   public final qd a(qd var1) {
      if (this.n == null) {
         this.n = new w(this, var1, this);
      }

      return this.n;
   }

   final qd[] a(String var1) {
      qd[] var2;
      if ((var2 = (qd[])((qd[])this.b.get(var1))) == null) {
         Vector var4 = new Vector();
         this.a(var1, var4);
         qd[] var3 = new qd[var4.size()];
         var4.copyInto(var3);
         var2 = var4.isEmpty() ? c : var3;
         this.b.put(var1, var2);
      }

      return var2;
   }

   private void a(String var1, Vector var2) {
      qd[] var3 = this.c();
      int var5;
      if (var1 == null) {
         for(int var4 = 0; var4 < var3.length; ++var4) {
            if (!var2.contains(var3[var4])) {
               var2.addElement(var3[var4]);
            }
         }
      } else {
         String var7 = nq.b(nq.c(this.b()) + '$' + var1);

         for(var5 = 0; var5 < var3.length; ++var5) {
            qd var6;
            if ((var6 = var3[var5]).b().equals(var7)) {
               if (!var2.contains(var6)) {
                  var2.addElement(var6);
               }

               return;
            }
         }
      }

      qd var8;
      if ((var8 = this.g()) != null) {
         var8.a(var1, var2);
      }

      qd[] var9 = this.d();

      for(var5 = 0; var5 < var9.length; ++var5) {
         var9[var5].a(var1, var2);
      }

      var8 = this.e();
      qd var10 = this.f();
      if (var8 != null) {
         var8.a(var1, var2);
      }

      if (var10 != null && var10 != var8) {
         var10.a(var1, var2);
      }

   }

   static {
      String[] var0 = new String[]{"BS", "BI", "SI", "CI", "BJ", "SJ", "CJ", "IJ", "BF", "SF", "CF", "IF", "JF", "BD", "SD", "CD", "ID", "JD", "FD"};

      for(int var1 = 0; var1 < var0.length; ++var1) {
         if (!a.contains(var0[var1])) {
            a.addElement(var0[var1]);
         }
      }

      c = new qd[0];
   }
}
