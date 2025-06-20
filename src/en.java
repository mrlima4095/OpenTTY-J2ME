import java.util.Calendar;
import java.util.Date;

public final class en {
   private static Calendar a;
   private String a;
   private int c;
   private long a = -1L;
   private int d;
   private int e;
   private short a = 0;
   private short b = -1;
   private byte[] a = null;
   private String b = null;
   int a;
   int b;

   public en(String var1) {
      int var2;
      if ((var2 = var1.length()) > 65535) {
         throw new IllegalArgumentException("name length is " + var2);
      } else {
         this.a = var1;
      }
   }

   final int a() {
      return (this.a & 8) == 0 ? 0 : this.e;
   }

   public final String a() {
      return this.a;
   }

   public final void a(long var1) {
      Calendar var3;
      synchronized(var3 = a()) {
         var3.setTime(new Date(var1));
         this.e = (var3.get(1) - 1980 & 127) << 25 | var3.get(2) + 1 << 21 | var3.get(5) << 16 | var3.get(11) << 11 | var3.get(12) << 5 | var3.get(13) >> 1;
      }

      this.a = (short)(this.a | 8);
   }

   public final long a() {
      if ((this.a & 16) == 0) {
         this.a = (short)(this.a | 16);
      }

      if ((this.a & 8) == 0) {
         return -1L;
      } else {
         int var1 = 2 * (this.e & 31);
         int var2 = this.e >> 5 & 63;
         int var3 = this.e >> 11 & 31;
         int var4 = this.e >> 16 & 31;
         int var5 = (this.e >> 21 & 15) - 1;
         int var6 = (this.e >> 25 & 127) + 1980;

         try {
            synchronized(a = a()) {
               a.set(1, var6);
               a.set(2, var5);
               a.set(5, var4);
               a.set(11, var3);
               a.set(12, var2);
               a.set(13, var1);
               return a.getTime().getTime();
            }
         } catch (RuntimeException var9) {
            this.a = (short)(this.a & -9);
            return -1L;
         }
      }
   }

   private static synchronized Calendar a() {
      if (a == null) {
         a = Calendar.getInstance();
      }

      return a;
   }

   public final void b(long var1) {
      if ((var1 & -4294967296L) != 0L) {
         throw new IllegalArgumentException();
      } else {
         this.c = (int)var1;
         this.a = (short)(this.a | 1);
      }
   }

   public final long b() {
      return (this.a & 1) != 0 ? (long)this.c & 4294967295L : -1L;
   }

   public final void c(long var1) {
      this.a = var1;
   }

   public final long c() {
      return this.a;
   }

   public final void d(long var1) {
      if ((var1 & -4294967296L) != 0L) {
         throw new IllegalArgumentException();
      } else {
         this.d = (int)var1;
         this.a = (short)(this.a | 4);
      }
   }

   public final long d() {
      return (this.a & 4) != 0 ? (long)this.d & 4294967295L : -1L;
   }

   public final void a(int var1) {
      if (var1 != 0 && var1 != 8) {
         throw new IllegalArgumentException();
      } else {
         this.b = (short)var1;
      }
   }

   public final int b() {
      return this.b;
   }

   public final String toString() {
      return this.a;
   }

   public final int hashCode() {
      return this.a.hashCode();
   }
}
