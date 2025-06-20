import java.util.Hashtable;

public final class jf extends bi {
   pm a = null;
   public ew a = null;
   Hashtable a = new Hashtable();

   public jf(aq var1, String var2, short var3, ct[] var4, gs[] var5, ew var6, il var7) {
      super(var1, var2, var3, new om(var1, 0), "<init>", var4, var5, var7);
      this.a = var6;
      if (var6 != null) {
         var6.a(this);
      }

   }

   public final String toString() {
      StringBuffer var1 = new StringBuffer();
      kg var2;
      if ((var2 = (kg)this.a()) instanceof ow) {
         var1.append(((ow)var2).b());
      } else if (var2 instanceof eo) {
         var1.append(((eo)var2).b());
      } else if (var2 instanceof er) {
         var1.append(((er)var2).b());
      } else if (var2 instanceof jl) {
         var1.append(((jl)var2).b());
      } else {
         var1.append(var2.b());
      }

      var1.append('(');
      ct[] var3 = this.a;

      for(int var4 = 0; var4 < var3.length; ++var4) {
         if (var4 > 0) {
            var1.append(", ");
         }

         var1.append(var3[var4].toString());
      }

      var1.append(')');
      return var1.toString();
   }

   public final void a(mq var1) {
      var1.a(this);
   }
}
