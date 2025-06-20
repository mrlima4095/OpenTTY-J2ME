public final class aj {
   public int a;
   public int b;
   public int c;
   public r[] a;
   public int d;
   public int e;
   public int f;
   public int g;
   public int[] a;
   public int h;
   public dv[] a;
   public int i;
   public kn[] a;
   public int j;
   public dp[] a;

   public final String a() {
      return this.b(this.e);
   }

   public final String a(int var1) {
      try {
         return ((ci)this.a[var1]).a();
      } catch (ClassCastException var3) {
         throw new ClassCastException("Expected Utf8Constant at index [" + var1 + "] in class [" + this.a() + "], found [" + var3.getMessage() + "]");
      }
   }

   public final String b(int var1) {
      try {
         return ((bc)this.a[var1]).a(this);
      } catch (ClassCastException var3) {
         throw new ClassCastException("Expected ClassConstant at index [" + var1 + "] in class [" + this.a() + "], found [" + var3.getMessage() + "]");
      }
   }

   public final String c(int var1) {
      try {
         return ((hw)this.a[var1]).a(this);
      } catch (ClassCastException var3) {
         throw new ClassCastException("Expected NameAndTypeConstant at index [" + var1 + "] in class [" + this.a() + "], found [" + var3.getMessage() + "]");
      }
   }

   public final String d(int var1) {
      try {
         return ((hw)this.a[var1]).b(this);
      } catch (ClassCastException var3) {
         throw new ClassCastException("Expected NameAndTypeConstant at index [" + var1 + "] in class [" + this.a() + "], found [" + var3.getMessage() + "]");
      }
   }

   private aj a() {
      return this.f != 0 ? ((bc)this.a[this.f]).a : null;
   }

   private aj a(int var1) {
      return ((bc)this.a[this.a[var1]]).a;
   }

   public final boolean a(aj var1) {
      if (this.equals(var1)) {
         return true;
      } else {
         aj var2;
         if ((var2 = this.a()) != null && var2.a(var1)) {
            return true;
         } else {
            for(int var4 = 0; var4 < this.g; ++var4) {
               aj var3;
               if ((var3 = this.a(var4)) != null && var3.a(var1)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   public final void a(boolean var1, boolean var2, boolean var3, boolean var4, dg var5) {
      if (var1) {
         var5.a_(this);
      }

      aj var6;
      if (var2 && (var6 = this.a()) != null) {
         var6.a(true, true, var3, false, var5);
      }

      if (var3) {
         for(int var7 = 0; var7 < this.g; ++var7) {
            aj var8;
            if ((var8 = this.a(var7)) != null) {
               var8.a(true, true, true, false, var5);
            }
         }
      }

   }

   public final void a(fs var1) {
      for(int var2 = 1; var2 < this.c; ++var2) {
         if (this.a[var2] != null) {
            this.a[var2].a(this, var1);
         }
      }

   }

   public final void a(int var1, fs var2) {
      this.a[var1].a(this, var2);
   }

   public final void a(ad var1) {
      for(int var2 = 0; var2 < this.h; ++var2) {
         this.a[var2].a(this, var1);
      }

   }

   public final void b(ad var1) {
      for(int var2 = 0; var2 < this.i; ++var2) {
         this.a[var2].a(this, var1);
      }

   }

   public final void a(jy var1) {
      for(int var2 = 0; var2 < this.j; ++var2) {
         this.a[var2].a(this, var1);
      }

   }
}
