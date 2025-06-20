public final class bx extends mm implements ff, jn, jy {
   private boolean[] a = new boolean[1024];
   private int[] a = new int[1024];
   private boolean a;
   private int a;
   private int b;

   public final int a() {
      return this.b;
   }

   public final void a(aj var1, kn var2, ac var3) {
      try {
         bx var9 = this;
         int var6 = var3.c;
         if (this.a.length < var6) {
            this.a = new boolean[var6];
            this.a = new int[var6];
         } else {
            for(int var7 = 0; var7 < var6; ++var7) {
               var9.a[var7] = false;
            }
         }

         var9.a = 0;
         var9.b = 0;
         var9.a(var1, var2, var3, 0);
         var3.a(var1, var2, (jn)var9);
      } catch (RuntimeException var8) {
         System.err.println("Unexpected error while computing stack sizes:");
         System.err.println("  Class       = [" + var1.a() + "]");
         System.err.println("  Method      = [" + var2.a(var1) + var2.b(var1) + "]");
         System.err.println("  Exception   = [" + var8.getClass().getName() + "] (" + var8.getMessage() + ")");
         throw var8;
      }
   }

   public final void a(aj var1, kn var2, ac var3, int var4, du var5) {
      byte var6 = var5.a;
      this.a = var6 == -84 || var6 == -83 || var6 == -82 || var6 == -81 || var6 == -80 || var6 == -79 || var6 == -65;
   }

   public final void a(aj var1, kn var2, ac var3, int var4, hf var5) {
      this.a = false;
   }

   public final void a(aj var1, kn var2, ac var3, int var4, b var5) {
      byte var6 = var5.a;
      this.a = var6 == -87;
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ae var5) {
      byte var6 = var5.a;
      this.a(var1, var2, var3, var4 + var5.a);
      if (var6 == -88 || var6 == -55) {
         --this.a;
         this.a(var1, var2, var3, var4 + var5.a(var4));
      }

      this.a = var6 == -89 || var6 == -56 || var6 == -88 || var6 == -55;
   }

   public final void a(aj var1, kn var2, ac var3, int var4, do var5) {
      int[] var6 = var5.a;

      for(int var7 = 0; var7 < var6.length; ++var7) {
         this.a(var1, var2, var3, var4 + var6[var7]);
      }

      this.a(var1, var2, var3, var4 + var5.a);
      this.a = true;
   }

   public final void a(aj var1, kn var2, ac var3, m var4) {
      this.a = 1;
      this.a(var1, var2, var3, var4.c);
   }

   private void a(aj var1, kn var2, ac var3, int var4) {
      int var5 = this.a;
      if (this.b < this.a) {
         this.b = this.a;
      }

      while(true) {
         if (!this.a[var4]) {
            this.a[var4] = true;
            hh var6 = c.a(var3.a, var4);
            this.a -= var6.a(var1);
            if (this.a < 0) {
               throw new IllegalArgumentException("Stack size becomes negative after instruction " + var6.a(var4) + " in [" + var1.a() + "." + var2.a(var1) + var2.b(var1) + "]");
            }

            this.a[var4] = this.a += var6.b(var1);
            if (this.b < this.a) {
               this.b = this.a;
            }

            int var7 = var4 + var6.a(var4);
            var6.a(var1, var2, var3, var4, this);
            if (!this.a) {
               var4 = var7;
               continue;
            }
         }

         this.a = var5;
         return;
      }
   }
}
