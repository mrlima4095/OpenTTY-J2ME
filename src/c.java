public final class c {
   public static hh a(byte[] var0, int var1) {
      int var2 = var1 + 1;
      byte var3 = var0[var1];
      boolean var4 = false;
      if (var3 == -60) {
         var3 = var0[var2++];
         var4 = true;
      }

      Object var5;
      switch(var3) {
      case -128:
      case -127:
      case -126:
      case -125:
      case -123:
      case -122:
      case -121:
      case -120:
      case -119:
      case -118:
      case -117:
      case -116:
      case -115:
      case -114:
      case -113:
      case -112:
      case -111:
      case -110:
      case -109:
      case -108:
      case -107:
      case -106:
      case -105:
      case -104:
      case -84:
      case -83:
      case -82:
      case -81:
      case -80:
      case -79:
      case -68:
      case -66:
      case -65:
      case -62:
      case -61:
      case 0:
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 8:
      case 9:
      case 10:
      case 11:
      case 12:
      case 13:
      case 14:
      case 15:
      case 16:
      case 17:
      case 46:
      case 47:
      case 48:
      case 49:
      case 50:
      case 51:
      case 52:
      case 53:
      case 79:
      case 80:
      case 81:
      case 82:
      case 83:
      case 84:
      case 85:
      case 86:
      case 87:
      case 88:
      case 89:
      case 90:
      case 91:
      case 92:
      case 93:
      case 94:
      case 95:
      case 96:
      case 97:
      case 98:
      case 99:
      case 100:
      case 101:
      case 102:
      case 103:
      case 104:
      case 105:
      case 106:
      case 107:
      case 108:
      case 109:
      case 110:
      case 111:
      case 112:
      case 113:
      case 114:
      case 115:
      case 116:
      case 117:
      case 118:
      case 119:
      case 120:
      case 121:
      case 122:
      case 123:
      case 124:
      case 125:
      case 126:
      case 127:
         var5 = new du();
         break;
      case -124:
      case -87:
      case 21:
      case 22:
      case 23:
      case 24:
      case 25:
      case 26:
      case 27:
      case 28:
      case 29:
      case 30:
      case 31:
      case 32:
      case 33:
      case 34:
      case 35:
      case 36:
      case 37:
      case 38:
      case 39:
      case 40:
      case 41:
      case 42:
      case 43:
      case 44:
      case 45:
      case 54:
      case 55:
      case 56:
      case 57:
      case 58:
      case 59:
      case 60:
      case 61:
      case 62:
      case 63:
      case 64:
      case 65:
      case 66:
      case 67:
      case 68:
      case 69:
      case 70:
      case 71:
      case 72:
      case 73:
      case 74:
      case 75:
      case 76:
      case 77:
      case 78:
         var5 = new b(var4);
         break;
      case -103:
      case -102:
      case -101:
      case -100:
      case -99:
      case -98:
      case -97:
      case -96:
      case -95:
      case -94:
      case -93:
      case -92:
      case -91:
      case -90:
      case -89:
      case -88:
      case -58:
      case -57:
      case -56:
      case -55:
         var5 = new ae();
         break;
      case -86:
         var5 = new dw();
         break;
      case -85:
         var5 = new gr();
         break;
      case -78:
      case -77:
      case -76:
      case -75:
      case -74:
      case -73:
      case -72:
      case -71:
      case -69:
      case -67:
      case -64:
      case -63:
      case -59:
      case 18:
      case 19:
      case 20:
         var5 = new hf();
         break;
      case -70:
      case -60:
      case -54:
      case -53:
      case -52:
      case -51:
      case -50:
      case -49:
      case -48:
      case -47:
      case -46:
      case -45:
      case -44:
      case -43:
      case -42:
      case -41:
      case -40:
      case -39:
      case -38:
      case -37:
      case -36:
      case -35:
      case -34:
      case -33:
      case -32:
      case -31:
      case -30:
      case -29:
      case -28:
      case -27:
      case -26:
      case -25:
      case -24:
      case -23:
      case -22:
      case -21:
      case -20:
      case -19:
      case -18:
      case -17:
      case -16:
      case -15:
      case -14:
      case -13:
      case -12:
      case -11:
      case -10:
      case -9:
      case -8:
      case -7:
      case -6:
      case -5:
      case -4:
      case -3:
      case -2:
      case -1:
      default:
         throw new IllegalArgumentException("Unknown instruction opcode [" + var3 + "] at offset " + var1);
      }

      ((hh)var5).a = var3;
      ((hh)var5).a(var0, var2);
      return (hh)var5;
   }
}
