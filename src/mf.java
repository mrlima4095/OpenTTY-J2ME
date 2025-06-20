public final class mf extends mb {
   public int a;
   public ji[] a;

   public mf() {
   }

   public mf(int var1) {
      this.a = var1 + 1 - 252;
   }

   public mf(ji[] var1) {
      this(var1.length, var1);
   }

   private mf(int var1, ji[] var2) {
      this.a = var1;
      this.a = var2;
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ir var5) {
      for(int var6 = 0; var6 < this.a; ++var6) {
         this.a[var6].a(var1, var2, var3, var4, var5);
      }

   }

   public final int a() {
      return 252 + this.a - 1;
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ee var5) {
      var5.a(var1, var2, var3, var4, this);
   }

   public final boolean equals(Object var1) {
      if (!super.equals(var1)) {
         return false;
      } else {
         mf var5 = (mf)var1;
         if (this.c == var5.c && this.a == var5.a) {
            for(int var2 = 0; var2 < this.a; ++var2) {
               ji var3 = this.a[var2];
               ji var4 = var5.a[var2];
               if (!var3.equals(var4)) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      }
   }

   public final int hashCode() {
      int var1 = super.hashCode();

      for(int var2 = 0; var2 < this.a; ++var2) {
         var1 ^= this.a[var2].hashCode();
      }

      return var1;
   }

   public final String toString() {
      StringBuffer var1 = (new StringBuffer(super.toString())).append("Var: ...");

      for(int var2 = 0; var2 < this.a; ++var2) {
         var1 = var1.append('[').append(this.a[var2].toString()).append(']');
      }

      var1.append(", Stack: (empty)");
      return var1.toString();
   }
}
