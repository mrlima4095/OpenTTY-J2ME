public final class m {
   public int a;
   public int b;
   public int c;
   public int d;

   public m() {
      this(0, 0, 0, 0);
   }

   public m(int var1, int var2, int var3, int var4) {
      this.a = var1;
      this.b = var2;
      this.c = var3;
      this.d = var4;
   }

   public final boolean a(int var1) {
      return var1 >= this.a && var1 < this.b;
   }
}
