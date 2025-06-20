public final class du extends hh {
   public int a;

   public final hh a() {
      switch(this.a) {
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 8:
      case 16:
      case 17:
         switch(this.b()) {
         case 0:
            this.a = (byte)(3 + this.a);
            return this;
         case 1:
            this.a = 16;
            return this;
         case 2:
            this.a = 17;
            return this;
         default:
            return this;
         }
      case 9:
      case 10:
         this.a = (byte)(9 + this.a);
         break;
      case 11:
      case 12:
      case 13:
         this.a = (byte)(11 + this.a);
         break;
      case 14:
      case 15:
         this.a = (byte)(14 + this.a);
      }

      return this;
   }

   protected final void a(byte[] var1, int var2) {
      int var3 = this.a();
      int var10001;
      if (var3 == 0) {
         switch(this.a) {
         case 2:
            var10001 = -1;
            break;
         case 3:
         case 9:
         case 11:
         case 14:
         default:
            var10001 = 0;
            break;
         case 4:
         case 10:
         case 12:
         case 15:
            var10001 = 1;
            break;
         case 5:
         case 13:
            var10001 = 2;
            break;
         case 6:
            var10001 = 3;
            break;
         case 7:
            var10001 = 4;
            break;
         case 8:
            var10001 = 5;
         }
      } else {
         var10001 = b(var1, var2, var3);
      }

      this.a = var10001;
   }

   protected final void b(byte[] var1, int var2) {
      int var3 = this.a();
      if (this.b() > var3) {
         throw new IllegalArgumentException("Instruction has invalid constant size (" + this.a(var2) + ")");
      } else {
         b(var1, var2, this.a, var3);
      }
   }

   public final int a(int var1) {
      return 1 + this.a();
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ff var5) {
      var5.a(var1, var2, var3, var4, this);
   }

   public final String toString() {
      return this.a() + (this.a() > 0 ? " " + this.a : "");
   }

   private int a() {
      if (this.a != 16 && this.a != -68) {
         return this.a == 17 ? 2 : 0;
      } else {
         return 1;
      }
   }

   private int b() {
      if (this.a >= -1 && this.a <= 5) {
         return 0;
      } else if ((byte)this.a == this.a) {
         return 1;
      } else {
         return (short)this.a == this.a ? 2 : 4;
      }
   }
}
