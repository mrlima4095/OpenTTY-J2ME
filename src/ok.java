public final class ok extends mu {
   public final mu a;
   public final gs a;
   public final mu[] a;
   protected qd a = null;

   public ok(aq var1, mu var2, gs var3, mu[] var4) {
      super(var1);
      this.a = var2;
      this.a = var3;
      this.a = var4;
   }

   public final String toString() {
      StringBuffer var1 = new StringBuffer();
      if (this.a != null) {
         var1.append(this.a.toString()).append('.');
      }

      var1.append("new ");
      if (this.a != null) {
         var1.append(this.a.toString());
      } else if (this.a != null) {
         var1.append(this.a.toString());
      } else {
         var1.append("???");
      }

      var1.append('(');

      for(int var2 = 0; var2 < this.a.length; ++var2) {
         if (var2 > 0) {
            var1.append(", ");
         }

         var1.append(this.a[var2].toString());
      }

      var1.append(')');
      return var1.toString();
   }

   public final void a(gm var1) {
      var1.a(this);
   }

   public final void a(bn var1) {
      var1.a(this);
   }
}
