import java.util.Enumeration;
import java.util.Vector;

final class al extends pm {
   private final jf a;
   private final cx a;

   al(cx var1, qd var2, jf var3) {
      var2.getClass();
      super(var2);
      this.a = var1;
      this.a = var3;
   }

   public final pe a() {
      switch(this.a.a & 7) {
      case 0:
         return pe.c;
      case 1:
         return pe.d;
      case 2:
         return pe.a;
      case 3:
      default:
         throw new RuntimeException("Invalid access");
      case 4:
         return pe.b;
      }
   }

   public final String a() {
      if (!((kg)this.a.a() instanceof kw)) {
         return super.a();
      } else {
         Vector var1 = new Vector();
         qd var2;
         if ((var2 = cx.a((cx)this.a, (dr)((kg)this.a.a())).f()) != null) {
            var1.addElement(var2.b());
         }

         Enumeration var4 = ((kg)this.a.a()).a.elements();

         while(var4.hasMoreElements()) {
            cm var3;
            if ((var3 = (cm)var4.nextElement()).a().startsWith("val$")) {
               var1.addElement(var3.a().b());
            }
         }

         ct[] var5 = this.a.a;

         for(int var6 = 0; var6 < var5.length; ++var6) {
            var1.addElement(cx.a((cx)this.a, (nr)var5[var6].a).b());
         }

         String[] var7 = new String[var1.size()];
         var1.copyInto(var7);
         return (new gf(var7, "V")).toString();
      }
   }

   public final qd[] a() {
      ct[] var1;
      qd[] var2 = new qd[(var1 = this.a.a).length];

      for(int var3 = 0; var3 < var1.length; ++var3) {
         var2[var3] = cx.a((cx)this.a, (nr)var1[var3].a);
      }

      return var2;
   }

   public final qd[] b() {
      qd[] var1 = new qd[this.a.a.length];

      for(int var2 = 0; var2 < var1.length; ++var2) {
         var1[var2] = cx.a((cx)this.a, (nr)this.a.a[var2]);
      }

      return var1;
   }

   public final String toString() {
      StringBuffer var1 = new StringBuffer();
      dr var2;
      if ((var2 = this.a.a()) instanceof ow) {
         var1.append(((ow)var2).b());
      } else if (var2 instanceof eo) {
         var1.append(((eo)var2).b());
      } else if (var2 instanceof er) {
         var1.append(((er)var2).b());
      } else if (var2 instanceof jl) {
         var1.append(((jl)var2).b());
      } else if (var2 instanceof in) {
         var1.append(((in)var2).b());
      } else if (var2 instanceof hq) {
         var1.append(((hq)var2).b());
      } else {
         var1.append(var2.b());
      }

      var1.append('(');
      ct[] var5 = this.a.a;

      for(int var3 = 0; var3 < var5.length; ++var3) {
         if (var3 != 0) {
            var1.append(", ");
         }

         try {
            var1.append(cx.a((cx)this.a, (nr)var5[var3].a).toString());
         } catch (ng var4) {
            var1.append("???");
         }
      }

      return var1.append(')').toString();
   }
}
