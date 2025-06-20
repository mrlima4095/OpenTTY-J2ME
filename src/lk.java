public class lk {
   private static final n a = new n();
   protected it[] a;
   private int a;
   private int b;

   public lk(int var1) {
      this.a = new it[var1];
   }

   public lk(lk var1) {
      this(var1.a.length);
      this.a(var1);
   }

   public void a(int var1) {
      if (var1 > this.a.length) {
         this.a = new it[var1];
      }

      this.a();
      this.b = 0;
   }

   public final void a(lk var1) {
      if (var1.a.length > this.a.length) {
         this.a = new it[var1.a.length];
      }

      System.arraycopy(var1.a, 0, this.a, 0, var1.a);
      this.a = var1.a;
      this.b = var1.b;
   }

   public final boolean a(lk var1) {
      if (this.a != var1.a) {
         throw new IllegalArgumentException("Stacks have different current sizes [" + this.a + "] and [" + var1.a + "]");
      } else {
         boolean var2 = false;

         for(int var3 = 0; var3 < this.a; ++var3) {
            it var4;
            if ((var4 = this.a[var3]) != null) {
               it var5 = null;
               it var6;
               if ((var6 = var1.a[var3]) != null) {
                  var5 = var4.a(var6);
               }

               var2 = var2 || !var4.equals(var5);
               this.a[var3] = var5;
            }
         }

         if (this.b < var1.b) {
            this.b = var1.b;
         }

         return var2;
      }
   }

   public void a() {
      for(int var1 = 0; var1 < this.a; ++var1) {
         this.a[var1] = null;
      }

      this.a = 0;
   }

   public final int a() {
      return this.a;
   }

   public final it b(int var1) {
      return this.a[var1];
   }

   public final it c(int var1) {
      return this.a[this.a - var1 - 1];
   }

   public void c(it var1) {
      if (var1.a()) {
         this.a[this.a++] = a;
      }

      this.a[this.a++] = var1;
      if (this.b < this.a) {
         this.b = this.a;
      }

   }

   public it b() {
      it var1 = this.a[--this.a];
      this.a[this.a] = null;
      if (var1.a()) {
         this.a[--this.a] = null;
      }

      return var1;
   }

   public final ps a() {
      return this.b().a();
   }

   public final dt a() {
      return this.b().a();
   }

   public final eb a() {
      return this.b().a();
   }

   public final oh a() {
      return this.b().a();
   }

   public final nc a() {
      return this.b().a();
   }

   public void b() {
      this.a[--this.a] = null;
   }

   public void c() {
      this.a[--this.a] = null;
      this.a[--this.a] = null;
   }

   public void d() {
      this.a[this.a] = this.a[this.a - 1].a();
      ++this.a;
      if (this.b < this.a) {
         this.b = this.a;
      }

   }

   public void e() {
      this.a[this.a] = this.a[this.a - 1].a();
      this.a[this.a - 1] = this.a[this.a - 2].a();
      this.a[this.a - 2] = this.a[this.a];
      ++this.a;
      if (this.b < this.a) {
         this.b = this.a;
      }

   }

   public void f() {
      this.a[this.a] = this.a[this.a - 1].a();
      this.a[this.a - 1] = this.a[this.a - 2];
      this.a[this.a - 2] = this.a[this.a - 3];
      this.a[this.a - 3] = this.a[this.a];
      ++this.a;
      if (this.b < this.a) {
         this.b = this.a;
      }

   }

   public void g() {
      this.a[this.a] = this.a[this.a - 2];
      this.a[this.a + 1] = this.a[this.a - 1];
      this.a += 2;
      if (this.b < this.a) {
         this.b = this.a;
      }

   }

   public void h() {
      this.a[this.a + 1] = this.a[this.a - 1];
      this.a[this.a] = this.a[this.a - 2];
      this.a[this.a - 1] = this.a[this.a - 3];
      this.a[this.a - 2] = this.a[this.a + 1];
      this.a[this.a - 3] = this.a[this.a];
      this.a += 2;
      if (this.b < this.a) {
         this.b = this.a;
      }

   }

   public void i() {
      this.a[this.a + 1] = this.a[this.a - 1];
      this.a[this.a] = this.a[this.a - 2];
      this.a[this.a - 1] = this.a[this.a - 3];
      this.a[this.a - 2] = this.a[this.a - 4];
      this.a[this.a - 3] = this.a[this.a + 1];
      this.a[this.a - 4] = this.a[this.a];
      this.a += 2;
      if (this.b < this.a) {
         this.b = this.a;
      }

   }

   public void j() {
      aa var1 = this.a[this.a - 1].a();
      aa var2 = this.a[this.a - 2].a();
      this.a[this.a - 1] = var2;
      this.a[this.a - 2] = var1;
   }

   public boolean equals(Object var1) {
      if (var1 != null && this.getClass() == var1.getClass()) {
         lk var5 = (lk)var1;
         if (this.a != var5.a) {
            return false;
         } else {
            int var2 = 0;

            while(true) {
               if (var2 >= this.a) {
                  return true;
               }

               it var3 = this.a[var2];
               it var4 = var5.a[var2];
               if (var3 == null) {
                  if (var4 != null) {
                     break;
                  }
               } else if (!var3.equals(var4)) {
                  break;
               }

               ++var2;
            }

            return false;
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
