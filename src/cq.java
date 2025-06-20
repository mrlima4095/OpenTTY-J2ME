import java.io.DataInput;
import java.io.IOException;

final class cq {
   private final DataInput a;

   public cq(DataInput var1) {
      this.a = var1;
   }

   public final double a() {
      try {
         return this.a.readDouble();
      } catch (IOException var1) {
         throw new RuntimeException(var1.getMessage());
      }
   }

   public final float a() {
      try {
         return this.a.readFloat();
      } catch (IOException var1) {
         throw new RuntimeException(var1.getMessage());
      }
   }

   public final void a(byte[] var1) {
      try {
         this.a.readFully(var1);
      } catch (IOException var2) {
         throw new RuntimeException(var2.getMessage());
      }
   }

   public final int a() {
      try {
         return this.a.readInt();
      } catch (IOException var1) {
         throw new RuntimeException(var1.getMessage());
      }
   }

   public final long a() {
      try {
         return this.a.readLong();
      } catch (IOException var1) {
         throw new RuntimeException(var1.getMessage());
      }
   }

   public final int b() {
      try {
         return this.a.readUnsignedByte();
      } catch (IOException var1) {
         throw new RuntimeException(var1.getMessage());
      }
   }

   public final int c() {
      try {
         return this.a.readUnsignedShort();
      } catch (IOException var1) {
         throw new RuntimeException(var1.getMessage());
      }
   }
}
