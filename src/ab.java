public final class ab extends mm implements dq, ee, ff, fw, ir, jn, jy, nn {
   private int a;
   private boolean a;
   private boolean b;
   private hh[] a = new hh[1024];
   private hh[] b = new hh[1024];
   private hh[] c = new hh[1024];
   private boolean[] a = new boolean[1024];
   private int[] a = new int[1024];
   private int b;
   private boolean c;
   private int c;
   private final je a = new je();
   private final dn a = new dn();
   private final qb a = new qb();

   public final void a(int var1) {
      this.a = var1;
      if (this.a.length < var1) {
         this.a = new hh[var1];
         this.b = new hh[var1];
         this.c = new hh[var1];
         this.a = new boolean[var1];
      } else {
         for(int var2 = 0; var2 < var1; ++var2) {
            this.a[var2] = null;
            this.b[var2] = null;
            this.c[var2] = null;
            this.a[var2] = false;
         }
      }

      this.a = false;
      this.b = true;
   }

   public final void a(int var1, hh var2) {
      if (var1 >= 0 && var1 < this.a) {
         this.a[var1] = var2;
         this.a = true;
         this.b = false;
      } else {
         throw new IllegalArgumentException("Invalid instruction offset [" + var1 + "] in code with length [" + this.a + "]");
      }
   }

   public final void b(int var1, hh var2) {
      if (var1 >= 0 && var1 < this.a) {
         this.b[var1] = var2;
         this.a = true;
      } else {
         throw new IllegalArgumentException("Invalid instruction offset [" + var1 + "] in code with length [" + this.a + "]");
      }
   }

   public final void a(aj var1, dp var2) {
   }

   public final void a(aj var1, kn var2, ac var3) {
      try {
         if (this.a) {
            ab var6 = this;
            int var9;
            boolean var10000;
            if (!this.b) {
               var10000 = false;
            } else {
               byte[] var8 = var3.a;
               var9 = var3.c;
               int var10 = 0;

               while(true) {
                  if (var10 >= var9) {
                     var10000 = true;
                     break;
                  }

                  hh var11;
                  if ((var11 = var6.b[var10]) != null && var11.a(var10) != c.a(var8, var10).a(var10)) {
                     var10000 = false;
                     break;
                  }

                  ++var10;
               }
            }

            int var28;
            if (var10000) {
               ac var26 = var3;
               var6 = this;
               var28 = var3.c;

               for(var9 = 0; var9 < var28; ++var9) {
                  hh var33;
                  if ((var33 = var6.b[var9]) != null) {
                     var33.a(var26, var9);
                  }
               }

               this.a.a(var1, var2, var3);
               this.a.a(var1, var2, var3);
               return;
            }

            byte[] var30 = var3.a;
            int var32 = var3.c;
            if (this.a == null || this.a.length < var32 + 1) {
               this.a = new int[var32 + 1];
            }

            int var15 = var32;
            byte[] var14 = var30;
            ab var13 = this;
            this.b = 0;
            this.c = false;
            int var16 = 0;

            do {
               hh var17 = c.a(var14, var16);
               var13.a[var16] = var13.b;
               hh var21;
               if ((var21 = var13.a[var16]) != null) {
                  var13.b += var21.a(var13.b);
               }

               hh var22;
               if ((var22 = var13.b[var16]) != null) {
                  var13.b += var22.a(var13.b);
               } else if (!var13.a[var16]) {
                  var13.b += var17.a(var13.b);
               }

               hh var23;
               if ((var23 = var13.c[var16]) != null) {
                  var13.b += var23.a(var13.b);
               }

               var16 += var17.a(var16);
               if (var13.b > var16) {
                  var13.c = true;
               }
            } while(var16 < var15);

            var13.a[var16] = var13.b;
            int var12 = var13.b;
            if (this.c) {
               var3.a = new byte[var12];
            }

            this.a.a(var12);
            int var18 = var32;
            byte[] var37 = var30;
            ac var36 = var3;
            kn var35 = var2;
            aj var34 = var1;
            var13 = this;
            this.b = 0;
            int var19 = 0;

            hh var20;
            do {
               var20 = c.a(var37, var19);
               hh var29;
               if ((var29 = var13.a[var19]) != null) {
                  var29.a(var34, var35, var36, var19, var13);
                  var13.b += var29.a(var13.b);
               }

               if ((var29 = var13.b[var19]) != null) {
                  var29.a(var34, var35, var36, var19, var13);
                  var13.b += var29.a(var13.b);
               } else if (!var13.a[var19]) {
                  var20.a(var34, var35, var36, var19, var13);
                  var13.b += var20.a(var13.b);
               }

               hh var27;
               if ((var27 = var13.c[var19]) != null) {
                  var27.a(var34, var35, var36, var19, var13);
                  var13.b += var27.a(var13.b);
               }
            } while((var19 += var20.a(var19)) < var18);

            var3.c = var12;
            var3.a(var1, var2, (jn)this);
            int var7 = var3.d;
            m[] var25 = var3.a;
            var28 = 0;

            for(var9 = 0; var9 < var7; ++var9) {
               m var31;
               if ((var31 = var25[var9]).a < var31.b) {
                  var25[var28++] = var31;
               }
            }

            var3.d = var28;
            this.a.a(var1, var2, var3);
            this.a.a(var1, var2, var3);
            var3.b(var1, var2, this);
            this.a.a(var1, var2, var3);
         }

      } catch (RuntimeException var24) {
         System.err.println("Unexpected error while editing code:");
         System.err.println("  Class       = [" + var1.a() + "]");
         System.err.println("  Method      = [" + var2.a(var1) + var2.b(var1) + "]");
         System.err.println("  Exception   = [" + var24.getClass().getName() + "] (" + var24.getMessage() + ")");
         throw var24;
      }
   }

   public final void a(aj var1, kn var2, ac var3, hg var4) {
      this.c = -1;
      var4.a(var1, var2, var3, (ee)this);
   }

   public final void a(aj var1, kn var2, ac var3, mk var4) {
      this.c = 0;
      var4.a(var1, var2, var3, (ee)this);
   }

   public final void a(aj var1, kn var2, ac var3, as var4) {
      var4.a(var1, var2, var3, (fw)this);
      int var9 = var3.c;
      int var8 = var4.a;
      ol[] var7 = var4.a;
      int var10 = 0;

      for(int var11 = 0; var11 < var8; ++var11) {
         ol var5;
         int var6;
         if ((var6 = (var5 = var7[var11]).a) < var9 && (var11 == 0 || var6 > var7[var11 - 1].a)) {
            var7[var10++] = var5;
         }
      }

      var4.a = var10;
   }

   public final void a(aj var1, kn var2, ac var3, pt var4) {
      var4.a(var1, var2, var3, (nn)this);
      int var8 = var3.b;
      int var7 = var4.a;
      ak[] var6 = var4.a;
      int var9 = 0;

      for(int var10 = 0; var10 < var7; ++var10) {
         ak var5;
         if ((var5 = var6[var10]).b > 0 && var5.e < var8) {
            var6[var9++] = var5;
         }
      }

      var4.a = var9;
   }

   public final void a(aj var1, kn var2, ac var3, hy var4) {
      var4.a(var1, var2, var3, (dq)this);
      int var8 = var3.b;
      int var7 = var4.a;
      hk[] var6 = var4.a;
      int var9 = 0;

      for(int var10 = 0; var10 < var7; ++var10) {
         hk var5;
         if ((var5 = var6[var10]).b > 0 && var5.e < var8) {
            var6[var9++] = var5;
         }
      }

      var4.a = var9;
   }

   public final void a(aj var1, kn var2, ac var3, int var4, du var5) {
      this.a.a(var1, var2, var3, this.b, var5);
   }

   public final void a(aj var1, kn var2, ac var3, int var4, hf var5) {
      this.a.a(var1, var2, var3, this.b, var5);
   }

   public final void a(aj var1, kn var2, ac var3, int var4, b var5) {
      this.a.a(var1, var2, var3, this.b, var5);
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ae var5) {
      var5.a = this.a(var4, var5.a);
      this.a.a(var1, var2, var3, this.b, var5);
   }

   public final void a(aj var1, kn var2, ac var3, int var4, dw var5) {
      var5.a = this.a(var4, var5.a);
      this.a(var4, var5.a);
      this.a.a(var1, var2, var3, this.b, (dw)var5);
   }

   public final void a(aj var1, kn var2, ac var3, int var4, gr var5) {
      var5.a = this.a(var4, var5.a);
      this.a(var4, var5.a);
      this.a.a(var1, var2, var3, this.b, (gr)var5);
   }

   public final void a(aj var1, kn var2, ac var3, m var4) {
      var4.a = this.a(var4.a);
      var4.b = this.a(var4.b);
      var4.c = this.a(var4.c);
   }

   public final void a(aj var1, kn var2, ac var3, int var4, mb var5) {
      int var6;
      int var7 = var6 = this.a(var4);
      if (this.c >= 0) {
         var7 -= this.c;
         this.c = var6 + 1;
      }

      var5.c = var7;
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ap var5) {
      this.a(var1, var2, var3, var4, (mb)var5);
      var5.a(var1, var2, var3, var4, (ir)this);
   }

   public final void a(aj var1, kn var2, ac var3, int var4, mf var5) {
      this.a(var1, var2, var3, var4, (mb)var5);
      var5.a(var1, var2, var3, var4, (ir)this);
   }

   public final void a(aj var1, kn var2, ac var3, int var4, jo var5) {
      this.a(var1, var2, var3, var4, (mb)var5);
      var5.a(var1, var2, var3, var4, (ir)this);
      var5.b(var1, var2, var3, var4, this);
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ji var5) {
   }

   public final void a(aj var1, kn var2, ac var3, int var4, js var5) {
      var5.a = this.a(var5.a);
   }

   public final void a(ol var1) {
      var1.a = this.a(var1.a);
   }

   public final void a(ak var1) {
      var1.b = this.a(var1.a, var1.b);
      var1.a = this.a(var1.a);
   }

   public final void a(hk var1) {
      var1.b = this.a(var1.a, var1.b);
      var1.a = this.a(var1.a);
   }

   private void a(int var1, int[] var2) {
      for(int var3 = 0; var3 < var2.length; ++var3) {
         var2[var3] = this.a(var1, var2[var3]);
      }

   }

   private int a(int var1, int var2) {
      return this.a(var1 + var2) - this.a(var1);
   }

   private int a(int var1) {
      if (var1 >= 0 && var1 <= this.a) {
         return this.a[var1];
      } else {
         throw new IllegalArgumentException("Invalid instruction offset [" + var1 + "] in code with length [" + this.a + "]");
      }
   }
}
