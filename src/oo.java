final class oo extends ip {
   private final gx a;
   private final gx b;
   private final fg a;

   public oo(fg var1, gx var2, gx var3) {
      super(var1, (ea)null);
      this.a = var1;
      this.a = var2;
      this.b = var3;
   }

   public final void a() {
      if (this.b.a == -1) {
         throw new RuntimeException("Cannot relocate branch to unset destination offset");
      } else {
         int var1;
         if ((var1 = this.b.a - this.a.a) <= 32767 && var1 >= -32768) {
            System.arraycopy(new byte[]{(byte)(var1 >> 8), (byte)var1}, 0, fg.a(this.a), ('\uffff' & this.a.a) + 1, 2);
         } else {
            throw new RuntimeException("Branch offset out of range");
         }
      }
   }
}
