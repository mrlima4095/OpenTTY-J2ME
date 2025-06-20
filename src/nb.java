import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public final class nb {
   private short d;
   private short e;
   private Vector c;
   public short a;
   public short b;
   public short c;
   public short[] a;
   public Vector a;
   public Vector b;
   private Vector d;
   private Hashtable a;

   public nb(short var1, String var2, String var3, String[] var4) {
      this.d = 45;
      this.e = 3;
      this.c = new Vector();
      this.c.addElement((Object)null);
      this.a = new Hashtable();
      this.a = var1;
      this.b = this.a(var2);
      this.c = this.a(var3);
      this.a = new short[var4.length];

      for(int var5 = 0; var5 < var4.length; ++var5) {
         this.a[var5] = this.a(var4[var5]);
      }

      this.a = new Vector();
      this.b = new Vector();
      this.d = new Vector();
   }

   public final void a(String var1) {
      this.d.addElement(new jw(this.c("SourceFile"), this.c(var1)));
   }

   public final void a() {
      this.d.addElement(new mi(this.c("Deprecated")));
   }

   public final lg a() {
      Short var1;
      if ((var1 = (Short)this.a.get(new cg("InnerClasses"))) == null) {
         return null;
      } else {
         Enumeration var3 = this.d.elements();

         gv var2;
         do {
            if (!var3.hasMoreElements()) {
               return null;
            }
         } while(gv.a(var2 = (gv)var3.nextElement()) != var1 || !(var2 instanceof lg));

         return (lg)var2;
      }
   }

   public final void a(ln var1) {
      lg var2;
      if ((var2 = this.a()) == null) {
         var2 = new lg(this.c("InnerClasses"));
         this.d.addElement(var2);
      }

      var2.a().addElement(var1);
   }

   public nb(InputStream var1) {
      DataInputStream var4;
      if ((var4 = var1 instanceof DataInputStream ? (DataInputStream)var1 : new DataInputStream(var1)).readInt() != -889275714) {
         throw new NoClassDefFoundError("Invalid magic number");
      } else {
         this.e = var4.readShort();
         this.d = var4.readShort();
         short var10000 = this.d;
         short var3 = this.e;
         short var2 = var10000;
         if ((var10000 != 45 || var3 != 3) && (var2 != 46 || var3 != 0) && (var2 != 47 || var3 != 0) && (var2 != 48 || var3 != 0) && (var2 != 49 || var3 != 0)) {
            throw new NoClassDefFoundError("Unrecognized class file format version " + this.d + "/" + this.e);
         } else {
            this.c = new Vector();
            this.a = new Hashtable();
            this.a(var4);
            this.a = var4.readShort();
            this.b = var4.readShort();
            this.c = var4.readShort();
            this.a = b(var4);
            this.a = this.a(var4);
            this.b = this.b(var4);
            this.d = this.c(var4);
         }
      }
   }

   public final String a() {
      return this.a(this.b).replace('/', '.');
   }

   public final short a(String var1) {
      if (var1.charAt(0) == 'L') {
         if (var1.charAt(0) != 'L') {
            throw new RuntimeException("Attempt to convert non-class descriptor \"" + var1 + "\" into internal form");
         }

         var1 = var1.substring(1, var1.length() - 1);
      } else {
         if (!nq.a(var1)) {
            throw new RuntimeException("\"" + nq.a(var1) + "\" is neither a class nor an array");
         }

         var1 = var1;
      }

      return this.a((ib)(new mv(this.c(var1))));
   }

   public final short a(String var1, String var2, String var3) {
      return this.a((ib)(new ou(this.a(var1), this.a(var2, var3))));
   }

   public final short b(String var1, String var2, String var3) {
      return this.a((ib)(new ao(this.a(var1), this.a(var2, var3))));
   }

   public final short c(String var1, String var2, String var3) {
      return this.a((ib)(new md(this.a(var1), this.a(var2, var3))));
   }

   public final short b(String var1) {
      return this.a((ib)(new ef(this.c(var1))));
   }

   public final short a(int var1) {
      return this.a((ib)(new ii(var1)));
   }

   public final short a(float var1) {
      return this.a((ib)(new ag(var1)));
   }

   public final short a(long var1) {
      return this.a((ib)(new dc(var1)));
   }

   public final short a(double var1) {
      return this.a((ib)(new iz(var1)));
   }

   private short a(String var1, String var2) {
      return this.a((ib)(new li(this.c(var1), this.c(var2))));
   }

   public final short c(String var1) {
      return this.a((ib)(new cg(var1)));
   }

   private short a(ib var1) {
      Short var2;
      if ((var2 = (Short)this.a.get(var1)) != null) {
         return var2;
      } else {
         short var3 = (short)this.c.size();
         this.c.addElement(var1);
         if (var1.a()) {
            this.c.addElement((Object)null);
         }

         this.a.put(var1, new Short(var3));
         return var3;
      }
   }

   public final mc a(short var1, String var2, String var3, Object var4) {
      Vector var5 = new Vector();
      if (var4 != null) {
         dj var10001 = new dj;
         short var10003 = this.c("ConstantValue");
         short var10004;
         if (var4 instanceof String) {
            var10004 = this.b((String)var4);
         } else if (!(var4 instanceof Byte) && !(var4 instanceof Short) && !(var4 instanceof Integer)) {
            if (var4 instanceof Boolean) {
               var10004 = this.a((Boolean)var4 ? 1 : 0);
            } else if (var4 instanceof Character) {
               var10004 = this.a((int)(Character)var4);
            } else if (var4 instanceof Float) {
               var10004 = this.a((Float)var4);
            } else if (var4 instanceof Long) {
               var10004 = this.a((Long)var4);
            } else {
               if (!(var4 instanceof Double)) {
                  throw new RuntimeException("Unexpected constant value type \"" + var4.getClass().getName() + "\"");
               }

               var10004 = this.a((Double)var4);
            }
         } else {
            var10004 = this.a(var4 instanceof Byte ? (Byte)var4 : (var4 instanceof Short ? (Short)var4 : (Integer)var4));
         }

         var10001.<init>(var10003, var10004);
         var5.addElement(var10001);
      }

      mc var7 = new mc(var1, this.c(var2), this.c(var3), var5);
      this.a.addElement(var7);
      return var7;
   }

   public final ig a(short var1, String var2, String var3) {
      ig var4 = new ig(this, var1, this.c(var2), this.c(var3), new Vector());
      this.b.addElement(var4);
      return var4;
   }

   public final ib a(short var1) {
      return (ib)this.c.elementAt(var1);
   }

   public final String a(short var1) {
      mv var2 = (mv)this.a(var1);
      return cg.a((cg)this.a(mv.a(var2)));
   }

   public final String b(short var1) {
      return cg.a((cg)this.a(var1));
   }

   private static short[] b(DataInputStream var0) {
      short var1;
      short[] var2 = new short[var1 = var0.readShort()];

      for(int var3 = 0; var3 < var1; ++var3) {
         var2[var3] = var0.readShort();
      }

      return var2;
   }

   private void a(DataInputStream var1) {
      this.c.removeAllElements();
      this.a.clear();
      short var2 = var1.readShort();
      this.c.addElement((Object)null);

      for(short var3 = 1; var3 < var2; ++var3) {
         ib var4 = ib.a(var1);
         this.c.addElement(var4);
         this.a.put(var4, new Short(var3));
         if (var4 instanceof dc || var4 instanceof iz) {
            this.c.addElement((Object)null);
            ++var3;
         }
      }

   }

   private Vector a(DataInputStream var1) {
      short var2 = var1.readShort();
      Vector var3 = new Vector(var2);

      for(int var4 = 0; var4 < var2; ++var4) {
         var3.addElement(new mc(var1.readShort(), var1.readShort(), var1.readShort(), this.c(var1)));
      }

      return var3;
   }

   private Vector b(DataInputStream var1) {
      short var2 = var1.readShort();
      Vector var3 = new Vector(var2);

      for(int var4 = 0; var4 < var2; ++var4) {
         var3.addElement(new ig(this, var1.readShort(), var1.readShort(), var1.readShort(), this.c(var1)));
      }

      return var3;
   }

   private Vector c(DataInputStream var1) {
      short var2 = var1.readShort();
      Vector var3 = new Vector(var2);

      for(int var4 = 0; var4 < var2; ++var4) {
         var3.addElement(this.a(var1));
      }

      return var3;
   }

   public final void a(OutputStream var1) {
      DataOutputStream var2;
      (var2 = var1 instanceof DataOutputStream ? (DataOutputStream)var1 : new DataOutputStream(var1)).writeInt(-889275714);
      var2.writeShort(this.e);
      var2.writeShort(this.d);
      b(var2, this.c);
      var2.writeShort(this.a);
      var2.writeShort(this.b);
      var2.writeShort(this.c);
      b(var2, this.a);
      c(var2, this.a);
      d(var2, this.b);
      e(var2, this.d);
   }

   private static void b(DataOutputStream var0, Vector var1) {
      var0.writeShort(var1.size());

      for(int var2 = 1; var2 < var1.size(); ++var2) {
         ib var3;
         if ((var3 = (ib)var1.elementAt(var2)) != null) {
            var3.a(var0);
         }
      }

   }

   private static void b(DataOutputStream var0, short[] var1) {
      var0.writeShort(var1.length);

      for(int var2 = 0; var2 < var1.length; ++var2) {
         var0.writeShort(var1[var2]);
      }

   }

   private static void c(DataOutputStream var0, Vector var1) {
      var0.writeShort(var1.size());

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         ((mc)var1.elementAt(var2)).a(var0);
      }

   }

   private static void d(DataOutputStream var0, Vector var1) {
      var0.writeShort(var1.size());

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         ((ig)var1.elementAt(var2)).a(var0);
      }

   }

   private static void e(DataOutputStream var0, Vector var1) {
      var0.writeShort(var1.size());

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         ((gv)var1.elementAt(var2)).b(var0);
      }

   }

   public static String a(String var0) {
      return var0.replace('.', '/') + ".class";
   }

   private gv a(DataInputStream var1) {
      short var2 = var1.readShort();
      byte[] var3 = new byte[var1.readInt()];
      var1.readFully(var3);
      ByteArrayInputStream var7 = new ByteArrayInputStream(var3);
      DataInputStream var4 = new DataInputStream(var7);
      String var5 = this.b(var2);
      gv var6;
      if ("ConstantValue".equals(var5)) {
         var6 = dj.a(var2, var4);
      } else if ("Code".equals(var5)) {
         var6 = lu.a(var2, this, var4);
      } else if ("Exceptions".equals(var5)) {
         var6 = dx.a(var2, var4);
      } else if ("InnerClasses".equals(var5)) {
         var6 = lg.a(var2, var4);
      } else if ("Synthetic".equals(var5)) {
         var6 = hb.a(var2, var4);
      } else if ("SourceFile".equals(var5)) {
         var6 = jw.a(var2, var4);
      } else if ("LineNumberTable".equals(var5)) {
         var6 = pn.a(var2, var4);
      } else if ("LocalVariableTable".equals(var5)) {
         var6 = iw.a(var2, var4);
      } else {
         if (!"Deprecated".equals(var5)) {
            return new pu(this, var2, var3);
         }

         var6 = mi.a(var2, var4);
      }

      if (var7.available() > 0) {
         throw new NoClassDefFoundError(var3.length - var7.available() + " bytes of trailing garbage in body of attribute \"" + var5 + "\"");
      } else {
         return var6;
      }
   }

   static void a(DataOutputStream var0, Vector var1) {
      e(var0, var1);
   }

   static short[] a(DataInputStream var0) {
      return b(var0);
   }

   static void a(DataOutputStream var0, short[] var1) {
      b(var0, var1);
   }

   static byte[] a(DataInputStream var0) {
      byte[] var1 = new byte[var0.readInt()];
      var0.readFully(var1);
      return var1;
   }

   static gv a(nb var0, DataInputStream var1) {
      return var0.a(var1);
   }
}
