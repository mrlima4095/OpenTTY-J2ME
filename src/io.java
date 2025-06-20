public final class io extends fl {
   private it a;
   private it b;
   private final fl a;
   private final fl b;
   private int b;

   public io(int var1) {
      super(var1);
      this.a = new fl(var1);
      this.b = new fl(var1);
   }

   public io(io var1) {
      super(var1);
      this.a = new fl(var1.a);
      this.b = new fl(var1.b);
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

   public final void a() {
      this.b = -1;
   }

   public final int b() {
      return this.b;
   }

   public final it c(int var1) {
      return this.a.a(var1);
   }

   public final void b(int var1, it var2) {
      this.a.a(var1, var2);
   }

   public final void a(int var1) {
      super.a(var1);
      this.a.a(var1);
      this.b.a(var1);
   }

   public final void a(io var1) {
      super.a(var1);
      this.a.a(var1.a);
      this.b.a(var1.b);
   }

   public final boolean a(io var1, boolean var2) {
      boolean var3 = super.a(var1, var2);
      boolean var4 = this.a.a(var1.a, var2);
      if (var3) {
         for(int var5 = 0; var5 < this.a; ++var5) {
            if (this.a[var5] == null) {
               this.a.a[var5] = null;
               this.b.a[var5] = null;
               if (var2) {
                  var1.a.a[var5] = null;
                  var1.b.a[var5] = null;
               }
            }
         }
      }

      return var3 || var4;
   }

   public final void a(int var1, it var2) {
      it var3;
      if ((var3 = super.b(var1)) == null || var3.a() != var2.a()) {
         this.b = var1;
      }

      super.a(var1, var2);
      this.a.a(var1, this.a);
      s var4 = new s();
      this.b.a(var1, var4);
      if (var2.a()) {
         this.a.a(var1 + 1, this.a);
         this.b.a(var1 + 1, var4);
      }

   }

   public final it b(int var1) {
      if (this.b != null) {
         this.b = this.b.a(this.a.b(var1));
      }

      ((s)this.b.b(var1)).a(this.a);
      return super.b(var1);
   }

   public final boolean equals(Object var1) {
      if (var1 != null && this.getClass() == var1.getClass()) {
         io var2 = (io)var1;
         return super.equals(var1) && this.a.equals(var2.a);
      } else {
         return false;
      }
   }

   public final int hashCode() {
      return super.hashCode() ^ this.a.hashCode();
   }

   public final String toString() {
      StringBuffer var1 = new StringBuffer();

      for(int var2 = 0; var2 < this.a(); ++var2) {
         it var3 = this.a[var2];
         it var4 = this.a.a(var2);
         it var5 = this.b.a(var2);
         var1 = var1.append('[').append(var4 == null ? "empty" : var4.toString()).append('>').append(var3 == null ? "empty" : var3.toString()).append('>').append(var5 == null ? "empty" : var5.toString()).append(']');
      }

      return var1.toString();
   }
}
