public class fl {
   private static final n a = new n();
   protected it[] a;
   protected int a;

   public fl(int var1) {
      this.a = new it[var1];
      this.a = var1;
   }

   public fl(fl var1) {
      this(var1.a);
      this.a(var1);
   }

   public void a(int var1) {
      if (var1 > this.a.length) {
         this.a = new it[var1];
      } else {
         for(int var2 = 0; var2 < this.a.length; ++var2) {
            this.a[var2] = null;
         }
      }

      this.a = var1;
   }

   public final void a(fl var1) {
      if (this.a < var1.a) {
         throw new IllegalArgumentException("Variable frame is too small [" + this.a + "] compared to other frame [" + var1.a + "]");
      } else {
         System.arraycopy(var1.a, 0, this.a, 0, var1.a);
      }
   }

   public final boolean a(fl var1, boolean var2) {
      if (this.a != var1.a) {
         throw new IllegalArgumentException("Variable frames have different sizes [" + this.a + "] and [" + var1.a + "]");
      } else {
         boolean var3 = false;

         for(int var4 = 0; var4 < this.a; ++var4) {
            it var5 = this.a[var4];
            it var6 = var1.a[var4];
            if (var5 != null && var6 != null && var5.a() == var6.a()) {
               var6 = var5.a(var6);
               var3 = var3 || !var5.equals(var6);
               this.a[var4] = var6;
            } else {
               var3 = var3 || var5 != null;
               this.a[var4] = null;
               if (var2) {
                  var1.a[var4] = null;
               }
            }
         }

         return var3;
      }
   }

   public final int a() {
      return this.a;
   }

   public final it a(int var1) {
      if (var1 >= 0 && var1 < this.a) {
         return this.a[var1];
      } else {
         throw new IndexOutOfBoundsException("Variable index [" + var1 + "] out of bounds [" + this.a + "]");
      }
   }

   public void a(int var1, it var2) {
      if (var1 >= 0 && var1 < this.a) {
         this.a[var1] = var2;
         if (var2.a()) {
            this.a[var1 + 1] = a;
         }

      } else {
         throw new IndexOutOfBoundsException("Variable index [" + var1 + "] out of bounds [" + this.a + "]");
      }
   }

   public it b(int var1) {
      if (var1 >= 0 && var1 < this.a) {
         return this.a[var1];
      } else {
         throw new IndexOutOfBoundsException("Variable index [" + var1 + "] out of bounds [" + this.a + "]");
      }
   }

   public final ps a(int var1) {
      return this.b(var1).a();
   }

   public final dt a(int var1) {
      return this.b(var1).a();
   }

   public final eb a(int var1) {
      return this.b(var1).a();
   }

   public final oh a(int var1) {
      return this.b(var1).a();
   }

   public final nc a(int var1) {
      return this.b(var1).a();
   }

   public final ev a(int var1) {
      return this.b(var1).a();
   }

   public boolean equals(Object var1) {
      if (var1 != null && this.getClass() == var1.getClass()) {
         fl var5 = (fl)var1;
         if (this.a != var5.a) {
            return false;
         } else {
            for(int var2 = 0; var2 < this.a; ++var2) {
               it var3 = this.a[var2];
               it var4 = var5.a[var2];
               if (var3 != null && var4 != null && var3.a() == var4.a() && !var3.equals(var4)) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int var1 = this.a;

      for(int var2 = 0; var2 < this.a; ++var2) {
         it var3;
         if ((var3 = this.a[var2]) != null) {
            var1 ^= var3.hashCode();
         }
      }

      return var1;
   }

   public String toString() {
      StringBuffer var1 = new StringBuffer();

      for(int var2 = 0; var2 < this.a; ++var2) {
         it var3 = this.a[var2];
         var1 = var1.append('[').append(var3 == null ? "empty" : var3.toString()).append(']');
      }

      return var1.toString();
   }
}
