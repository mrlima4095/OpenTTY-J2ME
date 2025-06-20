public final class lo extends RuntimeException {
   lo(String var1, Throwable var2) {
      super(var1 + " " + var2.getClass() + " - " + var2.getMessage());
   }
}
