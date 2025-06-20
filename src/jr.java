import java.io.DataInput;

public final class jr extends mm implements ad, dg, dq, ee, fs, fw, ir, jn, jy, mp, nn {
   private final cq a;

   public jr(DataInput var1) {
      this.a = new cq(var1);
   }

   public final void a_(aj var1) {
      var1.a = this.a.a();
      int var2 = this.a.c();
      int var3 = this.a.c();
      var1.b = ec.a(var3, var2);
      var1.c = this.a.c();
      var1.a = new r[var1.c];

      for(var2 = 1; var2 < var1.c; ++var2) {
         Object var10000;
         switch(var3 = this.a.b()) {
         case 1:
            var10000 = new ci();
            break;
         case 2:
         default:
            throw new RuntimeException("Unknown constant type [" + var3 + "] in constant pool");
         case 3:
            var10000 = new oq();
            break;
         case 4:
            var10000 = new fn();
            break;
         case 5:
            var10000 = new fm();
            break;
         case 6:
            var10000 = new gc();
            break;
         case 7:
            var10000 = new bc();
            break;
         case 8:
            var10000 = new e();
            break;
         case 9:
            var10000 = new f();
            break;
         case 10:
            var10000 = new jp();
            break;
         case 11:
            var10000 = new db();
            break;
         case 12:
            var10000 = new hw();
         }

         Object var4 = var10000;
         ((r)var10000).a(var1, this);
         var1.a[var2] = (r)var4;
         if ((var3 = ((r)var4).a()) == 5 || var3 == 6) {
            ++var2;
            var1.a[var2] = null;
         }
      }

      var1.d = this.a.c();
      var1.e = this.a.c();
      var1.f = this.a.c();
      var1.g = this.a.c();
      var1.a = new int[var1.g];

      for(var2 = 0; var2 < var1.g; ++var2) {
         var1.a[var2] = this.a.c();
      }

      var1.h = this.a.c();
      var1.a = new dv[var1.h];

      for(var2 = 0; var2 < var1.h; ++var2) {
         dv var5 = new dv();
         this.a(var1, var5);
         var1.a[var2] = var5;
      }

      var1.i = this.a.c();
      var1.a = new kn[var1.i];

      for(var2 = 0; var2 < var1.i; ++var2) {
         kn var6 = new kn();
         this.a(var1, var6);
         var1.a[var2] = var6;
      }

      var1.j = this.a.c();
      var1.a = new dp[var1.j];

      for(var2 = 0; var2 < var1.j; ++var2) {
         dp var7;
         (var7 = this.a(var1)).a(var1, this);
         var1.a[var2] = var7;
      }

   }

   public final void a(aj var1, dv var2) {
      var2.a = this.a.c();
      var2.b = this.a.c();
      var2.c = this.a.c();
      var2.d = this.a.c();
      var2.a = new dp[var2.d];

      for(int var3 = 0; var3 < var2.d; ++var3) {
         dp var4;
         (var4 = this.a(var1)).a(var1, (dv)var2, this);
         var2.a[var3] = var4;
      }

   }

   public final void a(aj var1, kn var2) {
      var2.a = this.a.c();
      var2.b = this.a.c();
      var2.c = this.a.c();
      var2.d = this.a.c();
      var2.a = new dp[var2.d];

      for(int var3 = 0; var3 < var2.d; ++var3) {
         dp var4;
         (var4 = this.a(var1)).a(var1, (kn)var2, this);
         var2.a[var3] = var4;
      }

   }

   public final void a(aj var1, oq var2) {
      var2.a = this.a.a();
   }

   public final void a(aj var1, fm var2) {
      var2.a = this.a.a();
   }

   public final void a(aj var1, fn var2) {
      var2.a = this.a.a();
   }

   public final void a(aj var1, gc var2) {
      var2.a = this.a.a();
   }

   public final void a(aj var1, e var2) {
      var2.a = this.a.c();
   }

   public final void a(aj var1, ci var2) {
      byte[] var3 = new byte[this.a.c()];
      this.a.a(var3);
      var2.a(var3);
   }

   public final void b(aj var1, kc var2) {
      var2.a = this.a.c();
      var2.b = this.a.c();
   }

   public final void a(aj var1, bc var2) {
      var2.a = this.a.c();
   }

   public final void a(aj var1, hw var2) {
      var2.a = this.a.c();
      var2.b = this.a.c();
   }

   public final void a(aj var1, ge var2) {
      byte[] var3 = new byte[var2.a];
      this.a.a(var3);
   }

   public final void a(aj var1, ob var2) {
      var2.a = this.a.c();
   }

   public final void a(aj var1, fq var2) {
      var2.a = this.a.c();
   }

   public final void a(aj var1, cz var2) {
      var2.a = this.a.c();
      var2.a = new nk[var2.a];

      for(int var4 = 0; var4 < var2.a; ++var4) {
         nk var3 = new nk();
         this.a(var3);
         var2.a[var4] = var3;
      }

   }

   public final void a(aj var1, jz var2) {
      var2.a = this.a.c();
      var2.b = this.a.c();
   }

   public final void a(aj var1, v var2) {
   }

   public final void a(aj var1, pp var2) {
   }

   public final void a(aj var1, ja var2) {
      var2.a = this.a.c();
   }

   public final void a(aj var1, ku var2) {
      var2.a = this.a.c();
   }

   public final void a(aj var1, bg var2) {
      var2.a = this.a.c();
      var2.a = new int[var2.a];

      for(int var3 = 0; var3 < var2.a; ++var3) {
         var2.a[var3] = this.a.c();
      }

   }

   public final void a(aj var1, kn var2, ac var3) {
      var3.a = this.a.c();
      var3.b = this.a.c();
      var3.c = this.a.a();
      byte[] var4 = new byte[var3.c];
      this.a.a(var4);
      var3.a = var4;
      var3.d = this.a.c();
      var3.a = new m[var3.d];

      int var6;
      for(var6 = 0; var6 < var3.d; ++var6) {
         m var5 = new m();
         this.a(var1, var2, var3, var5);
         var3.a[var6] = var5;
      }

      var3.e = this.a.c();
      var3.a = new dp[var3.e];

      for(var6 = 0; var6 < var3.e; ++var6) {
         dp var7;
         (var7 = this.a(var1)).a(var1, var2, var3, this);
         var3.a[var6] = var7;
      }

   }

   public final void a(aj var1, kn var2, ac var3, hg var4) {
      var4.a = this.a.c();
      var4.a = new jo[var4.a];

      for(int var5 = 0; var5 < var4.a; ++var5) {
         jo var6 = new jo();
         this.a(var1, var2, var3, var5, var6);
         var4.a[var5] = var6;
      }

   }

   public final void a(aj var1, kn var2, ac var3, mk var4) {
      var4.a = this.a.c();
      var4.a = new mb[var4.a];

      for(int var5 = 0; var5 < var4.a; ++var5) {
         int var6;
         Object var7;
         ((mb)(var7 = (var6 = this.a.b()) < 64 ? new op(var6) : (var6 < 247 ? new ap(var6) : (var6 < 248 ? new ap(var6) : (var6 < 251 ? new lp(var6) : (var6 < 252 ? new op(var6) : (var6 < 255 ? new mf(var6) : new jo()))))))).a(var1, var2, var3, 0, this);
         var4.a[var5] = (mb)var7;
      }

   }

   public final void a(aj var1, kn var2, ac var3, as var4) {
      var4.a = this.a.c();
      var4.a = new ol[var4.a];

      for(int var5 = 0; var5 < var4.a; ++var5) {
         ol var6 = new ol();
         this.a(var6);
         var4.a[var5] = var6;
      }

   }

   public final void a(aj var1, kn var2, ac var3, pt var4) {
      var4.a = this.a.c();
      var4.a = new ak[var4.a];

      for(int var5 = 0; var5 < var4.a; ++var5) {
         ak var6 = new ak();
         this.a(var6);
         var4.a[var5] = var6;
      }

   }

   public final void a(aj var1, kn var2, ac var3, hy var4) {
      var4.a = this.a.c();
      var4.a = new hk[var4.a];

      for(int var5 = 0; var5 < var4.a; ++var5) {
         hk var6 = new hk();
         this.a(var6);
         var4.a[var5] = var6;
      }

   }

   public final void a(nk var1) {
      var1.a = this.a.c();
      var1.b = this.a.c();
      var1.c = this.a.c();
      var1.d = this.a.c();
   }

   public final void a(aj var1, kn var2, ac var3, m var4) {
      var4.a = this.a.c();
      var4.b = this.a.c();
      var4.c = this.a.c();
      var4.d = this.a.c();
   }

   public final void a(aj var1, kn var2, ac var3, int var4, op var5) {
      if (var5.a() == 251) {
         var5.c = this.a.c();
      }

   }

   public final void a(aj var1, kn var2, ac var3, int var4, ap var5) {
      if (var5.a() == 247) {
         var5.c = this.a.c();
      }

      ji var6;
      (var6 = this.a()).a(var1, var2, var3, var4, this);
      var5.a = var6;
   }

   public final void a(aj var1, kn var2, ac var3, int var4, lp var5) {
      var5.c = this.a.c();
   }

   public final void a(aj var1, kn var2, ac var3, int var4, mf var5) {
      var5.c = this.a.c();
      var5.a = new ji[var5.a];

      for(int var6 = 0; var6 < var5.a; ++var6) {
         ji var7;
         (var7 = this.a()).a(var1, var2, var3, var4, this);
         var5.a[var6] = var7;
      }

   }

   public final void a(aj var1, kn var2, ac var3, int var4, jo var5) {
      var5.c = this.a.c();
      var5.a = this.a.c();
      var5.a = new ji[var5.a];

      int var6;
      ji var7;
      for(var6 = 0; var6 < var5.a; ++var6) {
         (var7 = this.a()).b(var1, var2, var3, var4, var6, this);
         var5.a[var6] = var7;
      }

      var5.b = this.a.c();
      var5.b = new ji[var5.b];

      for(var6 = 0; var6 < var5.b; ++var6) {
         (var7 = this.a()).a(var1, var2, var3, var4, var6, this);
         var5.b[var6] = var7;
      }

   }

   public final void a(aj var1, kn var2, ac var3, int var4, ji var5) {
   }

   public final void a(aj var1, kn var2, ac var3, int var4, cf var5) {
      var5.a = this.a.c();
   }

   public final void a(aj var1, kn var2, ac var3, int var4, js var5) {
      var5.a = this.a.c();
   }

   public final void a(ol var1) {
      var1.a = this.a.c();
      var1.b = this.a.c();
   }

   public final void a(ak var1) {
      var1.a = this.a.c();
      var1.b = this.a.c();
      var1.c = this.a.c();
      var1.d = this.a.c();
      var1.e = this.a.c();
   }

   public final void a(hk var1) {
      var1.a = this.a.c();
      var1.b = this.a.c();
      var1.c = this.a.c();
      var1.d = this.a.c();
      var1.e = this.a.c();
   }

   private dp a(aj var1) {
      int var2 = this.a.c();
      int var3 = this.a.a();
      Object var4;
      String var5;
      ((dp)(var4 = (var5 = var1.a(var2)).equals("SourceFile") ? new ob() : (var5.equals("SourceDir") ? new fq() : (var5.equals("InnerClasses") ? new cz() : (var5.equals("EnclosingMethod") ? new jz() : (var5.equals("Deprecated") ? new v() : (var5.equals("Synthetic") ? new pp() : (var5.equals("Signature") ? new ja() : (var5.equals("ConstantValue") ? new ku() : (var5.equals("Exceptions") ? new bg() : (var5.equals("Code") ? new ac() : (var5.equals("StackMap") ? new hg() : (var5.equals("StackMapTable") ? new mk() : (var5.equals("LineNumberTable") ? new as() : (var5.equals("LocalVariableTable") ? new pt() : (var5.equals("LocalVariableTypeTable") ? new hy() : new ge(var3))))))))))))))))).f = var2;
      return (dp)var4;
   }

   private ji a() {
      int var1;
      switch(var1 = this.a.b()) {
      case 0:
         return new jc();
      case 1:
         return new jx();
      case 2:
         return new ot();
      case 3:
         return new ch();
      case 4:
         return new k();
      case 5:
         return new mh();
      case 6:
         return new fo();
      case 7:
         return new cf();
      case 8:
         return new js();
      default:
         throw new RuntimeException("Unknown verification type [" + var1 + "] in stack map frame");
      }
   }
}
