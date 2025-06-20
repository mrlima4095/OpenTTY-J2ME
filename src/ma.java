final class ma extends ip {
   private final gx a;
   private final gx b;
   private final gx c;
   private final fg a;

   public ma(fg var1, gx var2, gx var3, gx var4) {
      super(var1, (ea)null);
      this.a = var1;
      this.a = var2;
      this.b = var3;
      this.c = var4;
   }

   public final void a() {
      if (this.b.a != -1 && this.c.a != -1) {
         int var1 = this.c.a - this.b.a;
         System.arraycopy(new byte[]{(byte)(var1 >> 24), (byte)(var1 >> 16), (byte)(var1 >> 8), (byte)var1}, 0, fg.a(this.a), '\uffff' & this.a.a, 4);
      } else {
         throw new RuntimeException("Cannot relocate offset branch to unset destination offset");
      }
   }
}
