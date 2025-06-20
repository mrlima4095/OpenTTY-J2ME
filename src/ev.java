public final class ev extends aa {
   public static final ev a = new ev();
   private int[] a;

   private ev() {
   }

   public ev(int var1) {
      this.a = new int[]{var1};
   }

   private ev(int[] var1) {
      this.a = var1;
   }

   public final int b() {
      return this.a == null ? 0 : this.a.length;
   }

   public final int a(int var1) {
      return this.a[var1];
   }

   private boolean a(int var1) {
      if (this.a != null) {
         for(int var2 = 0; var2 < this.a.length; ++var2) {
            if (this.a[var2] == var1) {
               return true;
            }
         }
      }

      return false;
   }

   public final it a(ev var1) {
      if (this.a == null) {
         return var1;
      } else if (var1.a == null) {
         return this;
      } else {
         int var2 = this.a.length;

         for(int var3 = 0; var3 < var1.a.length; ++var3) {
            if (!this.a(var1.a[var3])) {
               ++var2;
            }
         }

         if (var2 == var1.a.length) {
            return var1;
         } else {
            int[] var5 = new int[var2];
            var2 = 0;

            int var4;
            for(var4 = 0; var4 < this.a.length; ++var4) {
               if (!var1.a(this.a[var4])) {
                  var5[var2++] = this.a[var4];
               }
            }

            for(var4 = 0; var4 < var1.a.length; ++var4) {
               var5[var2++] = var1.a[var4];
            }

            return new ev(var5);
         }
      }
   }

   public final ev a() {
      return this;
   }

   public final it a(it var1) {
      return this.a(var1.a());
   }

   public final int a() {
      return 6;
   }

   public final boolean equals(Object var1) {
      if (var1 != null && this.getClass() == var1.getClass()) {
         ev var3 = (ev)var1;
         if (this.a == var3.a) {
            return true;
         } else if (this.a != null && var3.a != null && this.a.length == var3.a.length) {
            for(int var2 = 0; var2 < var3.a.length; ++var2) {
               if (!this.a(var3.a[var2])) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public final int hashCode() {
      int var1 = this.getClass().hashCode();
      if (this.a != null) {
         for(int var2 = 0; var2 < this.a.length; ++var2) {
            var1 ^= this.a[var2];
         }
      }

      return var1;
   }

   public final String toString() {
      StringBuffer var1 = new StringBuffer("o:");
      if (this.a != null) {
         for(int var2 = 0; var2 < this.a.length; ++var2) {
            if (var2 > 0) {
               var1.append(',');
            }

            var1.append(this.a[var2]);
         }
      }

      return var1.toString();
   }
}
