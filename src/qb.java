public final class qb extends mm implements ff, jy {
   private int a;
   private ab a;

   public final void a(int var1) {
      this.a = var1;
      if (this.a != null) {
         this.a.a(var1);
      }

   }

   public final void a(aj var1, kn var2, ac var3, int var4, du var5) {
      var5.a(var3, var4);
   }

   public final void a(aj var1, kn var2, ac var3, int var4, hf var5) {
      try {
         var5.a(var3, var4);
      } catch (IllegalArgumentException var6) {
         hh var7 = (new hf(var5.a, var5.a, var5.b)).a();
         this.c(var4, var7);
         var5.a = 0;
         var5.b = 0;
         var5.a(var3, var4);
      }
   }

   public final void a(aj var1, kn var2, ac var3, int var4, b var5) {
      try {
         var5.a(var3, var4);
      } catch (IllegalArgumentException var6) {
         hh var7 = (new b(var5.a, var5.a, var5.b)).a();
         this.c(var4, var7);
         var5.a = 0;
         var5.b = 0;
         var5.a(var3, var4);
      }
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ae var5) {
      try {
         var5.a(var3, var4);
      } catch (IllegalArgumentException var6) {
         Object var7 = new ae((byte)-56, var5.a);
         ae var8;
         switch(var5.a) {
         case -103:
         case -102:
         case -101:
         case -100:
         case -99:
         case -98:
         case -97:
         case -96:
         case -95:
         case -94:
         case -93:
         case -92:
         case -91:
         case -90:
            var8 = new ae((byte)((var5.a + 1 ^ 1) - 1), 8);
            this.b(var4, var8);
            break;
         case -89:
         case -88:
         case -87:
         case -86:
         case -85:
         case -84:
         case -83:
         case -82:
         case -81:
         case -80:
         case -79:
         case -78:
         case -77:
         case -76:
         case -75:
         case -74:
         case -73:
         case -72:
         case -71:
         case -70:
         case -69:
         case -68:
         case -67:
         case -66:
         case -65:
         case -64:
         case -63:
         case -62:
         case -61:
         case -60:
         case -59:
         default:
            var7 = (new ae(var5.a, var5.a)).a();
            break;
         case -58:
         case -57:
            var8 = new ae((byte)(var5.a ^ 1), 8);
            this.b(var4, var8);
         }

         this.c(var4, (hh)var7);
         var5.a = 0;
         var5.a(var3, var4);
      }
   }

   public final void a(aj var1, kn var2, ac var3, int var4, do var5) {
      var5.a(var3, var4);
   }

   public final void a(aj var1, kn var2, ac var3) {
      if (this.a != null) {
         this.a.a(var1, var2, var3);
         this.a = null;
      }

   }

   private void b(int var1, hh var2) {
      this.a();
      this.a.a(var1, var2);
   }

   private void c(int var1, hh var2) {
      this.a();
      this.a.b(var1, var2);
   }

   private void a() {
      if (this.a == null) {
         this.a = new ab();
         this.a.a(this.a);
      }

   }
}
