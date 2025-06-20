public final class b extends hh {
   private boolean a;
   public int a;
   public int b;

   public b() {
   }

   public b(boolean var1) {
      this.a = var1;
   }

   public b(byte var1, int var2, int var3) {
      this.a = var1;
      this.a = var2;
      this.b = var3;
      this.a = this.b() > 1 || this.d() > 1;
   }

   public final hh a() {
      byte var10001;
      switch(this.a) {
      case 26:
      case 27:
      case 28:
      case 29:
         var10001 = 21;
         break;
      case 30:
      case 31:
      case 32:
      case 33:
         var10001 = 22;
         break;
      case 34:
      case 35:
      case 36:
      case 37:
         var10001 = 23;
         break;
      case 38:
      case 39:
      case 40:
      case 41:
         var10001 = 24;
         break;
      case 42:
      case 43:
      case 44:
      case 45:
         var10001 = 25;
         break;
      case 46:
      case 47:
      case 48:
      case 49:
      case 50:
      case 51:
      case 52:
      case 53:
      case 54:
      case 55:
      case 56:
      case 57:
      case 58:
      default:
         var10001 = this.a;
         break;
      case 59:
      case 60:
      case 61:
      case 62:
         var10001 = 54;
         break;
      case 63:
      case 64:
      case 65:
      case 66:
         var10001 = 55;
         break;
      case 67:
      case 68:
      case 69:
      case 70:
         var10001 = 56;
         break;
      case 71:
      case 72:
      case 73:
      case 74:
         var10001 = 57;
         break;
      case 75:
      case 76:
      case 77:
      case 78:
         var10001 = 58;
      }

      this.a = var10001;
      if (this.a <= 3) {
         switch(this.a) {
         case 21:
            this.a = (byte)(26 + this.a);
            break;
         case 22:
            this.a = (byte)(30 + this.a);
            break;
         case 23:
            this.a = (byte)(34 + this.a);
            break;
         case 24:
            this.a = (byte)(38 + this.a);
            break;
         case 25:
            this.a = (byte)(42 + this.a);
         case 26:
         case 27:
         case 28:
         case 29:
         case 30:
         case 31:
         case 32:
         case 33:
         case 34:
         case 35:
         case 36:
         case 37:
         case 38:
         case 39:
         case 40:
         case 41:
         case 42:
         case 43:
         case 44:
         case 45:
         case 46:
         case 47:
         case 48:
         case 49:
         case 50:
         case 51:
         case 52:
         case 53:
         default:
            break;
         case 54:
            this.a = (byte)(59 + this.a);
            break;
         case 55:
            this.a = (byte)(63 + this.a);
            break;
         case 56:
            this.a = (byte)(67 + this.a);
            break;
         case 57:
            this.a = (byte)(71 + this.a);
            break;
         case 58:
            this.a = (byte)(75 + this.a);
         }
      }

      this.a = this.b() > 1 || this.d() > 1;
      return this;
   }

   protected final boolean a() {
      return this.a;
   }

   protected final void a(byte[] var1, int var2) {
      int var3 = this.a();
      int var4 = this.c();
      if (var3 == 0) {
         this.a = this.a < 59 ? this.a - 26 & 3 : this.a - 59 & 3;
      } else {
         this.a = a(var1, var2, var3);
         var2 += var3;
      }

      this.b = b(var1, var2, var4);
   }

   protected final void b(byte[] var1, int var2) {
      int var3 = this.a();
      int var4 = this.c();
      if (this.b() > var3) {
         throw new IllegalArgumentException("Instruction has invalid variable index size (" + this.a(var2) + ")");
      } else if (this.d() > var4) {
         throw new IllegalArgumentException("Instruction has invalid constant size (" + this.a(var2) + ")");
      } else {
         a(var1, var2, this.a, var3);
         var2 += var3;
         b(var1, var2, this.b, var4);
      }
   }

   public final int a(int var1) {
      return (this.a ? 2 : 1) + this.a() + this.c();
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ff var5) {
      var5.a(var1, var2, var3, var4, this);
   }

   public final String toString() {
      return this.a() + (this.a ? "_w" : "") + " v" + this.a + (this.c() > 0 ? ", " + this.b : "");
   }

   private int a() {
      if ((this.a < 26 || this.a > 45) && (this.a < 59 || this.a > 78)) {
         return this.a ? 2 : 1;
      } else {
         return 0;
      }
   }

   private int b() {
      if ((this.a & 3) == this.a) {
         return 0;
      } else if ((this.a & 255) == this.a) {
         return 1;
      } else {
         return (this.a & '\uffff') == this.a ? 2 : 4;
      }
   }

   private int c() {
      if (this.a != -124) {
         return 0;
      } else {
         return this.a ? 2 : 1;
      }
   }

   private int d() {
      if (this.a != -124) {
         return 0;
      } else if ((byte)this.b == this.b) {
         return 1;
      } else {
         return (short)this.b == this.b ? 2 : 4;
      }
   }
}
