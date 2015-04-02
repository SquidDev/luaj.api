package org.squiddev.luaj.api.builder;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

/**
 * Port of 1.8's Parameter class for 1.7
 */
public class Parameter implements AnnotatedElement {
	public final Annotation[] annotations;
	public final Class<?> type;

	public final Method method;

	public Parameter(Annotation[] annotations, Class<?> type, Method method) {
		this.annotations = annotations;
		this.type = type;
		this.method = method;
	}

	/**
	 * Get the type of this parameter
	 *
	 * @return The type of this parameter
	 */
	public Class<?> getType() {
		return type;
	}

	/**
	 * Get an array of parameters for a method
	 *
	 * @param m The method to extract from
	 * @return The parameters of this method
	 */
	public static Parameter[] getParameters(Method m) {
		Class<?>[] types = m.getParameterTypes();
		Annotation[][] annotations = m.getParameterAnnotations();

		int length = types.length;
		Parameter[] parameters = new Parameter[length];

		for (int i = 0; i < length; i++) {
			parameters[i] = new Parameter(annotations[i], types[i], m);
		}

		return parameters;
	}

	@Override
	public boolean isAnnotationPresent(Class<? extends Annotation> aClass) {
		return getAnnotation(aClass) != null;
	}

	/**
	 * Returns this element's annotation for the specified type if
	 * such an annotation is <em>present</em>, else null.
	 *
	 * @param annotationClass the Class object corresponding to the
	 *                        annotation type
	 * @return this element's annotation for the specified annotation type if
	 * present on this element, else null
	 * @throws NullPointerException if the given annotation class is null
	 * @since 1.5
	 */
	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		for (Annotation annon : annotations) {
			if (annotationClass.isInstance(annon)) {
				return annotationClass.cast(annon);
			}
		}

		return null;
	}

	/**
	 * Returns annotations that are <em>present</em> on this element.
	 * <p/>
	 * If there are no annotations <em>present</em> on this element, the return
	 * value is an array of length 0.
	 * <p/>
	 * The caller of this method is free to modify the returned array; it will
	 * have no effect on the arrays returned to other callers.
	 *
	 * @return annotations present on this element
	 * @since 1.5
	 */
	@Override
	public Annotation[] getAnnotations() {
		return getDeclaredAnnotations();
	}

	/**
	 * Returns annotations that are <em>directly present</em> on this element.
	 * This method ignores inherited annotations.
	 * <p/>
	 * If there are no annotations <em>directly present</em> on this element,
	 * the return value is an array of length 0.
	 * <p/>
	 * The caller of this method is free to modify the returned array; it will
	 * have no effect on the arrays returned to other callers.
	 *
	 * @return annotations directly present on this element
	 * @since 1.5
	 */
	@Override
	public Annotation[] getDeclaredAnnotations() {
		return annotations;
	}

	/**
	 * Return the {@code Method} which declares this parameter.
	 * <p/>
	 * Should return an Executable but that is only in 1.8
	 *
	 * @return The {@code Method} declaring this parameter.
	 */
	public Method getDeclaringExecutable() {
		return method;
	}
}
