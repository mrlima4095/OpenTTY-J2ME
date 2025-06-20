import java.util.Stack;

public final class im extends mm implements jn, jy {
   private final jj a;
   private final cs a;
   private final boolean a;
   private ev[] a;
   private ev[] b;
   private ev[] c;
   private ev[] d;
   private io[] a;
   private eq[] a;
   private io[] b;
   private eq[] b;
   private boolean[] a;
   private int[] a;
   private int[] b;
   private boolean b;
   private final gh a;
   private final ij a;

   public im() {
      this(new jj(), new cs(), true);
   }

   private im(jj var1, cs var2, boolean var3) {
      this.a = new ev[1024];
      this.b = new ev[1024];
      this.c = new ev[1024];
      this.d = new ev[1024];
      this.a = new io[1024];
      this.a = new eq[1024];
      this.b = new io[1024];
      this.b = new eq[1024];
      this.a = new boolean[1024];
      this.a = new int[1024];
      this.b = new int[1024];
      this.a = new ij();
      this.a = var1;
      this.a = var2;
      this.a = true;
      this.a = new gh();
   }

   public final void a(aj var1, dp var2) {
   }

   public final void a(aj var1, kn var2, ac var3) {
      try {
         ac var5 = var3;
         kn var4 = var2;
         aj var17 = var1;
         im var16 = this;
         io var6 = new io(var3.b);
         eq var7 = new eq(var3.a);
         io var12 = var6;
         ac var11 = var3;
         im var8 = this;
         int var13 = var3.c;
         int var14;
         if (this.b.length < var13) {
            this.a = new ev[var13];
            this.b = new ev[var13];
            this.c = new ev[var13];
            this.d = new ev[var13];
            this.a = new io[var13];
            this.a = new eq[var13];
            this.b = new io[var13];
            this.b = new eq[var13];
            this.a = new boolean[var13];
            this.a = new int[var13];
            this.b = new int[var13];

            for(var14 = 0; var14 < var13; ++var14) {
               var8.a[var14] = ev.a;
               var8.b[var14] = ev.a;
               var8.b[var14] = -2;
            }
         } else {
            for(var14 = 0; var14 < var13; ++var14) {
               var8.a[var14] = ev.a;
               var8.b[var14] = ev.a;
               var8.c[var14] = null;
               var8.d[var14] = null;
               var8.a[var14] = false;
               var8.a[var14] = 0;
               var8.b[var14] = -2;
               if (var8.a[var14] != null) {
                  var8.a[var14].a(var11.b);
               }

               if (var8.a[var14] != null) {
                  var8.a[var14].a(var11.a);
               }

               if (var8.b[var14] != null) {
                  var8.b[var14].a(var11.b);
               }

               if (var8.b[var14] != null) {
                  var8.b[var14].a(var11.a);
               }
            }
         }

         io var20 = new io(var11.b);
         ev var19 = new ev(-1);
         var20.a((it)var19);
         var19 = ev.a;
         var20.b(var19);
         var8.a.a(var1, (kn)var2, (fl)var20);
         var6.a(var20);
         ev var18 = new ev(-1);

         for(int var9 = 0; var9 < var20.a(); ++var9) {
            var12.b(var9, var18);
         }

         var3.a(var1, var2, (jy)this.a);
         this.a(var1, var2, var3, var6, var7, 0);

         do {
            var16.b = false;
            var5.a(var17, var4, (jn)var16);
         } while(var16.b);

      } catch (RuntimeException var15) {
         System.err.println("Unexpected error while performing partial evaluation:");
         System.err.println("  Class       = [" + var1.a() + "]");
         System.err.println("  Method      = [" + var2.a(var1) + var2.b(var1) + "]");
         System.err.println("  Exception   = [" + var15.getClass().getName() + "] (" + var15.getMessage() + ")");
         throw var15;
      }
   }

   public final boolean a(int var1) {
      return this.a[var1] > 0;
   }

   public final boolean b(int var1) {
      return this.a.b(var1) || this.a.c(var1);
   }

   public final int a(int var1) {
      return this.a.b(var1);
   }

   public final boolean a() {
      return this.a.a();
   }

   public final int a() {
      return this.a.a();
   }

   public final io a(int var1) {
      return this.a[var1];
   }

   public final io b(int var1) {
      return this.b[var1];
   }

   public final eq a(int var1) {
      return this.a[var1];
   }

   public final ev a(int var1) {
      return this.c[var1];
   }

   public final ev b(int var1) {
      return this.d[var1];
   }

   public final void a(aj var1, kn var2, ac var3, m var4) {
      int var5 = var4.a;
      int var6 = var4.b;
      int var12 = var6;
      im var10 = this;
      int var13 = var5;

      boolean var10000;
      while(true) {
         if (var13 >= var12) {
            var10000 = false;
            break;
         }

         if (var10.a(var13)) {
            var10000 = true;
            break;
         }

         ++var13;
      }

      if (var10000) {
         int var7 = var4.c;
         int var15 = var4.d;
         io var8 = new io(var3.b);
         eq var9 = new eq(var3.a);
         ev var20 = new ev(-1);
         var8.a((it)var20);
         var9.a((it)var20);
         io var17 = var8;
         boolean var22 = this.a;
         var12 = var6;
         var10 = this;
         boolean var19 = true;
         int var14 = -1;

         for(int var11 = var5; var11 < var12; ++var11) {
            if (var10.a(var11)) {
               io var23 = var10.a[var11];
               if (var19) {
                  var17.a(var23);
                  var19 = false;
               } else {
                  var17.a(var23, false);
               }

               var14 = var11;
            }
         }

         if (var22 && var14 >= 0) {
            io var21 = var10.b[var14];
            if (var19) {
               var17.a(var21);
            } else {
               var17.a(var21, false);
            }
         }

         if (var19) {
            var17.a(var17.a());
         }

         String var18 = var15 != 0 ? var1.b(var15) : "java/lang/Throwable";
         aj var16 = var15 != 0 ? ((bc)((aj)var1).a[var15]).a : null;
         var9.c(jj.a(var18, var16, false));
         var15 = this.a[var7];
         this.a(var1, var2, var3, var8, var9, var7);
         if (!this.b) {
            this.b = var15 < this.a[var7];
         }

      } else {
         if (this.a) {
            this.b = true;
         }

      }
   }

   private void a(aj var1, kn var2, ac var3, io var4, eq var5, int var6) {
      Stack var7 = new Stack();
      this.a(var1, var2, var3, var4, var5, var6, var7);

      while(!var7.empty()) {
         lw var8 = (lw)var7.pop();
         this.a(var1, var2, var3, lw.a(var8), lw.a(var8), lw.a(var8), var7);
      }

   }

   private void a(aj var1, kn var2, ac var3, io var4, eq var5, int var6, Stack var7) {
      byte[] var8 = var3.a;
      gi var9 = new gi(var4, var5, this.a, this.a, this.a);
      var6 = var6;

      int var10;
      hh var21;
      int var23;
      do {
         if ((var10 = this.a[var6]) == 0) {
            if (this.a[var6] == null) {
               this.a[var6] = new io(var4);
               this.a[var6] = new eq(var5);
            } else {
               this.a[var6].a(var4);
               this.a[var6].a(var5);
            }

            this.a[var6] = true;
         } else {
            boolean var11 = this.a[var6].a(var4, true);
            boolean var12 = this.a[var6].a(var5);
            if (!var11 && !var12 && this.a[var6]) {
               return;
            }

            if (var10 >= 5) {
               var4.a(this.a[var6], false);
               var5.a(this.a[var6]);
               this.a[var6] = true;
            } else {
               this.a[var6] = false;
            }
         }

         int var10002 = this.a[var6]++;
         ev var20 = new ev(var6);
         var4.a((it)var20);
         var5.a((it)var20);
         ev var22 = ev.a;
         var4.b(var22);
         var5.b(var22);
         var4.a();
         var21 = c.a(var8, var6);
         int var13 = var6 + var21.a(var6);
         ev var24 = new ev(var13);
         this.a.a();
         this.a.a(var24);

         try {
            var21.a(var1, var2, var3, var6, var9);
         } catch (RuntimeException var15) {
            System.err.println("Unexpected error while evaluating instruction:");
            System.err.println("  Class       = [" + var1.a() + "]");
            System.err.println("  Method      = [" + var2.a(var1) + var2.b(var1) + "]");
            System.err.println("  Instruction = " + var21.a(var6));
            System.err.println("  Exception   = [" + var15.getClass().getName() + "] (" + var15.getMessage() + ")");
            throw var15;
         }

         var24 = var4.a().a();
         ev var14 = var5.a().a();
         this.a[var6] = this.a[var6].a(var24).a();
         this.b[var6] = this.b[var6].a(var14).a();
         this.b[var6] = var4.b();
         int var25 = (var24 = this.a.a()).b();
         var4.b(var22);
         var5.b(var22);
         this.a.a(var22);
         if (var10 == 0) {
            if (this.b[var6] == null) {
               this.b[var6] = new io(var4);
               this.b[var6] = new eq(var5);
            } else {
               this.b[var6].a(var4);
               this.b[var6].a(var5);
            }
         } else {
            this.b[var6].a(var4, true);
            this.b[var6].a(var5);
         }

         if (this.a.a()) {
            this.d[var6] = this.d[var6] == null ? var24 : this.d[var6].a(var24).a();
            if (var25 == 0) {
               return;
            }

            ev var19 = new ev(var6);

            for(var23 = 0; var23 < var25; ++var23) {
               var6 = var24.a(var23);
               this.c[var6] = this.c[var6] == null ? var19 : this.c[var6].a(var19).a();
            }

            if (var25 > 1) {
               for(var23 = 0; var23 < var25; ++var23) {
                  io var10000 = new io(var4);
                  eq var10001 = new eq(var5);
                  int var18 = var24.a(var23);
                  eq var17 = var10001;
                  io var16 = var10000;
                  var7.push(new lw(var16, var17, var18, (ck)null));
               }

               return;
            }
         }

         var6 = var24.a(0);
      } while(var21.a != -88 && var21.a != -55);

      var10 = this.a.a(var6);

      for(var23 = var6; var23 < var10; ++var23) {
         if (this.a.a(var23)) {
            this.a[var23] = 0;
         }
      }

      this.a(var1, var2, var3, new io(var4), new eq(var5), var6);
      var3.a(var1, var2, var6, var10, (jn)this);
   }
}
