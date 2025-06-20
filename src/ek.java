import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;

public final class ek extends mm implements ad, dg, fs, jy {
   private ns a;
   private final km a = new km(this, (kq)null);
   private final pr a = new pr(this, (kq)null);
   private final cj a = new cj(this, (kq)null);
   private final ar a = new ar(this, (kq)null);

   public ek(DataOutput var1) {
      new bd(this, (kq)null);
      this.a = new ns(var1);
   }

   public final void a_(aj var1) {
      this.a.b(var1.a);
      this.a.c(ec.b(var1.b));
      this.a.c(ec.a(var1.b));
      this.a.c(var1.c);
      var1.a((fs)this);
      this.a.c(var1.d);
      this.a.c(var1.e);
      this.a.c(var1.f);
      this.a.c(var1.g);

      for(int var2 = 0; var2 < var1.g; ++var2) {
         this.a.c(var1.a[var2]);
      }

      this.a.c(var1.h);
      var1.a((ad)this);
      this.a.c(var1.i);
      var1.b(this);
      this.a.c(var1.j);
      var1.a((jy)this);
   }

   public final void a(aj var1, dv var2) {
      this.a.c(var2.a);
      this.a.c(var2.b);
      this.a.c(var2.c);
      this.a.c(var2.d);
      var2.a(var1, (jy)this);
   }

   public final void a(aj var1, kn var2) {
      this.a.c(var2.a);
      this.a.c(var2.b);
      this.a.c(var2.c);
      this.a.c(var2.d);
      var2.a(var1, (jy)this);
   }

   public final void a(aj var1, r var2) {
      this.a.a(var2.a());
      var2.a(var1, this.a);
   }

   public final void a(aj var1, dp var2) {
      this.a.c(var2.f);
      ByteArrayOutputStream var3 = new ByteArrayOutputStream();
      ns var4 = this.a;
      this.a = new ns(new DataOutputStream(var3));
      var2.a(var1, (kn)null, (ac)null, this.a);
      this.a = var4;
      byte[] var5 = var3.toByteArray();
      this.a.b(var5.length);
      this.a.a(var5);
   }

   static ns a(ek var0) {
      return var0.a;
   }

   static cj a(ek var0) {
      return var0.a;
   }

   static ar a(ek var0) {
      return var0.a;
   }
}
