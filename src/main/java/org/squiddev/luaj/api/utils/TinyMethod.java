package org.squiddev.luaj.api.utils;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.objectweb.asm.Opcodes.*;

/**
 * Stores very basic data about a method so we can inject it
 */
public class TinyMethod {
	public final String className;
	public final String name;
	public final String signature;

	public final Boolean isStatic;
	public final int flags;

	/**
	 * Construct a TinyMethod
	 *
	 * @param className The class name the method is in
	 * @param name      The name of the method
	 * @param signature The signature of the method ()V
	 * @param isStatic  If the method is static
	 * @param flags     Flags this method has
	 */
	public TinyMethod(String className, String name, String signature, boolean isStatic, int flags) {
		this.className = className;
		this.name = name;
		this.signature = signature;
		this.isStatic = isStatic;
		this.flags = flags & ~ACC_ABSTRACT;
	}

	/**
	 * Construct a non-static TinyMethod
	 *
	 * @param className The class name the method is in
	 * @param name      The name of the method
	 * @param signature The signature of the method
	 */
	public TinyMethod(String className, String name, String signature) {
		this(className, name, signature, false, ACC_PUBLIC);
	}

	/**
	 * Construct a TinyMethod from a {@link Method}
	 *
	 * @param m The method to load
	 */
	public TinyMethod(Method m) {
		this(Type.getInternalName(m.getDeclaringClass()), m.getName(), Type.getMethodDescriptor(m), Modifier.isStatic(m.getModifiers()), m.getModifiers());
	}

	/**
	 * Find a method from a class and create a TinyMethod from it
	 *
	 * @param classObj   The class to find the method in
	 * @param methodName The method name
	 * @param args       The arguments for the method
	 * @throws IllegalArgumentException On validation failure
	 */
	public TinyMethod(Class<?> classObj, String methodName, Class<?>... args) {
		this(getMethod(classObj, methodName, args));
	}

	/**
	 * Create a new method from a class and signature
	 *
	 * @param klass     Declaring class
	 * @param name      Name of the method
	 * @param signature For this method
	 * @param flags     Flags this method has
	 */
	public TinyMethod(Class<?> klass, String name, String signature, int flags) {
		this(Type.getInternalName(klass), name, signature, false, flags);
	}

	private static Method getMethod(Class<?> classObj, String methodName, Class<?>... args) {
		try {
			return classObj.getMethod(methodName, args);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public void inject(MethodVisitor mv) {
		mv.visitMethodInsn(isStatic ? INVOKESTATIC : INVOKEVIRTUAL, className, name, signature, false);
	}

	public MethodVisitor create(ClassVisitor cv) {
		return cv.visitMethod(flags, name, signature, null, null);
	}
}
