import java.io.DataOutput;
import java.io.IOException;

final class ns {
   private final DataOutput a;

   public ns(DataOutput var1) {
      this.a = var1;
   }

   public final void a(byte[] var1) {
      try {
         this.a.write(var1);
      } catch (IOException var2) {
         throw new RuntimeException(var2.getMessage());
      }
   }

   public final void a(byte[] var1, int var2, int var3) {
      try {
         this.a.write(var1, 0, var3);
      } catch (IOException var4) {
         throw new RuntimeException(var4.getMessage());
      }
   }

   public final void a(int var1) {
      try {
         this.a.writeByte(var1);
      } catch (IOException var2) {
         throw new RuntimeException(var2.getMessage());
      }
   }

   public final void a(double var1) {
      try {
         this.a.writeDouble(var1);
      } catch (IOException var3) {
         throw new RuntimeException(var3.getMessage());
      }
   }

   public final void a(float var1) {
      try {
         this.a.writeFloat(var1);
      } catch (IOException var2) {
         throw new RuntimeException(var2.getMessage());
      }
   }

   public final void b(int var1) {
      try {
         this.a.writeInt(var1);
      } catch (IOException var2) {
         throw new RuntimeException(var2.getMessage());
      }
   }

   public final void a(long var1) {
      try {
         this.a.writeLong(var1);
      } catch (IOException var3) {
         throw new RuntimeException(var3.getMessage());
      }
   }

   public final void c(int var1) {
      try {
         this.a.writeShort(var1);
      } catch (IOException var2) {
         throw new RuntimeException(var2.getMessage());
      }
   }
}
