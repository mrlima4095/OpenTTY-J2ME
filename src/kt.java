import java.io.OutputStream;

public class kt extends OutputStream {
   protected OutputStream a;
   private byte[] a;
   protected hn a;

   private void b() {
      int var1;
      while(!this.a.b() && (var1 = this.a.a(this.a, 0, this.a.length)) > 0) {
         this.a.write(this.a, 0, var1);
      }

      if (!this.a.b()) {
         throw new Error("Can't deflate all input?");
      }
   }

   public kt(OutputStream var1, hn var2) {
      this(var1, var2, 4096);
   }

   private kt(OutputStream var1, hn var2, int var3) {
      this.a = var1;
      this.a = new byte[4096];
      this.a = var2;
   }

   public void flush() {
      this.a.b();
      this.b();
      this.a.flush();
   }

   public void a() {
      this.a.c();

      int var1;
      while(!this.a.a() && (var1 = this.a.a(this.a, 0, this.a.length)) > 0) {
         this.a.write(this.a, 0, var1);
      }

      if (!this.a.a()) {
         throw new Error("Can't deflate all input?");
      } else {
         this.a.flush();
      }
   }

   public void close() {
      this.a();
      this.a.close();
   }

   public void write(int var1) {
      byte[] var2;
      (var2 = new byte[1])[0] = (byte)var1;
      this.write(var2, 0, 1);
   }

   public void write(byte[] var1) {
      this.write(var1, 0, var1.length);
   }

   public void write(byte[] var1, int var2, int var3) {
      this.a.a(var1, var2, var3);
      this.b();
   }
}
