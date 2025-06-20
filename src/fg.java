import java.io.DataOutputStream;
import java.util.Enumeration;
import java.util.Stack;
import java.util.Vector;

public final class fg {
   private nb a;
   private short a;
   private short b;
   private byte[] a;
   private gx a;
   private ex a;
   private ex b;
   private Vector a;
   private short c = 0;
   private final Stack a = new Stack();
   private final Vector b = new Vector();

   public fg(nb var1) {
      this.a = var1;
      this.a = 0;
      this.b = 0;
      this.a = new byte[100];
      this.a = new gx(this);
      this.a = new ex(this);
      this.b = this.a;
      this.a = new Vector();
      this.a.a = 0;
      this.a.a = 0;
      this.a.b = this.a;
      this.a.a = this.a;
   }

   public final nb a() {
      return this.a;
   }

   public final short a(short var1) {
      short var2 = this.c;
      this.c += var1;
      if (this.c > this.b) {
         this.b = this.c;
      }

      return var2;
   }

   public final void a() {
      this.a.push(new Short(this.c));
   }

   public final void b() {
      this.c = (Short)this.a.pop();
   }

   protected final void a(DataOutputStream var1, short var2) {
      var1.writeShort(this.a);
      var1.writeShort(this.b);
      var1.writeInt('\uffff' & this.a.a);
      var1.write(this.a, 0, '\uffff' & this.a.a);
      var1.writeShort(this.a.size());

      for(int var4 = 0; var4 < this.a.size(); ++var4) {
         kp var3 = (kp)this.a.elementAt(var4);
         var1.writeShort(kp.a(var3).a);
         var1.writeShort(kp.b(var3).a);
         var1.writeShort(kp.c(var3).a);
         var1.writeShort(kp.a(var3));
      }

      Vector var5 = new Vector();
      var1.writeShort(var5.size());
      Enumeration var6 = var5.elements();

      while(var6.hasMoreElements()) {
         ((gv)var6.nextElement()).b(var1);
      }

   }

   public final void a(String var1) {
      byte[] var2 = new byte['\uffff' & this.a.a];

      int var3;
      for(var3 = 0; var3 < var2.length; ++var3) {
         var2[var3] = -1;
      }

      this.a(var1, this.a, '\uffff' & this.a.a, 0, 0, var2);
      var3 = 0;

      int var4;
      while(var3 != this.a.size()) {
         for(var4 = 0; var4 < this.a.size(); ++var4) {
            kp var5 = (kp)this.a.elementAt(var4);
            if (var2['\uffff' & kp.a(var5).a] != -1) {
               this.a(var1, this.a, '\uffff' & this.a.a, '\uffff' & kp.c(var5).a, var2['\uffff' & kp.a(var5).a] + 1, var2);
               ++var3;
            }
         }
      }

      this.a = 0;

      for(var4 = 0; var4 < var2.length; ++var4) {
         byte var6;
         if ((var6 = var2[var4]) == -1) {
            throw new RuntimeException(var1 + ": Unexamined code at offset " + var4);
         }

         if (var6 > this.a) {
            this.a = (short)var6;
         }
      }

   }

   private void a(String var1, byte[] var2, int var3, int var4, int var5, byte[] var6) {
      while(var4 >= 0 && var4 < var3) {
         byte var7;
         if ((var7 = var6[var4]) == var5) {
            return;
         }

         if (var7 == -2) {
            throw new RuntimeException(var1 + ": Invalid offset");
         }

         if (var7 != -1) {
            throw new RuntimeException(var1 + ": Operand stack inconsistent at offset " + var4 + ": Previous size " + var7 + ", now " + var5);
         }

         var6[var4] = (byte)var5;
         var7 = var2[var4];
         int var8 = var4 + 1;
         int var9;
         if (var7 == -60) {
            var7 = var2[var8++];
            var9 = on.b[255 & var7];
         } else {
            var9 = on.a[255 & var7];
         }

         if (var9 == -1) {
            throw new RuntimeException(var1 + ": Invalid opcode " + (255 & var7) + " at offset " + var4);
         }

         switch(var9 & 31) {
         case 0:
         case 1:
         case 2:
         case 3:
         case 4:
         case 5:
         case 6:
            var5 += (var9 & 31) - 4;
            break;
         case 7:
            var5 = 0;
            break;
         case 8:
         case 17:
         default:
            throw new RuntimeException(var1 + ": Invalid stack delta");
         case 9:
            --var5;
         case 10:
            var5 += this.a((short)((var2[var8] << 8) + (255 & var2[var8 + 1])));
            break;
         case 11:
            --var5;
         case 12:
            var5 -= this.a((short)((var2[var8] << 8) + (255 & var2[var8 + 1])));
            break;
         case 13:
         case 14:
         case 16:
            --var5;
         case 15:
            var5 -= this.b((short)((var2[var8] << 8) + (255 & var2[var8 + 1])));
            break;
         case 18:
            var5 -= var2[var8 + 2] - 1;
         }

         String var12;
         if (var5 < 0) {
            var12 = this.a.a() + '.' + var1 + ": Operand stack underrun at offset " + var4;
            throw new RuntimeException(var12);
         }

         if (var5 > 127) {
            var12 = this.a.a() + '.' + var1 + ": Operand stack overflow at offset " + var4;
            throw new RuntimeException(var12);
         }

         int var11;
         int var10;
         label104:
         switch(var9 & 480) {
         case 0:
            break;
         case 32:
         case 64:
         case 128:
         case 192:
            ++var8;
            break;
         case 96:
         case 160:
         case 224:
            var8 += 2;
            break;
         case 256:
            this.a(var1, var2, var3, var4 + (var2[var8++] << 8) + (255 & var2[var8++]), var5, var6);
            break;
         case 288:
            this.a(var1, var2, var3, var4 + (var2[var8++] << 24) + ((255 & var2[var8++]) << 16) + ((255 & var2[var8++]) << 8) + (255 & var2[var8++]), var5, var6);
            break;
         case 320:
            while((var8 & 3) != 0) {
               ++var8;
            }

            this.a(var1, var2, var3, var4 + (var2[var8++] << 24) + ((255 & var2[var8++]) << 16) + ((255 & var2[var8++]) << 8) + (255 & var2[var8++]), var5, var6);
            var11 = (var2[var8++] << 24) + ((255 & var2[var8++]) << 16) + ((255 & var2[var8++]) << 8) + (255 & var2[var8++]);

            for(var10 = 0; var10 < var11; ++var10) {
               var8 += 4;
               this.a(var1, var2, var3, var4 + (var2[var8++] << 24) + ((255 & var2[var8++]) << 16) + ((255 & var2[var8++]) << 8) + (255 & var2[var8++]), var5, var6);
            }
            break;
         case 352:
            while((var8 & 3) != 0) {
               ++var8;
            }

            this.a(var1, var2, var3, var4 + (var2[var8++] << 24) + ((255 & var2[var8++]) << 16) + ((255 & var2[var8++]) << 8) + (255 & var2[var8++]), var5, var6);
            var10 = (var2[var8++] << 24) + ((255 & var2[var8++]) << 16) + ((255 & var2[var8++]) << 8) + (255 & var2[var8++]);
            var11 = (var2[var8++] << 24) + ((255 & var2[var8++]) << 16) + ((255 & var2[var8++]) << 8) + (255 & var2[var8++]);
            var10 = var10;

            while(true) {
               if (var10 > var11) {
                  break label104;
               }

               this.a(var1, var2, var3, var4 + (var2[var8++] << 24) + ((255 & var2[var8++]) << 16) + ((255 & var2[var8++]) << 8) + (255 & var2[var8++]), var5, var6);
               ++var10;
            }
         case 384:
            var11 = var4 + (var2[var8++] << 8) + (255 & var2[var8++]);
            if (var6[var11] == -1) {
               this.a(var1, var2, var3, var11, var5 + 1, var6);
            }
            break;
         default:
            throw new RuntimeException(var1 + ": Invalid OP1");
         }

         switch(var9 & 1536) {
         case 0:
            break;
         case 512:
            ++var8;
            break;
         case 1024:
            var8 += 2;
            break;
         default:
            throw new RuntimeException(var1 + ": Invalid OP2");
         }

         switch(var9 & 2048) {
         case 2048:
            ++var8;
         case 0:
            for(var11 = var4 + 1; var11 < var8; ++var11) {
               var6[var11] = -2;
            }

            if ((var9 & -32768) != 0) {
               return;
            }

            var4 = var8;
            break;
         default:
            throw new RuntimeException(var1 + ": Invalid OP3");
         }
      }

      throw new RuntimeException(var1 + ": Offset out of range");
   }

   public final void c() {
      for(gx var1 = this.a; var1 != this.a; var1 = var1.b) {
         if (var1 instanceof is) {
            ((is)var1).a();
         }
      }

   }

   public final void d() {
      for(int var1 = 0; var1 < this.b.size(); ++var1) {
         ((ip)this.b.elementAt(var1)).a();
      }

   }

   private int a(short var1) {
      ou var2 = (ou)this.a.a(var1);
      li var3 = (li)this.a.a(var2.a());
      return nq.a(((cg)this.a.a(var3.a())).a());
   }

   private int b(short var1) {
      ib var4 = this.a.a(var1);
      li var5 = (li)this.a.a(var4 instanceof md ? ((md)var4).a() : ((ao)var4).a());
      String var3;
      if ((var3 = ((cg)this.a.a(var5.a())).a()).charAt(0) != '(') {
         throw new RuntimeException("Method descriptor does not start with \"(\"");
      } else {
         int var6 = 1;
         int var2 = 0;

         label50:
         while(true) {
            switch(var3.charAt(var6++)) {
            case ')':
               return var2 - nq.a(var3.substring(var6));
            case 'B':
            case 'C':
            case 'F':
            case 'I':
            case 'S':
            case 'Z':
               ++var2;
               break;
            case 'D':
            case 'J':
               var2 += 2;
               break;
            case 'L':
               ++var2;

               while(true) {
                  if (var3.charAt(var6++) == ';') {
                     continue label50;
                  }
               }
            case '[':
               ++var2;

               while(var3.charAt(var6) == '[') {
                  ++var6;
               }

               if ("BCFISZDJ".indexOf(var3.charAt(var6)) != -1) {
                  ++var6;
               } else {
                  if (var3.charAt(var6) != 'L') {
                     throw new RuntimeException("Invalid char after \"[\"");
                  }

                  ++var6;

                  while(var3.charAt(var6++) != ';') {
                  }
               }
               break;
            default:
               throw new RuntimeException("Invalid method descriptor");
            }
         }
      }
   }

   public final void a(short var1, byte[] var2) {
      if (var2.length != 0) {
         if (var1 != -1) {
            label48: {
               for(gx var3 = this.b.a; var3 != this.a; var3 = var3.a) {
                  if (var3 instanceof gk) {
                     if (gk.a((gk)var3) == var1) {
                        break label48;
                     }
                     break;
                  }
               }

               gk var4;
               (var4 = new gk(this, this.b.a, var1)).a = this.b.a;
               var4.b = this.b;
               this.b.a.b = var4;
               this.b.a = var4;
            }
         }

         int var7 = '\uffff' & this.b.a;
         if (('\uffff' & this.a.a) + var2.length <= this.a.length) {
            System.arraycopy(this.a, var7, this.a, var7 + var2.length, ('\uffff' & this.a.a) - var7);
         } else {
            byte[] var5 = this.a;
            this.a = new byte[this.a.length + 128];
            if (this.a.length >= 65535) {
               throw new RuntimeException("Code attribute in class \"" + this.a.a() + "\" grows beyond 64 KB");
            }

            System.arraycopy(var5, 0, this.a, 0, var7);
            System.arraycopy(var5, var7, this.a, var7 + var2.length, ('\uffff' & this.a.a) - var7);
         }

         System.arraycopy(var2, 0, this.a, var7, var2.length);

         for(Object var6 = this.b; var6 != null; var6 = ((gx)var6).b) {
            ((gx)var6).a = (short)(((gx)var6).a + var2.length);
         }

      }
   }

   public final void a(short var1, int var2) {
      this.a((short)-1, new byte[]{(byte)(var2 >> 8), (byte)var2});
   }

   public final void a(short var1, int var2, gx var3) {
      this.b.addElement(new oo(this, this.a(), var3));
      this.a(var1, new byte[]{(byte)var2, -1, -1});
   }

   public final void a(short var1, gx var2, gx var3) {
      this.b.addElement(new ma(this, this.a(), var2, var3));
      this.a((short)-1, new byte[]{-1, -1, -1, -1});
   }

   public final gx a() {
      gx var1;
      (var1 = new gx(this)).b();
      return var1;
   }

   public final ex a() {
      ex var1;
      (var1 = new ex(this)).b();
      return var1;
   }

   public final ex b() {
      return this.b;
   }

   public final void a(ex var1) {
      if (ex.a(var1) != null) {
         throw new RuntimeException("An Inserter can only be pushed once at a time");
      } else {
         ex.a(var1, this.b);
         this.b = var1;
      }
   }

   public final void e() {
      ex var1;
      if ((var1 = ex.a(this.b)) == null) {
         throw new RuntimeException("Code inserter stack underflow");
      } else {
         ex.a(this.b, (ex)null);
         this.b = var1;
      }
   }

   public final void a(gx var1, gx var2, gx var3, String var4) {
      this.a.addElement(new kp(var1, var2, var3, var4 == null ? 0 : this.a.a(var4)));
   }

   static byte[] a(fg var0) {
      return var0.a;
   }

   static ex a(fg var0) {
      return var0.b;
   }

   static nb a(fg var0) {
      return var0.a;
   }
}
