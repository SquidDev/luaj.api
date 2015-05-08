package org.squiddev.luaj.api.builder;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.objectweb.asm.Type;
import org.squiddev.luaj.api.LuaObject;
import org.squiddev.luaj.api.utils.TinyMethod;

import static org.objectweb.asm.Opcodes.ACC_PROTECTED;

/**
 * Lots of helper variables and constants
 */
public abstract class BuilderConstants {
	// A CLASS_ Starts is the L...; a TYPE_ is not
	public static final String CLASS_VARARGS = Type.getDescriptor(Varargs.class);
	public static final String CLASS_LUAVALUE = Type.getDescriptor(LuaValue.class);
	public static final String CLASS_LUATABLE = Type.getDescriptor(LuaTable.class);
	public static final String TYPE_LUAVALUE = Type.getInternalName(LuaValue.class);
	public static final String TYPE_LUATABLE = Type.getInternalName(LuaTable.class);
	public static final String TYPE_LUAERROR = Type.getInternalName(LuaError.class);

	public static final String CLASS_LOADER = Type.getDescriptor(APIClassLoader.class);
	public static final String TYPE_LOADER = Type.getInternalName(APIClassLoader.class);

	public static final String INVOKE_SIGNATURE = "(" + CLASS_VARARGS + "I)" + CLASS_VARARGS;

	public static final String INSTANCE = "instance";
	public static final String LOADER = "LOADER";

	public static final String NAMES = "NAMES";
	public static final String NAMES_SIGNATURE = "[Ljava/lang/String;";

	public static final String METHOD_NAMES = "METHOD_NAMES";
	public static final String METHOD_NAMES_SIGNATURE = "[[Ljava/lang/String;";

	public static final String METHODS = "methods";
	public static final String METHODS_SIGNATURE = "[" + CLASS_LUAVALUE;

	public static final TinyMethod CREATE_TABLE = new TinyMethod(LuaObject.class, "createTable", "()" + CLASS_LUATABLE, ACC_PROTECTED);

	public static final TinyMethod VARARGS_NARGS = new TinyMethod(Varargs.class, "narg");
	public static final TinyMethod VARARGS_ARG = new TinyMethod(Varargs.class, "arg", int.class);
	public static final TinyMethod VARARGS_SUBARGS = new TinyMethod(Varargs.class, "subargs", int.class);
	public static final TinyMethod VARARGS_OF = new TinyMethod(LuaValue.class, "varargsOf", LuaValue[].class);
	public static final TinyMethod LIST_OF = new TinyMethod(LuaValue.class, "listOf", LuaValue[].class);
	public static final TinyMethod TABLE_SET_STRING = new TinyMethod(LuaValue.class, "rawset", String.class, LuaValue.class);

	public static final TinyMethod API_MAKE_INSTANCE = new TinyMethod(APIClassLoader.class, "makeInstance", Object.class);
	public static final TinyMethod API_GET_TABLE = new TinyMethod(LuaObject.class, "getTable");
}
