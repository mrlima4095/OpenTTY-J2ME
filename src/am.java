import java.util.Enumeration;
import java.util.Vector;

final class am extends qd {
   private qd[] a;
   private final ft a;
   private final dr a;
   private final cx a;

   am(cx var1, ft var2, dr var3) {
      this.a = var1;
      this.a = var2;
      this.a = var3;
      this.a = null;
   }

   protected final gw[] a() {
      gw[] var1 = new gw[this.a.b.size()];
      int var2 = 0;

      for(Enumeration var3 = this.a.b.elements(); var3.hasMoreElements(); var1[var2++] = this.a.a((kl)var3.nextElement())) {
      }

      return var1;
   }

   protected final qd[] a() {
      if (this.a == null) {
         qd[] var1 = new qd[this.a.c.size()];
         int var2 = 0;

         for(Enumeration var3 = this.a.c.elements(); var3.hasMoreElements(); var1[var2++] = cx.a((cx)this.a, (dr)((ft)var3.nextElement()))) {
         }

         this.a = var1;
      }

      return this.a;
   }

   protected final qd a() {
      Object var1;
      for(var1 = this.a; !(var1 instanceof ht); var1 = ((hu)var1).a()) {
         if (var1 instanceof fv) {
            return null;
         }
      }

      return cx.a((cx)this.a, (dr)((ft)((hu)var1).a()));
   }

   protected final qd b() {
      ft var1;
      return (var1 = (ft)cx.a((dr)this.a)) == null ? null : cx.a((cx)this.a, (dr)var1);
   }

   protected final String a() {
      if (this.a instanceof ow) {
         return nq.b(((ow)this.a).b());
      } else if (this.a instanceof eo) {
         return nq.b(((eo)this.a).b());
      } else if (this.a instanceof er) {
         return nq.b(((er)this.a).b());
      } else if (this.a instanceof jl) {
         return nq.b(((jl)this.a).b());
      } else if (this.a instanceof in) {
         return nq.b(((in)this.a).b());
      } else {
         return this.a instanceof hq ? nq.b(((hq)this.a).b()) : nq.b(this.a.b());
      }
   }

   public final boolean d() {
      return false;
   }

   protected final qd d() {
      throw new RuntimeException("SNO: Non-array type has no component type");
   }

   public final boolean e() {
      return false;
   }

   public final boolean f() {
      return false;
   }

   protected final pm[] a() {
      if (!(this.a instanceof kg)) {
         return new pm[0];
      } else {
         jf[] var1;
         pm[] var2 = new pm[(var1 = ((kg)this.a).a()).length];

         for(int var3 = 0; var3 < var1.length; ++var3) {
            var2[var3] = this.a.a(var1[var3]);
         }

         return var2;
      }
   }

   protected final cm[] a() {
      Vector var2;
      int var3;
      ov var4;
      int var5;
      cm[] var7;
      gb var8;
      cm[] var9;
      if (this.a instanceof kg) {
         kg var6 = (kg)this.a;
         var2 = new Vector();

         for(var3 = 0; var3 < var6.d.size(); ++var3) {
            if ((var4 = (ov)var6.d.elementAt(var3)) instanceof gb) {
               var8 = (gb)var4;
               var9 = this.a.a(var8);

               for(var5 = 0; var5 < var9.length; ++var5) {
                  var2.addElement(var9[var5]);
               }
            }
         }

         var7 = new cm[var2.size()];
         var2.copyInto(var7);
         return var7;
      } else if (!(this.a instanceof df)) {
         throw new RuntimeException("SNO: AbstractTypeDeclaration is neither ClassDeclaration nor InterfaceDeclaration");
      } else {
         df var1 = (df)this.a;
         var2 = new Vector();

         for(var3 = 0; var3 < var1.a.size(); ++var3) {
            if ((var4 = (ov)var1.a.elementAt(var3)) instanceof gb) {
               var8 = (gb)var4;
               var9 = this.a.a(var8);

               for(var5 = 0; var5 < var9.length; ++var5) {
                  var2.addElement(var9[var5]);
               }
            }
         }

         var7 = new cm[var2.size()];
         var2.copyInto(var7);
         return var7;
      }
   }

   public final cm[] b() {
      if (!(this.a instanceof kg)) {
         return new cm[0];
      } else {
         cm[] var1 = new cm[((kg)this.a).a.size()];
         int var2 = 0;

         for(Enumeration var3 = ((kg)this.a).a.elements(); var3.hasMoreElements(); ++var2) {
            var1[var2] = (cm)var3.nextElement();
         }

         return var1;
      }
   }

   protected final qd c() {
      qd var2;
      if (this.a instanceof eo) {
         return (var2 = cx.a((cx)this.a, (nr)((eo)this.a).a)).b() ? cx.a(this.a).a : var2;
      } else if (this.a instanceof fe) {
         fe var1;
         if ((var1 = (fe)this.a).a == null) {
            return cx.a(this.a).a;
         } else {
            if ((var2 = cx.a((cx)this.a, (nr)var1.a)).b()) {
               cx.a(this.a, "\"" + var2.toString() + "\" is an interface; classes can only extend a class", this.a.a());
            }

            return var2;
         }
      } else {
         return null;
      }
   }

   public final pe a() {
      return cx.a(this.a.a);
   }

   public final boolean a() {
      return (this.a.a & 16) != 0;
   }

   protected final qd[] b() {
      if (this.a instanceof eo) {
         qd var5;
         return (var5 = cx.a((cx)this.a, (nr)((eo)this.a).a)).b() ? new qd[]{var5} : new qd[0];
      } else {
         qd[] var2;
         int var3;
         if (this.a instanceof fe) {
            fe var4;
            var2 = new qd[(var4 = (fe)this.a).a.length];

            for(var3 = 0; var3 < var2.length; ++var3) {
               var2[var3] = cx.a((cx)this.a, (nr)var4.a[var3]);
               if (!var2[var3].b()) {
                  cx.a(this.a, "\"" + var2[var3].toString() + "\" is not an interface; classes can only implement interfaces", this.a.a());
               }
            }

            return var2;
         } else if (this.a instanceof df) {
            df var1;
            var2 = new qd[(var1 = (df)this.a).a.length];

            for(var3 = 0; var3 < var2.length; ++var3) {
               var2[var3] = cx.a((cx)this.a, (nr)var1.a[var3]);
               if (!var2[var3].b()) {
                  cx.a(this.a, "\"" + var2[var3].toString() + "\" is not an interface; interfaces can only extend interfaces", this.a.a());
               }
            }

            return var2;
         } else {
            throw new RuntimeException("SNO: AbstractTypeDeclaration is neither ClassDeclaration nor InterfaceDeclaration");
         }
      }
   }

   public final boolean c() {
      return this.a instanceof df || (this.a.a & 1024) != 0;
   }

   public final boolean b() {
      return this.a instanceof df;
   }
}
