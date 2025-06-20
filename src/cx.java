import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public final class cx {
   private static final RuntimeException a = new RuntimeException("SNO: This exception should have been caught and processed");
   private final Hashtable a = new Hashtable();
   private static final Hashtable b = new Hashtable();
   private static final Hashtable c;
   private fg a = null;
   private int a = 0;
   public final fv a;
   private final ha a;
   private Vector a;
   private final Hashtable d = new Hashtable();
   private final Vector b;
   private final Hashtable e = new Hashtable();
   private final Vector c = new Vector();

   public cx(fv var1, ha var2) {
      this.a = var1;
      this.a = var2;
      this.b = new Vector();
      this.b.addElement(new String[]{"java", "lang"});
      Enumeration var4 = this.a.a.elements();

      while(var4.hasMoreElements()) {
         fp var5 = (fp)var4.nextElement();

         try {
            var5.a(new ih(this));
         } catch (ce var3) {
            throw var3.a;
         }
      }

   }

   public final nb[] a() {
      Enumeration var1 = this.a.a.elements();

      while(var1.hasMoreElements()) {
         fp var2 = (fp)var1.nextElement();

         try {
            var2.a(new if(this));
         } catch (he var3) {
            throw var3.a;
         }
      }

      this.a = new Vector();
      var1 = this.a.b.elements();

      while(var1.hasMoreElements()) {
         this.a((dr)((hp)var1.nextElement()));
      }

      if (this.a > 0) {
         throw new ng(this.a + " error(s) while compiling unit \"" + this.a.a + "\"", (aq)null);
      } else {
         Vector var4;
         nb[] var5 = new nb[(var4 = this.a).size()];
         var4.copyInto(var5);
         return var5;
      }
   }

   private void a(dr var1) {
      la var3 = new la(this);

      try {
         var1.a((oy)var3);
      } catch (fy var2) {
         throw var2.a;
      }
   }

   public final void a(hp var1) {
      fv var2 = var1.a();
      String[] var3;
      if ((var3 = this.a(var1.a())) != null) {
         this.a("Package member type declaration \"" + var1.a() + "\" conflicts with single-type-import \"" + ls.a((Object[])var3, (String)".") + "\"", var1.a());
      }

      hp var4;
      if ((var4 = var2.a(var1.a())) != var1) {
         this.a("Redeclaration of type \"" + var1.a() + "\", previously declared in " + var4.a(), var1.a());
      }

      if (var1 instanceof fe) {
         this.a((kg)((fe)var1));
      } else if (var1 instanceof df) {
         this.a((df)var1);
      } else {
         throw new RuntimeException("PMTD of unexpected type " + var1.getClass().getName());
      }
   }

   private void a(kg var1) {
      qd var2 = this.a((dr)var1);
      int var4;
      if ((var1.a & 1024) == 0) {
         gw[] var3 = var2.b();

         for(var4 = 0; var4 < var3.length; ++var4) {
            if (var3[var4].b()) {
               this.a("Non-abstract class \"" + var2 + "\" must implement method \"" + var3[var4] + "\"", var1.a());
            }
         }
      }

      nb var9 = new nb((short)(var1.a | 32), var2.b(), var2.g().b(), qd.a(var2.d()));
      short var10;
      if (!(var1.a() instanceof fv)) {
         short var5;
         if (var1.a() instanceof il) {
            var10 = var9.a(var2.b());
            var5 = this instanceof dm ? var9.c(((dm)this).a()) : 0;
            var9.a(new ln(var10, (short)0, var5, var1.a));
         } else if (var1.a() instanceof ft) {
            var10 = var9.a(var2.b());
            var5 = var9.a(this.a((dr)((ft)var1.a())).b());
            short var6 = var9.c(((ah)var1).a());
            var9.a(new ln(var10, var5, var6, var1.a));
         }
      }

      String var11;
      String var12;
      if ((var12 = var1.a().a()) != null) {
         var11 = var12.substring(var12.lastIndexOf(47) + 1);
      } else if (var1 instanceof dm) {
         var11 = ((dm)var1).a() + ".java";
      } else {
         var11 = "ANONYMOUS.java";
      }

      var9.a(var11);
      if (var1 instanceof oe && ((oe)var1).a()) {
         var9.a();
      }

      il var16 = new il(var1.a());
      Enumeration var13 = var1.d.elements();

      ht var15;
      while(var13.hasMoreElements()) {
         if ((var15 = (ht)var13.nextElement()).b()) {
            ov var7 = (ov)var15;
            var16.a.addElement(var7);
         }
      }

      kl var14 = new kl(var1.a(), (String)null, (short)9, new om(var1.a(), 0), "<clinit>", new ct[0], new jb[0], var16);
      if (this.b((ov)var16)) {
         var14.a(var1);
         this.a((bi)var14, (nb)var9);
      }

      for(var4 = 0; var4 < var1.b.size(); ++var4) {
         this.a((bi)((kl)var1.b.elementAt(var4)), (nb)var9);
      }

      var4 = var1.b.size();
      int var17 = var1.a.size();
      jf[] var18 = var1.a();

      for(int var19 = 0; var19 < var18.length; ++var19) {
         this.a((bi)var18[var19], (nb)var9);
         if (var17 != var1.a.size()) {
            throw new RuntimeException("SNO: Compilation of constructor \"" + var18[var19] + "\" (" + var18[var19].a() + ") added synthetic fields!?");
         }
      }

      for(var17 = var4; var17 < var1.b.size(); ++var17) {
         this.a((bi)((kl)var1.b.elementAt(var17)), (nb)var9);
      }

      var13 = var1.d.elements();

      while(var13.hasMoreElements()) {
         if ((var15 = (ht)var13.nextElement()) instanceof gb) {
            this.a((gb)var15, var9);
         }
      }

      var13 = var1.a.elements();

      while(var13.hasMoreElements()) {
         cm var20 = (cm)var13.nextElement();
         var9.a((short)0, var20.a(), var20.a().b(), (Object)null);
      }

      var13 = var1.c.elements();

      while(var13.hasMoreElements()) {
         ft var21 = (ft)var13.nextElement();
         this.a((dr)var21);
         short var22 = var9.a(this.a((dr)var21).b());
         short var8 = var9.a(var2.b());
         var10 = var9.c(((ah)var21).a());
         var9.a(new ln(var22, var8, var10, var21.a));
      }

      this.a.addElement(var9);
   }

   private void a(gb var1, nb var2) {
      for(int var3 = 0; var3 < var1.a.length; ++var3) {
         cb var4 = var1.a[var3];
         Object var5 = var1.a;

         for(int var6 = 0; var6 < var4.a; ++var6) {
            var5 = new nx((gs)var5);
         }

         Object var8 = null;
         if ((var1.a & 16) != 0 && var4.a != null) {
            if (var4.a instanceof mu) {
               var8 = this.a((mu)var4.a);
            }

            if (var8 == mu.d) {
               var8 = null;
            }
         }

         mc var7;
         if (fd.a(var1.a)) {
            var7 = var2.a(fd.a(var1.a, (short)0), var4.a, this.a((nr)var5).b(), var8);
         } else {
            var7 = var2.a(var1.a, var4.a, this.a((nr)var5).b(), var8);
         }

         if (var1.a()) {
            var7.a((gv)(new mi(var2.c("Deprecated"))));
         }
      }

   }

   public final void a(eo var1) {
      this.a((kw)var1);
   }

   public final void a(jl var1) {
      this.a((kw)var1);
   }

   private void a(kw var1) {
      Vector var2;
      int var3;
      if ((var3 = (var2 = a((dr)var1)).size()) >= 2) {
         var1.a(new lz(this.a((dr)var1), "this$" + (var3 - 2), this.a((dr)((ft)var2.elementAt(1)))));
      }

      this.a((kg)var1);
   }

   public final void a(er var1) {
      this.a((kw)var1);
   }

   public final void a(df var1) {
      var1.a = new qd[var1.a.length];
      String[] var2 = new String[var1.a.length];

      for(int var3 = 0; var3 < var1.a.length; ++var3) {
         var1.a[var3] = this.a((nr)var1.a[var3]);
         var2[var3] = var1.a[var3].b();
      }

      nb var9 = new nb((short)(var1.a | 32 | 512 | 1024), this.a((dr)var1).b(), "Ljava/lang/Object;", var2);
      String var4;
      String var7;
      if ((var4 = var1.a().a()) != null) {
         var7 = var4.substring(var4.lastIndexOf(47) + 1);
      } else {
         var7 = var1.a + ".java";
      }

      var9.a(var7);
      if (var1.a()) {
         var9.a();
      }

      if (!var1.a.isEmpty()) {
         il var8;
         (var8 = new il(var1.a())).b(var1.a);
         if (this.b((ov)var8)) {
            kl var11;
            (var11 = new kl(var1.a(), (String)null, (short)9, new om(var1.a(), 0), "<clinit>", new ct[0], new jb[0], var8)).a(var1);
            this.a((bi)var11, (nb)var9);
         }
      }

      int var10;
      for(var10 = 0; var10 < var1.b.size(); ++var10) {
         this.a((bi)((kl)var1.b.elementAt(var10)), (nb)var9);
      }

      for(var10 = 0; var10 < var1.a.size(); ++var10) {
         ov var12;
         if ((var12 = (ov)var1.a.elementAt(var10)) instanceof gb) {
            this.a((gb)var12, var9);
         }
      }

      Enumeration var13 = var1.c.elements();

      while(var13.hasMoreElements()) {
         ft var14 = (ft)var13.nextElement();
         this.a((dr)var14);
         short var5 = var9.a(this.a((dr)var14).b());
         short var6 = var9.a(this.a((dr)var1).b());
         short var15 = var9.c(((ah)var14).a());
         var9.a(new ln(var5, var6, var15, var1.a));
      }

      this.a.addElement(var9);
   }

   private boolean a(ov var1) {
      boolean[] var2 = new boolean[1];
      kz var4 = new kz(this, var2);

      try {
         var1.a((lr)var4);
         return var2[0];
      } catch (cr var3) {
         throw var3.a;
      }
   }

   private boolean b(il var1) {
      this.a.a();

      try {
         boolean var2 = true;
         int var3 = 0;

         while(true) {
            if (var3 < var1.a.size()) {
               ov var4 = (ov)var1.a.elementAt(var3);
               if (var2) {
                  var2 = this.a(var4);
                  ++var3;
                  continue;
               }

               this.a("Statement is unreachable", var4.a());
            }

            boolean var7 = var2;
            return var7;
         }
      } finally {
         this.a.b();
      }
   }

   private boolean a(os var1) {
      this.a.a();

      label127: {
         boolean var3;
         try {
            if (var1.a != null) {
               this.a(var1.a);
            }

            if (var1.a == null) {
               boolean var8 = this.a((nu)var1, (ov)var1.b, (mu[])var1.a);
               return var8;
            }

            Object var2;
            if ((var2 = this.a(var1.a)) == null || !Boolean.TRUE.equals(var2)) {
               fg var10002 = this.a;
               var10002.getClass();
               gx var7 = new gx(var10002);
               this.a(var1, -89, var7);
               fg var10003 = this.a;
               var10003.getClass();
               var1.b = new gx(var10003);
               var1.a = false;
               gx var9 = this.a.a();
               boolean var4 = this.a(var1.b);
               var1.b.b();
               if (var1.a != null && (var4 || var1.a)) {
                  for(int var10 = 0; var10 < var1.a.length; ++var10) {
                     this.b(var1.a[var10]);
                  }
               }

               var7.b();
               this.a(var1.a, var9, true);
               break label127;
            }

            var3 = this.a((nu)var1, (ov)var1.b, (mu[])var1.a);
         } finally {
            this.a.b();
         }

         return var3;
      }

      if (var1.a != null) {
         var1.a.b();
      }

      return true;
   }

   private boolean a(nu var1, ov var2, mu[] var3) {
      if (var3 == null) {
         var1.b = this.a.a();
         var1.a = false;
         if (this.a(var2)) {
            this.a(var1, -89, var1.b);
         }

         if (var1.a == null) {
            return false;
         } else {
            var1.a.b();
            return true;
         }
      } else {
         var3 = var3;
         cx var5 = this;
         fg var10003 = this.a;
         var10003.getClass();
         var1.b = new gx(var10003);
         var1.a = false;
         gx var4 = this.a.a();
         boolean var6 = this.a(var2);
         var1.b.b();
         if (var6 || var1.a) {
            for(int var7 = 0; var7 < var3.length; ++var7) {
               var5.b(var3[var7]);
            }

            var5.a(var1, -89, var4);
         }

         if (var1.a == null) {
            return false;
         } else {
            var1.a.b();
            return true;
         }
      }
   }

   private static int[] a(Hashtable var0) {
      int[] var1 = new int[var0.size()];
      int var2 = 0;

      for(Enumeration var4 = var0.keys(); var4.hasMoreElements(); ++var2) {
         var1[var2] = (Integer)var4.nextElement();
      }

      for(var2 = var1.length - 1; var2 > 0; --var2) {
         for(int var3 = 0; var3 < var2; ++var3) {
            if (var1[var3] > var1[var3 + 1]) {
               int var5 = var1[var3];
               var1[var3] = var1[var3 + 1];
               var1[var3 + 1] = var5;
            }
         }
      }

      return var1;
   }

   private boolean a(hx var1) {
      Object var2 = this.a(var1.a);
      Object var3 = var1.b != null ? var1.b : new g(var1.a.a());
      gx var9;
      if (!(var2 instanceof Boolean)) {
         fg var10002;
         if (this.b(var1.a)) {
            if (this.b((ov)var3)) {
               var10002 = this.a;
               var10002.getClass();
               var9 = new gx(var10002);
               var10002 = this.a;
               var10002.getClass();
               gx var11 = new gx(var10002);
               this.a(var1.a, var9, false);
               boolean var12;
               if (var12 = this.a(var1.a)) {
                  this.a(var1, -89, var11);
               }

               var9.b();
               boolean var13 = this.a((ov)var3);
               var11.b();
               return var12 || var13;
            } else {
               var10002 = this.a;
               var10002.getClass();
               var9 = new gx(var10002);
               this.a(var1.a, var9, false);
               this.a(var1.a);
               var9.b();
               return true;
            }
         } else if (this.b((ov)var3)) {
            var10002 = this.a;
            var10002.getClass();
            var9 = new gx(var10002);
            this.a(var1.a, var9, true);
            this.a((ov)var3);
            var9.b();
            return true;
         } else {
            qd var10;
            if ((var10 = this.b(var1.a)) != qd.i) {
               this.a("Not a boolean expression", var1.a());
            }

            this.b((t)var1, (qd)var10);
            return true;
         }
      } else {
         this.a(var1.a);
         Object var4;
         if ((Boolean)var2) {
            var2 = var1.a;
            var4 = var3;
         } else {
            var2 = var3;
            var4 = var1.a;
         }

         ex var5 = this.a.a();
         if (this.a((ov)var2)) {
            return true;
         } else {
            hu var8;
            for(var8 = var1.a(); var8 instanceof il; var8 = var8.a()) {
            }

            if (var8 instanceof bi) {
               throw a;
            } else {
               var9 = this.a.a();
               this.a.a(var5);

               try {
                  this.a((t)var1, (Object)(new Integer(0)));
                  this.a(var1, -102, var9);
               } finally {
                  this.a.e();
               }

               return this.a((ov)var4);
            }
         }
      }
   }

   private static jl a(hu var0, String var1) {
      hu var2;
      for(; !((var2 = var0.a()) instanceof fv); var0 = var2) {
         if (var0 instanceof ov && var2 instanceof il) {
            ov var6 = (ov)var0;
            il var3;
            Enumeration var7 = (var3 = (il)var2).a.elements();

            ov var4;
            do {
               ox var5;
               if ((var4 = (ov)var7.nextElement()) instanceof ox && (var5 = (ox)var4).a.a.equals(var1)) {
                  return var5.a;
               }
            } while(var4 != var6);
         }
      }

      return null;
   }

   private po a(jv var1, cb var2) {
      if (var2.a == null) {
         Object var3 = var1.a;

         for(int var4 = 0; var4 < var2.a; ++var4) {
            var3 = new nx((gs)var3);
         }

         var2.a = new po((var1.a & 16) != 0, this.a((nr)var3));
      }

      return var2.a;
   }

   private boolean a(pk var1) {
      if (!this.a.a.a(this.b(var1.a))) {
         this.a("Monitor object of \"synchronized\" statement is not a subclass of \"Object\"", var1.a());
      }

      this.a.a();
      boolean var2 = false;

      try {
         var1.a = this.a.a((short)1);
         this.b(var1, 89);
         this.a(var1, this.a.a, (short)var1.a);
         this.b(var1, -62);
         fg var10002 = this.a;
         var10002.getClass();
         gx var3 = new gx(var10002);
         gx var4 = this.a.a();
         if (var2 = this.a(var1.a)) {
            this.a(var1, -89, var3);
         }

         gx var5 = this.a.a();
         this.a.a(var4, var5, var5, (String)null);
         this.a((ov)var1, (qd)this.a.a("Ljava/lang/Throwable;"));
         this.b(var1, -65);
         if (var2) {
            var3.b();
            this.a((ov)var1, (qd)null);
         }
      } finally {
         this.a.b();
      }

      return var2;
   }

   private boolean a(ga var1) {
      if (var1.a != null) {
         fg var10003 = this.a;
         var10003.getClass();
         var1.a = new gx(var10003);
      }

      gx var2 = this.a.a();
      fg var10002 = this.a;
      var10002.getClass();
      gx var3 = new gx(var10002);
      this.a.a();

      boolean var24;
      try {
         short var4 = var1.a != null ? this.a.a((short)1) : 0;
         boolean var5 = this.a(var1.a);
         gx var6 = this.a.a();
         if (var5) {
            this.a(var1, -89, var3);
         }

         if (var2.a != var6.a) {
            this.a.a();

            try {
               short var7 = this.a.a((short)1);

               for(int var8 = 0; var8 < var1.a.size(); ++var8) {
                  iq var9 = (iq)var1.a.elementAt(var8);
                  qd var10 = this.a((nr)var9.a.a);
                  this.a.a(var2, var6, this.a.a(), var10.b());
                  this.a(var9, var10, (short)var7);
                  this.a(var9.a).a = var7;
                  if (this.a((ov)var9.a)) {
                     var5 = true;
                     if (var8 < var1.a.size() - 1 || var1.a != null) {
                        this.a(var9, -89, var3);
                     }
                  }
               }
            } finally {
               this.a.b();
            }
         }

         if (var1.a != null) {
            gx var23 = this.a.a();
            this.a.a(var2, var23, var23, (String)null);
            this.a.a();

            try {
               short var25 = this.a.a((short)1);
               this.a(var1.a, this.a.a, (short)var25);
               this.a(var1.a, -88, var1.a);
               this.a(var1.a, this.a.a, (int)var25);
               this.b(var1.a, -65);
               var1.a.b();
               this.a(var1.a, this.a.a, (short)var4);
               if (this.a((ov)var1.a)) {
                  this.b(var1.a, -87);
                  this.a((int)var4);
               }
            } finally {
               this.a.b();
            }
         }

         var3.b();
         if (var5) {
            this.a((ov)var1, (qd)null);
         }

         var24 = var5;
      } finally {
         this.a.b();
      }

      return var24;
   }

   private void a(bi var1, nb var2) {
      ig var3;
      if (fd.a(var1.a)) {
         if (var1 instanceof kl && !var1.a) {
            var3 = var2.a((short)(fd.a(var1.a, (short)0) | 8), var1.a + '$', gf.a(this.a((kl)var1).a(), this.a(var1.a()).b()));
         } else {
            var3 = var2.a(fd.a(var1.a, (short)0), var1.a, this.a(var1).a());
         }
      } else {
         var3 = var2.a(var1.a, var1.a, this.a(var1).a());
      }

      short var4 = var2.c("Exceptions");
      short[] var5 = new short[var1.a.length];

      for(int var6 = 0; var6 < var1.a.length; ++var6) {
         var5[var6] = var2.a(this.a((nr)var1.a[var6]).b());
      }

      var3.a((gv)(new dx(var4, var5)));
      if (var1.a()) {
         var3.a((gv)(new mi(var2.c("Deprecated"))));
      }

      if ((var1.a & 1280) == 0) {
         fg var15 = new fg(var3.a());
         fg var16 = this.a(var15);

         try {
            if ((var1.a & 8) == 0) {
               this.a.a((short)1);
            }

            po var9;
            if (var1 instanceof jf) {
               jf var17;
               Enumeration var7 = ((kg)(var17 = (jf)var1).a()).a.elements();

               while(var7.hasMoreElements()) {
                  cm var8 = (cm)var7.nextElement();
                  (var9 = new po(true, var8.a())).a = this.a.a(nq.a(var8.a().b()));
                  var17.a.put(var8.a(), var9);
               }
            }

            Vector var18 = new Vector();

            for(int var19 = 0; var19 < var1.a.length; ++var19) {
               ct var22 = var1.a[var19];
               if (var18.contains(var22.a)) {
                  this.a("Redefinition of formal parameter \"" + var22.a + "\"", var1.a());
               }

               (var9 = this.a(var22)).a = this.a.a(nq.a(var9.a.b()));
               if (!var18.contains(var22.a)) {
                  var18.addElement(var22.a);
               }
            }

            if (var1 instanceof jf) {
               jf var21;
               if ((var21 = (jf)var1).a != null) {
                  this.a((ov)var21.a);
                  if (var21.a instanceof nd) {
                     this.a(var21);
                     this.b(var21);
                  }
               } else {
                  py var23 = null;
                  qd var24;
                  if ((var24 = this.a((dr)((kg)var21.a())).g().f()) != null) {
                     var23 = new py(var21.a(), new hz(var21.a(), var24));
                  }

                  nd var20;
                  (var20 = new nd(var21.a(), var23, new mu[0])).a((hu)var1);
                  this.a((ov)var20);
                  this.a(var21);
                  this.b(var21);
               }
            }

            try {
               if (this.a((ov)var1.a)) {
                  if (this.a(var1) != qd.a) {
                     this.a("Method must return a value", var1.a());
                  }

                  this.b(var1, -79);
               }
            } catch (RuntimeException var13) {
               if (var13 != a) {
                  throw var13;
               }
            }
         } finally {
            this.a(var16);
         }

         if (this.a <= 0) {
            var15.c();
            var15.d();
            var15.a(var1.toString());
            var3.a((gv)(new lc(this, var2.c("Code"), var15)));
         }
      }
   }

   private po a(ct var1) {
      if (var1.a == null) {
         var1.a = new po(var1.a, this.a((nr)var1.a));
      }

      return var1.a;
   }

   private void a(mu var1) {
      fg var10001 = new fg;
      fg var2;
      if ((var2 = this.a) == null) {
         throw new RuntimeException("S.N.O.: Null CodeContext");
      } else {
         var10001.<init>(var2.a());
         var2 = this.a(var10001);

         try {
            this.a(var1);
            this.a(var1);
         } finally {
            this.a(var2);
         }

      }
   }

   private void b(mu var1) {
      lb var3 = new lb(this);

      try {
         var1.a((bn)var3);
      } catch (oc var2) {
         throw var2.a;
      }
   }

   private void a(mu var1, gx var2, boolean var3) {
      le var5 = new le(this, var2, var3);

      try {
         var1.a((bn)var5);
      } catch (nv var4) {
         throw var4.a;
      }
   }

   private void b(mu var1, gx var2, boolean var3) {
      qd var4 = this.b(var1);
      ha var5 = this.a;
      if (var4 == var5.a("Ljava/lang/Boolean;")) {
         this.d(var1, var5.a("Ljava/lang/Boolean;"), qd.i);
      } else if (var4 != qd.i) {
         this.a("Not a boolean expression", var1.a());
      }

      this.a(var1, var3 ? -102 : -103, var2);
   }

   private int a(mu var1) {
      int[] var2 = new int[1];
      ld var4 = new ld(this, var2);

      try {
         var1.a((bn)var4);
         return var2[0];
      } catch (me var3) {
         throw var3.a;
      }
   }

   private qd a(mu var1) {
      qd[] var2 = new qd[1];
      lf var4 = new lf(this, var2);

      try {
         var1.a((bn)var4);
         return var2[0];
      } catch (jq var3) {
         throw var3.a;
      }
   }

   private qd a(ml var1) {
      fg var10002 = this.a;
      var10002.getClass();
      gx var2 = new gx(var10002);
      this.a(var1, var2, true);
      this.b(var1, 3);
      var10002 = this.a;
      var10002.getClass();
      gx var3 = new gx(var10002);
      this.a(var1, -89, var3);
      var2.b();
      this.b(var1, 4);
      var3.b();
      return qd.i;
   }

   private void a(fu var1, qd var2) {
      if (!var2.d()) {
         String var4 = "Array initializer not allowed for non-array type \"" + var2.toString() + "\"";
         this.a((String)var4, (aq)null);
      }

      var2 = var2.h();
      this.a((t)var1, (Object)(new Integer(var1.a.length)));
      this.a(var1, 1, 0, var2);

      for(int var3 = 0; var3 < var1.a.length; ++var3) {
         this.b(var1, 89);
         this.a((t)var1, (Object)(new Integer(var3)));
         ho var5;
         if ((var5 = var1.a[var3]) instanceof mu) {
            mu var6 = (mu)var5;
            this.a((t)var1, (qd)this.b(var6), (qd)var2, (Object)this.a(var6));
         } else {
            if (!(var5 instanceof fu)) {
               throw new RuntimeException("Unexpected array initializer or rvalue class " + var5.getClass().getName());
            }

            this.a((fu)var5, var2);
         }

         this.b(var1, 79 + c(var2));
      }

   }

   private qd b(mu var1) {
      Object var2;
      if ((var2 = this.a(var1)) != null) {
         this.a(var1);
         this.a((t)var1, (Object)var2);
         return this.a((nr)var1);
      } else {
         this.a(var1);
         return this.a(var1);
      }
   }

   public final Object a(mu var1) {
      if (var1.c != mu.b) {
         return var1.c;
      } else {
         Object[] var2 = new Object[1];
         bo var4 = new bo(this, var2);

         try {
            var1.a((bn)var4);
            var1.c = var2[0];
            return var1.c;
         } catch (jh var3) {
            throw var3.a;
         }
      }
   }

   private final Object b(mu var1) {
      Object[] var2 = new Object[1];
      bp var4 = new bp(this, var2);

      try {
         var1.a((bn)var4);
         return var2[0];
      } catch (kd var3) {
         throw var3.a;
      }
   }

   private Object a(ne var1) {
      if (var1.a instanceof Integer) {
         return new Integer(-(Integer)var1.a);
      } else if (var1.a instanceof Long) {
         return new Long(-(Long)var1.a);
      } else if (var1.a instanceof Float) {
         return new Float(-(Float)var1.a);
      } else if (var1.a instanceof Double) {
         return new Double(-(Double)var1.a);
      } else {
         this.a("Cannot negate this literal", var1.a());
         return null;
      }
   }

   private boolean b(ov var1) {
      boolean[] var2 = new boolean[1];
      bj var4 = new bj(this, var2);

      try {
         var1.a((lr)var4);
         return var2[0];
      } catch (de var3) {
         throw var3.a;
      }
   }

   public final boolean a(lh var1) {
      return this.b((ov)var1.a);
   }

   public final boolean a(il var1) {
      for(int var2 = 0; var2 < var1.a.size(); ++var2) {
         if (this.b((ov)var1.a.elementAt(var2))) {
            return true;
         }
      }

      return false;
   }

   public final boolean a(gb var1) {
      for(int var2 = 0; var2 < var1.a.length; ++var2) {
         cb var3 = var1.a[var2];
         if (this.a(var1, var3) != null) {
            return true;
         }
      }

      return false;
   }

   private void a(ov var1, qd var2) {
      bk var3 = new bk(this, var2);
      var1.a((lr)var3);
   }

   public final void a(pk var1) {
      this.a(var1, this.a.a, (int)var1.a);
      this.b(var1, -61);
   }

   public final void a(ga var1, qd var2) {
      if (var1.a != null) {
         this.a.a();

         try {
            short var3 = 0;
            if (var2 != null) {
               var3 = this.a.a(nq.a(var2.b()));
               this.a(var1, var2, (short)var3);
            }

            this.a(var1, -88, var1.a);
            if (var2 != null) {
               this.a(var1, var2, (int)var3);
            }
         } finally {
            this.a.b();
         }

      }
   }

   private void a(jt var1) {
      bl var3 = new bl(this);

      try {
         var1.a(var3);
      } catch (fr var2) {
         throw var2.a;
      }
   }

   private qd a(nr var1) {
      qd[] var2 = new qd[1];
      bm var3 = new bm(this, var2);

      try {
         var1.a(var3);
         return var2[0] != null ? var2[0] : this.a.a;
      } catch (og var4) {
         throw var4.a;
      }
   }

   private qd a(jb var1) {
      ov var2 = null;
      dr var3 = null;
      hu var5 = var1.a();

      while(true) {
         if (var5 instanceof ov && var2 == null) {
            var2 = (ov)var5;
         }

         if (var5 instanceof dr && var3 == null) {
            var3 = (dr)var5;
         }

         if (var5 instanceof fv) {
            fv var4 = (fv)var5;
            String var6;
            qd var13;
            if (var1.a.length == 1) {
               String var19 = var1.a[0];
               jl var20;
               if ((var20 = a(var1.a(), var19)) != null) {
                  return this.a((dr)var20);
               }

               Object var21;
               if (var3 != null) {
                  for(var21 = var3; !(var21 instanceof fv); var21 = ((hu)var21).a()) {
                     if (var21 instanceof dr && (var13 = this.a(this.a((dr)((ft)var21)), var19, var1.a())) != null) {
                        return var13;
                     }
                  }
               }

               qd var22;
               if (var4 != null) {
                  if ((var22 = this.a(var19, var1.a())) != null) {
                     return var22;
                  }

                  hp var23;
                  if ((var23 = var4.a(var19)) != null) {
                     return this.a((dr)((ft)var23));
                  }
               }

               String var10000 = var4.a == null ? null : var4.a.a;
               var6 = var10000;
               String var16 = var10000 == null ? var19 : var6 + "." + var19;

               qd var14;
               try {
                  var14 = this.a.b(nq.b(var16));
               } catch (ClassNotFoundException var11) {
                  if (var11.getMessage().indexOf("CompileException") != -1) {
                     throw new ng(var11.getMessage().substring(var11.getMessage().indexOf(91) + 1, var11.getMessage().lastIndexOf(93)), (aq)null);
                  }

                  throw new ng(var16, var1.a(), var11);
               }

               if (var14 != null) {
                  return var14;
               }

               if ((var22 = this.b(var19, var1.a())) != null) {
                  return var22;
               }

               if ((var21 = this.e.get(var19)) instanceof qd) {
                  return (qd)var21;
               }

               var22 = null;
               Enumeration var17 = this.c.elements();

               while(var17.hasMoreElements()) {
                  qd[] var15 = ((qd)var17.nextElement()).c();

                  for(int var7 = 0; var7 < var15.length; ++var7) {
                     qd var8 = var15[var7];
                     if (this.a(var8, var2) && var8.b().endsWith('$' + var19 + ';')) {
                        if (var22 != null) {
                           String var9 = "Ambiguous static imports: \"" + var22.toString() + "\" vs. \"" + var8.toString() + "\"";
                           this.a((String)var9, (aq)null);
                        }

                        var22 = var8;
                     }
                  }
               }

               if (var22 != null) {
                  return var22;
               }

               this.a("Cannot determine simple type name \"" + var19 + "\"", var1.a());
               return this.a.a;
            }

            nr var18;
            if ((var18 = this.a(var1.a(), var1.a(), var1.a, var1.a.length - 1)) instanceof ju) {
               var6 = ls.a((Object[])var1.a, (String)".");

               try {
                  var13 = this.a.b(nq.b(var6));
               } catch (ClassNotFoundException var10) {
                  if (var10.getMessage().indexOf("CompileException") != -1) {
                     throw new ng(var10.getMessage().substring(var10.getMessage().indexOf(91) + 1, var10.getMessage().lastIndexOf(93)), (aq)null);
                  }

                  throw new ng(var6, var1.a(), var10);
               }

               if (var13 != null) {
                  return var13;
               }

               this.a("Class \"" + var6 + "\" not found", var1.a());
               return this.a.a;
            }

            var6 = var1.a[var1.a.length - 1];
            qd[] var12;
            if ((var12 = this.a((nr)this.a(var18)).a(var6)).length == 1) {
               return var12[0];
            }

            if (var12.length == 0) {
               this.a("\"" + var18 + "\" declares no member type \"" + var6 + "\"", var1.a());
            } else {
               this.a("\"" + var18 + "\" and its supertypes declare more than one member type \"" + var6 + "\"", var1.a());
            }

            return this.a.a;
         }

         var5 = var5.a();
      }
   }

   private qd a(qd var1) {
      qd var2;
      return (var2 = this.c(var1)) != null ? var2 : var1;
   }

   private qd a(ne var1) {
      if (var1.a instanceof Integer) {
         return qd.f;
      } else if (var1.a instanceof Long) {
         return qd.g;
      } else if (var1.a instanceof Float) {
         return qd.e;
      } else if (var1.a instanceof Double) {
         return qd.d;
      } else if (var1.a instanceof String) {
         return this.a.b;
      } else if (var1.a instanceof Character) {
         return qd.c;
      } else if (var1.a instanceof Boolean) {
         return qd.i;
      } else if (var1.a == null) {
         return qd.a;
      } else {
         throw new RuntimeException("SNO: Unidentifiable literal type \"" + var1.a.getClass().getName() + "\"");
      }
   }

   private boolean a(nr var1) {
      boolean[] var2 = new boolean[1];
      bt var4 = new bt(this, var2);

      try {
         var1.a(var4);
         return var2[0];
      } catch (lt var3) {
         throw var3.a;
      }
   }

   private void a(fb var1, ov var2) {
      qd var10001 = var1.b();
      pe var10002 = var1.a();
      ov var3 = var2;
      pe var6 = var10002;
      qd var4 = var10001;
      String var5;
      if ((var5 = this.a(var4, var6, var3)) != null) {
         this.a(var5, var3.a());
      }

   }

   private String a(qd var1, pe var2, ov var3) {
      if (var2 == pe.d) {
         return null;
      } else {
         hu var7;
         for(var7 = var3.a(); !(var7 instanceof dr); var7 = var7.a()) {
         }

         qd var6 = this.a((dr)var7);
         if (var6 == var1) {
            return null;
         } else {
            qd var8 = var1;

            qd var4;
            for(var4 = var1.e(); var4 != null; var4 = var4.e()) {
               var8 = var4;
            }

            var4 = var6;

            for(qd var5 = var6.e(); var5 != null; var5 = var5.e()) {
               var4 = var5;
            }

            if (var8 == var4) {
               return null;
            } else if (var2 == pe.a) {
               return "Private member cannot be accessed from type \"" + var6 + "\".";
            } else {
               String var10000 = var1.b();
               String var10 = var6.b();
               String var9 = nq.d(var10000);
               var10 = nq.d(var10);
               if (var9 == null ? var10 == null : var9.equals(var10)) {
                  return null;
               } else if (var2 == pe.c) {
                  return "Member with \"" + var2 + "\" access cannot be accessed from type \"" + var6 + "\".";
               } else {
                  return !var1.a(var6) ? "Protected member cannot be accessed from type \"" + var6 + "\", which is neither declared in the same package as nor is a subclass of \"" + var1 + "\"." : null;
               }
            }
         }
      }
   }

   private boolean a(qd var1, ov var2) {
      return null == this.a(var1, var2);
   }

   private String a(qd var1, ov var2) {
      qd var3;
      if ((var3 = var1.e()) != null) {
         return this.a(var3, var1.a(), var2);
      } else if (var1.a() == pe.d) {
         return null;
      } else if (var1.a() != pe.c) {
         throw new RuntimeException("\"" + var1 + "\" has unexpected access \"" + var1.a() + "\"");
      } else {
         hu var6;
         for(var6 = var2.a(); !(var6 instanceof dr); var6 = var6.a()) {
         }

         qd var4 = this.a((dr)var6);
         String var7 = nq.d(var1.b());
         String var5 = nq.d(var4.b());
         if (var7 == null) {
            if (var5 != null) {
               return "\"" + var1 + "\" is inaccessible from this package";
            }
         } else if (!var7.equals(var5)) {
            return "\"" + var1 + "\" is inaccessible from this package";
         }

         return null;
      }
   }

   private final gs a(nr var1) {
      gs var2;
      if ((var2 = var1.a()) == null) {
         this.a("Expression \"" + var1.toString() + "\" is not a type", var1.a());
         return new hz(var1.a(), this.a.a);
      } else {
         return var2;
      }
   }

   private final mu a(nr var1) {
      mu var2;
      if ((var2 = var1.a()) == null) {
         this.a("Expression \"" + var1.toString() + "\" is not an rvalue", var1.a());
         return new ne(var1.a(), "X");
      } else {
         return var2;
      }
   }

   private jt a(nr var1) {
      jt var2;
      if ((var2 = var1.a()) == null) {
         this.a("Expression \"" + var1.toString() + "\" is not an lvalue", var1.a());
         return new bu(this, var1.a(), var1);
      } else {
         return var2;
      }
   }

   private void a(jf var1) {
      Enumeration var2 = ((kg)var1.a()).a.elements();

      while(var2.hasMoreElements()) {
         cm var3 = (cm)var2.nextElement();
         po var4;
         if ((var4 = (po)var1.a.get(var3.a())) == null) {
            throw new RuntimeException("SNO: Synthetic parameter for synthetic field \"" + var3.a() + "\" not found");
         }

         try {
            nh var6;
            (var6 = new nh(new pq(var1.a(), new gq(var1.a(), new cc(var1.a()), var3), "=", new qc(var1.a(), var4)))).a(var1);
            this.a((ov)var6);
         } catch (pc var5) {
            throw new RuntimeException("S.N.O.");
         }
      }

   }

   private void b(jf var1) {
      for(int var2 = 0; var2 < ((kg)var1.a()).d.size(); ++var2) {
         ht var3;
         if (!(var3 = (ht)((kg)var1.a()).d.elementAt(var2)).b()) {
            ov var4 = (ov)var3;
            if (!this.a(var4)) {
               this.a("Instance variable declarator or instance initializer does not complete normally", var4.a());
            }
         }
      }

   }

   private void a(hu var1, hu var2, qd var3) {
      for(var1 = var1; var1 != var2; var1 = var1.a()) {
         if (var1 instanceof ov) {
            this.a((ov)var1, var3);
         }
      }

   }

   private qd a(t var1, qd var2, String var3, mu var4) {
      Vector var5;
      (var5 = new Vector(1)).addElement(var4);
      return this.a(var1, var2, var5.elements(), var3);
   }

   private qd a(t var1, qd var2, Enumeration var3, String var4) {
      int var5;
      mu var6;
      ex var7;
      qd var8;
      qd var20;
      if (var4 != "|" && var4 != "^" && var4 != "&") {
         qd var16;
         if (var4 != "*" && var4 != "/" && var4 != "%" && var4 != "+" && var4 != "-") {
            if (var4 != "<<" && var4 != ">>" && var4 != ">>>") {
               throw new RuntimeException("Unexpected operator \"" + var4 + "\"");
            } else {
               var5 = var4 == "<<" ? 120 : (var4 == ">>" ? 122 : (var4 == ">>>" ? 124 : Integer.MAX_VALUE));

               do {
                  var6 = (mu)var3.nextElement();
                  if (var2 == null) {
                     var2 = this.b(var6);
                  } else {
                     var7 = this.a.a();
                     var8 = this.b(var6);
                     this.a.a(var7);

                     try {
                        var20 = this.a(var1, var2);
                     } finally {
                        this.a.e();
                     }

                     if (var20 != qd.f && var20 != qd.g) {
                        this.a("Shift operation not allowed on operand type \"" + var2 + "\"", var1.a());
                     }

                     if ((var16 = this.a(var1, var8)) != qd.f && var16 != qd.g) {
                        this.a("Shift distance of type \"" + var8 + "\" is not allowed", var1.a());
                     }

                     if (var16 == qd.g) {
                        this.b(var1, -120);
                     }

                     this.b(var1, var20 == qd.g ? var5 + 1 : var5);
                     var2 = var20;
                  }
               } while(var3.hasMoreElements());

               return var2;
            }
         } else {
            var5 = var4 == "*" ? 104 : (var4 == "/" ? 108 : (var4 == "%" ? 112 : (var4 == "+" ? 96 : (var4 == "-" ? 100 : Integer.MAX_VALUE))));

            do {
               var6 = (mu)var3.nextElement();
               qd var18 = this.a((nr)var6);
               ha var19 = this.a;
               if (var4 == "+" && (var2 == var19.b || var18 == var19.b)) {
                  return this.a(var1, var2, var6, var3);
               }

               if (var2 == null) {
                  var2 = this.b(var6);
               } else {
                  ex var21 = this.a.a();
                  var16 = this.b(var6);
                  int var17;
                  if ((var2 = this.a(var1, var2, var21, var16)) == qd.f) {
                     var17 = var5;
                  } else if (var2 == qd.g) {
                     var17 = var5 + 1;
                  } else if (var2 == qd.e) {
                     var17 = var5 + 2;
                  } else if (var2 == qd.d) {
                     var17 = var5 + 3;
                  } else {
                     this.a("Unexpected promoted type \"" + var2 + "\"", var1.a());
                     var17 = var5;
                  }

                  this.b(var1, var17);
               }
            } while(var3.hasMoreElements());

            return var2;
         }
      } else {
         var5 = var4 == "&" ? 126 : (var4 == "|" ? -128 : (var4 == "^" ? -126 : Integer.MAX_VALUE));

         do {
            var6 = (mu)var3.nextElement();
            if (var2 == null) {
               var2 = this.b(var6);
            } else {
               var7 = this.a.a();
               var8 = this.b(var6);
               if (var2.f() && var8.f()) {
                  if ((var20 = this.a(var1, var2, var7, var8)) == qd.f) {
                     this.b(var1, var5);
                  } else if (var20 == qd.g) {
                     this.b(var1, var5 + 1);
                  } else {
                     this.a("Operator \"" + var4 + "\" not defined on types \"" + var2 + "\" and \"" + var8 + "\"", var1.a());
                  }

                  var2 = var20;
               } else if (var2 == qd.i && this.a(var8) == qd.i || this.a(var2) == qd.i && var8 == qd.i) {
                  ha var9 = this.a;
                  if (var2 == var9.a("Ljava/lang/Boolean;")) {
                     this.a.a(var7);

                     try {
                        this.d(var1, var9.a("Ljava/lang/Boolean;"), qd.i);
                     } finally {
                        this.a.e();
                     }
                  }

                  if (var8 == var9.a("Ljava/lang/Boolean;")) {
                     this.d(var1, var9.a("Ljava/lang/Boolean;"), qd.i);
                  }

                  this.b(var1, var5);
                  var2 = qd.i;
               } else {
                  this.a("Operator \"" + var4 + "\" not defined on types \"" + var2 + "\" and \"" + var8 + "\"", var1.a());
                  var2 = qd.f;
               }
            }
         } while(var3.hasMoreElements());

         return var2;
      }
   }

   private qd a(t var1, qd var2, mu var3, Enumeration var4) {
      boolean var9;
      if (var2 != null) {
         this.a(var1, var2);
         var9 = true;
      } else {
         var9 = false;
      }

      Vector var5 = new Vector();

      do {
         Object var6;
         if ((var6 = this.a(var3)) == null) {
            var5.addElement(new bv(this, var1, var3));
            var3 = var4.hasMoreElements() ? (mu)var4.nextElement() : null;
         } else {
            if (var4.hasMoreElements()) {
               var3 = (mu)var4.nextElement();
               Object var7;
               if ((var7 = this.a(var3)) != null) {
                  StringBuffer var10 = (new StringBuffer(var6.toString())).append(var7);

                  while(true) {
                     if (!var4.hasMoreElements()) {
                        var3 = null;
                        break;
                     }

                     var3 = (mu)var4.nextElement();
                     Object var8;
                     if ((var8 = this.a(var3)) == null) {
                        break;
                     }

                     var10.append(var8);
                  }

                  var6 = var10.toString();
               }
            } else {
               var3 = null;
            }

            String[] var11 = b(var6.toString());

            for(int var12 = 0; var12 < var11.length; ++var12) {
               String var15 = var11[var12];
               var5.addElement(new bw(this, var1, var15));
            }
         }
      } while(var3 != null);

      Enumeration var14;
      if (var5.size() <= (var9 ? 2 : 3)) {
         var14 = var5.elements();

         while(var14.hasMoreElements()) {
            ((np)var14.nextElement()).a();
            if (var9) {
               this.b(var1, -74);
               this.b("Ljava/lang/String;", "concat", "(Ljava/lang/String;)Ljava/lang/String;");
            } else {
               var9 = true;
            }
         }

         return this.a.b;
      } else {
         var14 = var5.elements();
         String var13 = "Ljava/lang/StringBuffer;";
         if (var9) {
            this.b(var1, -69);
            this.a(var13);
            this.b(var1, 90);
            this.b(var1, 95);
         } else {
            this.b(var1, -69);
            this.a(var13);
            this.b(var1, 89);
            ((np)var14.nextElement()).a();
         }

         this.b(var1, -73);
         this.b(var13, "<init>", "(Ljava/lang/String;)V");

         while(var14.hasMoreElements()) {
            ((np)var14.nextElement()).a();
            this.b(var1, -74);
            this.b(var13, "append", "(Ljava/lang/String;)" + var13);
         }

         this.b(var1, -74);
         this.b(var13, "toString", "()Ljava/lang/String;");
         return this.a.b;
      }
   }

   private void a(t var1, qd var2) {
      this.b(var1, -72);
      this.b("Ljava/lang/String;", "valueOf", "(" + (var2 != qd.i && var2 != qd.c && var2 != qd.g && var2 != qd.e && var2 != qd.d ? (var2 != qd.b && var2 != qd.h && var2 != qd.f ? "Ljava/lang/Object;" : "I") : var2.b()) + ")" + "Ljava/lang/String;");
   }

   private void a(t var1, hu var2, mu var3, qd var4, mu[] var5) {
      pm[] var6;
      if ((var6 = var4.b()).length == 0) {
         throw new RuntimeException("SNO: Target class \"" + var4.b() + "\" has no constructors");
      } else {
         pm var21;
         qd[] var7 = (var21 = (pm)this.a((t)var1, (cn[])var6, (mu[])var5)).b();

         for(int var8 = 0; var8 < var7.length; ++var8) {
            this.a(var1, var7[var8], var2);
         }

         qd var25;
         if (var3 != null && (var25 = var4.f()) != null) {
            qd var18 = this.b(var3);
            if (!var25.a(var18)) {
               this.a("Type of enclosing instance (\"" + var18 + "\") is not assignable to \"" + var25 + "\"", var1.a());
            }
         }

         cm[] var26 = var4.b();

         hu var22;
         for(var22 = var2; !(var22 instanceof ht); var22 = var22.a()) {
         }

         ht var19;
         dr var23;
         if (!((var23 = (var19 = (ht)var22).a()) instanceof kg)) {
            if (var26.length > 0) {
               throw new RuntimeException("SNO: Target class has synthetic fields");
            }
         } else {
            kg var24 = (kg)var23;

            for(int var9 = 0; var9 < var26.length; ++var9) {
               cm var10;
               if ((var10 = var26[var9]).a().startsWith("val$")) {
                  po var28;
                  if ((cm)var24.a.get(var10.a()) != null) {
                     if (var19 instanceof kl) {
                        this.a(var1, this.a((dr)var24), (int)0);
                        this.b(var1, -76);
                        this.a(this.a((dr)var24).b(), var10.a(), var10.a().b());
                     } else if (var19 instanceof jf) {
                        jf var29;
                        if ((var28 = (po)(var29 = (jf)var19).a.get(var10.a())) == null) {
                           this.a("Compiler limitation: Constructor cannot access local variable \"" + var10.a().substring(4) + "\" declared in an enclosing block because none of the methods accesses it. As a workaround, declare a dummy method that accesses the local variable.", var1.a());
                           this.b(var1, 1);
                        } else {
                           this.a(var1, var28);
                        }
                     } else {
                        this.a("Compiler limitation: Initializers cannot access local variables declared in an enclosing block.", var1.a());
                        this.b(var1, 1);
                     }
                  } else {
                     String var11 = var10.a().substring(4);
                     hu var12 = var2;

                     label115:
                     while(true) {
                        if (!(var12 instanceof ov)) {
                           while(!(var12 instanceof bi)) {
                              var12 = var12.a();
                           }

                           bi var30 = (bi)var12;

                           for(int var32 = 0; var32 < var30.a.length; ++var32) {
                              ct var35;
                              if ((var35 = var30.a[var32]).a.equals(var11)) {
                                 var28 = this.a(var35);
                                 break label115;
                              }
                           }

                           throw new RuntimeException("SNO: Synthetic field \"" + var10.a() + "\" neither maps a synthetic field of an enclosing instance nor a local variable");
                        }

                        ov var13;
                        hu var14;
                        if ((var14 = (var13 = (ov)var12).a()) instanceof il) {
                           il var15;
                           Enumeration var31 = (var15 = (il)var14).a.elements();

                           label104:
                           while(true) {
                              ov var33;
                              do {
                                 if ((var33 = (ov)var31.nextElement()) == var13) {
                                    break label104;
                                 }
                              } while(!(var33 instanceof jv));

                              jv var34;
                              cb[] var16 = (var34 = (jv)var33).a;

                              for(int var17 = 0; var17 < var16.length; ++var17) {
                                 if (var16[var17].a.equals(var11)) {
                                    var28 = this.a(var34, var16[var17]);
                                    break label115;
                                 }
                              }
                           }
                        }

                        var12 = var12.a();
                     }

                     this.a(var1, var28);
                  }
               }
            }
         }

         qd[] var27 = var21.a();

         for(int var20 = 0; var20 < var5.length; ++var20) {
            this.a(var1, this.b(var5[var20]), var27[var20], this.a(var5[var20]));
         }

         this.b(var1, -73);
         this.b(var4.b(), "<init>", var21.a());
      }
   }

   final cm[] a(gb var1) {
      cm[] var2 = new cm[var1.a.length];

      for(int var3 = 0; var3 < var2.length; ++var3) {
         cb var4 = var1.a[var3];
         qd var10005 = this.a(var1.a());
         var10005.getClass();
         var2[var3] = new av(this, var10005, var1, var4);
      }

      return var2;
   }

   private ho a(gb var1, cb var2) {
      if (var2.a == null) {
         return null;
      } else {
         return (var1.a & 8) != 0 && (var1.a & 16) != 0 && var2.a instanceof mu && this.a((mu)var2.a) != null ? null : var2.a;
      }
   }

   private nr a(gp var1) {
      if (var1.a == null) {
         var1.a = this.a(var1.a(), var1.a(), var1.a, var1.a);
      }

      return var1.a;
   }

   private nr a(aq var1, hu var2, String[] var3, int var4) {
      if (var4 == 1) {
         return this.a(var1, var2, var3[0]);
      } else {
         nr var5 = this.a(var1, var2, var3, var4 - 1);
         String var12 = var3[var4 - 1];
         if (var5 instanceof ju) {
            String var16 = ((ju)var5).a + '.' + var12;

            qd var18;
            try {
               var18 = this.a.b(nq.b(var16));
            } catch (ClassNotFoundException var9) {
               if (var9.getMessage().indexOf("CompileException") != -1) {
                  throw new ng(var9.getMessage().substring(var9.getMessage().indexOf(91) + 1, var9.getMessage().lastIndexOf(93)), (aq)null);
               }

               throw new ng(var16, var1, var9);
            }

            return (nr)(var18 != null ? new hz(var1, var18) : new ju(var1, var16));
         } else if (var12.equals("length") && this.a(var5).d()) {
            cl var15 = new cl(var1, this.a(var5));
            if (!(var2 instanceof ov)) {
               String var10 = "\".length\" only allowed in expression context";
               this.a((String)var10, (aq)null);
               return var15;
            } else {
               var15.a((ov)((ov)var2));
               return var15;
            }
         } else {
            qd var6 = this.a(var5);
            cm var7;
            if ((var7 = this.a(var6, var12, var1)) != null) {
               gq var14;
               (var14 = new gq(var1, var5, var7)).a((ov)((ov)var2));
               return var14;
            } else {
               qd[] var17 = var6.c();

               for(int var13 = 0; var13 < var17.length; ++var13) {
                  String var8;
                  qd var11;
                  if ((var8 = nq.c((var11 = var17[var13]).b())).substring(var8.lastIndexOf(36) + 1).equals(var12)) {
                     return new hz(var1, var11);
                  }
               }

               this.a("\"" + var12 + "\" is neither a method, a field, nor a member class of \"" + var6 + "\"", var1);
               return new au(this, var1, var3);
            }
         }
      }
   }

   private nr a(aq var1, hu var2, String var3) {
      ov var4 = null;
      ht var5 = null;
      ft var6 = null;
      hu var8 = var2;
      if (var2 instanceof ov) {
         var4 = (ov)var2;
      }

      while((var8 instanceof ov || var8 instanceof iq) && !(var8 instanceof ht)) {
         var8 = var8.a();
      }

      if (var8 instanceof ht) {
         var5 = (ht)var8;
         var8 = var8.a();
      }

      if (var8 instanceof dr) {
         var6 = (ft)var8;
         var8 = var8.a();
      }

      while(!(var8 instanceof fv)) {
         var8 = var8.a();
      }

      fv var7 = (fv)var8;
      var8 = var2;
      if (var2 instanceof ov) {
         po var9;
         if ((var9 = this.a((ov)var2, var3)) != null) {
            qc var32 = new qc(var1, var9);
            if (!(var2 instanceof ov)) {
               throw new RuntimeException("SNO: Local variable access in non-block statement context!?");
            }

            var32.a((ov)((ov)var2));
            return var32;
         }

         var8 = var2.a();
      }

      while(var8 instanceof ov || var8 instanceof iq) {
         var8 = var8.a();
      }

      qd var11;
      String var14;
      if (var8 instanceof bi && (var8 = var8.a()) instanceof kw) {
         kw var21 = (kw)var8;
         if ((var8 = var8.a()) instanceof eo) {
            var8 = var8.a();
         }

         while(var8 instanceof ov) {
            po var10;
            if ((var10 = this.a((ov)var8, var3)) != null) {
               if (!var10.a) {
                  var14 = "Cannot access non-final local variable \"" + var3 + "\" from inner class";
                  this.a((String)var14, (aq)null);
               }

               var11 = var10.a;
               lz var12 = new lz(this.a((dr)var21), "val$" + var3, var11);
               var21.a(var12);
               gq var18;
               (var18 = new gq(var1, new py(var1, new hz(var1, this.a((dr)var21))), var12)).a((ov)((ov)var2));
               return var18;
            }

            while((var8 = var8.a()) instanceof ov) {
            }

            if (!(var8 instanceof bi) || !((var8 = var8.a()) instanceof kw)) {
               break;
            }

            var21 = (kw)var8;
            var8 = var8.a();
         }
      }

      ov var38 = null;

      qd var23;
      cm var27;
      for(hu var22 = var2; !(var22 instanceof fv); var22 = var22.a()) {
         if (var22 instanceof ov && var38 == null) {
            var38 = (ov)var22;
         }

         if (var22 instanceof dr) {
            var23 = this.a((dr)((ft)var22));
            if ((var27 = this.a(var23, var3, var1)) != null) {
               hz var33 = new hz(var6.a(), var23);
               Object var19;
               if (var5.b()) {
                  var19 = var33;
               } else if (var27.a()) {
                  var19 = var33;
               } else {
                  var19 = new py(var1, var33);
               }

               gq var29;
               (var29 = new gq(var1, (nr)var19, var27)).a((ov)var38);
               return var29;
            }
         }
      }

      Object var24;
      gq var28;
      if ((var24 = this.e.get(var3)) instanceof cm) {
         (var28 = new gq(var1, new hz(var1, ((cm)var24).b()), (cm)var24)).a((ov)var38);
         return var28;
      } else {
         cm var26 = null;
         Enumeration var25 = this.c.elements();

         int var20;
         while(var25.hasMoreElements()) {
            cm[] var34 = ((qd)var25.nextElement()).c();

            for(var20 = 0; var20 < var34.length; ++var20) {
               if ((var27 = var34[var20]).a().equals(var3)) {
                  qd var10001 = var27.b();
                  pe var15 = var27.a();
                  qd var39 = var10001;
                  if (null == this.a(var39, var15, var38)) {
                     if (var26 != null) {
                        var14 = "Ambiguous static field import: \"" + var26.toString() + "\" vs. \"" + var27.toString() + "\"";
                        this.a((String)var14, (aq)null);
                     }

                     var26 = var27;
                  }
               }
            }
         }

         if (var26 != null) {
            if (!var26.a()) {
               var14 = "Cannot static-import non-static field";
               this.a((String)var14, (aq)null);
            }

            (var28 = new gq(var1, new hz(var1, var26.b()), var26)).a((ov)var38);
            return var28;
         } else if (var3.equals("java")) {
            return new ju(var1, var3);
         } else {
            jl var30;
            if ((var30 = a(var2, var3)) != null) {
               return new hz(var1, this.a((dr)var30));
            } else {
               qd var31;
               if (var6 != null && (var31 = this.a(this.a((dr)var6), var3, var1)) != null) {
                  return new hz(var1, var31);
               } else if (var7 != null && (var31 = this.a(var3, var1)) != null) {
                  return new hz(var1, var31);
               } else {
                  hp var35;
                  if (var7 != null && (var35 = var7.a(var3)) != null) {
                     return new hz(var1, this.a((dr)((ft)var35)));
                  } else {
                     if (var7 != null) {
                        String var36 = var7.a == null ? var3 : var7.a.a + '.' + var3;

                        try {
                           var23 = this.a.b(nq.b(var36));
                        } catch (ClassNotFoundException var17) {
                           if (var17.getMessage().indexOf("CompileException") != -1) {
                              throw new ng(var17.getMessage().substring(var17.getMessage().indexOf(91) + 1, var17.getMessage().lastIndexOf(93)), (aq)null);
                           }

                           throw new ng(var36, var1, var17);
                        }

                        if (var23 != null) {
                           return new hz(var1, var23);
                        }
                     }

                     if (var7 != null && (var31 = this.b(var3, var1)) != null) {
                        return new hz(var1, var31);
                     } else if ((var24 = this.e.get(var3)) instanceof qd) {
                        return new hz((aq)null, (qd)var24);
                     } else {
                        var31 = null;
                        var25 = this.c.elements();

                        while(var25.hasMoreElements()) {
                           qd[] var37 = ((qd)var25.nextElement()).c();

                           for(var20 = 0; var20 < var37.length; ++var20) {
                              var11 = var37[var20];
                              if (this.a(var11, var4) && var11.b().endsWith('$' + var3 + ';')) {
                                 if (var31 != null) {
                                    var14 = "Ambiguous static type import: \"" + var31.toString() + "\" vs. \"" + var11.toString() + "\"";
                                    this.a((String)var14, (aq)null);
                                 }

                                 var31 = var11;
                              }
                           }
                        }

                        if (var31 != null) {
                           return new hz((aq)null, var31);
                        } else {
                           return new ju(var1, var3);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private po a(ov var1, String var2) {
      hu var3;
      for(Object var8 = var1; !(var8 instanceof fv); var8 = var3) {
         var3 = ((hu)var8).a();
         ov var4;
         po var9;
         if (var8 instanceof os && (var4 = ((os)var8).a) instanceof jv && (var9 = this.a((jv)var4, var2)) != null) {
            return var9;
         }

         ov var5;
         Enumeration var11;
         if (var3 instanceof il) {
            il var10;
            var11 = (var10 = (il)var3).a.elements();

            do {
               po var6;
               if ((var5 = (ov)var11.nextElement()) instanceof jv && (var6 = this.a((jv)var5, var2)) != null) {
                  return var6;
               }
            } while(var5 != var8);
         }

         if (var3 instanceof bq) {
            bq var12;
            var11 = (var12 = (bq)var3).a.elements();

            label78:
            while(true) {
               Enumeration var15 = ((jk)var11.nextElement()).b.elements();

               while(var15.hasMoreElements()) {
                  po var7;
                  if ((var5 = (ov)var15.nextElement()) instanceof jv && (var7 = this.a((jv)var5, var2)) != null) {
                     return var7;
                  }

                  if (var5 == var8) {
                     break label78;
                  }
               }
            }
         }

         if (var8 instanceof bi) {
            bi var16;
            ct[] var17 = (var16 = (bi)var8).a;

            for(int var13 = 0; var13 < var17.length; ++var13) {
               if (var17[var13].a.equals(var2)) {
                  return this.a(var17[var13]);
               }
            }

            return null;
         }

         iq var14;
         if (var8 instanceof iq && (var14 = (iq)var8).a.a.equals(var2)) {
            return this.a(var14.a);
         }
      }

      return null;
   }

   private po a(jv var1, String var2) {
      cb[] var3 = var1.a;

      for(int var4 = 0; var4 < var3.length; ++var4) {
         if (var3[var4].a.equals(var2)) {
            return this.a(var1, var3[var4]);
         }
      }

      return null;
   }

   private void a(mz var1) {
      if (var1.a == null) {
         qd var2 = this.a(var1.a);
         if (var1.a.equals("length") && var2.d()) {
            var1.a = new cl(var1.a(), this.a(var1.a));
         } else {
            cm var3;
            if ((var3 = this.a(var2, var1.a, var1.a())) == null) {
               this.a("\"" + this.a(var1.a).toString() + "\" has no field \"" + var1.a + "\"", var1.a());
               var1.a = new at(this, var1.a());
               return;
            }

            var1.a = new gq(var1.a(), var1.a, var3);
         }

         var1.a.a(var1.a());
      }
   }

   private void a(cu var1) {
      if (var1.a == null) {
         cc var3;
         (var3 = new cc(var1.a())).a((ov)var1.a());
         qd var2;
         if (var1.a != null) {
            var2 = this.a((nr)var1.a);
         } else {
            var2 = this.a((nr)var3);
         }

         nj var4 = new nj(var1.a(), new hz(var1.a(), var2.g()), var3);
         cm var5;
         if ((var5 = this.a(this.a((nr)var4), var1.a, var1.a())) == null) {
            this.a("Class has no field \"" + var1.a + "\"", var1.a());
            var1.a = new az(this, var1.a());
         } else {
            var1.a = new gq(var1.a(), var4, var5);
            var1.a.a(var1.a());
         }
      }
   }

   private gw a(gt var1) {
      gw var2;
      label54: {
         Object var3;
         if (var1.a == null) {
            for(var3 = var1.a(); !(var3 instanceof fv); var3 = ((hu)var3).a()) {
               if (var3 instanceof dr) {
                  dr var4 = (dr)var3;
                  if ((var2 = this.a((t)var1, (qd)this.a(var4), (String)var1.a, (mu[])var1.a)) != null) {
                     break label54;
                  }
               }
            }
         }

         if (var1.a == null || (var2 = this.a((t)var1, (qd)this.a(var1.a), (String)var1.a, (mu[])var1.a)) == null) {
            label56: {
               qd var7;
               if ((var3 = this.e.get(var1.a)) instanceof Vector) {
                  var7 = ((gw)((Vector)var3).elementAt(0)).b();
                  if ((var2 = this.a((t)var1, (qd)var7, (String)var1.a, (mu[])var1.a)) != null) {
                     break label56;
                  }
               }

               var2 = null;
               Enumeration var6 = this.c.elements();

               while(var6.hasMoreElements()) {
                  var7 = (qd)var6.nextElement();
                  gw var8;
                  if ((var8 = this.a((t)var1, (qd)var7, (String)var1.a, (mu[])var1.a)) != null) {
                     if (var2 != null) {
                        String var5 = "Ambiguous static method import: \"" + var2.toString() + "\" vs. \"" + var8.toString() + "\"";
                        this.a((String)var5, (aq)null);
                     }

                     var2 = var8;
                  }
               }

               if (var2 == null) {
                  this.a("A method named \"" + var1.a + "\" is not declared in any enclosing class nor any supertype, nor through a static import", var1.a());
                  return this.a(this.a.a, var1.a, var1.a);
               }
            }
         }
      }

      this.a((h)var1, (gw)var2);
      return var2;
   }

   private gw a(t var1, qd var2, String var3, mu[] var4) {
      Vector var5 = new Vector();
      this.a(var2, var3, var5);
      if (var5.size() == 0) {
         return null;
      } else {
         gw[] var6 = new gw[var5.size()];
         var5.copyInto(var6);
         return (gw)this.a((t)var1, (cn[])var6, (mu[])var4);
      }
   }

   private gw a(qd var1, String var2, mu[] var3) {
      qd[] var4 = new qd[var3.length];

      for(int var5 = 0; var5 < var3.length; ++var5) {
         var4[var5] = this.a((nr)var3[var5]);
      }

      var1.getClass();
      return new ay(this, var1, var2, var4);
   }

   private void a(qd var1, String var2, Vector var3) {
      gw[] var4 = var1.a(var2);

      for(int var5 = 0; var5 < var4.length; ++var5) {
         var3.addElement(var4[var5]);
      }

      qd var9;
      if ((var9 = var1.g()) != null) {
         this.a(var9, var2, var3);
      }

      qd[] var10 = var1.d();

      for(int var6 = 0; var6 < var10.length; ++var6) {
         this.a(var10[var6], var2, var3);
      }

      if (var9 == null && var10.length == 0 && var1.b()) {
         gw[] var11 = this.a.a.a(var2);

         for(int var7 = 0; var7 < var11.length; ++var7) {
            gw var8;
            if (!(var8 = var11[var7]).a() && var8.a() == pe.d) {
               var3.addElement(var8);
            }
         }
      }

   }

   private gw a(u var1) {
      Object var2 = var1.a();

      while(true) {
         bi var3;
         if (var2 instanceof bi && ((var3 = (bi)var2).a & 8) != 0) {
            this.a("Superclass method cannot be invoked in static context", var1.a());
         }

         if (var2 instanceof kg) {
            kg var4 = (kg)var2;
            qd var5 = this.a((dr)var4).g();
            gw var6;
            if ((var6 = this.a((t)var1, (qd)var5, (String)var1.a, (mu[])var1.a)) == null) {
               this.a("Class \"" + var5 + "\" has no method named \"" + var1.a + "\"", var1.a());
               return this.a(var5, var1.a, var1.a);
            }

            this.a((h)var1, (gw)var6);
            return var6;
         }

         var2 = ((hu)var2).a();
      }
   }

   private cn a(t var1, cn[] var2, mu[] var3) {
      qd[] var4 = new qd[var3.length];

      int var5;
      for(var5 = 0; var5 < var3.length; ++var5) {
         var4[var5] = this.a((nr)var3[var5]);
      }

      cn var7;
      if ((var7 = this.a(var1, var2, var4, false)) != null) {
         return var7;
      } else if ((var7 = this.a(var1, var2, var4, true)) != null) {
         return var7;
      } else {
         StringBuffer var6 = new StringBuffer("No applicable constructor/method found for ");
         if (var4.length == 0) {
            var6.append("zero actual parameters");
         } else {
            var6.append("actual parameters \"").append(var4[0]);

            for(var5 = 1; var5 < var4.length; ++var5) {
               var6.append(", ").append(var4[var5]);
            }

            var6.append("\"");
         }

         var6.append("; candidates are: ").append('"' + var2[0].toString() + '"');

         for(var5 = 1; var5 < var2.length; ++var5) {
            var6.append(", ").append('"' + var2[var5].toString() + '"');
         }

         this.a(var6.toString(), var1.a());
         qd var10003;
         if (var2[0] instanceof pm) {
            var10003 = var2[0].b();
            var10003.getClass();
            return new ax(this, var10003, var4);
         } else if (var2[0] instanceof gw) {
            var10003 = var2[0].b();
            var10003.getClass();
            return new aw(this, var10003, var2, var4);
         } else {
            return var2[0];
         }
      }
   }

   private cn a(t var1, cn[] var2, qd[] var3, boolean var4) {
      Vector var5 = new Vector();

      qd[] var8;
      int var9;
      label207:
      for(int var6 = 0; var6 < var2.length; ++var6) {
         cn var7;
         if ((var8 = (var7 = var2[var6]).a()).length == var3.length) {
            for(var9 = 0; var9 < var3.length; ++var9) {
               qd var10001 = var3[var9];
               qd var10 = var8[var9];
               qd var12 = var10001;
               qd var14;
               if (!(var12 == var10 ? true : (c(var12, var10) ? true : (d(var12, var10) ? true : (var4 && (var14 = this.b(var12)) != null ? a(var14, var10) || d(var14, var10) : (var4 && (var14 = this.c(var12)) != null ? a(var14, var10) || c(var14, var10) : false)))))) {
                  continue label207;
               }
            }

            var5.addElement(var7);
         }
      }

      if (var5.size() == 0) {
         return null;
      } else if (var5.size() == 1) {
         return (cn)var5.elementAt(0);
      } else {
         Vector var23 = new Vector();

         for(int var24 = 0; var24 < var5.size(); ++var24) {
            cn var26 = (cn)var5.elementAt(var24);
            var9 = 0;
            int var18 = 0;

            for(int var29 = 0; var29 < var23.size(); ++var29) {
               cn var11 = (cn)var23.elementAt(var29);
               if (var26.a(var11)) {
                  ++var9;
               } else if (var11.a(var26)) {
                  ++var18;
               }
            }

            if (var9 == var23.size()) {
               var23.removeAllElements();
               var23.addElement(var26);
            } else if (var18 < var23.size()) {
               var23.addElement(var26);
            }
         }

         if (var23.size() == 1) {
            return (cn)var23.elementAt(0);
         } else {
            if (var23.size() > 1 && var2[0] instanceof gw) {
               gw var25 = (gw)var23.elementAt(0);
               var8 = null;
               gw var19;
               Enumeration var30;
               qd[] var32 = (var19 = (gw)(var30 = var23.elements()).nextElement()).a();

               label159:
               while(true) {
                  var19.b();
                  int var20;
                  if (!var30.hasMoreElements()) {
                     Vector var31 = new Vector();
                     qd[][] var21 = new qd[var23.size()][];
                     Enumeration var33 = var23.elements();

                     int var35;
                     for(var35 = 0; var35 < var21.length; ++var35) {
                        var21[var35] = ((gw)var33.nextElement()).b();
                     }

                     for(var35 = 0; var35 < var21.length; ++var35) {
                        label138:
                        for(var20 = 0; var20 < var21[var35].length; ++var20) {
                           qd var15 = var21[var35][var20];

                           for(int var16 = 0; var16 < var21.length; ++var16) {
                              if (var16 != var35) {
                                 int var17 = 0;

                                 while(true) {
                                    if (var17 >= var21[var16].length) {
                                       continue label138;
                                    }

                                    if (var21[var16][var17].a(var15)) {
                                       break;
                                    }

                                    ++var17;
                                 }
                              }
                           }

                           if (!var31.contains(var15)) {
                              var31.addElement(var15);
                           }
                        }
                     }

                     qd[] var22 = new qd[var31.size()];
                     var31.copyInto(var22);
                     qd var10003 = var25.b();
                     var10003.getClass();
                     return new an(this, var10003, var25, var22);
                  }

                  qd[] var34 = (var19 = (gw)var30.nextElement()).a();

                  for(var20 = 0; var20 < var34.length; ++var20) {
                     if (var34[var20] != var32[var20]) {
                        break label159;
                     }
                  }
               }
            }

            StringBuffer var27 = new StringBuffer("Invocation of constructor/method with actual parameter type(s) \"");

            int var28;
            for(var28 = 0; var28 < var3.length; ++var28) {
               if (var28 > 0) {
                  var27.append(", ");
               }

               var27.append(nq.a(var3[var28].b()));
            }

            var27.append("\" is ambiguous: ");

            for(var28 = 0; var28 < var23.size(); ++var28) {
               if (var28 > 0) {
                  var27.append(" vs. ");
               }

               var27.append("\"" + var23.elementAt(var28) + "\"");
            }

            this.a(var27.toString(), var1.a());
            return (gw)var2[0];
         }
      }
   }

   private void a(h var1, gw var2) {
      qd[] var4 = var2.b();

      for(int var3 = 0; var3 < var4.length; ++var3) {
         this.a((t)var1, (qd)var4[var3], (hu)var1.a());
      }

   }

   private void a(t var1, qd var2, hu var3) {
      if (!this.a.a("Ljava/lang/Throwable;").a(var2)) {
         this.a("Thrown object of type \"" + var2 + "\" is not assignable to \"Throwable\"", var1.a());
      }

      if (!this.a.a("Ljava/lang/RuntimeException;").a(var2) && !this.a.a("Ljava/lang/Error;").a(var2)) {
         label47:
         while(true) {
            int var5;
            if (var3 instanceof ga) {
               ga var4 = (ga)var3;

               for(var5 = 0; var5 < var4.a.size(); ++var5) {
                  iq var6 = (iq)var4.a.elementAt(var5);
                  if (this.a((nr)var6.a.a).a(var2)) {
                     return;
                  }
               }
            } else {
               if (var3 instanceof bi) {
                  bi var7 = (bi)var3;
                  var5 = 0;

                  while(true) {
                     if (var5 >= var7.a.length) {
                        break label47;
                     }

                     if (this.a((nr)var7.a[var5]).a(var2)) {
                        return;
                     }

                     ++var5;
                  }
               }

               if (var3 instanceof ht) {
                  break;
               }
            }

            var3 = var3.a();
         }

         this.a("Thrown exception of type \"" + var2 + "\" is neither caught by a \"try...catch\" block nor declared in the \"throws\" clause of the declaring function", var1.a());
      }
   }

   private qd a(py var1) {
      if (var1.a == null) {
         var1.a = this.a((nr)var1.a);
      }

      return var1.a;
   }

   private po a(gz var1) {
      if (!(var1.a instanceof gp)) {
         return null;
      } else {
         gp var3 = (gp)var1.a;
         nr var4;
         if (!((var4 = this.a(var3)) instanceof qc)) {
            return null;
         } else {
            po var2;
            qc var5;
            if ((var2 = (var5 = (qc)var4).a).a) {
               this.a("Must not increment or decrement \"final\" local variable", var5.a());
            }

            return var2.a != qd.b && var2.a != qd.h && var2.a != qd.f && var2.a != qd.c ? null : var2;
         }
      }
   }

   private qd a(dr var1) {
      ft var2;
      if ((var2 = (ft)var1).a == null) {
         var2.a = new am(this, var2, var1);
      }

      return var2.a;
   }

   private void a(t var1, kg var2, ht var3, qd var4) {
      Vector var5 = a((dr)var2);
      if (var3.b()) {
         this.a("No current instance available in static context", var1.a());
      }

      int var6 = 0;

      while(true) {
         if (var6 >= var5.size()) {
            this.a("\"" + var2 + "\" is not enclosed by \"" + var4 + "\"", var1.a());
            break;
         }

         if (var4.a(this.a((dr)((ft)var5.elementAt(var6))))) {
            break;
         }

         ++var6;
      }

      int var9;
      if (var3 instanceof jf) {
         if (var6 == 0) {
            this.b(var1, 42);
            return;
         }

         jf var10 = (jf)var3;
         String var12 = "this$" + (var5.size() - 2);
         po var7;
         if ((var7 = (po)var10.a.get(var12)) == null) {
            throw new RuntimeException("SNO: Synthetic parameter \"" + var12 + "\" not found");
         }

         this.a(var1, var7);
         var9 = 1;
      } else {
         this.b(var1, 42);
         var9 = 0;
      }

      while(var9 < var6) {
         String var11 = "this$" + (var5.size() - var9 - 2);
         kw var13 = (kw)var5.elementAt(var9);
         qd var14 = this.a((dr)((ft)var13));
         dr var8 = (dr)var5.elementAt(var9 + 1);
         qd var15 = this.a((dr)((ft)var8));
         var13.a(new lz(var14, var11, var15));
         this.b(var1, -76);
         this.a(var14.b(), var11, var15.b());
         ++var9;
      }

   }

   private static Vector a(dr var0) {
      Vector var1 = new Vector();

      for(var0 = var0; var0 != null; var0 = a(var0)) {
         var1.addElement(var0);
      }

      return var1;
   }

   static dr a(dr var0) {
      if (var0 instanceof ow) {
         return null;
      } else if (!(var0 instanceof jl)) {
         if (var0 instanceof er && (((er)var0).a & 8) != 0) {
            return null;
         } else {
            Object var2;
            for(var2 = var0; !(var2 instanceof ht); var2 = ((hu)var2).a()) {
               if (var2 instanceof ew) {
                  return null;
               }

               if (var2 instanceof fv) {
                  return null;
               }
            }

            if (((ht)var2).b()) {
               return null;
            } else {
               return (ft)((hu)var2).a();
            }
         }
      } else {
         hu var1;
         for(var1 = var0.a(); !(var1 instanceof bi); var1 = var1.a()) {
         }

         if (var1 instanceof kl && (((bi)var1).a & 8) != 0) {
            return null;
         } else {
            while(!(var1 instanceof dr)) {
               var1 = var1.a();
            }

            return (var0 = (dr)var1) instanceof kg ? var0 : null;
         }
      }
   }

   private qd a(cc var1) {
      if (var1.a == null) {
         Object var2;
         for(var2 = var1.a(); var2 instanceof ic || var2 instanceof iq; var2 = ((hu)var2).a()) {
         }

         bi var3;
         if (var2 instanceof bi && ((var3 = (bi)var2).a & 8) != 0) {
            this.a("No current instance available in static method", var1.a());
         }

         while(!(var2 instanceof dr)) {
            var2 = ((hu)var2).a();
         }

         if (!(var2 instanceof kg)) {
            this.a("Only methods of classes can have a current instance", var1.a());
         }

         var1.a = this.a((dr)((kg)var2));
      }

      return var1.a;
   }

   private qd a(bi var1) {
      if (var1.a == null) {
         var1.a = this.a((nr)var1.a);
      }

      return var1.a;
   }

   final pm a(jf var1) {
      if (var1.a != null) {
         return var1.a;
      } else {
         qd var10004 = this.a((dr)((ft)var1.a()));
         var10004.getClass();
         var1.a = new al(this, var10004, var1);
         return var1.a;
      }
   }

   public final gw a(kl var1) {
      if (var1.a != null) {
         return var1.a;
      } else {
         qd var10004 = this.a((dr)((ft)var1.a()));
         var10004.getClass();
         var1.a = new z(this, var10004, var1);
         return var1.a;
      }
   }

   private cn a(bi var1) {
      if (var1 instanceof jf) {
         return this.a((jf)var1);
      } else if (var1 instanceof kl) {
         return this.a((kl)var1);
      } else {
         throw new RuntimeException("FunctionDeclarator is neither ConstructorDeclarator nor MethodDeclarator");
      }
   }

   private qd a(String var1, aq var2) {
      String[] var4;
      if ((var4 = this.a(var1)) == null) {
         return null;
      } else {
         qd var3;
         if ((var3 = this.a(var4)) == null) {
            this.a("Imported class \"" + ls.a((Object[])var4, (String)".") + "\" could not be loaded", var2);
            return this.a.a;
         } else {
            return var3;
         }
      }
   }

   private String[] a(String var1) {
      return (String[])((String[])this.d.get(var1));
   }

   private qd b(String var1, aq var2) {
      qd var3;
      if ((var3 = (qd)this.a.get(var1)) != null) {
         return var3;
      } else {
         Enumeration var4 = this.b.elements();

         while(var4.hasMoreElements()) {
            String[] var5;
            String[] var7 = new String[(var5 = (String[])((String[])var4.nextElement())).length + 1];
            System.arraycopy(var5, 0, var7, 0, var5.length);
            var7[var5.length] = var1;
            qd var8;
            if ((var8 = this.a(var7)) != null) {
               if (var3 != null && var3 != var8) {
                  this.a("Ambiguous class name: \"" + var3 + "\" vs. \"" + var8 + "\"", var2);
               }

               var3 = var8;
            }
         }

         if (var3 == null) {
            return null;
         } else {
            this.a.put(var1, var3);
            return var3;
         }
      }
   }

   private void a(pb var1) {
      aq var2 = var1.a();

      Object var3;
      for(var3 = var1.a(); !(var3 instanceof ft); var3 = ((hu)var3).a()) {
      }

      ft var11 = (ft)var3;
      il var12 = new il(var2);
      gt var4 = new gt(var2, new hz(var2, this.a.c), "forName", new mu[]{new gp(var2, new String[]{"className"})});

      qd var5;
      try {
         var5 = this.a.b("Ljava/lang/ClassNotFoundException;");
      } catch (ClassNotFoundException var9) {
         throw new RuntimeException("Loading class \"ClassNotFoundException\": " + var9.getMessage());
      }

      if (var5 == null) {
         throw new RuntimeException("SNO: Cannot load \"ClassNotFoundException\"");
      } else {
         qd var6;
         try {
            var6 = this.a.b("Ljava/lang/NoClassDefFoundError;");
         } catch (ClassNotFoundException var8) {
            throw new RuntimeException("Loading class \"NoClassDefFoundError\": " + var8.getMessage());
         }

         if (var6 == null) {
            throw new RuntimeException("SNO: Cannot load \"NoClassFoundError\"");
         } else {
            il var7;
            (var7 = new il(var2)).a((ov)(new fi(var2, new ok(var2, (mu)null, new hz(var2, var6), new mu[]{new gt(var2, new gp(var2, new String[]{"ex"}), "getMessage", new mu[0])}))));
            Vector var15;
            (var15 = new Vector()).addElement(new iq(var2, new ct(var2, true, new hz(var2, var5), "ex"), var7));
            ga var13 = new ga(var2, new y(var2, var4), var15, (il)null);
            var12.a((ov)var13);
            ct var14 = new ct(var2, false, new hz(var2, this.a.b), "className");
            kl var10 = new kl(var2, (String)null, (short)8, new hz(var2, this.a.c), "class$", new ct[]{var14}, new gs[0], var12);
            var11.a(var10);
            if (var11.a != null) {
               var11.a.a = null;
               var11.a.a = null;
            }

         }
      }
   }

   private qd a(t var1, Object var2) {
      if (!(var2 instanceof Integer) && !(var2 instanceof Short) && !(var2 instanceof Character) && !(var2 instanceof Byte)) {
         if (var2 instanceof Long) {
            long var14;
            if ((var14 = (Long)var2) >= 0L && var14 <= 1L) {
               this.b(var1, 9 + (int)var14);
            } else {
               this.b(var1, 20);
               this.a(var14);
            }

            return qd.g;
         } else if (var2 instanceof Float) {
            float var13;
            if (Float.floatToIntBits(var13 = (Float)var2) != Float.floatToIntBits(0.0F) && var13 != 1.0F && var13 != 2.0F) {
               this.a(var1, this.a.a().a(var13));
            } else {
               this.b(var1, 11 + (int)var13);
            }

            return qd.e;
         } else if (var2 instanceof Double) {
            double var12;
            if (Double.doubleToLongBits(var12 = (Double)var2) != Double.doubleToLongBits(0.0D) && var12 != 1.0D) {
               this.b(var1, 20);
               fg var9;
               (var9 = this.a).a((short)-1, var9.a().a(var12));
            } else {
               this.b(var1, 14 + (int)var12);
            }

            return qd.d;
         } else if (!(var2 instanceof String)) {
            if (var2 instanceof Boolean) {
               this.b(var1, (Boolean)var2 ? 4 : 3);
               return qd.i;
            } else if (var2 == mu.d) {
               this.b(var1, 1);
               return qd.a;
            } else {
               throw new RuntimeException("Unknown literal type \"" + var2.getClass().getName() + "\"");
            }
         } else {
            String var11;
            String[] var4 = b(var11 = (String)var2);
            this.a(var1, this.a(var4[0]));

            for(int var10 = 1; var10 < var4.length; ++var10) {
               this.a(var1, this.a(var4[var10]));
               this.b(var1, -74);
               this.b("Ljava/lang/String;", "concat", "(Ljava/lang/String;)Ljava/lang/String;");
            }

            return this.a.b;
         }
      } else {
         int var3;
         if ((var3 = var2 instanceof Character ? (Character)var2 : (var2 instanceof Integer ? (Integer)var2 : (var2 instanceof Short ? (Short)var2 : (Byte)var2))) >= -1 && var3 <= 5) {
            this.b(var1, var3 + 3);
         } else if (var3 >= -128 && var3 <= 127) {
            this.b(var1, 16);
            this.a((int)((byte)var3));
         } else {
            this.a(var1, this.a.a().a(var3));
         }

         return qd.f;
      }
   }

   private static String[] b(String var0) {
      if (var0.length() < 21845) {
         return new String[]{var0};
      } else {
         int var1 = var0.length();
         int var2 = 0;
         int var3 = 0;
         Vector var4 = new Vector();
         int var5 = 0;

         while(true) {
            if (var5 == var1) {
               var4.addElement(var0.substring(var3));
               break;
            }

            if (var2 >= 65532) {
               var4.addElement(var0.substring(var3, var5));
               if (var5 + 21845 > var1) {
                  var4.addElement(var0.substring(var5));
                  break;
               }

               var3 = var5;
               var2 = 0;
            }

            char var6;
            if ((var6 = var0.charAt(var5)) >= 1 && var6 <= 127) {
               ++var2;
            } else if (var6 > 2047) {
               var2 += 3;
            } else {
               var2 += 2;
            }

            ++var5;
         }

         String[] var7 = new String[var4.size()];
         var4.copyInto(var7);
         return var7;
      }
   }

   private void a(t var1, short var2) {
      if (var2 <= 255) {
         this.b(var1, 18);
         this.a((int)((byte)var2));
      } else {
         this.b(var1, 19);
         this.b(var2);
      }
   }

   private void a(t var1, qd var2, qd var3, Object var4) {
      if (!b(var2, var3)) {
         if (!this.a(var1, var2, var3)) {
            if (!d(var2, var3)) {
               qd var5;
               if ((var5 = this.b(var2)) != null) {
                  if (b(var5, var3)) {
                     this.c(var1, var2, var5);
                     return;
                  }

                  if (d(var5, var3)) {
                     this.c(var1, var2, var5);
                     return;
                  }
               }

               if ((var5 = this.c(var2)) != null) {
                  if (b(var5, var3)) {
                     this.d(var1, var2, var5);
                     return;
                  }

                  if (c(var5, var3)) {
                     this.d(var1, var2, var5);
                     this.a(var1, var5, var3);
                     return;
                  }
               }

               if (var4 != null) {
                  boolean var10000;
                  label116: {
                     int var6;
                     if (var4 instanceof Byte) {
                        var6 = (Byte)var4;
                     } else if (var4 instanceof Short) {
                        var6 = (Short)var4;
                     } else if (var4 instanceof Integer) {
                        var6 = (Integer)var4;
                     } else {
                        if (!(var4 instanceof Character)) {
                           var10000 = false;
                           break label116;
                        }

                        var6 = (Character)var4;
                     }

                     if (var3 == qd.b) {
                        var10000 = var6 >= -128 && var6 <= 127;
                     } else if (var3 == qd.h) {
                        var10000 = var6 >= -32768 && var6 <= 32767;
                     } else if (var3 == qd.c) {
                        var10000 = var6 >= 0 && var6 <= 65535;
                     } else {
                        ha var8 = this.a;
                        if (var3 == var8.a("Ljava/lang/Byte;") && var6 >= -128 && var6 <= 127) {
                           this.c(var1, qd.b, var3);
                           var10000 = true;
                        } else if (var3 == var8.a("Ljava/lang/Short;") && var6 >= -32768 && var6 <= 32767) {
                           this.c(var1, qd.h, var3);
                           var10000 = true;
                        } else if (var3 == var8.a("Ljava/lang/Character;") && var6 >= 0 && var6 <= 65535) {
                           this.c(var1, qd.c, var3);
                           var10000 = true;
                        } else {
                           var10000 = false;
                        }
                     }
                  }

                  if (var10000) {
                     return;
                  }
               }

               this.a("Assignment conversion not possible from type \"" + var2 + "\" to type \"" + var3 + "\"", var1.a());
            }
         }
      }
   }

   private qd a(t var1, qd var2) {
      var2 = this.b(var1, var2);
      qd var3 = this.c(var1, var2);
      this.b(var1, var2, var3);
      return var3;
   }

   private void a(t var1, qd var2, qd var3) {
      qd var4;
      qd var5 = (var4 = this.c(var3)) != null ? var4 : var3;
      if (!b(var2, var5) && !this.b(var1, var2, var5)) {
         throw new RuntimeException("SNO: reverse unary numeric promotion failed");
      } else {
         if (var4 != null) {
            this.c(var1, var4, var3);
         }

      }
   }

   private qd b(t var1, qd var2) {
      if (var2.f()) {
         return var2;
      } else {
         qd var3;
         if ((var3 = this.c(var2)) != null) {
            this.d(var1, var2, var3);
            return var3;
         } else {
            this.a("Object of type \"" + var2.toString() + "\" cannot be converted to a numeric type", var1.a());
            return var2;
         }
      }
   }

   private void b(t var1, qd var2, qd var3) {
      if (!b(var2, var3) && !this.a(var1, var2, var3)) {
         throw new RuntimeException("SNO: Conversion failed");
      }
   }

   private qd c(t var1, qd var2) {
      if (!var2.f()) {
         this.a("Unary numeric promotion not possible on non-numeric-primitive type \"" + var2 + "\"", var1.a());
      }

      if (var2 == qd.d) {
         return qd.d;
      } else if (var2 == qd.e) {
         return qd.e;
      } else {
         return var2 == qd.g ? qd.g : qd.f;
      }
   }

   private qd a(t var1, qd var2, ex var3, qd var4) {
      return this.a(var1, var2, var3, var4, this.a.b());
   }

   private qd a(t var1, qd var2, ex var3, qd var4, ex var5) {
      qd var6 = this.c(var2);
      qd var7 = this.c(var4);
      var6 = this.a(var1, var6 != null ? var6 : var2, var7 != null ? var7 : var4);
      if (var3 != null) {
         this.a.a(var3);

         try {
            this.b(var1, this.b(var1, var2), var6);
         } finally {
            this.a.e();
         }
      }

      if (var5 != null) {
         this.a.a(var5);

         try {
            this.b(var1, this.b(var1, var4), var6);
         } finally {
            this.a.e();
         }
      }

      return var6;
   }

   private qd a(t var1, qd var2, qd var3) {
      if (!var2.f() || !var3.f()) {
         this.a("Binary numeric promotion not possible on types \"" + var2 + "\" and \"" + var3 + "\"", var1.a());
      }

      if (var2 != qd.d && var3 != qd.d) {
         if (var2 != qd.e && var3 != qd.e) {
            return var2 != qd.g && var3 != qd.g ? qd.f : qd.g;
         } else {
            return qd.e;
         }
      } else {
         return qd.d;
      }
   }

   private static boolean a(qd var0, qd var1) {
      return var0 == var1;
   }

   private static boolean b(qd var0, qd var1) {
      return var0 == var1;
   }

   private static boolean c(qd var0, qd var1) {
      return b.get(var0.b() + var1.b()) != null;
   }

   private boolean a(t var1, qd var2, qd var3) {
      byte[] var4;
      if ((var4 = (byte[])((byte[])b.get(var2.b() + var3.b()))) != null) {
         this.a(var1, var4);
         return true;
      } else {
         return false;
      }
   }

   private static void a(Object[] var0, Hashtable var1) {
      byte[] var2 = null;

      for(int var3 = 0; var3 < var0.length; ++var3) {
         Object var4;
         if ((var4 = var0[var3]) instanceof byte[]) {
            var2 = (byte[])((byte[])var4);
         } else {
            var1.put(var4, var2);
         }
      }

   }

   private static boolean d(qd var0, qd var1) {
      return !var1.e() && var0 != var1 ? var1.a(var0) : false;
   }

   private boolean b(t var1, qd var2, qd var3) {
      byte[] var4;
      if ((var4 = (byte[])((byte[])c.get(var2.b() + var3.b()))) != null) {
         this.a(var1, var4);
         return true;
      } else {
         return false;
      }
   }

   private boolean e(qd var1, qd var2) {
      if (var1.e()) {
         return false;
      } else if (var1 == var2) {
         return false;
      } else if (var1.a(var2)) {
         return true;
      } else if (var2.b() && !var1.a() && !var2.a(var1)) {
         return true;
      } else if (var1 == this.a.a && var2.d()) {
         return true;
      } else if (var1 == this.a.a && var2.b()) {
         return true;
      } else if (var1.b() && !var2.a()) {
         return true;
      } else if (var1.b() && var2.a() && var1.a(var2)) {
         return true;
      } else if (var1.b() && var2.b() && !var2.a(var1)) {
         return true;
      } else {
         if (var1.d() && var2.d()) {
            var1 = var1.h();
            var2 = var2.h();
            if (c.containsKey(var1.b() + var2.b()) || this.e(var1, var2)) {
               return true;
            }
         }

         return false;
      }
   }

   private qd b(qd var1) {
      ha var2 = this.a;
      if (var1 == qd.i) {
         return var2.a("Ljava/lang/Boolean;");
      } else if (var1 == qd.b) {
         return var2.a("Ljava/lang/Byte;");
      } else if (var1 == qd.c) {
         return var2.a("Ljava/lang/Character;");
      } else if (var1 == qd.h) {
         return var2.a("Ljava/lang/Short;");
      } else if (var1 == qd.f) {
         return var2.a("Ljava/lang/Integer;");
      } else if (var1 == qd.g) {
         return var2.a("Ljava/lang/Long;");
      } else if (var1 == qd.e) {
         return var2.a("Ljava/lang/Float;");
      } else {
         return var1 == qd.d ? var2.a("Ljava/lang/Double;") : null;
      }
   }

   private void c(t var1, qd var2, qd var3) {
      if (var3.a("valueOf", new qd[]{var2})) {
         this.b(var1, -72);
         this.b(var3.b(), "valueOf", '(' + var2.b() + ')' + var3.b());
      } else {
         this.b(var1, -69);
         this.a(var3.b());
         if (nq.b(var2.b())) {
            this.b(var1, 91);
            this.b(var1, 91);
            this.b(var1, 87);
         } else {
            this.b(var1, 90);
            this.b(var1, 95);
         }

         this.b(var1, -73);
         this.b(var3.b(), "<init>", '(' + var2.b() + ')' + "V");
      }
   }

   private qd c(qd var1) {
      ha var2 = this.a;
      if (var1 == var2.a("Ljava/lang/Boolean;")) {
         return qd.i;
      } else if (var1 == var2.a("Ljava/lang/Byte;")) {
         return qd.b;
      } else if (var1 == var2.a("Ljava/lang/Character;")) {
         return qd.c;
      } else if (var1 == var2.a("Ljava/lang/Short;")) {
         return qd.h;
      } else if (var1 == var2.a("Ljava/lang/Integer;")) {
         return qd.f;
      } else if (var1 == var2.a("Ljava/lang/Long;")) {
         return qd.g;
      } else if (var1 == var2.a("Ljava/lang/Float;")) {
         return qd.e;
      } else {
         return var1 == var2.a("Ljava/lang/Double;") ? qd.d : null;
      }
   }

   private void d(t var1, qd var2, qd var3) {
      this.b(var1, -74);
      this.b(var2.b(), var3.toString() + "Value", "()" + var3.b());
   }

   private qd a(String[] var1) {
      int[] var2 = new int[var1.length - 1];
      StringBuffer var3 = new StringBuffer("L");
      int var4 = 0;

      while(true) {
         var3.append(var1[var4]);
         if (var4 == var1.length - 1) {
            var3.append(';');
            var4 = var2.length - 1;

            while(true) {
               qd var6;
               try {
                  var6 = this.a.b(var3.toString());
               } catch (ClassNotFoundException var5) {
                  if (var5.getMessage().indexOf("CompileException") != -1) {
                     throw new ng(var5.getMessage().substring(var5.getMessage().indexOf(91) + 1, var5.getMessage().lastIndexOf(93)), (aq)null);
                  }

                  throw new ng(var3.toString(), (aq)null, var5);
               }

               if (var6 != null) {
                  return var6;
               }

               if (var4 < 0) {
                  return null;
               }

               var3.setCharAt(var2[var4], '$');
               --var4;
            }
         }

         var2[var4] = var3.length();
         var3.append('/');
         ++var4;
      }
   }

   private qd a(t var1, po var2) {
      this.a(var1, var2.a, (int)var2.a);
      return var2.a;
   }

   private void a(t var1, qd var2, int var3) {
      if (var3 <= 3) {
         this.b(var1, 26 + 4 * b(var2) + var3);
      } else if (var3 <= 255) {
         this.b(var1, 21 + b(var2));
         this.a(var3);
      } else {
         this.b(var1, -60);
         this.b(var1, 21 + b(var2));
         this.b(var3);
      }
   }

   private void a(t var1, po var2) {
      this.a(var1, var2.a, var2.a);
   }

   private void a(t var1, qd var2, short var3) {
      if (var3 <= 3) {
         this.b(var1, 59 + 4 * b(var2) + var3);
      } else if (var3 <= 255) {
         this.b(var1, 54 + b(var2));
         this.a((int)var3);
      } else {
         this.b(var1, -60);
         this.b(var1, 54 + b(var2));
         this.b(var3);
      }
   }

   private void a(t var1, int var2) {
      switch(var2) {
      case 0:
         return;
      case 1:
         this.b(var1, 89);
         return;
      case 2:
         this.b(var1, 92);
         return;
      default:
         throw new RuntimeException("dup(" + var2 + ")");
      }
   }

   private void b(t var1, qd var2, int var3) {
      if (var3 >= 0 && var3 <= 2) {
         int var4 = var3 + 89;
         var3 += 92;
         this.b(var1, var2 != qd.g && var2 != qd.d ? var4 : var3);
      } else {
         throw new RuntimeException("SNO: x has value " + var3);
      }
   }

   private void b(t var1, qd var2) {
      if (var2 != qd.a) {
         this.b(var1, var2 != qd.g && var2 != qd.d ? 87 : 88);
      }
   }

   private static int a(qd var0) {
      if (var0 != qd.b && var0 != qd.c && var0 != qd.f && var0 != qd.h && var0 != qd.i) {
         if (var0 == qd.g) {
            return 1;
         } else if (var0 == qd.e) {
            return 2;
         } else if (var0 == qd.d) {
            return 3;
         } else {
            throw new RuntimeException("Unexpected type \"" + var0 + "\"");
         }
      } else {
         return 0;
      }
   }

   private static int a(qd var0, int var1, int var2, int var3, int var4) {
      if (var0 != qd.b && var0 != qd.c && var0 != qd.f && var0 != qd.h && var0 != qd.i) {
         if (var0 == qd.g) {
            return 10;
         } else if (var0 == qd.e) {
            return 12;
         } else if (var0 == qd.d) {
            return 15;
         } else {
            throw new RuntimeException("Unexpected type \"" + var0 + "\"");
         }
      } else {
         return 4;
      }
   }

   private static int b(qd var0) {
      return !var0.e() ? 4 : a(var0);
   }

   private static int c(qd var0) {
      if (var0 == qd.f) {
         return 0;
      } else if (var0 == qd.g) {
         return 1;
      } else if (var0 == qd.e) {
         return 2;
      } else if (var0 == qd.d) {
         return 3;
      } else if (!var0.e()) {
         return 4;
      } else if (var0 != qd.i && var0 != qd.b) {
         if (var0 == qd.c) {
            return 6;
         } else if (var0 == qd.h) {
            return 7;
         } else {
            throw new RuntimeException("Unexpected type \"" + var0 + "\"");
         }
      } else {
         return 5;
      }
   }

   private cm a(qd var1, String var2, aq var3) {
      cm[] var4 = var1.c();

      for(int var5 = 0; var5 < var4.length; ++var5) {
         cm var6 = var4[var5];
         if (var2.equals(var6.a())) {
            return var6;
         }
      }

      cm var9 = null;
      qd var10;
      if ((var10 = var1.g()) != null) {
         var9 = this.a(var10, var2, var3);
      }

      qd[] var11 = var1.d();

      for(int var7 = 0; var7 < var11.length; ++var7) {
         cm var8;
         if ((var8 = this.a(var11[var7], var2, var3)) != null) {
            if (var9 != null) {
               throw new ng("Access to field \"" + var2 + "\" is ambiguous - both \"" + var9.b() + "\" and \"" + var8.b() + "\" declare it", var3);
            }

            var9 = var8;
         }
      }

      return var9;
   }

   private qd a(qd var1, String var2, aq var3) {
      qd[] var5;
      if ((var5 = var1.a(var2)).length == 0) {
         return null;
      } else if (var5.length == 1) {
         return var5[0];
      } else {
         StringBuffer var6 = new StringBuffer("Type \"" + var2 + "\" is ambiguous: " + var5[0].toString());

         for(int var4 = 1; var4 < var5.length; ++var4) {
            var6.append(" vs. ").append(var5[var4].toString());
         }

         this.a(var6.toString(), var3);
         return var5[0];
      }
   }

   public final qd a(String var1) {
      String var10000 = this.a.a == null ? null : this.a.a.a;
      String var2 = var10000;
      if (var10000 != null) {
         if (!var1.startsWith(var2 + '.')) {
            return null;
         }

         var1 = var1.substring(var2.length() + 1);
      }

      boolean var5 = false;
      int var3;
      if ((var3 = var1.indexOf("$", 0)) == -1) {
         var3 = var1.length();
      }

      var2 = var1.substring(0, var3);
      Object var4;
      if ((var4 = this.a.a(var2)) == null) {
         return null;
      } else {
         do {
            if (var3 == var1.length()) {
               return this.a((dr)((ft)var4));
            }

            int var6 = var3 + 1;
            if ((var3 = var1.indexOf("$", var6)) == -1) {
               var3 = var1.length();
            }

            var2 = var1.substring(var6, var3);
         } while((var4 = ((dr)var4).a(var2)) != null);

         return null;
      }
   }

   private void a(String var1, aq var2) {
      ++this.a;
      throw new ng(var1, var2);
   }

   private fg a(fg var1) {
      fg var2 = this.a;
      this.a = var1;
      return var2;
   }

   private void a(int var1) {
      this.a.a((short)-1, new byte[]{(byte)var1});
   }

   private void b(int var1) {
      this.a.a((short)-1, new byte[]{(byte)(var1 >> 8), (byte)var1});
   }

   private void c(int var1) {
      this.a.a((short)-1, new byte[]{(byte)(var1 >> 24), (byte)(var1 >> 16), (byte)(var1 >> 8), (byte)var1});
   }

   private void b(t var1, int var2) {
      this.a.a(var1.a().a(), new byte[]{(byte)var2});
   }

   private void a(t var1, byte[] var2) {
      this.a.a(var1.a().a(), var2);
   }

   private void a(t var1, int var2, gx var3) {
      this.a.a(var1.a().a(), var2, var3);
   }

   private void a(gx var1, gx var2) {
      this.a.a((short)-1, var1, var2);
   }

   private void a(String var1) {
      fg var2;
      (var2 = this.a).a((short)-1, var2.a().a(var1));
   }

   private void a(String var1, String var2, String var3) {
      fg var4;
      (var4 = this.a).a((short)-1, var4.a().a(var1, var2, var3));
   }

   private void b(String var1, String var2, String var3) {
      fg var4;
      (var4 = this.a).a((short)-1, var4.a().b(var1, var2, var3));
   }

   private short a(String var1) {
      return this.a.a().b(var1);
   }

   private void a(long var1) {
      fg var3;
      (var3 = this.a).a((short)-1, var3.a().a(var1));
   }

   private gx a(gg var1) {
      if (var1.a == null) {
         fg var10003 = this.a;
         var10003.getClass();
         var1.a = new gx(var10003);
      }

      return var1.a;
   }

   private ht a(py var1) {
      if (var1.a == null) {
         Object var2;
         for(var2 = var1.a(); !(var2 instanceof ht); var2 = ((hu)var2).a()) {
         }

         var1.a = (ht)var2;
         if (var1.a.b()) {
            this.a("No current instance available in static method", var1.a());
         }

         var1.a = (kg)var1.a.a();
      }

      return var1.a;
   }

   private qd a(t var1, int var2, int var3, qd var4) {
      if (var2 == 1 && var3 == 0 && var4.e()) {
         this.b(var1, -68);
         this.a(var4 == qd.i ? 4 : (var4 == qd.c ? 5 : (var4 == qd.e ? 6 : (var4 == qd.d ? 7 : (var4 == qd.b ? 8 : (var4 == qd.h ? 9 : (var4 == qd.f ? 10 : (var4 == qd.g ? 11 : -1))))))));
         return var4.a(this.a.a);
      } else {
         qd var5;
         if (var2 == 1) {
            var5 = var4.a(var3, this.a.a);
            this.b(var1, -67);
            this.a(var5.b());
            return var5.a(this.a.a);
         } else {
            var5 = var4.a(var2 + var3, this.a.a);
            this.b(var1, -59);
            this.a(var5.b());
            this.a(var2);
            return var5;
         }
      }
   }

   private static String a(String[] var0) {
      if (var0.length == 0) {
         throw new IllegalArgumentException("SNO: Empty string array");
      } else {
         return var0[var0.length - 1];
      }
   }

   static void a(cx var0, my var1) {
      String[] var4;
      String var2 = a(var4 = var1.a);
      String[] var3;
      if ((var3 = (String[])((String[])var0.d.put(var2, var4))) != null && !qd.a((Object[])var3, (Object[])var4)) {
         String var5 = "Class \"" + var2 + "\" was previously imported as \"" + ls.a((Object[])var3, (String)".") + "\", now as \"" + ls.a((Object[])var4, (String)".") + "\"";
         var0.a((String)var5, (aq)null);
      }

   }

   static void a(cx var0, bf var1) {
      var0.b.addElement(var1.a);
   }

   static void a(cx var0, or var1) {
      String var2 = a(var1.a);
      Object var3;
      qd var4;
      if ((var4 = var0.a(var1.a)) != null) {
         var3 = var4;
      } else {
         String[] var8;
         if ((var8 = var1.a).length == 0) {
            throw new IllegalArgumentException("SNO: Empty string array");
         }

         String[] var10 = new String[var8.length - 1];
         System.arraycopy(var8, 0, var10, 0, var10.length);
         qd var9;
         if ((var9 = var0.a(var10)) == null) {
            var0.a("Could not load \"" + ls.a((Object[])var10, (String)".") + "\"", var1.a());
            return;
         }

         cm[] var5 = var9.c();
         int var6 = 0;

         while(true) {
            if (var6 >= var5.length) {
               gw[] var13;
               if ((var13 = var9.a(var2)).length <= 0) {
                  var0.a("\"" + ls.a((Object[])var10, (String)".") + "\" has no static member \"" + var2 + "\"", var1.a());
                  return;
               }

               Vector var14 = new Vector(var13.length);

               for(int var11 = 0; var11 < var13.length; ++var11) {
                  var14.addElement(var13[var11]);
               }

               var3 = var14;
               break;
            }

            cm var7;
            if ((var7 = var5[var6]).a().equals(var2)) {
               if (!var7.a()) {
                  var0.a("Filed \"" + var2 + "\" of \"" + ls.a((Object[])var10, (String)".") + "\" must be static", var1.a());
               }

               var3 = var7;
               break;
            }

            ++var6;
         }
      }

      Object var12;
      if ((var12 = var0.e.put(var2, var3)) != null && !var12.equals(var3)) {
         var0.a("\"" + var2 + "\" was previously statically imported as \"" + var12.toString() + "\", now as \"" + var3.toString() + "\"", var1.a());
      }

   }

   static void a(cx var0, x var1) {
      qd var2;
      if ((var2 = (var0 = var0).a(var1.a)) == null) {
         var0.a("Could not load \"" + ls.a((Object[])var1.a, (String)".") + "\"", var1.a());
      } else {
         var0.c.addElement(var2);
      }
   }

   static boolean a(cx var0, lh var1) {
      return var0.a((ov)var1.a);
   }

   static boolean a(cx var0, gb var1) {
      var1 = var1;
      var0 = var0;

      for(int var2 = 0; var2 < var1.a.length; ++var2) {
         cb var3 = var1.a[var2];
         ho var4;
         if ((var4 = var0.a(var1, var3)) != null) {
            if ((var1.a & 8) == 0) {
               var0.b(var1, 42);
            }

            qd var5 = var0.a((nr)var1.a);
            if (var4 instanceof mu) {
               mu var7 = (mu)var4;
               qd var6 = var0.b(var7);
               var5 = var5.a(var3.a, var0.a.a);
               var0.a((t)var1, (qd)var6, (qd)var5, (Object)var0.a(var7));
            } else {
               if (!(var4 instanceof fu)) {
                  throw new RuntimeException("Unexpected array initializer or rvalue class " + var4.getClass().getName());
               }

               var0.a((fu)var4, var5);
            }

            if ((var1.a & 8) != 0) {
               var0.b(var1, -77);
            } else {
               var0.b(var1, -75);
            }

            var0.a(var0.a(var1.a()).b(), var3.a, var5.b());
         }
      }

      return true;
   }

   static boolean a(cx var0, p var1) {
      boolean var2 = var0.a((ov)var1.a);
      if (var1.a != null) {
         var1.a.b();
         var2 = true;
      }

      return var2;
   }

   static boolean a(cx var0, il var1) {
      return var0.b(var1);
   }

   static boolean a(cx var0, nh var1) {
      var0.b(var1.a);
      return true;
   }

   static boolean a(cx var0, hx var1) {
      return var0.a(var1);
   }

   static boolean a(cx var0, os var1) {
      return var0.a(var1);
   }

   static boolean a(cx var0, bh var1) {
      Object var2;
      if ((var2 = (var0 = var0).a(var1.a)) != null && Boolean.TRUE.equals(var2)) {
         return var0.a((nu)var1, (ov)var1.a, (mu[])null);
      } else {
         fg var10003 = var0.a;
         var10003.getClass();
         var1.b = new gx(var10003);
         var1.a = false;
         var0.a(var1, -89, var1.b);
         gx var3 = var0.a.a();
         var0.a(var1.a);
         var1.b.b();
         var0.a(var1.a, var3, true);
         if (var1.a != null) {
            var1.a.b();
         }

         return true;
      }
   }

   static boolean a(cx var0, ga var1) {
      return var0.a(var1);
   }

   static boolean a(cx var0, bq var1) {
      cx var10000 = var0;
      var1 = var1;
      var0 = var0;
      qd var2 = var10000.b(var1.a);
      var0.a((t)var1, (qd)var2, (qd)qd.f, (Object)null);
      Hashtable var3 = new Hashtable();
      gx var4 = null;
      gx[] var5 = new gx[var1.a.size()];

      int var8;
      for(int var6 = 0; var6 < var1.a.size(); ++var6) {
         jk var7 = (jk)var1.a.elementAt(var6);
         fg var10004 = var0.a;
         var10004.getClass();
         var5[var6] = new gx(var10004);

         for(var8 = 0; var8 < var7.a.size(); ++var8) {
            mu var9 = (mu)var7.a.elementAt(var8);
            Object var10;
            if ((var10 = var0.a(var9)) == null) {
               var0.a("Value of \"case\" label does not pose a constant value", var9.a());
               var10 = new Integer(99);
            }

            qd var11 = var0.a((nr)var9);
            var0.a((t)var1, (qd)var11, (qd)var2, (Object)var10);
            Integer var12;
            if (var10 instanceof Integer) {
               var12 = (Integer)var10;
            } else if (!(var10 instanceof Byte) && !(var10 instanceof Short)) {
               if (var10 instanceof Character) {
                  var12 = new Integer((Character)var10);
               } else {
                  var0.a("Value of case label must be a char, byte, short or int constant", var9.a());
                  var12 = new Integer(99);
               }
            } else {
               var12 = var10 instanceof Byte ? new Integer((Byte)var10) : new Integer((Short)var10);
            }

            if (var3.containsKey(var12)) {
               var0.a("Duplicate \"case\" switch label value", var9.a());
            }

            var3.put(var12, var5[var6]);
         }

         if (var7.a) {
            if (var4 != null) {
               var0.a("Duplicate \"default\" switch label", var7.a());
            }

            var4 = var5[var6];
         }
      }

      if (var4 == null) {
         var4 = var0.a((gg)var1);
      }

      gx var13 = var0.a.a();
      int[] var14 = a(var3);
      int var15;
      int var18;
      if (!var3.isEmpty()) {
         if (var14[0] + var3.size() >= var14[var14.length - 1] - var3.size()) {
            var8 = var14[0];
            var15 = var14[var14.length - 1];
            var0.b(var1, -86);
            (new eh(var0.a)).b();
            var0.a(var13, var4);
            var0.c(var8);
            var0.c(var15);
            int var17 = var8;

            for(var18 = 0; var18 < var14.length; ++var18) {
               for(int var20 = var14[var18]; var17 < var20; ++var17) {
                  var0.a(var13, var4);
               }

               var0.a(var13, (gx)var3.get(new Integer(var14[var18])));
               ++var17;
            }
         } else {
            var0.b(var1, -85);
            (new eh(var0.a)).b();
            var0.a(var13, var4);
            var0.c(var3.size());

            for(var8 = 0; var8 < var14.length; ++var8) {
               var0.c(var14[var8]);
               var0.a(var13, (gx)var3.get(new Integer(var14[var8])));
            }
         }
      }

      boolean var16 = true;

      for(var15 = 0; var15 < var1.a.size(); ++var15) {
         jk var19 = (jk)var1.a.elementAt(var15);
         var5[var15].b();
         var16 = true;

         for(var18 = 0; var18 < var19.b.size(); ++var18) {
            ov var21 = (ov)var19.b.elementAt(var18);
            if (!var16) {
               var0.a("Statement is unreachable", var21.a());
               break;
            }

            var16 = var0.a(var21);
         }
      }

      if (var1.a != null) {
         var1.a.b();
         var16 = true;
      }

      return var16;
   }

   static boolean a(cx var0, pk var1) {
      return var0.a(var1);
   }

   static boolean a(cx var0, eg var1) {
      Object var2;
      if ((var2 = (var0 = var0).a(var1.a)) != null && Boolean.TRUE.equals(var2)) {
         return var0.a((nu)var1, (ov)var1.a, (mu[])null);
      } else {
         fg var10003 = var0.a;
         var10003.getClass();
         var1.b = new gx(var10003);
         var1.a = false;
         gx var3 = var0.a.a();
         if (!var0.a(var1.a) && !var1.a) {
            if (var1.a == null) {
               return false;
            }

            var1.a.b();
         } else {
            var1.b.b();
            var0.a(var1.a, var3, true);
            if (var1.a != null) {
               var1.a.b();
            }
         }

         return true;
      }
   }

   static boolean a(cx var0, jv var1) {
      var1 = var1;
      var0 = var0;
      if ((var1.a & -17) != 0) {
         var0.a("The only allowed modifier in local variable declarations is \"final\"", var1.a());
      }

      for(int var2 = 0; var2 < var1.a.length; ++var2) {
         cb var3 = var1.a[var2];
         po var4 = var0.a(var1, var3);
         if (var0.a((ov)var1, (String)var3.a) != var4) {
            var0.a("Redefinition of local variable \"" + var3.a + "\" ", var3.a());
         }

         var4.a = var0.a.a(nq.a(var4.a.b()));
         if (var3.a != null) {
            if (var3.a instanceof mu) {
               mu var5 = (mu)var3.a;
               var0.a((t)var1, (qd)var0.b(var5), (qd)var4.a, (Object)var0.a(var5));
            } else {
               if (!(var3.a instanceof fu)) {
                  throw new RuntimeException("Unexpected rvalue or array initialized class " + var3.a.getClass().getName());
               }

               var0.a((fu)var3.a, var4.a);
            }

            var0.a((t)var1, (po)var4);
         }
      }

      return true;
   }

   static boolean a(cx var0, y var1) {
      bi var2 = null;

      hu var3;
      for(var3 = var1.a().a(); var3 instanceof ic || var3 instanceof iq; var3 = var3.a()) {
      }

      var2 = (bi)var3;
      qd var5;
      if ((var5 = var0.a(var2)) == qd.a) {
         if (var1.a != null) {
            var0.a("Method must not return a value", var1.a());
         }

         var0.a((hu)var1.a(), (hu)var2, (qd)null);
         var0.b(var1, -79);
         return false;
      } else if (var1.a == null) {
         var0.a("Method must return a value", var1.a());
         return false;
      } else {
         qd var4 = var0.b(var1.a);
         var0.a((t)var1, (qd)var4, (qd)var5, (Object)var0.a(var1.a));
         var0.a((hu)var1.a(), (hu)var2, (qd)var5);
         var0.b(var1, -84 + b(var5));
         return false;
      }
   }

   static boolean a(cx var0, fi var1) {
      qd var2 = (var0 = var0).b(var1.a);
      var0.a((t)var1, (qd)var2, (hu)var1.a());
      var0.b(var1, -65);
      return false;
   }

   static boolean a(cx var0, ki var1) {
      var1 = var1;
      Object var2 = null;
      hu var3;
      if (var1.a == null) {
         for(var3 = var1.a(); var3 instanceof ic || var3 instanceof iq; var3 = var3.a()) {
            if (var3 instanceof gg) {
               var2 = (gg)var3;
               break;
            }
         }

         if (var2 == null) {
            var0.a("\"break\" statement is not enclosed by a breakable statement", var1.a());
            return false;
         }
      } else {
         for(var3 = var1.a(); var3 instanceof ic || var3 instanceof iq; var3 = var3.a()) {
            p var4;
            if (var3 instanceof p && (var4 = (p)var3).a.equals(var1.a)) {
               var2 = var4;
               break;
            }
         }

         if (var2 == null) {
            var0.a("Statement \"break " + var1.a + "\" is not enclosed by a breakable statement with label \"" + var1.a + "\"", var1.a());
            return false;
         }
      }

      var0.a((hu)var1.a(), (hu)((gg)var2).a(), (qd)null);
      var0.a(var1, -89, var0.a((gg)var2));
      return false;
   }

   static boolean a(cx var0, af var1) {
      var1 = var1;
      nu var2 = null;
      hu var3;
      if (var1.a == null) {
         for(var3 = var1.a(); var3 instanceof ic || var3 instanceof iq; var3 = var3.a()) {
            if (var3 instanceof nu) {
               var2 = (nu)var3;
               break;
            }
         }

         if (var2 == null) {
            var0.a("\"continue\" statement is not enclosed by a continuable statement", var1.a());
            return false;
         }
      } else {
         for(var3 = var1.a(); var3 instanceof ic || var3 instanceof iq; var3 = var3.a()) {
            p var4;
            if (var3 instanceof p && (var4 = (p)var3).a.equals(var1.a)) {
               ic var5;
               for(var5 = var4.a; var5 instanceof p; var5 = ((p)var5).a) {
               }

               if (!(var5 instanceof nu)) {
                  var0.a("Labeled statement is not continuable", var5.a());
                  return false;
               }

               var2 = (nu)var5;
               break;
            }
         }

         if (var2 == null) {
            var0.a("Statement \"continue " + var1.a + "\" is not enclosed by a continuable statement with label \"" + var1.a + "\"", var1.a());
            return false;
         }
      }

      var2.a = true;
      var0.a((hu)var1.a(), (hu)var2.a(), (qd)null);
      var0.a(var1, -89, var2.b);
      return false;
   }

   static boolean a(cx var0, g var1) {
      return true;
   }

   static boolean a(cx var0, ox var1) {
      jl var2;
      if ((var2 = a((hu)var1, (String)var1.a.a)) != var1.a) {
         String var3 = "Redeclaration of local class \"" + var1.a.a + "\"; previously declared in " + var2.a();
         var0.a((String)var3, (aq)null);
      }

      var0.a((dr)var1.a);
      return true;
   }

   static boolean a(cx var0, dh var1) {
      jf var2 = (jf)var1.a();
      qd var3 = var0.a((dr)((kg)var2.a()));
      var0.b(var1, 42);
      if (var3.f() != null) {
         var0.b(var1, 43);
      }

      var0.a(var1, (hu)var2, (mu)null, var3, (mu[])var1.a);
      return true;
   }

   static boolean a(cx var0, nd var1) {
      jf var2 = (jf)var1.a();
      var0.b(var1, 42);
      kg var3 = (kg)var2.a();
      qd var5 = var0.a((dr)var3).g();
      Object var4;
      if (var1.a != null) {
         var4 = var1.a;
      } else {
         qd var6;
         if ((var6 = var5.f()) == null) {
            var4 = null;
         } else {
            ((mu)(var4 = new py(var1.a(), new hz(var1.a(), var6)))).a((ov)var1);
         }
      }

      var0.a(var1, (hu)var2, (mu)var4, var5, (mu[])var1.a);
      return true;
   }

   static void a(cx var0, mu var1) {
      (var0 = var0).b((t)var1, (qd)var0.b(var1));
   }

   static void a(cx var0, pq var1) {
      if (var1.a == "=") {
         var0.a((mu)var1.a);
         var0.a((t)var1, (qd)var0.b(var1.a), (qd)var0.a((nr)var1.a), (Object)var0.a(var1.a));
      } else {
         int var2 = var0.a((mu)var1.a);
         var0.a(var1, (int)var2);
         qd var4 = var0.a((mu)var1.a);
         qd var3;
         if (!b(var3 = var0.a((t)var1, (qd)var4, (String)var1.a.substring(0, var1.a.length() - 1).intern(), (mu)var1.a), var4) && !var0.b((t)var1, (qd)var3, (qd)var4)) {
            throw new RuntimeException("SNO: \"" + var1.a + "\" reconversion failed");
         }
      }

      var0.a(var1.a);
   }

   static void a(cx var0, gz var1) {
      po var2;
      if ((var2 = (var0 = var0).a(var1)) != null) {
         var0.b(var1, -124);
         var0.a((int)var2.a);
         var0.a(var1.a == "++" ? 1 : -1);
      } else {
         int var4 = var0.a((mu)var1.a);
         var0.a(var1, (int)var4);
         qd var5 = var0.a((mu)var1.a);
         qd var3 = var0.a((t)var1, (qd)var5);
         var0.b(var1, a(var3, 4, 10, 12, 15));
         if (var1.a == "++") {
            var0.b(var1, 96 + a(var3));
         } else if (var1.a == "--") {
            var0.b(var1, 100 + a(var3));
         } else {
            var0.a("Unexpected operator \"" + var1.a + "\"", var1.a());
         }

         var0.a((t)var1, (qd)var3, (qd)var5);
         var0.a(var1.a);
      }
   }

   static void a(cx var0, ke var1) {
      var0.b(var1.a);
   }

   static void a(cx var0, mu var1, gx var2, boolean var3) {
      var0.b(var1, var2, var3);
   }

   static void a(cx var0, kh var1, gx var2, boolean var3) {
      if (var1.a == "!") {
         var0.a(var1.a, var2, !var3);
      } else {
         var0.a("Boolean expression expected", var1.a());
      }
   }

   static void a(cx var0, qa var1, gx var2, boolean var3) {
      var1 = var1;
      var0 = var0;
      if (var1.a != "|" && var1.a != "^" && var1.a != "&") {
         if (var1.a != "||" && var1.a != "&&") {
            if (var1.a == "==" || var1.a == "!=" || var1.a == "<=" || var1.a == ">=" || var1.a == "<" || var1.a == ">") {
               label554: {
                  int var11 = var1.a == "==" ? 0 : (var1.a == "!=" ? 1 : (var1.a == "<" ? 2 : (var1.a == ">=" ? 3 : (var1.a == ">" ? 4 : (var1.a == "<=" ? 5 : Integer.MIN_VALUE)))));
                  if (!var3) {
                     var11 ^= 1;
                  }

                  boolean var12 = var0.a(var1.a) == mu.d;
                  boolean var14 = var0.a(var1.b) == mu.d;
                  qd var10;
                  if (!var12 && !var14) {
                     qd var13 = var0.b(var1.a);
                     ex var15 = var0.a.a();
                     var10 = var0.b(var1.b);
                     if (var0.a(var13).f() && var0.a(var10).f() && (var1.a != "==" && var1.a != "!=" || var13.e() || var10.e())) {
                        qd var16;
                        if ((var16 = var0.a((t)var1, (qd)var13, (ex)var15, (qd)var10)) == qd.f) {
                           var0.a(var1, var11 + -97, var2);
                           return;
                        } else if (var16 == qd.g) {
                           var0.b(var1, -108);
                           var0.a(var1, var11 + -103, var2);
                           return;
                        } else {
                           if (var16 == qd.e) {
                              var0.b(var1, -106);
                              var0.a(var1, var11 + -103, var2);
                           } else {
                              if (var16 != qd.d) {
                                 throw new RuntimeException("Unexpected promoted type \"" + var16 + "\"");
                              }

                              var0.b(var1, -104);
                              var0.a(var1, var11 + -103, var2);
                           }

                           return;
                        }
                     }

                     if ((var13 != qd.i || var0.a(var10) != qd.i) && (var10 != qd.i || var0.a(var13) != qd.i)) {
                        if (!var13.e() && !var10.e()) {
                           if (var1.a != "==" && var1.a != "!=") {
                              var0.a("Operator \"" + var1.a + "\" not allowed on reference operands", var1.a());
                           }

                           var0.a(var1, var11 + -91, var2);
                           return;
                        }

                        var0.a("Cannot compare types \"" + var13 + "\" and \"" + var10 + "\"", var1.a());
                        break label554;
                     }

                     if (var1.a != "==" && var1.a != "!=") {
                        var0.a("Operator \"" + var1.a + "\" not allowed on boolean operands", var1.a());
                     }

                     ha var7 = var0.a;
                     if (var13 == var7.a("Ljava/lang/Boolean;")) {
                        var0.a.a(var15);

                        try {
                           var0.d(var1, var7.a("Ljava/lang/Boolean;"), qd.i);
                        } finally {
                           var0.a.e();
                        }
                     }

                     if (var10 == var7.a("Ljava/lang/Boolean;")) {
                        var0.d(var1, var7.a("Ljava/lang/Boolean;"), qd.i);
                     }

                     var0.a(var1, var11 + -97, var2);
                     return;
                  }

                  if (var1.a != "==" && var1.a != "!=") {
                     var0.a("Operator \"" + var1.a + "\" not allowed on operand \"null\"", var1.a());
                  }

                  if ((var10 = var0.b(var12 ? var1.b : var1.a)).e()) {
                     var0.a("Cannot compare \"null\" with primitive type \"" + var10.toString() + "\"", var1.a());
                  }

                  var0.a(var1, var11 + -58, var2);
                  return;
               }
            }

            var0.a("Boolean expression expected", var1.a());
         } else {
            Object var4;
            if ((var4 = var0.a(var1.a)) instanceof Boolean) {
               if (!((Boolean)var4 ^ var1.a == "||")) {
                  var0.a(var1.a, var2, true ^ !var3);
                  var0.a(var1.b);
                  return;
               }

               var0.a(var1.b, var2, true ^ !var3);
            } else {
               Object var5;
               if ((var5 = var0.a(var1.b)) instanceof Boolean) {
                  if (!((Boolean)var5 ^ var1.a == "||")) {
                     var0.b((t)var1.a, (qd)var0.b(var1.a));
                     var0.a(var1.b, var2, true ^ !var3);
                     return;
                  }

                  var0.a(var1.a, var2, true ^ !var3);
               } else {
                  if (!(var1.a == "||" ^ !var3)) {
                     fg var10002 = var0.a;
                     var10002.getClass();
                     gx var6 = new gx(var10002);
                     var0.a(var1.a, var6, false ^ !var3);
                     var0.a(var1.b, var2, true ^ !var3);
                     var6.b();
                     return;
                  }

                  var0.a(var1.a, var2, true ^ !var3);
                  var0.a(var1.b, var2, true ^ !var3);
               }
            }
         }

      } else {
         var0.b(var1, var2, var3);
      }
   }

   static void a(cx var0, ke var1, gx var2, boolean var3) {
      var0.a(var1.a, var2, var3);
   }

   static int a(cx var0, cl var1) {
      if (!(var0 = var0).b(var1.a).d()) {
         var0.a("Cannot determine length of non-array type", var1.a());
      }

      return 1;
   }

   static int a(cx var0, mu var1) {
      return 0;
   }

   static int a(cx var0, gp var1) {
      return (var0 = var0).a(var0.a(var0.a(var1)));
   }

   static int a(cx var0, kr var1) {
      qd var2;
      if (!(var2 = (var0 = var0).b(var1.a)).d()) {
         var0.a("Subscript not allowed on non-array type \"" + var2.toString() + "\"", var1.a());
      }

      if (!b(var2 = var0.b(var1.b), qd.f) && !var0.a((t)var1, (qd)var2, (qd)qd.f)) {
         var0.a("Index expression of type \"" + var2 + "\" cannot be widened to \"int\"", var1.a());
      }

      return 2;
   }

   static int a(cx var0, gq var1) {
      if (var1.a.a()) {
         var0.a((nr)var0.a(var1.a));
         return 0;
      } else {
         var0.b(var0.a(var1.a));
         return 1;
      }
   }

   static int a(cx var0, mz var1) {
      (var0 = var0).a(var1);
      return var0.a(var1.a);
   }

   static int a(cx var0, cu var1) {
      (var0 = var0).a(var1);
      return var0.a(var1.a);
   }

   static int a(cx var0, ke var1) {
      return var0.a(var1.a);
   }

   static qd a(cx var0, cl var1) {
      var0.b(var1, -66);
      return qd.f;
   }

   static qd a(cx var0, pq var1) {
      int var2;
      qd var3;
      qd var4;
      if (var1.a == "=") {
         var2 = var0.a((mu)var1.a);
         var3 = var0.b(var1.a);
         var4 = var0.a((nr)var1.a);
         Object var5 = var0.a(var1.a);
         var0.a((t)var1, (qd)var3, (qd)var4, (Object)var5);
         var0.b(var1, var4, var2);
         var0.a(var1.a);
         return var4;
      } else {
         var2 = var0.a((mu)var1.a);
         var0.a(var1, (int)var2);
         var3 = var0.a((mu)var1.a);
         if (!b(var4 = var0.a((t)var1, (qd)var3, (String)var1.a.substring(0, var1.a.length() - 1).intern(), (mu)var1.a), var3) && !var0.b((t)var1, (qd)var4, (qd)var3)) {
            throw new RuntimeException("SNO: \"" + var1.a + "\" reconversion failed");
         } else {
            var0.b(var1, var3, var2);
            var0.a(var1.a);
            return var3;
         }
      }
   }

   static qd a(cx var0, kh var1) {
      if (var1.a == "!") {
         return var0.a((ml)var1);
      } else if (var1.a == "+") {
         return var0.a((t)var1, (qd)var0.b((t)var1, (qd)var0.b(var1.a)));
      } else {
         qd var2;
         if (var1.a == "-") {
            if (var1.a instanceof ne) {
               ne var3 = (ne)var1.a;
               var0.a((t)var1, (Object)var0.a(var3));
               return var0.a((t)var1, (qd)var0.a(var3));
            } else {
               var2 = var0.a((t)var1, (qd)var0.b((t)var1, (qd)var0.b(var1.a)));
               var0.b(var1, 116 + a(var2));
               return var2;
            }
         } else {
            if (var1.a == "~") {
               var2 = var0.b(var1.a);
               if ((var2 = var0.a((t)var1, (qd)var2)) == qd.f) {
                  var0.b(var1, 2);
                  var0.b(var1, -126);
                  return qd.f;
               }

               if (var2 == qd.g) {
                  var0.b(var1, 20);
                  var0.a(-1L);
                  var0.b(var1, -125);
                  return qd.g;
               }

               var0.a("Operator \"~\" not applicable to type \"" + var2 + "\"", var1.a());
            }

            var0.a("Unexpected operator \"" + var1.a + "\"", var1.a());
            return var0.a.a;
         }
      }
   }

   static qd a(cx var0, qa var1) {
      return var1.a != "||" && var1.a != "&&" && var1.a != "==" && var1.a != "!=" && var1.a != "<" && var1.a != ">" && var1.a != "<=" && var1.a != ">=" ? var0.a((t)var1, (qd)null, (Enumeration)var1.a(), (String)var1.a) : var0.a((ml)var1);
   }

   static qd a(cx var0, nj var1) {
      qd var2 = (var0 = var0).a((nr)var1.a);
      qd var3;
      if (!b(var3 = var0.b(var1.a), var2) && !var0.a((t)var1, (qd)var3, (qd)var2) && !var0.b((t)var1, (qd)var3, (qd)var2) && !(!var2.e() && var3 != var2 ? var2.a(var3) : false)) {
         boolean var10000;
         if (!var0.e(var3, var2)) {
            var10000 = false;
         } else {
            var0.b(var1, -64);
            var0.a(var2.b());
            var10000 = true;
         }

         if (!var10000) {
            if (var0.b(var3) == var2) {
               var0.c(var1, var3, var2);
               var10000 = true;
            } else {
               var10000 = false;
            }

            if (!var10000) {
               if (var0.c(var3) == var2) {
                  var0.d(var1, var3, var2);
                  var10000 = true;
               } else {
                  var10000 = false;
               }

               if (!var10000) {
                  var0.a("Cannot cast \"" + var3 + "\" to \"" + var2 + "\"", var1.a());
               }
            }
         }
      }

      return var2;
   }

   static qd a(cx var0, pb var1) {
      var0 = var0;
      aq var2 = var1.a();
      ha var3 = var0.a;
      qd var4;
      if ((var4 = var0.a((nr)var1.a)).e()) {
         var0.b(var1, -78);
         String var12;
         if ((var12 = var4 == qd.a ? "Ljava/lang/Void;" : (var4 == qd.b ? "Ljava/lang/Byte;" : (var4 == qd.c ? "Ljava/lang/Character;" : (var4 == qd.d ? "Ljava/lang/Double;" : (var4 == qd.e ? "Ljava/lang/Float;" : (var4 == qd.f ? "Ljava/lang/Integer;" : (var4 == qd.g ? "Ljava/lang/Long;" : (var4 == qd.h ? "Ljava/lang/Short;" : (var4 == qd.i ? "Ljava/lang/Boolean;" : null))))))))) == null) {
            throw new RuntimeException("SNO: Unidentifiable primitive type \"" + var4 + "\"");
         } else {
            var0.a(var12, "TYPE", "Ljava/lang/Class;");
            return var3.c;
         }
      } else {
         Object var6;
         for(var6 = var1.a(); !(var6 instanceof dr); var6 = ((hu)var6).a()) {
         }

         ft var5 = (ft)var6;
         boolean var13 = false;
         Enumeration var7 = var5.b.elements();

         while(var7.hasMoreElements()) {
            if (((kl)var7.nextElement()).a.equals("class$")) {
               var13 = true;
               break;
            }
         }

         if (!var13) {
            var0.a(var1);
         }

         Vector var14;
         if (var5 instanceof kg) {
            var14 = ((kg)var5).d;
         } else {
            if (!(var5 instanceof df)) {
               throw new RuntimeException("SNO: AbstractTypeDeclaration is neither ClassDeclaration nor InterfaceDeclaration");
            }

            var14 = ((df)var5).a;
         }

         String var8;
         String var11;
         if ((var8 = nq.c(var4.b())).startsWith("[")) {
            if ((var11 = "array" + var8.replace('.', '$').replace('[', '$')).endsWith(";")) {
               var11 = var11.substring(0, var11.length() - 1);
            }
         } else {
            var11 = "class$" + var8.replace('.', '$');
         }

         var13 = false;
         var7 = var14.elements();

         gb var18;
         label105:
         while(var7.hasMoreElements()) {
            ht var9;
            if ((var9 = (ht)var7.nextElement()).b() && var9 instanceof gb) {
               var18 = (gb)var9;
               cm[] var19 = var0.a(var18);

               for(int var10 = 0; var10 < var19.length; ++var10) {
                  if (var19[var10].a().equals(var11)) {
                     var13 = true;
                     break label105;
                  }
               }
            }
         }

         if (!var13) {
            hz var15 = new hz(var2, var3.c);
            var18 = new gb(var2, (String)null, (short)8, var15, new cb[]{new cb(var2, var11, 0, (ho)null)});
            if (var5 instanceof kg) {
               ((kg)var5).a((ht)var18);
            } else {
               if (!(var5 instanceof df)) {
                  throw new RuntimeException("SNO: AbstractTypeDeclaration is neither ClassDeclaration nor InterfaceDeclaration");
               }

               ((df)var5).a(var18);
            }
         }

         hz var16 = new hz(var2, var0.a((dr)var5));
         mz var17 = new mz(var2, var16, var11);
         da var20;
         (var20 = new da(var2, new qa(var2, var17, "!=", new i(var2, (Object)null)), var17, new pq(var2, var17, "=", new gt(var2, var16, "class$", new mu[]{new i(var2, var8)})))).a((ov)var1.a());
         return var0.a((mu)var20);
      }
   }

   static qd a(cx var0, da var1) {
      fg var10002 = var0.a;
      var10002.getClass();
      gx var6 = new gx(var10002);
      Object var2;
      qd var3;
      ex var4;
      ex var5;
      qd var7;
      if ((var2 = var0.a(var1.a)) instanceof Boolean) {
         if ((Boolean)var2) {
            var7 = var0.b(var1.b);
            var4 = var0.a.a();
            var3 = var0.a((nr)var1.c);
            var5 = null;
         } else {
            var7 = var0.a((nr)var1.b);
            var4 = null;
            var3 = var0.b(var1.c);
            var5 = var0.a.b();
         }
      } else {
         var10002 = var0.a;
         var10002.getClass();
         gx var8 = new gx(var10002);
         var0.a(var1.a, var8, false);
         var7 = var0.b(var1.b);
         var4 = var0.a.a();
         var0.a(var1, -89, var6);
         var8.b();
         var3 = var0.b(var1.c);
         var5 = var0.a.b();
      }

      if (var7 == var3) {
         var3 = var7;
      } else if (var7.f() && var3.f()) {
         var3 = var0.a(var1, (qd)var7, (ex)var4, var3, (ex)var5);
      } else if (var0.a(var1.b) == mu.d && !var3.e()) {
         var3 = var3;
      } else if (!var7.e() && var0.a(var1.c) == mu.d) {
         var3 = var7;
      } else {
         if (var7.e() || var3.e()) {
            var0.a("Incompatible expression types \"" + var7 + "\" and \"" + var3 + "\"", var1.a());
            return var0.a.a;
         }

         if (var7.a(var3)) {
            var3 = var7;
         } else {
            if (!var3.a(var7)) {
               var0.a("Reference types \"" + var7 + "\" and \"" + var3 + "\" don't match", var1.a());
               return var0.a.a;
            }

            var3 = var3;
         }
      }

      var6.b();
      return var3;
   }

   static qd a(cx var0, i var1) {
      return var0.a((t)var1, (Object)var1.a);
   }

   static qd a(cx var0, gz var1) {
      po var2;
      if ((var2 = (var0 = var0).a(var1)) != null) {
         if (!var1.a) {
            var0.a((t)var1, (po)var2);
         }

         var0.b(var1, -124);
         var0.a((int)var2.a);
         var0.a(var1.a == "++" ? 1 : -1);
         if (var1.a) {
            var0.a((t)var1, (po)var2);
         }

         return var2.a;
      } else {
         int var5 = var0.a((mu)var1.a);
         var0.a(var1, (int)var5);
         qd var3 = var0.a((mu)var1.a);
         if (!var1.a) {
            var0.b(var1, var3, var5);
         }

         qd var4 = var0.a((t)var1, (qd)var3);
         var0.b(var1, a(var4, 4, 10, 12, 15));
         if (var1.a == "++") {
            var0.b(var1, 96 + a(var4));
         } else if (var1.a == "--") {
            var0.b(var1, 100 + a(var4));
         } else {
            var0.a("Unexpected operator \"" + var1.a + "\"", var1.a());
         }

         var0.a((t)var1, (qd)var4, (qd)var3);
         if (var1.a) {
            var0.b(var1, var3, var5);
         }

         var0.a(var1.a);
         return var3;
      }
   }

   static qd a(cx var0, nl var1) {
      qd var2 = (var0 = var0).b(var1.a);
      qd var3;
      if ((var3 = var0.a((nr)var1.a)).a(var2)) {
         var0.b((t)var1, (qd)var2);
         var0.b(var1, 4);
      } else if (!var2.b() && !var3.b() && !var2.a(var3)) {
         var0.a("\"" + var2 + "\" can never be an instance of \"" + var3 + "\"", var1.a());
      } else {
         var0.b(var1, -63);
         var0.a(var3.b());
      }

      return qd.i;
   }

   static qd a(cx var0, gt var1) {
      cx var10000 = var0;
      var1 = var1;
      var0 = var0;
      gw var2 = var10000.a(var1);
      if (var1.a == null) {
         Object var5;
         for(var5 = var1.a(); !(var5 instanceof ht); var5 = ((hu)var5).a()) {
         }

         ht var3 = (ht)var5;
         if (!(var5 instanceof kg)) {
            var5 = ((hu)var5).a();
         }

         kg var4 = (kg)var5;
         if (!var2.a()) {
            if (var3.b()) {
               var0.a("Instance method \"" + var2.toString() + "\" cannot be invoked in static context", var1.a());
            }

            var0.a((t)var1, (kg)var4, (ht)var3, (qd)var2.b());
         }
      } else {
         boolean var8;
         if (var8 = var0.a(var1.a)) {
            var0.a((nr)var0.a(var1.a));
         } else {
            var0.b(var0.a(var1.a));
         }

         if (var2.a()) {
            if (!var8) {
               var0.b((t)var1.a, (qd)var0.a(var1.a));
            }
         } else if (var8) {
            var0.a("Instance method \"" + var1.a + "\" cannot be invoked in static context", var1.a());
         }
      }

      qd[] var9 = var2.a();

      int var11;
      for(var11 = 0; var11 < var1.a.length; ++var11) {
         var0.a((t)var1, (qd)var0.b(var1.a[var11]), (qd)var9[var11], (Object)var0.a(var1.a[var11]));
      }

      var0.a((fb)var2, (ov)var1.a());
      if (var2.b().b()) {
         var0.b(var1, -71);
         String var10001 = var2.b().b();
         String var10002 = var2.a_();
         String var14 = var2.a();
         String var12 = var10002;
         String var10 = var10001;
         fg var6;
         (var6 = var0.a).a((short)-1, var6.a().c(var10, var12, var14));
         qd[] var13 = var2.a();
         int var15 = 1;

         for(int var7 = 0; var7 < var13.length; ++var7) {
            var15 += nq.a(var13[var7].b());
         }

         var0.a(var15);
         var0.a((int)0);
      } else if (!var2.a() && var2.a() == pe.a) {
         var0.b(var1, -72);
         var0.b(var2.b().b(), var2.a_() + '$', gf.a(var2.a(), var2.b().b()));
      } else {
         var11 = var2.a() ? -72 : -74;
         var0.b(var1, var11);
         var0.b(var2.b().b(), var2.a_(), var2.a());
      }

      return var2.a();
   }

   static qd a(cx var0, u var1) {
      cx var10000 = var0;
      var1 = var1;
      var0 = var0;
      gw var2 = var10000.a(var1);

      Object var3;
      for(var3 = var1.a(); var3 instanceof ic || var3 instanceof iq; var3 = ((hu)var3).a()) {
      }

      bi var5;
      if ((var5 = var3 instanceof bi ? (bi)var3 : null) == null) {
         var0.a("Cannot invoke superclass method in non-method scope", var1.a());
         return qd.f;
      } else {
         if ((var5.a & 8) != 0) {
            var0.a("Cannot invoke superclass method in static context", var1.a());
         }

         var0.a(var1, var0.a(var5.a()), (int)0);
         qd[] var6 = var2.a();

         for(int var4 = 0; var4 < var1.a.length; ++var4) {
            var0.a((t)var1, (qd)var0.b(var1.a[var4]), (qd)var6[var4], (Object)var0.a(var1.a[var4]));
         }

         var0.b(var1, -73);
         var0.b(var2.b().b(), var1.a, var2.a());
         return var2.a();
      }
   }

   static qd a(cx var0, ne var1) {
      if (var1.a == el.a || var1.a == el.a) {
         var0.a("This literal value may only appear in a negated context", var1.a());
      }

      return var0.a((t)var1, (Object)(var1.a == null ? mu.d : var1.a));
   }

   static qd a(cx var0, mr var1) {
      eo var2 = var1.a;
      pm[] var3;
      if ((var3 = var0.a((dr)var2).g().b()).length == 0) {
         throw new RuntimeException("SNO: Base class has no constructors");
      } else {
         pm var13;
         qd[] var4 = (var13 = (pm)var0.a((t)var1, (cn[])var3, (mu[])var1.a)).a();
         aq var6 = var1.a();
         Vector var7 = new Vector();
         if (var1.a != null) {
            var7.addElement(new ct(var6, true, new hz(var6, var0.a((nr)var1.a)), "this$base"));
         }

         for(int var8 = 0; var8 < var4.length; ++var8) {
            var7.addElement(new ct(var6, true, new hz(var6, var4[var8]), "p" + var8));
         }

         ct[] var17 = new ct[var7.size()];
         var7.copyInto(var17);
         ct[] var5 = var17;
         qd[] var15;
         gs[] var18 = new gs[(var15 = var13.b()).length];

         int var14;
         for(var14 = 0; var14 < var15.length; ++var14) {
            var18[var14] = new hz(var6, var15[var14]);
         }

         var14 = 0;
         lj var16;
         if (var1.a == null) {
            var16 = null;
         } else {
            ++var14;
            var16 = new lj(var6, var17[0]);
         }

         mu[] var9 = new mu[var4.length];

         for(int var10 = 0; var10 < var4.length; ++var10) {
            var9[var10] = new lj(var6, var5[var14++]);
         }

         var2.a(new jf(var6, (String)null, (short)0, var5, var18, new nd(var6, var16, var9), new il(var6)));
         var0.a((dr)var2);
         var0.b(var1, -69);
         var0.a(var0.a((dr)var1.a).b());
         var0.b(var1, 89);
         mu[] var19;
         if (var1.a == null) {
            var19 = var1.a;
         } else {
            (var19 = new mu[var1.a.length + 1])[0] = var1.a;
            System.arraycopy(var1.a, 0, var19, 1, var1.a.length);
         }

         Object var11;
         for(var11 = var1.a(); !(var11 instanceof ht); var11 = ((hu)var11).a()) {
         }

         cc var12;
         if (((ht)var11).b()) {
            var12 = null;
         } else {
            (var12 = new cc(var6)).a((ov)var1.a());
         }

         var0.a(var1, (hu)var1.a(), (mu)var12, var0.a((dr)var1.a), (mu[])var19);
         return var0.a((dr)var1.a);
      }
   }

   static qd a(cx var0, d var1) {
      var1 = var1;
      var0 = var0;

      for(int var2 = 0; var2 < var1.a.length; ++var2) {
         qd var3;
         if ((var3 = var0.b(var1.a[var2])) != qd.f && var0.a((t)var1, (qd)var3) != qd.f) {
            var0.a("Invalid array size expression type", var1.a());
         }
      }

      return var0.a(var1, var1.a.length, var1.a, var0.a((nr)var1.a));
   }

   static qd a(cx var0, ey var1) {
      qd var2 = (var0 = var0).a((nr)var1.a);
      var0.a(var1.a, var2);
      return var2;
   }

   static qd a(cx var0, ok var1) {
      if (var1.a == null) {
         var1.a = var0.a((nr)var1.a);
      }

      var0.b(var1, -69);
      var0.a(var1.a.b());
      var0.b(var1, 89);
      if (var1.a.b()) {
         var0.a("Cannot instantiate \"" + var1.a + "\"", var1.a());
      }

      qd var10001 = var1.a;
      ov var4 = var1.a();
      qd var3 = var10001;
      String var5;
      if ((var5 = var0.a(var3, var4)) != null) {
         var0.a(var5, var4.a());
      }

      if (var1.a.c()) {
         var0.a("Cannot instantiate abstract \"" + var1.a + "\"", var1.a());
      }

      Object var2;
      if (var1.a != null) {
         if (var1.a.f() == null) {
            var5 = "Static member class cannot be instantiated with qualified NEW";
            var0.a((String)var5, (aq)null);
         }

         var2 = var1.a;
      } else {
         for(var2 = var1.a(); !(var2 instanceof ht); var2 = ((hu)var2).a()) {
         }

         ht var7 = (ht)var2;
         if ((dr)((hu)var2).a() instanceof kg && !var7.b()) {
            qd var6;
            if ((var6 = var1.a.e()) == null) {
               var2 = null;
            } else {
               ((mu)(var2 = new py(var1.a(), new hz(var1.a(), var6)))).a(var1.a());
            }
         } else {
            if (var1.a.f() != null) {
               var0.a("Instantiation of \"" + var1.a + "\" requires an enclosing instance", var1.a());
            }

            var2 = null;
         }
      }

      var0.a(var1, (hu)var1.a(), (mu)var2, var1.a, (mu[])var1.a);
      return var1.a;
   }

   static qd a(cx var0, lj var1) {
      po var2 = (var0 = var0).a(var1.a);
      var0.a((t)var1, (po)var2);
      return var2.a;
   }

   static qd a(cx var0, py var1) {
      cx var10000 = var0 = var0;
      if (var1.a == null) {
         var0.a(var1);
      }

      var10000.a((t)var1, (kg)var1.a, (ht)var0.a(var1), (qd)var0.a(var1));
      return var0.a(var1);
   }

   static qd a(cx var0, cc var1) {
      (var0 = var0).b(var1, 42);
      return var0.a(var1);
   }

   static qd a(cx var0, gp var1) {
      return (var0 = var0).a(var0.a(var0.a(var1)));
   }

   static qd a(cx var0, kr var1) {
      qd var2 = (var0 = var0).a((nr)var1);
      var0.b(var1, 46 + c(var2));
      return var2;
   }

   static qd a(cx var0, gq var1) {
      (var0 = var0).a((fb)var1.a, (ov)var1.a());
      if (var1.a.a()) {
         var0.b(var1, -78);
      } else {
         var0.b(var1, -76);
      }

      var0.a(var1.a.b().b(), var1.a.a(), var1.a.a().b());
      return var1.a.a();
   }

   static qd a(cx var0, mz var1) {
      (var0 = var0).a(var1);
      return var0.a(var1.a);
   }

   static qd a(cx var0, cu var1) {
      (var0 = var0).a(var1);
      return var0.a(var1.a);
   }

   static qd a(cx var0, qc var1) {
      return var0.a((t)var1, (po)var1.a);
   }

   static qd a(cx var0, ke var1) {
      return var0.a(var1.a);
   }

   static Object a(cx var0, mu var1) {
      return null;
   }

   static Object a(cx var0, kh var1) {
      if (var1.a.equals("+")) {
         return var0.a(var1.a);
      } else if (var1.a.equals("-")) {
         return var0.b(var1.a);
      } else {
         Boolean var10000;
         if (var1.a.equals("!")) {
            Object var2;
            if (!((var2 = var0.a(var1.a)) instanceof Boolean)) {
               return null;
            }

            var10000 = (Boolean)var2 ? Boolean.FALSE : Boolean.TRUE;
         } else {
            var10000 = null;
         }

         return var10000;
      }
   }

   static Object a(cx var0, qa var1) {
      var1 = var1;
      var0 = var0;
      Boolean var10000;
      if ((var1.a == "==" || var1.a == "!=") && var0.a(var1.a) == mu.d && var0.a(var1.b) == mu.d) {
         if (var1.a != "==") {
            return Boolean.FALSE;
         }

         var10000 = Boolean.TRUE;
      } else {
         if (var1.a != "|" && var1.a != "^" && var1.a != "&" && var1.a != "*" && var1.a != "/" && var1.a != "%" && var1.a != "+" && var1.a != "-") {
            Object var15;
            if ((var1.a == "&&" || var1.a == "||") && (var15 = var0.a(var1.a)) instanceof Boolean) {
               boolean var16 = (Boolean)var15;
               if (var1.a == "&&") {
                  if (!var16) {
                     var10000 = Boolean.FALSE;
                     return var10000;
                  }
               } else if (var16) {
                  var10000 = Boolean.TRUE;
                  return var10000;
               }

               return var0.a(var1.b);
            }
         } else {
            Vector var2 = new Vector();
            Enumeration var3 = var1.a();

            while(true) {
               Object var4;
               if (!var3.hasMoreElements()) {
                  var4 = (var3 = var2.elements()).nextElement();

                  while(var3.hasMoreElements()) {
                     Object var14 = var3.nextElement();
                     if (var1.a == "+" && (var4 instanceof String || var14 instanceof String)) {
                        StringBuffer var20;
                        (var20 = new StringBuffer()).append(var4.toString()).append(var14.toString());

                        while(var3.hasMoreElements()) {
                           var20.append(var3.nextElement().toString());
                        }

                        return var20.toString();
                     }

                     if (!(var4 instanceof Byte) && !(var4 instanceof Double) && !(var4 instanceof Float) && !(var4 instanceof Integer) && !(var4 instanceof Long) && !(var4 instanceof Short) || !(var14 instanceof Byte) && !(var14 instanceof Double) && !(var14 instanceof Float) && !(var14 instanceof Integer) && !(var14 instanceof Long) && !(var14 instanceof Short)) {
                        return null;
                     }

                     if (!(var4 instanceof Double) && !(var14 instanceof Double)) {
                        if (!(var4 instanceof Float) && !(var14 instanceof Float)) {
                           if (!(var4 instanceof Long) && !(var14 instanceof Long)) {
                              int var19 = var4 instanceof Byte ? (Byte)var4 : (var4 instanceof Short ? (Short)var4 : (Integer)var4);
                              int var21 = var14 instanceof Byte ? (Byte)var14 : (var14 instanceof Short ? (Short)var14 : (Integer)var14);
                              int var24;
                              if (var1.a == "|") {
                                 var24 = var19 | var21;
                              } else if (var1.a == "^") {
                                 var24 = var19 ^ var21;
                              } else if (var1.a == "&") {
                                 var24 = var19 & var21;
                              } else if (var1.a == "*") {
                                 var24 = var19 * var21;
                              } else if (var1.a == "/") {
                                 var24 = var19 / var21;
                              } else if (var1.a == "%") {
                                 var24 = var19 % var21;
                              } else if (var1.a == "+") {
                                 var24 = var19 + var21;
                              } else {
                                 if (var1.a != "-") {
                                    return null;
                                 }

                                 var24 = var19 - var21;
                              }

                              var4 = new Integer(var24);
                           } else {
                              long var18 = (Long)var4;
                              long var23 = (Long)var14;
                              long var25;
                              if (var1.a == "|") {
                                 var25 = var18 | var23;
                              } else if (var1.a == "^") {
                                 var25 = var18 ^ var23;
                              } else if (var1.a == "&") {
                                 var25 = var18 & var23;
                              } else if (var1.a == "*") {
                                 var25 = var18 * var23;
                              } else if (var1.a == "/") {
                                 var25 = var18 / var23;
                              } else if (var1.a == "%") {
                                 var25 = var18 % var23;
                              } else if (var1.a == "+") {
                                 var25 = var18 + var23;
                              } else {
                                 if (var1.a != "-") {
                                    return null;
                                 }

                                 var25 = var18 - var23;
                              }

                              var4 = new Long(var25);
                           }
                        } else {
                           float var17 = (Float)var4;
                           float var9 = (Float)var14;
                           float var22;
                           if (var1.a == "*") {
                              var22 = var17 * var9;
                           } else if (var1.a == "/") {
                              var22 = var17 / var9;
                           } else if (var1.a == "%") {
                              var22 = var17 % var9;
                           } else if (var1.a == "+") {
                              var22 = var17 + var9;
                           } else {
                              if (var1.a != "-") {
                                 return null;
                              }

                              var22 = var17 - var9;
                           }

                           var4 = new Float(var22);
                        }
                     } else {
                        double var8 = (Double)var4;
                        double var10 = (Double)var14;
                        double var12;
                        if (var1.a == "*") {
                           var12 = var8 * var10;
                        } else if (var1.a == "/") {
                           var12 = var8 / var10;
                        } else if (var1.a == "%") {
                           var12 = var8 % var10;
                        } else if (var1.a == "+") {
                           var12 = var8 + var10;
                        } else {
                           if (var1.a != "-") {
                              return null;
                           }

                           var12 = var8 - var10;
                        }

                        var4 = new Double(var12);
                     }
                  }

                  return var4;
               }

               if ((var4 = var0.a((mu)var3.nextElement())) == null) {
                  break;
               }

               var2.addElement(var4);
            }
         }

         var10000 = null;
      }

      return var10000;
   }

   static Object a(cx var0, nj var1) {
      Object var2;
      if ((var2 = (var0 = var0).a(var1.a)) != null && (var2 instanceof Byte || var2 instanceof Double || var2 instanceof Float || var2 instanceof Integer || var2 instanceof Long || var2 instanceof Short)) {
         qd var3;
         if ((var3 = var0.a((nr)var1.a)) == qd.b) {
            return new Byte((Byte)var2);
         }

         if (var3 == qd.h) {
            return new Short((Short)var2);
         }

         if (var3 == qd.f) {
            return new Integer((Integer)var2);
         }

         if (var3 == qd.g) {
            return new Long((Long)var2);
         }

         if (var3 == qd.e) {
            return new Float((Float)var2);
         }

         if (var3 == qd.d) {
            return new Double((Double)var2);
         }
      }

      return null;
   }

   static Object a(cx var0, i var1) {
      return var1.a;
   }

   static Object a(cx var0, ne var1) {
      if (var1.a == el.a || var1.a == el.a) {
         var0.a("This literal value may only appear in a negated context", var1.a());
      }

      return var1.a == null ? mu.d : var1.a;
   }

   static Object a(cx var0, gp var1) {
      return (var0 = var0).a(var0.a(var0.a(var1)));
   }

   static Object a(cx var0, gq var1) {
      return var1.a.a();
   }

   static Object a(cx var0, ke var1) {
      return var0.a(var1.a);
   }

   static Object b(cx var0, mu var1) {
      return null;
   }

   static Object b(cx var0, kh var1) {
      if (var1.a.equals("+")) {
         return var0.b(var1.a);
      } else {
         return var1.a.equals("-") ? var0.a(var1.a) : null;
      }
   }

   static Object b(cx var0, ne var1) {
      return var0.a(var1);
   }

   static Object b(cx var0, ke var1) {
      return var0.b(var1.a);
   }

   static void a(cx var0, gp var1) {
      (var0 = var0).a(var0.a(var0.a(var1)));
   }

   static void a(cx var0, kr var1) {
      (var0 = var0).b(var1, 79 + c(var0.a((nr)var1)));
   }

   static void a(cx var0, gq var1) {
      (var0 = var0).a((fb)var1.a, (ov)var1.a());
      var0.b(var1, var1.a.a() ? -77 : -75);
      var0.a(var1.a.b().b(), var1.a.a(), var1.a.a().b());
   }

   static void a(cx var0, mz var1) {
      (var0 = var0).a(var1);
      var0.a(var0.a((nr)var1.a));
   }

   static void a(cx var0, cu var1) {
      (var0 = var0).a(var1);
      var0.a(var0.a((nr)var1.a));
   }

   static void a(cx var0, qc var1) {
      var0.a((t)var1, (po)var1.a);
   }

   static void b(cx var0, ke var1) {
      (var0 = var0).a(var0.a((nr)var1.a));
   }

   static qd a(cx var0, ju var1) {
      (var0 = var0).a("Unknown variable or type \"" + var1.a + "\"", var1.a());
      return var0.a.a;
   }

   static qd a(cx var0, nx var1) {
      return (var0 = var0).a((nr)var1.a).a(var0.a.a);
   }

   static qd a(cx var0, om var1) {
      switch(var1.a) {
      case 0:
         return qd.a;
      case 1:
         return qd.b;
      case 2:
         return qd.h;
      case 3:
         return qd.c;
      case 4:
         return qd.f;
      case 5:
         return qd.g;
      case 6:
         return qd.e;
      case 7:
         return qd.d;
      case 8:
         return qd.i;
      default:
         throw new RuntimeException("Invalid index " + var1.a);
      }
   }

   static qd a(cx var0, jb var1) {
      return var0.a(var1);
   }

   static qd a(cx var0, kv var1) {
      qd var2 = (var0 = var0).a((nr)var1.a);
      qd var3;
      if ((var3 = var0.a(var2, var1.a, var1.a())) == null) {
         var0.a("\"" + var2 + "\" has no member type \"" + var1.a + "\"", var1.a());
      }

      return var3;
   }

   static qd a(cx var0, hz var1) {
      return var1.a;
   }

   static qd b(cx var0, cl var1) {
      return qd.f;
   }

   static qd b(cx var0, pq var1) {
      return var0.a((nr)var1.a);
   }

   static qd b(cx var0, kh var1) {
      if (var1.a == "!") {
         return qd.i;
      } else if (var1.a != "+" && var1.a != "-" && var1.a != "~") {
         var0.a("Unexpected operator \"" + var1.a + "\"", var1.a());
         return qd.i;
      } else {
         return var0.c((t)var1, var0.a(var0.a((nr)var1.a)));
      }
   }

   static qd b(cx var0, qa var1) {
      var1 = var1;
      var0 = var0;
      if (var1.a != "||" && var1.a != "&&" && var1.a != "==" && var1.a != "!=" && var1.a != "<" && var1.a != ">" && var1.a != "<=" && var1.a != ">=") {
         qd var10000;
         qd var2;
         if (var1.a != "|" && var1.a != "^" && var1.a != "&") {
            if (var1.a == "*" || var1.a == "/" || var1.a == "%" || var1.a == "+" || var1.a == "-") {
               ha var6 = var0.a;
               Enumeration var3 = var1.a();
               qd var4 = var0.a(var0.a((nr)((mu)var3.nextElement())));
               if (var1.a == "+" && var4 == var6.b) {
                  return var6.b;
               }

               do {
                  qd var5 = var0.a(var0.a((nr)((mu)var3.nextElement())));
                  if (var1.a == "+" && var5 == var6.b) {
                     return var6.b;
                  }

                  var4 = var0.a((t)var1, (qd)var4, (qd)var5);
               } while(var3.hasMoreElements());

               return var4;
            }

            if (var1.a == "<<" || var1.a == ">>" || var1.a == ">>>") {
               var2 = var0.a((nr)var1.a);
               return var0.c((t)var1, var2);
            }

            var0.a("Unexpected operator \"" + var1.a + "\"", var1.a());
            var10000 = var0.a.a;
         } else {
            if ((var2 = var0.a((nr)var1.a)) != qd.i && var2 != var0.a.a("Ljava/lang/Boolean;")) {
               return var0.a((t)var1, (qd)var2, (qd)var0.a((nr)var1.b));
            }

            var10000 = qd.i;
         }

         return var10000;
      } else {
         return qd.i;
      }
   }

   static qd b(cx var0, nj var1) {
      return var0.a((nr)var1.a);
   }

   static qd b(cx var0, pb var1) {
      return var0.a.c;
   }

   static qd b(cx var0, da var1) {
      qd var2 = (var0 = var0).a((nr)var1.b);
      qd var3 = var0.a((nr)var1.c);
      if (var2 == var3) {
         return var2;
      } else if (var2.f() && var3.f()) {
         return var0.a((t)var1, (qd)var2, (qd)var3);
      } else if (var0.a(var1.b) == mu.d && !var3.e()) {
         return var3;
      } else if (!var2.e() && var0.a(var1.c) == mu.d) {
         return var2;
      } else if (!var2.e() && !var3.e()) {
         if (var2.a(var3)) {
            return var2;
         } else if (var3.a(var2)) {
            return var3;
         } else {
            var0.a("Reference types \"" + var2 + "\" and \"" + var3 + "\" don't match", var1.a());
            return var0.a.a;
         }
      } else {
         var0.a("Incompatible expression types \"" + var2 + "\" and \"" + var3 + "\"", var1.a());
         return var0.a.a;
      }
   }

   static qd b(cx var0, i var1) {
      qd var2;
      if ((var2 = var1.a instanceof Integer ? qd.f : (var1.a instanceof Long ? qd.g : (var1.a instanceof Float ? qd.e : (var1.a instanceof Double ? qd.d : (var1.a instanceof String ? var0.a.b : (var1.a instanceof Character ? qd.c : (var1.a instanceof Boolean ? qd.i : (var1.a == mu.d ? qd.a : null)))))))) == null) {
         throw new RuntimeException("SNO: Unidentifiable constant value type \"" + var1.a.getClass().getName() + "\"");
      } else {
         return var2;
      }
   }

   static qd b(cx var0, gz var1) {
      return var0.a((nr)var1.a);
   }

   static qd b(cx var0, nl var1) {
      return qd.i;
   }

   static qd b(cx var0, gt var1) {
      if (var1.a == null) {
         var1.a = var0.a(var1);
      }

      return var1.a.a();
   }

   static qd b(cx var0, u var1) {
      return var0.a(var1).a();
   }

   static qd b(cx var0, ne var1) {
      return var0.a(var1);
   }

   static qd b(cx var0, mr var1) {
      return var0.a((dr)var1.a);
   }

   static qd b(cx var0, d var1) {
      return (var0 = var0).a((nr)var1.a).a(var1.a.length + var1.a, var0.a.a);
   }

   static qd b(cx var0, ey var1) {
      return var0.a((nr)var1.a);
   }

   static qd b(cx var0, ok var1) {
      if (var1.a == null) {
         var1.a = var0.a((nr)var1.a);
      }

      return var1.a;
   }

   static qd b(cx var0, lj var1) {
      return var0.a(var1.a).a;
   }

   static qd b(cx var0, py var1) {
      return var0.a(var1);
   }

   static qd b(cx var0, cc var1) {
      return var0.a(var1);
   }

   static qd b(cx var0, gp var1) {
      return (var0 = var0).a(var0.a(var1));
   }

   static qd b(cx var0, kr var1) {
      return var0.a((nr)var1.a).h();
   }

   static qd b(cx var0, gq var1) {
      return var1.a.a();
   }

   static qd b(cx var0, mz var1) {
      (var0 = var0).a(var1);
      return var0.a((nr)var1.a);
   }

   static qd b(cx var0, cu var1) {
      (var0 = var0).a(var1);
      return var0.a((nr)var1.a);
   }

   static qd b(cx var0, qc var1) {
      return var1.a.a;
   }

   static qd b(cx var0, ke var1) {
      return var0.a((nr)var1.a);
   }

   static boolean a(cx var0, nr var1) {
      return var1 instanceof gs;
   }

   static boolean a(cx var0, gp var1) {
      return (var0 = var0).a(var0.a(var1));
   }

   static qd a(cx var0, mu var1) {
      return var0.b(var1);
   }

   static void a(cx var0, t var1, qd var2) {
      var0.a(var1, var2);
   }

   static qd a(cx var0, t var1, Object var2) {
      return var0.a(var1, var2);
   }

   static ha a(cx var0) {
      return var0.a;
   }

   static qd a(cx var0, nr var1) {
      return var0.a(var1);
   }

   static Object a(cx var0, t var1, Object var2, qd var3) {
      Object var4 = null;
      if (var3 == qd.i) {
         if (var2 instanceof Boolean) {
            var4 = var2;
         }
      } else if (var3 == var0.a.b) {
         if (var2 instanceof String) {
            var4 = var2;
         }
      } else {
         int var5;
         char var6;
         if (var3 == qd.b) {
            if (var2 instanceof Byte) {
               var4 = var2;
            } else if (!(var2 instanceof Short) && !(var2 instanceof Integer)) {
               if (var2 instanceof Character && (var6 = (Character)var2) >= -128 && var6 <= 127) {
                  var4 = new Byte((byte)var6);
               }
            } else {
               int var10000 = var2 instanceof Short ? (Short)var2 : (Integer)var2;
               var5 = var10000;
               if (var10000 >= -128 && var5 <= 127) {
                  var4 = new Byte((byte)var5);
               }
            }
         } else if (var3 == qd.h) {
            if (var2 instanceof Byte) {
               var4 = new Short((new Integer((Byte)var2)).shortValue());
            } else if (var2 instanceof Short) {
               var4 = var2;
            } else if (var2 instanceof Character) {
               if ((var6 = (Character)var2) >= -32768 && var6 <= 32767) {
                  var4 = new Short((short)var6);
               }
            } else if (var2 instanceof Integer && (var5 = (Integer)var2) >= -32768 && var5 <= 32767) {
               var4 = new Short((short)var5);
            }
         } else if (var3 == qd.c) {
            if (var2 instanceof Character) {
               var4 = var2;
            } else if ((var2 instanceof Byte || var2 instanceof Short || var2 instanceof Integer) && (var5 = var2 instanceof Byte ? (Byte)var2 : (var2 instanceof Short ? (Short)var2 : (Integer)var2)) >= 0 && var5 <= 65535) {
               var4 = new Character((char)var5);
            }
         } else if (var3 == qd.f) {
            if (var2 instanceof Integer) {
               var4 = var2;
            } else if (!(var2 instanceof Byte) && !(var2 instanceof Short)) {
               if (var2 instanceof Character) {
                  var4 = new Integer((Character)var2);
               }
            } else {
               var4 = var2 instanceof Byte ? new Integer((Byte)var2) : new Integer((Short)var2);
            }
         } else if (var3 == qd.g) {
            if (var2 instanceof Long) {
               var4 = var2;
            } else if (!(var2 instanceof Byte) && !(var2 instanceof Short) && !(var2 instanceof Integer)) {
               if (var2 instanceof Character) {
                  var4 = new Long((long)(Character)var2);
               }
            } else {
               var4 = new Long(var2 instanceof Byte ? (long)(Byte)var2 : (var2 instanceof Short ? (long)(Short)var2 : (long)(Integer)var2));
            }
         } else if (var3 == qd.e) {
            if (var2 instanceof Float) {
               var4 = var2;
            } else if (!(var2 instanceof Byte) && !(var2 instanceof Short) && !(var2 instanceof Integer) && !(var2 instanceof Long)) {
               if (var2 instanceof Character) {
                  var4 = new Float((float)(Character)var2);
               }
            } else if (var2 instanceof Byte) {
               var4 = new Float((new Integer((Byte)var2)).floatValue());
            } else if (var2 instanceof Short) {
               var4 = new Float((new Integer((Short)var2)).floatValue());
            } else if (var2 instanceof Integer) {
               var4 = new Float(((Integer)var2).floatValue());
            } else {
               var4 = new Float(((Long)var2).floatValue());
            }
         } else if (var3 == qd.d) {
            if (var2 instanceof Double) {
               var4 = var2;
            } else if (!(var2 instanceof Byte) && !(var2 instanceof Short) && !(var2 instanceof Integer) && !(var2 instanceof Long) && !(var2 instanceof Float)) {
               if (var2 instanceof Character) {
                  var4 = new Double((double)(Character)var2);
               }
            } else if (var2 instanceof Byte) {
               var4 = new Double((new Integer((Byte)var2)).doubleValue());
            } else if (var2 instanceof Short) {
               var4 = new Double((new Integer((Short)var2)).doubleValue());
            } else if (var2 instanceof Integer) {
               var4 = new Double(((Integer)var2).doubleValue());
            } else if (var2 instanceof Long) {
               var4 = new Double(((Long)var2).doubleValue());
            } else {
               var4 = new Double(((Float)var2).doubleValue());
            }
         } else if (var2 == mu.d && !var3.e()) {
            var4 = var2;
         }
      }

      if (var4 == null) {
         var0.a("Cannot convert constant of type \"" + var2.getClass().getName() + "\" to type \"" + var3.toString() + "\"", var1.a());
      }

      return var4;
   }

   static qd a(cx var0, dr var1) {
      return var0.a(var1);
   }

   static void a(cx var0, String var1, aq var2) {
      var0.a(var1, var2);
   }

   static pe a(short var0) {
      if ((var0 & 1) != 0) {
         return pe.d;
      } else if ((var0 & 4) != 0) {
         return pe.b;
      } else {
         return (var0 & 2) != 0 ? pe.a : pe.c;
      }
   }

   static qd a(cx var0, bi var1) {
      return var0.a(var1);
   }

   static {
      a(new Object[]{new byte[0], "BS", "BI", "SI", "CI", new byte[]{-123}, "BJ", "SJ", "CJ", "IJ", new byte[]{-122}, "BF", "SF", "CF", "IF", new byte[]{-119}, "JF", new byte[]{-121}, "BD", "SD", "CD", "ID", new byte[]{-118}, "JD", new byte[]{-115}, "FD"}, b);
      c = new Hashtable();
      a(new Object[]{new byte[0], "BC", "SC", "CS", new byte[]{-111}, "SB", "CB", "IB", new byte[]{-109}, "IS", "IC", new byte[]{-120, -111}, "JB", new byte[]{-120, -109}, "JS", "JC", new byte[]{-120}, "JI", new byte[]{-117, -111}, "FB", new byte[]{-117, -109}, "FS", "FC", new byte[]{-117}, "FI", new byte[]{-116}, "FJ", new byte[]{-114, -111}, "DB", new byte[]{-114, -109}, "DS", "DC", new byte[]{-114}, "DI", new byte[]{-113}, "DJ", new byte[]{-112}, "DF"}, c);
   }
}
