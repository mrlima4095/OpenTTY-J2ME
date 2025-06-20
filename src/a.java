import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Vector;

public final class a extends kt {
   private Vector a = new Vector();
   private oj a = new oj();
   private en a = null;
   private int a;
   private int b;
   private int c = 0;
   private byte[] a = new byte[0];
   private int d = 8;
   private static char[] a = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

   public a(OutputStream var1) {
      super(var1, new hn(-1, true));
   }

   public final void a(int var1) {
      this.a.a(var1);
   }

   private void b(int var1) {
      this.a.write(var1 & 255);
      this.a.write(var1 >> 8 & 255);
   }

   private void c(int var1) {
      this.b(var1);
      this.b(var1 >> 16);
   }

   public final void a(en var1) {
      if (this.a == null) {
         throw new id("ZipOutputStream was finished");
      } else {
         int var2 = var1.b();
         byte var3 = 0;
         if (var2 == -1) {
            var2 = this.d;
         }

         if (var2 == 0) {
            if (var1.c() >= 0L) {
               if (var1.b() < 0L) {
                  var1.b(var1.c());
               } else if (var1.b() != var1.c()) {
                  throw new id("Method STORED, but compressed size != size");
               }
            } else {
               var1.c(var1.b());
            }

            if (var1.b() < 0L) {
               throw new id("Method STORED, but size not set");
            }

            if (var1.d() < 0L) {
               throw new id("Method STORED, but crc not set");
            }
         } else if (var2 == 8 && (var1.c() < 0L || var1.b() < 0L || var1.d() < 0L)) {
            var3 = 8;
         }

         if (this.a != null) {
            this.b();
         }

         if (var1.a() < 0L) {
            var1.a(System.currentTimeMillis());
         }

         var1.a = var3;
         var1.b = this.c;
         var1.a(var2);
         this.a = var2;
         long var7 = 67324752L;
         this.c((int)var7);
         this.b(var2 == 0 ? 10 : 20);
         this.b(var3);
         this.b(var2);
         this.c(var1.a());
         if ((var3 & 8) == 0) {
            this.c((int)var1.d());
            this.c((int)var1.c());
            this.c((int)var1.b());
         } else {
            this.c(0);
            this.c(0);
            this.c(0);
         }

         byte[] var10;
         try {
            var10 = var1.a().getBytes("UTF-8");
         } catch (UnsupportedEncodingException var9) {
            throw new Error(var9.toString());
         }

         if (var10.length > 65535) {
            throw new id("Name too long.");
         } else {
            byte[] var4 = null;
            if (null == null) {
               var4 = new byte[0];
            }

            this.b(var10.length);
            this.b(var4.length);
            this.a.write(var10);
            this.a.write(var4);
            this.c += 30 + var10.length + var4.length;
            this.a = var1;
            this.a.a();
            if (var2 == 8) {
               this.a.a();
            }

            this.b = 0;
         }
      }
   }

   private void b() {
      if (this.a == null) {
         throw new id("No open entry");
      } else {
         if (this.a == 8) {
            super.a();
         }

         int var1 = this.a == 8 ? this.a.a() : this.b;
         if (this.a.b() < 0L) {
            this.a.b((long)this.b);
         } else if (this.a.b() != (long)this.b) {
            throw new id("size was " + this.b + ", but I expected " + this.a.b());
         }

         if (this.a.c() < 0L) {
            this.a.c((long)var1);
         } else if (this.a.c() != (long)var1) {
            throw new id("compressed size was " + var1 + ", but I expected " + this.a.b());
         }

         if (this.a.d() < 0L) {
            this.a.d(this.a.a());
         } else if (this.a.d() != this.a.a()) {
            throw new id("crc was " + a(this.a.a()) + ", but I expected " + a(this.a.d()));
         }

         this.c += var1;
         if (this.a == 8 && (this.a.a & 8) != 0) {
            long var3 = 134695760L;
            this.c((int)var3);
            this.c((int)this.a.d());
            this.c((int)this.a.c());
            this.c((int)this.a.b());
            this.c += 16;
         }

         this.a.addElement(this.a);
         this.a = null;
      }
   }

   public final void write(byte[] var1, int var2, int var3) {
      if (this.a == null) {
         throw new id("No open entry.");
      } else {
         switch(this.a) {
         case 0:
            this.a.write(var1, var2, var3);
            break;
         case 8:
            super.write(var1, var2, var3);
         }

         this.a.a(var1, var2, var3);
         this.b += var3;
      }
   }

   public final void a() {
      if (this.a != null) {
         if (this.a != null) {
            this.b();
         }

         int var1 = 0;
         int var2 = 0;

         byte[] var6;
         long var13;
         byte[] var17;
         byte[] var18;
         for(Enumeration var3 = this.a.elements(); var3.hasMoreElements(); var2 += 46 + var17.length + var6.length + var18.length) {
            en var4;
            int var5 = (var4 = (en)var3.nextElement()).b();
            var13 = 33639248L;
            this.c((int)var13);
            this.b(var5 == 0 ? 10 : 20);
            this.b(var5 == 0 ? 10 : 20);
            this.b(var4.a);
            this.b(var5);
            this.c(var4.a());
            this.c((int)var4.d());
            this.c((int)var4.c());
            this.c((int)var4.b());

            try {
               var17 = var4.a().getBytes("UTF-8");
            } catch (UnsupportedEncodingException var15) {
               throw new Error(var15.toString());
            }

            if (var17.length > 65535) {
               throw new id("Name too long.");
            }

            var6 = null;
            if (null == null) {
               var6 = new byte[0];
            }

            Object var7 = null;

            try {
               var18 = var7 != null ? ((String)var7).getBytes("UTF-8") : new byte[0];
            } catch (UnsupportedEncodingException var16) {
               throw new Error(var16.toString());
            }

            if (var18.length > 65535) {
               throw new id("Comment too long.");
            }

            this.b(var17.length);
            this.b(var6.length);
            this.b(var18.length);
            this.b(0);
            this.b(0);
            this.c(0);
            this.c(var4.b);
            this.a.write(var17);
            this.a.write(var6);
            this.a.write(var18);
            ++var1;
         }

         var13 = 101010256L;
         this.c((int)var13);
         this.b(0);
         this.b(0);
         this.b(var1);
         this.b(var1);
         this.c(var2);
         this.c(this.c);
         this.b(this.a.length);
         this.a.write(this.a);
         this.a.flush();
         this.a = null;
      }
   }

   private static String a(long var0) {
      if (var0 >= 0L && (long)((int)var0) == var0) {
         return Integer.toHexString((int)var0);
      } else {
         char[] var2 = new char[64];
         int var3 = 64;

         do {
            --var3;
            var2[var3] = a[(int)var0 & 15];
         } while((var0 >>>= 4) != 0L);

         return new String(var2, var3, 64 - var3);
      }
   }
}
