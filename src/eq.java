public final class eq extends lk {
   private it a;
   private it b;
   private final lk a;
   private final lk b;

   public eq(int var1) {
      super(var1);
      this.a = new lk(var1);
      this.b = new lk(var1);
   }

   public eq(eq var1) {
      super(var1);
      this.a = new lk(var1.a);
      this.b = new lk(var1.b);
   }

   public final void a(it var1) {
      this.a = var1;
   }

   public final void b(it var1) {
      this.b = var1;
   }

   public final it a() {
      return this.b;
   }

   public final it a(int var1) {
      return this.a.c(var1);
   }

   public final void a(int var1) {
      super.a(var1);
      this.a.a(var1);
      this.b.a(var1);
   }

   public final void a(eq var1) {
      super.a(var1);
      this.a.a(var1.a);
      this.b.a(var1.b);
   }

   public final boolean a(eq var1) {
      return super.a(var1) | this.a.a(var1.a) | this.b.a(var1.b);
   }

   public final void a() {
      super.a();
      this.a.a();
      this.b.a();
   }

   public final void c(it var1) {
      super.c(var1);
      this.k();
      if (var1.a()) {
         this.k();
      }

   }

   public final it b() {
      it var1 = super.b();
      this.m();
      if (var1.a()) {
         this.m();
      }

      return var1;
   }

   public final void b() {
      super.b();
      this.m();
   }

   public final void c() {
      super.c();
      this.m();
      this.m();
   }

   public final void d() {
      super.d();
      this.b(0);
      this.a.d();
      this.n();
      this.l();
      this.l();
   }

   public final void e() {
      super.e();
      this.b(0);
      this.a.e();
      this.n();
      this.l();
      this.b.j();
      this.l();
   }

   public final void f() {
      super.f();
      this.b(0);
      this.a.f();
      this.n();
      this.l();
      this.b.f();
      this.b.b();
      this.l();
   }

   public final void g() {
      super.g();
      this.b(0);
      this.b(1);
      this.a.g();
      this.n();
      this.n();
      this.l();
      this.l();
      this.l();
      this.l();
   }

   public final void h() {
      super.h();
      this.b(0);
      this.b(1);
      this.a.h();
      this.n();
      this.n();
      this.l();
      this.l();
      this.b.h();
      this.b.c();
      this.l();
      this.l();
   }

   public final void i() {
      super.i();
      this.b(0);
      this.b(1);
      this.a.i();
      this.n();
      this.n();
      this.l();
      this.l();
      this.b.i();
      this.b.c();
      this.l();
      this.l();
   }

   public final void j() {
      super.j();
      this.b(0);
      this.b(1);
      this.a.j();
      this.n();
      this.n();
      this.l();
      this.l();
   }

   public final boolean equals(Object var1) {
      if (var1 != null && this.getClass() == var1.getClass()) {
         eq var2 = (eq)var1;
         return super.equals(var1) && this.a.equals(var2.a) && this.b.equals(var2.b);
      } else {
         return false;
      }
   }

   public final int hashCode() {
      return super.hashCode() ^ this.a.hashCode() ^ this.b.hashCode();
   }

   public final String toString() {
      StringBuffer var1 = new StringBuffer();

      for(int var2 = 0; var2 < this.a(); ++var2) {
         it var3 = this.a[var2];
         it var4 = this.a.b(var2);
         it var5 = this.b.b(var2);
         var1 = var1.append('[').append(var4 == null ? "empty" : var4.toString()).append('>').append(var3 == null ? "empty" : var3.toString()).append('>').append(var5 == null ? "empty" : var5.toString()).append(']');
      }

      return var1.toString();
   }

   private void k() {
      this.a.c(this.a);
      this.l();
   }

   private void l() {
      this.b.c(new s());
   }

   private void m() {
      it var2 = this.a.b();
      this.d(var2);
      this.n();
   }

   private void n() {
      ((s)this.b.b()).a(this.a);
   }

   private void b(int var1) {
      this.d(this.a.c(var1));
   }

   private void d(it var1) {
      if (this.b != null) {
         this.b = this.b.a(var1);
      }

   }
}
