public final class cs extends mm implements ad, fs {
   private final jj a = new jj();
   private boolean a;
   private boolean b;
   private lk a;
   private aj a;

   public final void a(aj var1, kn var2, fl var3) {
      String var4 = var2.b(var1);
      boolean var8 = (var2.a() & 8) != 0;
      int var5 = ec.c(var4);
      if (!var8) {
         ++var5;
      }

      var3.a(var5);
      cy var9 = new cy(var4);
      var5 = 0;
      it var6;
      if (!var8) {
         var6 = this.a(ec.a(var1.a()), var1);
         ++var5;
         var3.a(0, var6);
      }

      while(var9.a()) {
         String var7 = var9.a();
         var6 = this.a((String)var7, (aj)null);
         var3.a(var5++, var6);
         if (var6.a()) {
            ++var5;
         }
      }

   }

   public final void a(aj var1, hf var2, lk var3) {
      int var4 = var2.a;
      switch(var2.a) {
      case -78:
         this.a = true;
         this.b = true;
         break;
      case -77:
         this.a = true;
         this.b = false;
         break;
      case -76:
         this.a = false;
         this.b = true;
         break;
      case -75:
         this.a = false;
         this.b = false;
         break;
      case -74:
      case -73:
      case -71:
         this.a = false;
         break;
      case -72:
         this.a = true;
      }

      this.a = var3;
      var1.a(var4, this);
      this.a = null;
   }

   public final void a(aj var1, f var2) {
      if (!this.b) {
         this.a.b();
      }

      if (!this.a) {
         this.a.a();
      }

      if (this.b) {
         String var3 = var2.c(var1);
         lk var10000 = this.a;
         this.a = null;
         var10000.c(this.a.a(var3, this.a, true));
      }

   }

   public final void a(aj var1, kc var2) {
      String var4;
      int var3 = ec.b(var4 = var2.c(var1));
      if (!this.a) {
         ++var3;
      }

      --var3;

      while(var3 >= 0) {
         this.a.b();
         --var3;
      }

      String var5;
      if ((var5 = ec.c(var4)).charAt(0) != 'V') {
         lk var10000 = this.a;
         this.a = null;
         var10000.c(this.a.a(var5, this.a, true));
      }

   }

   private it a(String var1, aj var2) {
      return this.a.a(var1, var2, true);
   }

   public final void a(aj var1, dv var2) {
      this.a = null;
   }

   public final void a(aj var1, kn var2) {
   }
}
