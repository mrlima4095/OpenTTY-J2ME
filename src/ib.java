import java.io.DataInputStream;
import java.io.DataOutputStream;

public abstract class ib {
   public abstract void a(DataOutputStream var1);

   public abstract boolean a();

   static ib a(DataInputStream var0) {
      byte var1;
      switch(var1 = var0.readByte()) {
      case 1:
         return new cg(var0.readUTF());
      case 2:
      default:
         throw new NoClassDefFoundError("Invalid constant pool tag " + var1);
      case 3:
         return new ii(var0.readInt());
      case 4:
         return new ag(var0.readFloat());
      case 5:
         return new dc(var0.readLong());
      case 6:
         return new iz(var0.readDouble());
      case 7:
         return new mv(var0.readShort());
      case 8:
         return new ef(var0.readShort());
      case 9:
         return new ou(var0.readShort(), var0.readShort());
      case 10:
         return new ao(var0.readShort(), var0.readShort());
      case 11:
         return new md(var0.readShort(), var0.readShort());
      case 12:
         return new li(var0.readShort(), var0.readShort());
      }
   }
}
