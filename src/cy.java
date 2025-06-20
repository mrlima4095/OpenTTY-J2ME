public final class cy {
   private String a;
   private int a;

   public cy(String var1) {
      this.a(var1);
   }

   public cy() {
   }

   final void a(String var1) {
      this.a = var1;
      this.a = this.a.indexOf(40) + 1;
      if (this.a < 1) {
         throw new IllegalArgumentException("Missing opening parenthesis in descriptor [" + this.a + "]");
      }
   }

   public final boolean a() {
      return this.a.charAt(this.a) != ')';
   }

   public final String a() {
      int var1;
      for(var1 = this.a; this.a.charAt(this.a) == '['; ++this.a) {
      }

      if (this.a.charAt(this.a) == 'L') {
         this.a = this.a.indexOf(59, this.a + 1);
         if (this.a < 0) {
            throw new IllegalArgumentException("Missing closing class type in descriptor [" + this.a + "]");
         }
      }

      return this.a.substring(var1, ++this.a);
   }
}
