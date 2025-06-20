import java.io.IOException;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

public final class lv {
   private static int a = 20;
   private List a;
   private Image a = null;
   private Image b = null;
   private Image c = null;
   String a;
   private boolean a;
   private final String[] a = new String[]{"com.nokia.mid.sound.Sound", "....Listener", "...ui.DeviceControl", "....DirectGraphics", "....DirectUtils", "....FullCanvas", ".siemens.mp.color_game.GameCanvas", "....Layer", "....LayerManager", "....Sprite", "....TiledManager", "...game.ExtendedImage", "....GraphicObject", "....GraphicObjectManager", "....Light", "....Melody", "....MelodyComposer", "....Sound", "....Sprite", "....TiledBackground", "....Vibrator", "...gsm.Call", "....PhoneBook", "....SMS", "...io.Connection", "....ConnectionListener", "....File", "...lcdui.Image", "...m55.Ledcontrol", "...media.control.ToneControl", ".....VolumeControl", "...media.Control", "....Controllable", "....Manager", "....MediaException", "....Player", "....PlayerListener", "....TimeBase", "...ui.Image", "...MIDlet", "...NotAllowedException", "java.io.ByteArrayInputStream", "..ByteArrayOutputStream", "..DataInput", "..DataInputStream", "..DataOutput", "..DataOutputStream", "..EOFException", "..InputStream", "..InputStreamReader", "..InterruptedIOException", "..IOException", "..OutputStream", "..OutputStreamWriter", "..PrintStream", "..Reader", "..UnsupportedEncodingException", "..UTFDataFormatException", "..Writer", ".lang.ref.Reference", "...WeakReference", "..ArithmeticException", "..ArrayIndexOutOfBoundsException", "..ArrayStoreException", "..Boolean", "..Byte", "..Character", "..Class", "..ClassCastException", "..ClassNotFoundException", "..Error", "..Exception", "..IllegalAccessException", "..IllegalArgumentException", "..IllegalMonitorStateException", "..IllegalStateException", "..IllegalThreadStateException", "..IndexOutOfBoundsException", "..InstantiationException", "..Integer", "..InterruptedException", "..Long", "..Math", "..NegativeArraySizeException", "..NullPointerException", "..Object", "..OutOfMemoryError", "..Runnable", "..Runtime", "..RuntimeException", "..SecurityException", "..Short", "..String", "..StringIndexOutOfBoundsException", "..StringBuffer", "..System", "..Thread", "..Throwable", "..VirtualMachineError", ".util.Calendar", "..Date", "..EmptyStackException", "..Enumeration", "..Hashtable", "..NoSuchElementException", "..Random", "..Stack", "..Timer", "..TimerTask", "..TimeZone", "..Vector", "javax.bluetooth.BluetoothConnectionException", "..BluetoothStateException", "..DataElement", "..DeviceClass", "..DiscoveryAgent", "..DiscoveryListener", "..L2CAPConnection", "..L2CAPConnectionNotifier", "..LocalDevice", "..RemoteDevice", "..ServiceRecord", "..ServiceRegistrationException", "..UUID", ".microedition.io.CommConnection", "...Connection", "...ConnectionNotFoundException", "...Connector", "...ContentConnection", "...Datagram", "...DatagramConnection", "...HttpConnection", "...HttpsConnection", "...InputConnection", "...OutputConnection", "...PushRegistry", "...SecureConnection", "...SecurtyInfo", "...ServerSocketConnection", "...SocketConnection", "...StreamConnection", "...StreamConnectionNotifier", "...UDPDatagramConnection", "..lcdui.game.GameCanvas", "....Layer", "....LayerManager", "....Sprite", "....TiledLayer", "...Alert", "...AlertType", "...Canvas", "...Choice", "...ChoiceGroup", "...Command", "...CommandListener", "...CustomItem", "...DateField", "...Display", "...Displayable", "...Font", "...Form", "...Gauge", "...Graphics", "...Image", "...ImageItem", "...Item", "...ItemCommandListener", "...ItemStateListener", "...List", "...Screen", "...Spacer", "...StringItem", "...TextBox", "...TextField", "...Ticker", "..location.AddressInfo", "...Coordinates", "...Criteria", "...Landmark", "...LandmarkException", "...LandmarkStore", "...Location", "...LocationException", "...LocationListener", "...LocationProvider", "...Orientation", "...ProximityListener", "...QualifiedCoordinates", "..m3g.AnimationController", "...AnimationTrack", "...Appearance", "...Background", "...Camera", "...CompositingMode", "...Fog", "...Graphics3D", "...Group", "...Image2D", "...IndexBuffer", "...KeyframeSequence", "...Light", "...Loader", "...Material", "...Mesh", "...MorphingMesh", "...Node", "...Object3D", "...PolygonMode", "...RayIntersection", "...SkinnedMesh", "...Sprite3D", "...Texture2D", "...Transform", "...Transformable", "...TriangleStripArray", "...VertexArray", "...VertexBuffer", "...World", "..media.control.FramePositioningControl", "....GUIControl", "....MetaDataControl", "....MIDIControl", "....PitchControl", "....RateControl", "....RecordControl", "....StopTimeControl", "....TempoControl", "....ToneControl", "....VideoControl", "....VolumeControl", "...Control", "...Controllable", "...Manager", "...MediaException", "...Player", "...PlayerListener", "...TimeBase", "..midlet.MIDlet", "...MIDletStateChangeException", "..pki.Certificate", "...CertificateException", "..rms.InvalidRecordIDException", "...RecordComparator", "...RecordEnumeration", "...RecordFilter", "...RecordListener", "...RecordStore", "...RecordStoreException", "...RecordStoreFullException", "...RecordStoreNotFoundException", "...RecordStoreNotOpenException", ".wireless.messaging.BinaryMessage", "...Message", "...MessageConnection", "...MessageListener", "...TextMessage"};

   public lv(Displayable var1) {
      try {
         this.a = Image.createImage("/img/package.png");
         this.b = Image.createImage("/img/java.png");
         this.c = Image.createImage("/img/up.png");
      } catch (IOException var2) {
      }

      this.a = (List)var1;
      this.a = "";
      this.a();
   }

   private String a(String[] var1, int var2, String var3) {
      if (null != var1 && 0 <= var2 && !var1[var2].equals(var3)) {
         String[] var7 = var1;
         StringBuffer var4 = new StringBuffer();
         if (null != var1) {
            int var5 = 0;

            while(true) {
               var4.append(var7[var5]);
               ++var5;
               if (var5 == var7.length || 0 == var7[var5].length()) {
                  break;
               }

               var4.append('.');
            }
         }

         var4.toString();
         Image var6;
         if (Character.isLowerCase((var3 = var1[var2]).charAt(0))) {
            var6 = this.a;
         } else {
            var6 = this.b;
            if (this.a) {
               this.a = false;
               this.a.append("*", (Image)null);
            }
         }

         this.a.append(var3, var6);
      }

      return var3;
   }

   final void a() {
      this.a.deleteAll();
      String var8 = this.a;
      int var9 = 0;
      if (null != var8 && 0 < var8.length()) {
         int var10 = 0;

         do {
            ++var9;
            var10 = var8.indexOf(46, var10) + 1;
         } while(0 != var10);
      }

      String[] var16 = new String[var9];
      int var12 = 0;

      int var13;
      int var14;
      for(var13 = 0; var13 < var9; ++var13) {
         if (-1 == (var14 = var8.indexOf(46, var12))) {
            var14 = var8.length();
         }

         var16[var13] = var8.substring(var12, var14);
         var12 = var14 + 1;
      }

      String[] var1 = var16;
      if (null != this.a && 0 != this.a.length()) {
         this.a.setTitle(this.a);
         this.a.append("..", this.c);
         this.a = true;
      } else {
         this.a.setTitle("Package Browser");
      }

      String[] var2 = new String[a];
      int[] var3 = new int[a];
      boolean var4 = true;
      int var5 = 0;
      String var6 = "";
      int var7 = 0;

      while(var7 < this.a.length) {
         String var10001 = this.a[var7];
         String[] var11 = var2;
         int[] var17 = var3;
         String var15 = var10001;
         if (null != var15) {
            int[] var18 = var3;
            var8 = var15;
            var13 = 0;
            if (null != var15 && null != var3) {
               var13 = -1;
               var14 = 0;

               do {
                  if (-1 != (var13 = var8.indexOf(46, var13 + 1))) {
                     var18[var14] = var13;
                  } else {
                     var18[var14] = var8.length();
                  }

                  ++var14;
               } while(var14 < var18.length && -1 != var13);

               var13 = var14;
            }

            var12 = var13;
            var13 = -1;

            for(var14 = 0; var14 < var12; ++var14) {
               if (1 < var17[var14] - var13) {
                  var11[var14] = var15.substring(var13 + 1, var17[var14]);
               }

               var13 = var17[var14];
            }

            if (var14 < var11.length) {
               var11[var14] = "";
            }
         }

         if (0 == var1.length) {
            var6 = this.a(var2, var5, var6);
         } else {
            label62: {
               if (var4) {
                  if (!var2[var5].equals(var1[var5])) {
                     break label62;
                  }

                  if (var1.length != var5 + 1) {
                     ++var5;
                     continue;
                  }

                  var4 = false;
               } else if (!var2[var5].equals(var1[var5])) {
                  break;
               }

               var6 = this.a(var2, var5 + 1, var6);
            }
         }

         ++var7;
      }

   }
}
