public final class ae extends hh {
   public int a;

   public ae() {
   }

   public ae(byte var1, int var2) {
      this.a = var1;
      this.a = var2;
   }

   public final hh a() {
      if (this.b() == 2) {
         if (this.a == -56) {
            this.a = -89;
         } else if (this.a == -55) {
            this.a = -88;
         }
      } else if (this.a == -89) {
         this.a = -56;
      } else {
         if (this.a != -88) {
            throw new IllegalArgumentException("Branch instruction can't be widened (" + this.toString() + ")");
         }

         this.a = -55;
      }

      return this;
   }

   protected final void a(byte[] var1, int var2) {
      this.a = b(var1, var2, this.a());
   }

   protected final void b(byte[] var1, int var2) {
      if (this.b() > this.a()) {
         throw new IllegalArgumentException("Instruction has invalid branch offset size (" + this.a(var2) + ")");
      } else {
         b(var1, var2, this.a, this.a());
      }
   }

   public final int a(int var1) {
      return 1 + this.a();
   }

   public final void a(aj var1, kn var2, ac var3, int var4, ff var5) {
      var5.a(var1, var2, var3, var4, this);
   }

   public final String a(int var1) {
      return "[" + var1 + "] " + this.toString() + " (target=" + (var1 + this.a) + ")";
   }

   public final String toString() {
      return this.a() + " " + (this.a >= 0 ? "+" : "") + this.a;
   }

   private int a() {
      return this.a != -56 && this.a != -55 ? 2 : 4;
   }

   private int b() {
      return (short)this.a == this.a ? 2 : 4;
   }
}
