public final class jo extends mb {
   public int a;
   public ji[] a;
   public int b;
   public ji[] b;

   public jo() {
   }

   public jo(int var1, ji[] var2, ji[] var3) {
      this(var1, var2.length, var2, var3.length, var3);
   }

   private jo(int var1, int var2, ji[] var3, int var4, ji[] var5) {
      this.c = var1;
      this.a = var2;
      this.a = var3;
      this.b = var4;
      this.b = var5;
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ir var5) {
      for(int var6 = 0; var6 < this.a; ++var6) {
         this.a[var6].b(var1, var2, var3, var4, var6, var5);
      }

   }

   public final void b(aj var1, kn var2, ac var3, int var4, ir var5) {
      for(int var6 = 0; var6 < this.b; ++var6) {
         this.b[var6].a(var1, var2, var3, var4, var6, var5);
      }

   }

   public final int a() {
      return 255;
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ee var5) {
      var5.a(var1, var2, var3, var4, this);
   }

   public final boolean equals(Object var1) {
      if (!super.equals(var1)) {
         return false;
      } else {
         jo var5 = (jo)var1;
         if (this.c == var5.c && this.a == var5.a && this.b == var5.b) {
            int var2;
            ji var3;
            ji var4;
            for(var2 = 0; var2 < this.a; ++var2) {
               var3 = this.a[var2];
               var4 = var5.a[var2];
               if (!var3.equals(var4)) {
                  return false;
               }
            }

            for(var2 = 0; var2 < this.b; ++var2) {
               var3 = this.b[var2];
               var4 = var5.b[var2];
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

      int var2;
      for(var2 = 0; var2 < this.a; ++var2) {
         var1 ^= this.a[var2].hashCode();
      }

      for(var2 = 0; var2 < this.b; ++var2) {
         var1 ^= this.b[var2].hashCode();
      }

      return var1;
   }

   public final String toString() {
      StringBuffer var1 = (new StringBuffer(super.toString())).append("Var: ");

      int var2;
      for(var2 = 0; var2 < this.a; ++var2) {
         var1 = var1.append('[').append(this.a[var2].toString()).append(']');
      }

      var1.append(", Stack: ");

      for(var2 = 0; var2 < this.b; ++var2) {
         var1 = var1.append('[').append(this.b[var2].toString()).append(']');
      }

      return var1.toString();
   }
}
