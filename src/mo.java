import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

public final class mo extends pf {
   private FileConnection a = null;

   public mo() {
      super(1, 2, 3);
   }

   public final void a(String var1, int var2) {
      this.a = (FileConnection)Connector.open("file:///" + var1, var2);
   }

   public final void a() {
      this.a.close();
      this.a = null;
   }

   public final Enumeration a(String var1, boolean var2) {
      return this.a.list(var1, true);
   }

   public final InputStream a() {
      return this.a.openInputStream();
   }

   public final OutputStream a() {
      return this.a.openOutputStream();
   }

   public final boolean a() {
      return this.a.exists();
   }

   public final void b() {
      this.a.delete();
   }

   public final void c() {
      this.a.create();
   }

   public final void a(String var1) {
      this.a.rename(var1);
   }

   public final long a() {
      return this.a.fileSize();
   }

   public final void d() {
      this.a.mkdir();
   }
}
