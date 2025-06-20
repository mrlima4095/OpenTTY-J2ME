import javax.microedition.rms.RecordComparator;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordFilter;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

public final class mx {
   public static boolean a(String var0) {
      String[] var1;
      if ((var1 = RecordStore.listRecordStores()) != null) {
         for(int var2 = 0; var2 < var1.length; ++var2) {
            if (var1[var2].toLowerCase().equals(var0.toLowerCase())) {
               return true;
            }
         }
      }

      return false;
   }

   public static boolean b(String var0) {
      RecordEnumeration var1 = null;

      try {
         if (var0.toLowerCase().equals("sdkset")) {
            RecordStore var5;
            RecordEnumeration var6 = (var5 = RecordStore.openRecordStore(var0, true)).enumerateRecords((RecordFilter)null, (RecordComparator)null, false);

            while(var6.hasNextElement()) {
               var5.deleteRecord(var6.nextRecordId());
            }

            String var7 = "";
            var7 = var7 + "<bgcolor>255,255,255</bgcolor>";
            var7 = var7 + "<caretcolor>0,0,0</caretcolor>";
            var7 = var7 + "<menubar>1</menubar>";
            var7 = var7 + "<screensize></screensize>";
            var7 = var7 + "<fontset>0</fontset>";
            var7 = var7 + "<fontcolors>0,5,2,1,3,1,4</fontcolors>";
            var7 = var7 + "<rememberIndent>1</rememberIndent>";
            var7 = var7 + "<undolevels>15</undolevels>";
            var5.addRecord(var7.getBytes(), 0, var7.length());
            var5.closeRecordStore();
            return true;
         } else {
            RecordStore var2;
            String var4;
            if (var0.toLowerCase().equals("hotkeys")) {
               var2 = null;
               var1 = (var2 = RecordStore.openRecordStore(var0, true)).enumerateRecords((RecordFilter)null, (RecordComparator)null, false);

               while(var1.hasNextElement()) {
                  var2.deleteRecord(var1.nextRecordId());
               }

               var4 = "<keyindex>8</keyindex><value>-8</value><keyindex>11</keyindex><value>-5</value><keyindex>3</keyindex><value>-4</value><keyindex>2</keyindex><value>-3</value><keyindex>1</keyindex><value>-2</value><keyindex>0</keyindex><value>-1</value><keyindex>12</keyindex><value>35</value><keyindex>6</keyindex><value>50</value><keyindex>4</keyindex><value>51</value><keyindex>13</keyindex><value>53</value><keyindex>7</keyindex><value>56</value><keyindex>5</keyindex><value>57</value>";
               if (pf.a == 2) {
                  var4 = "<keyindex>3</keyindex><value>-62</value><keyindex>2</keyindex><value>-61</value><keyindex>1</keyindex><value>-60</value><keyindex>0</keyindex><value>-59</value><keyindex>11</keyindex><value>-26</value><keyindex>8</keyindex><value>-12</value><keyindex>8</keyindex><value>-8</value><keyindex>11</keyindex><value>-5</value><keyindex>3</keyindex><value>-4</value><keyindex>2</keyindex><value>-3</value><keyindex>1</keyindex><value>-2</value><keyindex>0</keyindex><value>-1</value><keyindex>12</keyindex><value>35</value><keyindex>14</keyindex><value>42</value><keyindex>4</keyindex><value>50</value><keyindex>4</keyindex><value>51</value><keyindex>6</keyindex><value>52</value><keyindex>13</keyindex><value>53</value><keyindex>7</keyindex><value>54</value><keyindex>5</keyindex><value>56</value><keyindex>5</keyindex><value>57</value>";
               }

               var2.addRecord(var4.getBytes(), 0, var4.length());
               var2.closeRecordStore();
               return true;
            } else if (!var0.toLowerCase().equals("templates")) {
               return false;
            } else {
               var2 = null;
               var1 = (var2 = RecordStore.openRecordStore(var0, true)).enumerateRecords((RecordFilter)null, (RecordComparator)null, false);

               while(var1.hasNextElement()) {
                  var2.deleteRecord(var1.nextRecordId());
               }

               var4 = "<name>if Block</name><code>if () {\n \n}</code><name>if-else Block</name><code>if () {\n \n} else {\n \n}</code><name>switch Block</name><code>switch () {\n case :\n  \n  break;\n}</code><name>for Block</name><code>for ( ; ; ) {\n \n}</code><name>while Block</name><code>while () {\n \n}</code><name>do-while Block</name><code>do {\n \n} while ();</code><name>try-catch Block</name><code>try {\n \n} catch (Exception exc) { }</code>";
               var2.addRecord(var4.getBytes(), 0, var4.length());
               var2.closeRecordStore();
               return true;
            }
         }
      } catch (RecordStoreException var3) {
         throw new lo("createStore(" + var0 + ")", var3);
      }
   }

   public static int[] a(String var0) {
      try {
         RecordStore var1;
         if ((var1 = RecordStore.openRecordStore(var0, false)).getNumRecords() == 0) {
            return null;
         } else {
            int[] var2 = new int[var1.getNumRecords()];
            RecordEnumeration var3 = var1.enumerateRecords((RecordFilter)null, (RecordComparator)null, false);

            for(int var4 = 0; var4 < var2.length; ++var4) {
               var2[var4] = var3.nextRecordId();
            }

            var1.closeRecordStore();
            return var2;
         }
      } catch (RecordStoreException var5) {
         throw new lo("loadStore(" + var0 + ")", var5);
      }
   }

   public static String a(String var0, int var1) {
      try {
         RecordStore var2 = RecordStore.openRecordStore(var0, false);
         var0 = new String(var2.getRecord(var1));
         var2.closeRecordStore();
         return var0;
      } catch (RecordStoreException var3) {
         throw new lo("getRecordData()", var3);
      }
   }

   public static int a(String var0, int var1, String var2) {
      try {
         RecordStore var3 = RecordStore.openRecordStore(var0, false);
         if (var1 < 0) {
            int var5 = var3.addRecord(var2.getBytes(), 0, var2.length());
            var3.closeRecordStore();
            return var5;
         } else {
            var3.setRecord(var1, var2.getBytes(), 0, var2.length());
            var3.closeRecordStore();
            return var1;
         }
      } catch (RecordStoreException var4) {
         throw new lo("setRecord(" + var0 + "," + var1 + "," + ",[data])", var4);
      }
   }
}
