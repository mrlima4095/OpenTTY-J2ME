public final class kb extends mm implements ff, jn, jy {
   private final ij a = new ij();
   private final ni a = new ni();
   private boolean a;
   private jn a;

   public final void a(aj var1, dp var2) {
   }

   public final void a(aj var1, kn var2, ac var3) {
      this.a.a(var1, var2, var3);
      ac var5 = var3;
      kb var4 = this;
      int var6 = 0;

      boolean var10000;
      while(true) {
         if (var6 >= var5.c) {
            var10000 = false;
            break;
         }

         if (var4.a.d(var6)) {
            var10000 = true;
            break;
         }

         ++var6;
      }

      if (var10000) {
         this.a = false;
         this.a = this;
         this.a.a();
         ac var7 = var3;
         kn var12 = var2;
         aj var11 = var1;
         var4 = this;
         this.a.a(var3.c);

         int var10;
         for(int var8 = 0; var8 < var7.c; var8 += var10) {
            hh var9;
            var10 = (var9 = c.a(var7.a, var8)).a(var8);
            if (var4.a.f(var8) && var4.a.g(var8)) {
               var4.a.b(var8);
            } else {
               var9.a(var11, var12, var7, var8, var4);
            }
         }

         var7.a(var11, var12, (jn)var4);
         var4.a.b(var7.c);
         var4.a.b();
         if (this.a) {
            this.a.a(var1, var2, var3);
         }

      }
   }

   public final void a_(int var1, hh var2) {
      this.a.b(var1, var2.a());
   }

   public final void a(aj var1, kn var2, ac var3, int var4, b var5) {
      if (var5.a == -87) {
         if (this.a.a(var4) == var4 + var5.a(var4)) {
            this.a.b(var4);
         } else {
            hh var6 = (new ae((byte)-89, this.a.a(var4) - var4)).a();
            this.a.b(var4, var6);
         }
      } else if (this.a.e(var4)) {
         this.a.b(var4);
      } else {
         this.a.b(var4, var5);
      }
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ae var5) {
      byte var6;
      if ((var6 = var5.a) != -88 && var6 != -55) {
         this.a.b(var4, var5);
      } else {
         int var9 = var5.a;
         int var10 = var4 + var9;
         if (!this.a.g(var10)) {
            hh var8 = (new ae((byte)-89, var9)).a();
            this.a.b(var4, var8);
            this.a = true;
            return;
         }

         this.a.b(var4);
         var9 = var10;
         var10 = this.a.a(var10);
         jn var7 = this.a;
         this.a = new cp(var4, this.a);
         this.a.a(var3.c);
         var3.a(var1, var2, var9, var10, (ff)this);
         this.a.b(var10);
         this.a = var7;
         var3.a(var1, var2, var9, var10, this.a);
         this.a.b();
         this.a = true;
      }

   }

   public final void a(aj var1, kn var2, ac var3, m var4) {
      int var10 = var4.a;
      int var11 = var4.b;
      int var5 = var4.c;
      int var6 = var4.d;

      int var9;
      for(int var7 = var10; var7 < var11; var7 += var9) {
         hh var8;
         var9 = (var8 = c.a(var3.a, var7)).a(var7);
         if (this.a.d(var7) && !var4.a(var7 + ((ae)var8).a)) {
            this.a.a(new m(var10, var7, var5, var6));
            var10 = var7 + var9;
         }
      }

      this.a.a(new m(var10, var11, var5, var6));
   }
}
