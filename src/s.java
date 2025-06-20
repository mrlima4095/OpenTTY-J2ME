final class s extends aa {
   private it a;

   public final void a(it var1) {
      s var3;
      it var2 = (var3 = this.a()).a;
      var3.a = var2 == null ? var1 : var1.a(var2);
   }

   public final it a(it var1) {
      s var5 = (s)var1;
      s var4 = this.a();
      var5 = var5.a();
      it var2 = var4.a;
      it var3 = var5.a;
      if (var4 != var5) {
         var5.a = var4;
      }

      var4.a = var2 == null ? var3 : (var3 == null ? var2 : var2.a(var3));
      return var4;
   }

   public final int a() {
      return 0;
   }

   public final boolean equals(Object var1) {
      if (var1 != null && this.getClass() == var1.getClass()) {
         s var4 = (s)var1;
         it var3 = this.a().a;
         it var5 = var4.a().a;
         if (var3 == null) {
            return var5 == null;
         } else {
            return var3.equals(var5);
         }
      } else {
         return false;
      }
   }

   public final int hashCode() {
      it var1 = this.a().a;
      return this.getClass().hashCode() ^ (var1 == null ? 0 : var1.hashCode());
   }

   public final String toString() {
      return this.a == null ? "none" : this.a.toString();
   }

   private s a() {
      s var1;
      for(var1 = this; var1.a instanceof s; var1 = (s)var1.a) {
      }

      return var1;
   }
}
