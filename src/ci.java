import java.io.UnsupportedEncodingException;

public final class ci extends r {
   private byte[] a;
   private String a;

   public ci() {
   }

   public ci(String var1) {
      this.a = null;
      this.a = var1;
   }

   public final void a(byte[] var1) {
      this.a = var1;
      this.a = null;
   }

   public final byte[] a() {
      try {
         ci var7;
         if ((var7 = this).a != null) {
            return var7.a;
         } else {
            int var1 = 0;
            int var2 = var7.a.length();

            for(int var3 = 0; var3 < var2; ++var3) {
               char var4 = var7.a.charAt(var3);
               var1 += var4 == 0 ? 2 : (var4 < 128 ? 1 : (var4 < 2048 ? 2 : 3));
            }

            byte[] var8 = new byte[var1];
            int var9 = 0;

            for(var1 = 0; var1 < var2; ++var1) {
               char var5;
               if ((var5 = var7.a.charAt(var1)) == 0) {
                  var8[var9++] = -64;
                  var8[var9++] = -128;
               } else if (var5 < 128) {
                  var8[var9++] = (byte)var5;
               } else if (var5 < 2048) {
                  var8[var9++] = (byte)(-64 | var5 >>> 6 & 31);
                  var8[var9++] = (byte)(-128 | var5 & 63);
               } else {
                  var8[var9++] = (byte)(-32 | var5 >>> 12 & 15);
                  var8[var9++] = (byte)(-128 | var5 >>> 6 & 63);
                  var8[var9++] = (byte)(-128 | var5 & 63);
               }
            }

            return var8;
         }
      } catch (UnsupportedEncodingException var6) {
         throw new RuntimeException(var6.getMessage());
      }
   }

   public final String a() {
      try {
         if (this.a == null) {
            this.a = new String(this.a, "UTF-8");
            this.a = null;
         }
      } catch (UnsupportedEncodingException var2) {
         throw new RuntimeException(var2.getMessage());
      }

      return this.a;
   }

   public final int a() {
      return 1;
   }

   public final void a(aj var1, fs var2) {
      var2.a(var1, this);
   }
}
