public final class nq {
   public static boolean a(String var0) {
      return var0.charAt(0) == '[';
   }

   public static short a(String var0) {
      if (var0.equals("V")) {
         return 0;
      } else if (var0.length() == 1 ? "BCFISZ".indexOf(var0) != -1 : var0.length() > 1) {
         return 1;
      } else if (b(var0)) {
         return 2;
      } else {
         throw new RuntimeException("No size defined for type \"" + a(var0) + "\"");
      }
   }

   public static boolean b(String var0) {
      return var0.equals("J") || var0.equals("D");
   }

   public static String a(String var0) {
      int var1 = 0;
      StringBuffer var2 = new StringBuffer();
      if (var0.charAt(0) == '(') {
         ++var1;
         var2.append("(");

         for(; var1 < var0.length() && var0.charAt(var1) != ')'; var1 = a(var0, var1, var2)) {
            if (var1 != 1) {
               var2.append(", ");
            }
         }

         if (var1 >= var0.length()) {
            throw new RuntimeException("Invalid descriptor \"" + var0 + "\"");
         }

         var2.append(") => ");
         ++var1;
      }

      a(var0, var1, var2);
      return var2.toString();
   }

   private static int a(String var0, int var1, StringBuffer var2) {
      int var3;
      for(var3 = 0; var1 < var0.length() && var0.charAt(var1) == '['; ++var1) {
         ++var3;
      }

      if (var1 >= var0.length()) {
         throw new RuntimeException("Invalid descriptor \"" + var0 + "\"");
      } else {
         switch(var0.charAt(var1)) {
         case 'B':
            var2.append("byte");
            break;
         case 'C':
            var2.append("char");
            break;
         case 'D':
            var2.append("double");
            break;
         case 'E':
         case 'G':
         case 'H':
         case 'K':
         case 'M':
         case 'N':
         case 'O':
         case 'P':
         case 'Q':
         case 'R':
         case 'T':
         case 'U':
         case 'W':
         case 'X':
         case 'Y':
         default:
            throw new RuntimeException("Invalid descriptor \"" + var0 + "\"");
         case 'F':
            var2.append("float");
            break;
         case 'I':
            var2.append("int");
            break;
         case 'J':
            var2.append("long");
            break;
         case 'L':
            int var4;
            if ((var4 = var0.indexOf(59, var1)) == -1) {
               throw new RuntimeException("Invalid descriptor \"" + var0 + "\"");
            }

            var2.append(var0.substring(var1 + 1, var4).replace('/', '.'));
            var1 = var4;
            break;
         case 'S':
            var2.append("short");
            break;
         case 'V':
            var2.append("void");
            break;
         case 'Z':
            var2.append("boolean");
         }

         while(var3 > 0) {
            var2.append("[]");
            --var3;
         }

         return var1 + 1;
      }
   }

   public static String b(String var0) {
      if (var0.equals("void")) {
         return "V";
      } else if (var0.equals("byte")) {
         return "B";
      } else if (var0.equals("char")) {
         return "C";
      } else if (var0.equals("double")) {
         return "D";
      } else if (var0.equals("float")) {
         return "F";
      } else if (var0.equals("int")) {
         return "I";
      } else if (var0.equals("long")) {
         return "J";
      } else if (var0.equals("short")) {
         return "S";
      } else if (var0.equals("boolean")) {
         return "Z";
      } else {
         return var0.startsWith("[") ? var0.replace('.', '/') : 'L' + var0.replace('.', '/') + ';';
      }
   }

   public static String c(String var0) {
      if (var0.length() == 1) {
         if (var0.equals("V")) {
            return "void";
         }

         if (var0.equals("B")) {
            return "byte";
         }

         if (var0.equals("C")) {
            return "char";
         }

         if (var0.equals("D")) {
            return "double";
         }

         if (var0.equals("F")) {
            return "float";
         }

         if (var0.equals("I")) {
            return "int";
         }

         if (var0.equals("J")) {
            return "long";
         }

         if (var0.equals("S")) {
            return "short";
         }

         if (var0.equals("Z")) {
            return "boolean";
         }
      } else {
         char var1;
         if ((var1 = var0.charAt(0)) == 'L' && var0.endsWith(";")) {
            return var0.substring(1, var0.length() - 1).replace('/', '.');
         }

         if (var1 == '[') {
            return var0.replace('/', '.');
         }
      }

      throw new RuntimeException("(Invalid field descriptor \"" + var0 + "\")");
   }

   public static String d(String var0) {
      if (var0.charAt(0) != 'L') {
         throw new RuntimeException("Attempt to get package name of non-class descriptor \"" + var0 + "\"");
      } else {
         int var1;
         return (var1 = var0.lastIndexOf(47)) == -1 ? null : var0.substring(1, var1).replace('/', '.');
      }
   }
}
