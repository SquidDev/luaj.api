package org.squiddev.luaj.api.builder.generator;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.squiddev.luaj.api.LuaObject;
import org.squiddev.luaj.api.builder.BuilderSettings;
import org.squiddev.luaj.api.builder.tree.LuaClass;
import org.squiddev.luaj.api.builder.tree.LuaField;
import org.squiddev.luaj.api.builder.tree.LuaMethod;

import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;
import static org.squiddev.luaj.api.builder.BuilderConstants.*;
import static org.squiddev.luaj.api.utils.AsmUtils.constantOpcode;


/**
 * Builds ASM code to call an API
 */
public abstract class ClassBuilder {
	/**
	 * The {@link ClassWriter} for the wrapper class
	 */
	protected final ClassWriter writer;

	/**
	 * The name of the original class
	 *
	 * @see #klass
	 */
	public final String originalName;

	/**
	 * The whole name of the original name ('L' + ... + ';')
	 */
	public final String originalWhole;

	/**
	 * The name of the generated class
	 */
	public final String className;

	/**
	 * Settings for class generation
	 */
	public final BuilderSettings settings;

	/**
	 * Data about the class
	 */
	public final LuaClass klass;

	/**
	 * Create a new {@link ClassBuilder}
	 *
	 * @param name  The name of the generated class
	 * @param klass The class we build a wrapper around
	 */
	public ClassBuilder(String name, LuaClass klass) {
		this.klass = klass;
		this.settings = klass.settings;
		className = name;

		originalName = Type.getInternalName(klass.klass);
		originalWhole = Type.getDescriptor(klass.klass);

		writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

		write();
	}

	/**
	 * Write everything!
	 */
	protected void write() {
		// Declare class name
		writer.visit(V1_6, ACC_PUBLIC + ACC_SUPER, className, null, Type.getInternalName(settings.parentClass), null);

		// Declare METHOD_NAMES
		writer.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, METHOD_NAMES, METHOD_NAMES_SIGNATURE, null, null).visitEnd();

		// Declare NAMES
		writer.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, NAMES, NAMES_SIGNATURE, null, null).visitEnd();

		// LOADER is setup in the class loader. This allows us to load other classes
		writer.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, LOADER, CLASS_LOADER, null, null).visitEnd();

		writeInit();
		writeStaticInit();
		writeSetup();
		writeInvoke();

		writer.visitEnd();
	}

	/**
	 * Write the static constructor
	 *
	 * This constructs the array of names, array of array method names and also sets up the class loader
	 */
	protected void writeStaticInit() {
		MethodVisitor mv = writer.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
		mv.visitCode();

		constantOpcode(mv, klass.methods.size());
		mv.visitTypeInsn(ANEWARRAY, "[Ljava/lang/String;");

		int counter = 0;
		for (LuaMethod m : klass.methods) {
			// For key <counter>
			mv.visitInsn(DUP);
			constantOpcode(mv, counter);

			Set<String> names = m.names;

			// Create an array of length <names.length>
			constantOpcode(mv, names.size());
			mv.visitTypeInsn(ANEWARRAY, "java/lang/String");

			int nameCounter = 0;
			for (String name : names) {
				mv.visitInsn(DUP);
				constantOpcode(mv, nameCounter);
				mv.visitLdcInsn(name);
				mv.visitInsn(AASTORE);

				++nameCounter;
			}

			// And store
			mv.visitInsn(AASTORE);

			++counter;
		}

		mv.visitFieldInsn(PUTSTATIC, className, METHOD_NAMES, METHOD_NAMES_SIGNATURE);

		// Visit names
		if (klass.names.size() == 0) {
			mv.visitInsn(ACONST_NULL);
		} else {
			constantOpcode(mv, klass.names.size());
			mv.visitTypeInsn(ANEWARRAY, "java/lang/String");

			counter = 0;
			for (String name : klass.names) {
				mv.visitInsn(DUP);
				constantOpcode(mv, counter);
				mv.visitLdcInsn(name);
				mv.visitInsn(AASTORE);

				++counter;
			}
		}

		mv.visitFieldInsn(PUTSTATIC, className, NAMES, NAMES_SIGNATURE);

		// Setup the class loader
		mv.visitLdcInsn(Type.getType("L" + className + ";"));
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
		mv.visitTypeInsn(CHECKCAST, TYPE_LOADER);
		mv.visitFieldInsn(PUTSTATIC, className, LOADER, CLASS_LOADER);

		mv.visitInsn(RETURN);

		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	/**
	 * Write the constructor. This calls the parent constructor,
	 * sets the instance and sets the method names to be the static field
	 */
	protected void writeInit() {
		MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", "(" + originalWhole + ")V", null, null);
		mv.visitCode();

		// Parent constructor with argument
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(settings.parentClass), "<init>", "(Ljava/lang/Object;)V", false);

		writeInitFields(mv);

		// And return
		mv.visitInsn(RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	/**
	 * Write the fields to set in a
	 *
	 * @param mv The constructor's method visitor
	 */
	protected void writeInitFields(MethodVisitor mv) {
		// Set method names
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETSTATIC, className, METHOD_NAMES, METHOD_NAMES_SIGNATURE);
		mv.visitFieldInsn(PUTFIELD, className, "methodNames", METHOD_NAMES_SIGNATURE);

		// Set method API names
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETSTATIC, className, NAMES, NAMES_SIGNATURE);
		mv.visitFieldInsn(PUTFIELD, className, "names", NAMES_SIGNATURE);
	}

	/**
	 * Write an override to the
	 * {@link LuaObject#setup()} method
	 */
	protected void writeSetup() {
		Set<LuaField> fields = klass.fields;
		if (fields.size() == 0) return;

		MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "setup", "()V", null, null);
		mv.visitCode();

		// Parent setup
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(settings.parentClass), "setup", "()V", false);

		for (LuaField field : fields) {
			field.setup.inject(mv, field);

			// Load instance
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, className, "instance", "Ljava/lang/Object;");
			mv.visitTypeInsn(CHECKCAST, originalName);
			mv.visitInsn(SWAP);
			mv.visitFieldInsn(PUTFIELD, originalName, field.field.getName(), Type.getDescriptor(field.field.getType()));
		}

		// And return
		mv.visitInsn(RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

	}

	/**
	 * Write the invoke function
	 */
	protected abstract void writeInvoke();

	/**
	 * Create the builder for a method
	 *
	 * @param method The method to write
	 * @return The builder to use
	 */
	public abstract MethodBuilder createBuilder(LuaMethod method);

	/**
	 * Write this class and any additional classes required
	 *
	 * @param extras Extra classes to populate with
	 * @return The current bytes
	 */
	public byte[] writeClasses(Map<String, byte[]> extras) {
		return writer.toByteArray();
	}
}
