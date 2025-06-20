public abstract class hh {
   private static final boolean[] a = new boolean[]{false, false, false, false, false, false, false, false, false, true, true, false, false, false, true, true, false, false, false, false, true, false, true, false, true, false, false, false, false, false, true, true, true, true, false, false, false, false, true, true, true, true, false, false, false, false, false, true, false, true, false, false, false, false, false, true, false, true, false, false, false, false, false, true, true, true, true, false, false, false, false, true, true, true, true, false, false, false, false, false, true, false, true, false, false, false, false, false, true, false, false, false, true, true, true, false, false, true, false, true, false, true, false, true, false, true, false, true, false, true, false, true, false, true, false, true, false, true, false, true, false, true, false, true, false, true, false, true, false, true, false, true, false, false, false, false, true, true, true, false, false, false, true, true, true, false, false, false, true, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false};
   private static final int[] a = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 2, 1, 1, 1, 1, 1, 2, 2, 2, 2, 1, 1, 1, 1, 2, 2, 2, 2, 1, 1, 1, 1, 3, 4, 3, 4, 3, 3, 3, 3, 1, 2, 1, 2, 3, 2, 3, 4, 2, 2, 4, 2, 4, 2, 4, 2, 4, 2, 4, 2, 4, 2, 4, 2, 4, 2, 4, 2, 4, 1, 2, 1, 2, 2, 3, 2, 3, 2, 3, 2, 4, 2, 4, 2, 4, 0, 1, 1, 1, 2, 2, 2, 1, 1, 1, 2, 2, 2, 1, 1, 1, 4, 2, 2, 4, 4, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 0, 0, 0, 1, 1, 1, 2, 1, 2, 1, 0, 0, 0, 1, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 0, 0};
   private static final int[] b = new int[]{0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 1, 1, 1, 2, 2, 1, 1, 1, 1, 2, 1, 2, 1, 2, 1, 1, 1, 1, 1, 2, 2, 2, 2, 1, 1, 1, 1, 2, 2, 2, 2, 1, 1, 1, 1, 1, 2, 1, 2, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 3, 4, 4, 5, 6, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 0, 2, 1, 2, 1, 1, 2, 1, 2, 2, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1};
   public byte a;

   public abstract hh a();

   public final void a(ac var1, int var2) {
      this.c(var1.a, var2);
   }

   public final void c(byte[] var1, int var2) {
      if (this.a()) {
         var1[var2++] = -60;
      }

      var1[var2++] = this.a;
      this.b(var1, var2);
   }

   protected boolean a() {
      return false;
   }

   protected abstract void a(byte[] var1, int var2);

   protected abstract void b(byte[] var1, int var2);

   public abstract int a(int var1);

   public abstract void a(aj var1, kn var2, ac var3, int var4, ff var5);

   public String a(int var1) {
      return "[" + var1 + "] " + this.toString();
   }

   public final String a() {
      return dk.a[this.a & 255];
   }

   public final boolean b() {
      return a[this.a & 255];
   }

   public int a(aj var1) {
      return a[this.a & 255];
   }

   public int b(aj var1) {
      return b[this.a & 255];
   }

   protected static int a(byte[] var0, int var1) {
      return var0[var1++] << 24 | (var0[var1++] & 255) << 16 | (var0[var1++] & 255) << 8 | var0[var1] & 255;
   }

   protected static int a(byte[] var0, int var1, int var2) {
      switch(var2) {
      case 0:
         return 0;
      case 1:
         return var0[var1] & 255;
      case 2:
         return ((var0 = var0)[var1++] & 255) << 8 | var0[var1] & 255;
      case 3:
      default:
         throw new IllegalArgumentException("Unsupported value size [" + var2 + "]");
      case 4:
         return a(var0, var1);
      }
   }

   protected static int b(byte[] var0, int var1, int var2) {
      switch(var2) {
      case 0:
         return 0;
      case 1:
         return var0[var1];
      case 2:
         return (var0 = var0)[var1++] << 8 | var0[var1] & 255;
      case 3:
      default:
         throw new IllegalArgumentException("Unsupported value size [" + var2 + "]");
      case 4:
         return a(var0, var1);
      }
   }

   protected static void a(byte[] var0, int var1, int var2) {
      if (var2 > 255) {
         throw new IllegalArgumentException("Unsigned byte value larger than 0xff [" + var2 + "]");
      } else {
         var0[var1] = (byte)var2;
      }
   }

   protected static void b(byte[] var0, int var1, int var2) {
      var0[var1++] = (byte)(var2 >> 24);
      var0[var1++] = (byte)(var2 >> 16);
      var0[var1++] = (byte)(var2 >> 8);
      var0[var1] = (byte)var2;
   }

   protected static void a(byte[] var0, int var1, int var2, int var3) {
      switch(var3) {
      case 0:
         return;
      case 1:
         a(var0, var1, var2);
         return;
      case 2:
         if (var2 > 65535) {
            throw new IllegalArgumentException("Unsigned short value larger than 0xffff [" + var2 + "]");
         }

         var0[var1++] = (byte)(var2 >> 8);
         var0[var1] = (byte)var2;
         return;
      case 3:
      default:
         throw new IllegalArgumentException("Unsupported value size [" + var3 + "]");
      case 4:
         b(var0, var1, var2);
      }
   }

   protected static void b(byte[] var0, int var1, int var2, int var3) {
      switch(var3) {
      case 0:
         return;
      case 1:
         if ((byte)var2 != var2) {
            throw new IllegalArgumentException("Signed byte value out of range [" + var2 + "]");
         }

         var0[var1] = (byte)var2;
         return;
      case 2:
         if ((short)var2 != var2) {
            throw new IllegalArgumentException("Signed short value out of range [" + var2 + "]");
         }

         var0[var1++] = (byte)(var2 >> 8);
         var0[var1] = (byte)var2;
         return;
      case 3:
      default:
         throw new IllegalArgumentException("Unsupported value size [" + var3 + "]");
      case 4:
         b(var0, var1, var2);
      }
   }
}
