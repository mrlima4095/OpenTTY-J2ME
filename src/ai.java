import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public final class ai extends qd {
   private final nb a;
   private final ha a;
   private final short a;
   private final Hashtable b = new Hashtable();
   private final Hashtable c = new Hashtable();
   private final Hashtable d = new Hashtable();

   public ai(nb var1, ha var2) {
      this.a = var1;
      this.a = var2;
      this.a = var1.a;
   }

   protected final pm[] a() {
      Vector var1 = new Vector();
      Enumeration var2 = this.a.b.elements();

      while(var2.hasMoreElements()) {
         ig var3 = (ig)var2.nextElement();

         cn var6;
         try {
            var6 = this.a(var3);
         } catch (ClassNotFoundException var4) {
            throw new RuntimeException(var4.getMessage());
         }

         if (var6 instanceof pm) {
            var1.addElement(var6);
         }
      }

      pm[] var5 = new pm[var1.size()];
      var1.copyInto(var5);
      return var5;
   }

   protected final gw[] a() {
      Vector var1 = new Vector();
      Enumeration var2 = this.a.b.elements();

      while(var2.hasMoreElements()) {
         ig var3;
         if (((var3 = (ig)var2.nextElement()).a() & 4096) == 0) {
            cn var6;
            try {
               var6 = this.a(var3);
            } catch (ClassNotFoundException var4) {
               throw new RuntimeException(var4.getMessage());
            }

            if (var6 instanceof gw) {
               var1.addElement(var6);
            }
         }
      }

      gw[] var5 = new gw[var1.size()];
      var1.copyInto(var5);
      return var5;
   }

   protected final cm[] a() {
      cm[] var1 = new cm[this.a.a.size()];

      for(int var2 = 0; var2 < this.a.a.size(); ++var2) {
         try {
            mc var4 = (mc)this.a.a.elementAt(var2);
            cm var5;
            Object var10002;
            if ((var5 = (cm)this.b.get(var4)) != null) {
               var10002 = var5;
            } else {
               String var13 = this.a.b(var4.b());
               String var6 = this.a.b(var4.c());
               qd var15 = this.a(var6);
               dj var7 = null;
               gv[] var8 = var4.a();

               for(int var9 = 0; var9 < var8.length; ++var9) {
                  gv var10;
                  if ((var10 = var8[var9]) instanceof dj) {
                     var7 = (dj)var10;
                     break;
                  }
               }

               Object var17 = null;
               if (var7 != null) {
                  ib var12;
                  if (!((var12 = this.a.a(var7.a())) instanceof kx)) {
                     throw new RuntimeException("Unexpected constant pool info type \"" + var12.getClass().getName() + "\"");
                  }

                  var17 = ((kx)var12).a(this.a);
               }

               pe var16 = a(var4.a());
               pg var14 = new pg(this, var17, var13, var15, var4, var16);
               this.b.put(var4, var14);
               var10002 = var14;
            }

            var1[var2] = (cm)var10002;
         } catch (ClassNotFoundException var11) {
            throw new RuntimeException(var11.getMessage());
         }
      }

      return var1;
   }

   protected final qd[] a() {
      lg var1;
      if ((var1 = this.a.a()) == null) {
         return new qd[0];
      } else {
         Vector var5 = var1.a();
         Vector var2 = new Vector();
         Enumeration var6 = var5.elements();

         while(var6.hasMoreElements()) {
            ln var3;
            if ((var3 = (ln)var6.nextElement()).b == this.a.b) {
               try {
                  var2.addElement(this.a(var3.a));
               } catch (ClassNotFoundException var4) {
                  throw new ng(var4.getMessage(), (aq)null);
               }
            }
         }

         qd[] var7 = new qd[var2.size()];
         var2.copyInto(var7);
         return var7;
      }
   }

   protected final qd a() {
      lg var1;
      if ((var1 = this.a.a()) == null) {
         return null;
      } else {
         Enumeration var4 = var1.a().elements();

         ln var2;
         do {
            if (!var4.hasMoreElements()) {
               return null;
            }
         } while((var2 = (ln)var4.nextElement()).a != this.a.b);

         if (var2.b == 0) {
            return null;
         } else {
            try {
               return this.a(var2.b);
            } catch (ClassNotFoundException var3) {
               throw new ng(var3.getMessage(), (aq)null);
            }
         }
      }
   }

   protected final qd b() {
      lg var1;
      if ((var1 = this.a.a()) == null) {
         return null;
      } else {
         Enumeration var4 = var1.a().elements();

         ln var2;
         do {
            if (!var4.hasMoreElements()) {
               return null;
            }
         } while((var2 = (ln)var4.nextElement()).a != this.a.b);

         if (var2.b == 0) {
            return null;
         } else if ((var2.d & 8) != 0) {
            return null;
         } else {
            try {
               return this.a(var2.b);
            } catch (ClassNotFoundException var3) {
               throw new ng(var3.getMessage(), (aq)null);
            }
         }
      }
   }

   protected final qd c() {
      if (this.a.c == 0) {
         return null;
      } else {
         try {
            return this.a(this.a.c);
         } catch (ClassNotFoundException var1) {
            throw new ng(var1.getMessage(), (aq)null);
         }
      }
   }

   public final pe a() {
      return a(this.a);
   }

   public final boolean a() {
      return (this.a & 16) != 0;
   }

   protected final qd[] b() {
      return this.a(this.a.a);
   }

   public final boolean c() {
      return (this.a & 1024) != 0;
   }

   protected final String a() {
      return nq.b(this.a.a());
   }

   public final boolean b() {
      return (this.a & 512) != 0;
   }

   public final boolean d() {
      return false;
   }

   public final boolean e() {
      return false;
   }

   public final boolean f() {
      return false;
   }

   protected final qd d() {
      return null;
   }

   private qd a(short var1) {
      String var2;
      return this.a((var2 = this.a.a(var1)).charAt(0) == '[' ? var2 : 'L' + var2 + ';');
   }

   private qd a(String var1) {
      qd var2;
      if ((var2 = (qd)this.c.get(var1)) != null) {
         return var2;
      } else if ((var2 = this.a.b(var1)) == null) {
         throw new ClassNotFoundException(var1);
      } else {
         this.c.put(var1, var2);
         return var2;
      }
   }

   private qd[] a(short[] var1) {
      qd[] var2 = new qd[var1.length];

      for(int var3 = 0; var3 < var2.length; ++var3) {
         try {
            var2[var3] = this.a(var1[var3]);
         } catch (ClassNotFoundException var4) {
            throw new ng(var4.getMessage(), (aq)null);
         }
      }

      return var2;
   }

   private cn a(ig var1) {
      cn var2;
      if ((var2 = (cn)this.d.get(var1)) != null) {
         return var2;
      } else {
         String var10 = this.a.b(var1.b());
         gf var3 = new gf(this.a.b(var1.c()));
         qd var4 = this.a(var3.a);
         qd[] var5 = new qd[var3.a.length];

         for(int var6 = 0; var6 < var5.length; ++var6) {
            var5[var6] = this.a(var3.a[var6]);
         }

         qd[] var13 = null;
         gv[] var12 = var1.a();

         for(int var7 = 0; var7 < var12.length; ++var7) {
            gv var8;
            if ((var8 = var12[var7]) instanceof dx) {
               short[] var15;
               var13 = new qd[(var15 = ((dx)var8).a()).length];

               for(int var9 = 0; var9 < var15.length; ++var9) {
                  var13[var9] = this.a(var15[var9]);
               }
            }
         }

         qd[] var14 = var13 == null ? new qd[0] : var13;
         pe var16 = a(var1.a());
         Object var11;
         if (var10.equals("<init>")) {
            var11 = new pi(this, var5, var14, var16);
         } else {
            var11 = new ph(this, var10, var4, var1, var5, var14, var16);
         }

         this.d.put(var1, var11);
         return (cn)var11;
      }
   }

   private static pe a(short var0) {
      if ((var0 & 1) != 0) {
         return pe.d;
      } else if ((var0 & 4) != 0) {
         return pe.b;
      } else {
         return (var0 & 2) != 0 ? pe.a : pe.c;
      }
   }
}
