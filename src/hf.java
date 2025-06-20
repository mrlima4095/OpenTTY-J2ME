public final class hf extends hh implements fs {
   public int a;
   public int b;
   private int c;
   private int d;

   public hf() {
   }

   public hf(byte var1, int var2, int var3) {
      this.a = var1;
      this.a = var2;
      this.b = var3;
   }

   public final hh a() {
      if (this.c() == 1) {
         if (this.a == 19) {
            this.a = 18;
         }
      } else if (this.a == 18) {
         this.a = 19;
      }

      return this;
   }

   protected final void a(byte[] var1, int var2) {
      int var3 = this.a();
      int var4 = this.b();
      this.a = a(var1, var2, var3);
      var2 += var3;
      this.b = a(var1, var2, var4);
   }

   protected final void b(byte[] var1, int var2) {
      int var3 = this.a();
      int var4 = this.b();
      if (this.c() > var3) {
         throw new IllegalArgumentException("Instruction has invalid constant index size (" + this.a(var2) + ")");
      } else {
         a(var1, var2, this.a, var3);
         var2 += var3;
         a(var1, var2, this.b, var4);
      }
   }

   public final int a(int var1) {
      return 1 + this.a() + this.b();
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ff var5) {
      var5.a(var1, var2, var3, var4, this);
   }

   public final int a(aj var1) {
      int var2 = super.a(var1);
      switch(this.a) {
      case -77:
      case -75:
         var1.a(this.a, this);
         var2 += this.d;
      case -76:
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
      default:
         break;
      case -74:
      case -73:
      case -72:
      case -71:
         var1.a(this.a, this);
         var2 += this.c;
         break;
      case -59:
         var2 += this.b;
      }

      return var2;
   }

   public final int b(aj var1) {
      int var2 = super.b(var1);
      switch(this.a) {
      case -78:
      case -76:
      case -74:
      case -73:
      case -72:
      case -71:
         var1.a(this.a, this);
         var2 += this.d;
      case -77:
      case -75:
      default:
         return var2;
      }
   }

   public final void a(aj var1, oq var2) {
   }

   public final void a(aj var1, fm var2) {
   }

   public final void a(aj var1, fn var2) {
   }

   public final void a(aj var1, gc var2) {
   }

   public final void a(aj var1, e var2) {
   }

   public final void a(aj var1, ci var2) {
   }

   public final void a(aj var1, bc var2) {
   }

   public final void a(aj var1, hw var2) {
   }

   public final void a(aj var1, f var2) {
      String var3 = var2.c(var1);
      this.d = ec.d(ec.c(var3));
   }

   public final void a(aj var1, db var2) {
      this.a(var1, (kc)var2);
   }

   public final void a(aj var1, jp var2) {
      this.a(var1, (kc)var2);
   }

   private void a(aj var1, kc var2) {
      String var3 = var2.c(var1);
      this.c = ec.c(var3);
      this.d = ec.d(ec.c(var3));
   }

   public final String toString() {
      return this.a() + " #" + this.a;
   }

   private int a() {
      return this.a == 18 ? 1 : 2;
   }

   private int b() {
      if (this.a == -59) {
         return 1;
      } else {
         return this.a == -71 ? 2 : 0;
      }
   }

   private int c() {
      if ((this.a & 255) == this.a) {
         return 1;
      } else {
         return (this.a & '\uffff') == this.a ? 2 : 4;
      }
   }
}
