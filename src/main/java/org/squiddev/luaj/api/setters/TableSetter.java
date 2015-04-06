package org.squiddev.luaj.api.setters;

import org.luaj.vm2.LuaTable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.squiddev.luaj.api.builder.APIBuilder;
import org.squiddev.luaj.api.builder.IInjector;
import org.squiddev.luaj.api.builder.tree.LuaField;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETFIELD;

/**
 * Sets the field to be the API's table
 */
public class TableSetter implements IInjector<LuaField> {
	protected static final String TYPE_TABLE = Type.getDescriptor(LuaTable.class);

	/**
	 * Inject bytecode into a visitor
	 *
	 * @param visitor The method visitor to inject into
	 * @param field   The field that we are injecting with
	 */
	@Override
	public void inject(MethodVisitor visitor, LuaField field) {
		if (!field.field.getType().isAssignableFrom(LuaTable.class)) {
			throw new APIBuilder.BuilderException("Cannot convert " + field.field.getType().getName() + " to LuaTable", field);
		}

		visitor.visitVarInsn(ALOAD, 0);
		visitor.visitFieldInsn(GETFIELD, field.klass.name, "table", TYPE_TABLE);
	}
}
