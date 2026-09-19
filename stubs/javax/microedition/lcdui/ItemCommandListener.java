package javax.microedition.lcdui;

/**
 * Desktop stub of ItemCommandListener.
 *
 * NOTE: the standard MIDP interface declares {@code itemCommandAction(...)};
 * here it is declared as {@code commandAction(Command, Item)} because that is
 * the method Lua.LuaFunction actually implements (see src/Lua.java), so the
 * cast `(ItemCommandListener) new LuaFunction("item", ...)` compiles and works
 * on desktop without touching the J2ME source.
 */
public interface ItemCommandListener {
    void commandAction(Command c, Item item);
}