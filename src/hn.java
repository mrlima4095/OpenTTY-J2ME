public final class hn {
   private int a;
   private boolean a;
   private int b;
   private long a;
   private dl a;
   private fj a;

   public hn() {
      this(-1, false);
   }

   public hn(int var1, boolean var2) {
      this.a = new dl();
      this.a = new fj(this.a);
      this.a = var2;
      boolean var3 = false;
      this.a.a(0);
      this.a(6);
      this.a();
   }

   public final void a() {
      this.b = this.a ? 16 : 0;
      this.a = 0L;
      dl var1;
      (var1 = this.a).a = var1.b = var1.c = 0;
      this.a.a();
   }

   public final int a() {
      return (int)this.a;
   }

   final void b() {
      this.b |= 4;
   }

   public final void c() {
      this.b |= 12;
   }

   public final boolean a() {
      return this.b == 30 && this.a.a();
   }

   public final boolean b() {
      return this.a.a();
   }

   public final void a(byte[] var1, int var2, int var3) {
      if ((this.b & 8) != 0) {
         throw new IllegalStateException("finish()/end() already called");
      } else {
         this.a.a(var1, var2, var3);
      }
   }

   public final void a(int var1) {
      if (var1 == -1) {
         var1 = 6;
      } else if (var1 < 0 || var1 > 9) {
         throw new IllegalArgumentException();
      }

      if (this.a != var1) {
         this.a = var1;
         this.a.b(var1);
      }

   }

   public final int a(byte[] var1, int var2, int var3) {
      int var4 = var3;
      if (this.b == 127) {
         throw new IllegalStateException("Deflater closed");
      } else {
         int var6;
         if (this.b < 16) {
            boolean var5 = false;
            if ((var6 = this.a - 1 >> 1) < 0 || var6 > 3) {
               var6 = 3;
            }

            var6 = 30720 | var6 << 6;
            if ((this.b & 1) != 0) {
               var6 |= 32;
            }

            var6 += 31 - var6 % 31;
            this.a.b(var6);
            if ((this.b & 1) != 0) {
               var6 = this.a.a();
               this.a.b();
               this.a.b(var6 >> 16);
               this.a.b(var6 & '\uffff');
            }

            this.b = 16 | this.b & 12;
         }

         while(true) {
            var6 = this.a.a(var1, var2, var3);
            var2 += var6;
            this.a += (long)var6;
            if ((var3 -= var6) == 0 || this.b == 30) {
               return var4 - var3;
            }

            if (!this.a.a((this.b & 4) != 0, (this.b & 8) != 0)) {
               if (this.b == 16) {
                  return var4 - var3;
               }

               if (this.b != 20) {
                  if (this.b == 28) {
                     this.a.a();
                     if (!this.a) {
                        var6 = this.a.a();
                        this.a.b(var6 >> 16);
                        this.a.b(var6 & '\uffff');
                     }

                     this.b = 30;
                  }
               } else {
                  if (this.a != 0) {
                     for(var6 = 8 + (-this.a.c & 7); var6 > 0; var6 -= 10) {
                        this.a.a(2, 10);
                     }
                  }

                  this.b = 16;
               }
            }
         }
      }
   }
}
