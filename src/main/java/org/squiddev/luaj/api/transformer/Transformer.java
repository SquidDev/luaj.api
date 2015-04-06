package org.squiddev.luaj.api.transformer;

import org.squiddev.luaj.api.builder.tree.LuaArgument;
import org.squiddev.luaj.api.builder.tree.LuaClass;
import org.squiddev.luaj.api.builder.tree.LuaField;
import org.squiddev.luaj.api.builder.tree.LuaMethod;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * Transforms various items
 */
public class Transformer {
	public Map<Class<? extends Annotation>, AnnotationWrapper<LuaClass, ? extends Annotation>> classTransformers = new HashMap<>();
	public Map<Class<? extends Annotation>, AnnotationWrapper<LuaMethod, ? extends Annotation>> methodTransformers = new HashMap<>();
	public Map<Class<? extends Annotation>, AnnotationWrapper<LuaArgument, ? extends Annotation>> argumentTransformers = new HashMap<>();
	public Map<Class<? extends Annotation>, AnnotationWrapper<LuaField, ? extends Annotation>> fieldTransformers = new HashMap<>();

	public <A extends Annotation> void addClassTransformer(Class<A> annotation, ITransformer<LuaClass, A> transformer) {
		classTransformers.put(annotation, new AnnotationWrapper<>(transformer));
	}

	public <A extends Annotation> void addMethodTransformer(Class<A> annotation, ITransformer<LuaMethod, A> transformer) {
		methodTransformers.put(annotation, new AnnotationWrapper<>(transformer));
	}

	public <A extends Annotation> void addArgumentTransformer(Class<A> annotation, ITransformer<LuaArgument, A> transformer) {
		argumentTransformers.put(annotation, new AnnotationWrapper<>(transformer));
	}

	public <A extends Annotation> void addFieldTransformer(Class<A> annotation, ITransformer<LuaField, A> transformer) {
		fieldTransformers.put(annotation, new AnnotationWrapper<>(transformer));
	}

	public void transform(LuaClass klass) {
		for (Annotation annotation : klass.klass.getAnnotations()) {
			AnnotationWrapper<LuaClass, ? extends Annotation> transformer = classTransformers.get(annotation.annotationType());
			if (transformer != null) {
				transformer.transform(klass, annotation);
			}
		}
	}

	public void transform(LuaMethod method) {
		for (Annotation annotation : method.method.getAnnotations()) {
			AnnotationWrapper<LuaMethod, ? extends Annotation> transformer = methodTransformers.get(annotation.annotationType());
			if (transformer != null) {
				transformer.transform(method, annotation);
			}
		}
	}

	public void transform(LuaArgument arg) {
		for (Annotation annotation : arg.parameter.getAnnotations()) {
			AnnotationWrapper<LuaArgument, ? extends Annotation> transformer = argumentTransformers.get(annotation.annotationType());
			if (transformer != null) {
				transformer.transform(arg, annotation);
			}
		}
	}

	public void transform(LuaField field) {
		for (Annotation annotation : field.field.getAnnotations()) {
			AnnotationWrapper<LuaField, ? extends Annotation> transformer = fieldTransformers.get(annotation.annotationType());
			if (transformer != null) {
				transformer.transform(field, annotation);
			}
		}
	}

	private static class AnnotationWrapper<T, A extends Annotation> {
		public final ITransformer<T, A> transformer;

		private AnnotationWrapper(ITransformer<T, A> transformer) {
			this.transformer = transformer;
		}

		@SuppressWarnings("unchecked")
		public void transform(T item, Annotation annotation) {
			transformer.transform(item, (A) annotation);
		}
	}
}
