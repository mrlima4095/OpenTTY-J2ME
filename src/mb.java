public abstract class mb {
   public int c;

   public abstract int a();

   public abstract void a(aj var1, kn var2, ac var3, int var4, ee var5);

   public boolean equals(Object var1) {
      if (var1 != null && this.getClass() == var1.getClass()) {
         mb var2 = (mb)var1;
         return this.c == var2.c;
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.getClass().hashCode() ^ this.c;
   }

   public String toString() {
      return "[" + this.c + "] ";
   }
}
