import com.siemens.mp.io.file.FileSystemRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

public abstract class pf {
   public static int a = 0;
   public int b = 1;
   public int c = 2;
   public int d = 3;

   public pf(int var1, int var2, int var3) {
   }

   public static pf a() {
      a = 0;

      label31: {
         try {
            if (Class.forName("javax.microedition.io.file.FileConnection") != null) {
               a = 1;
               break label31;
            }
         } catch (ClassNotFoundException var1) {
         }

         try {
            if (Class.forName("com.siemens.mp.io.file.FileConnection") != null) {
               a = 2;
            }
         } catch (ClassNotFoundException var0) {
         }
      }

      if (a == 0) {
         throw new IOException();
      } else {
         return (pf)(a == 1 ? new mo() : new ei());
      }
   }

   public abstract void a(String var1, int var2);

   public abstract void a();

   public static Enumeration a() {
      if (a == 0) {
         throw new IOException();
      } else {
         return a == 2 ? FileSystemRegistry.listRoots() : javax.microedition.io.file.FileSystemRegistry.listRoots();
      }
   }

   public abstract Enumeration a(String var1, boolean var2);

   public abstract InputStream a();

   public abstract OutputStream a();

   public abstract boolean a();

   public abstract void b();

   public abstract void c();

   public abstract void a(String var1);

   public abstract long a();

   public abstract void d();
}
