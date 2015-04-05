package org.squiddev.luaj.api.transformer;

import org.squiddev.luaj.api.builder.LuaMethod;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * Transforms various items
 */
public class Transformer {
	public Map<Class<? extends Annotation>, AnnotationWrapper<LuaMethod, ? extends Annotation>> methodTransformers = new HashMap<>();
	public Map<Class<? extends Annotation>, AnnotationWrapper<LuaMethod.LuaArgument, ? extends Annotation>> argumentTransformers = new HashMap<>();

	public <A extends Annotation> void addMethodTransformer(Class<A> annotation, ITransformer<LuaMethod, A> transformer) {
		methodTransformers.put(annotation, new AnnotationWrapper<>(transformer));
	}

	public <A extends Annotation> void addArgumentTransformer(Class<A> annotation, ITransformer<LuaMethod.LuaArgument, A> transformer) {
		argumentTransformers.put(annotation, new AnnotationWrapper<>(transformer));
	}

	public void transform(LuaMethod method) {
		for (Annotation annotation : method.method.getAnnotations()) {
			AnnotationWrapper<LuaMethod, ? extends Annotation> transformer = methodTransformers.get(annotation.annotationType());
			if (transformer != null) {
				transformer.transform(method, annotation);
			}
		}

		for (LuaMethod.LuaArgument arg : method.arguments) {
			for (Annotation annotation : arg.parameter.getAnnotations()) {
				AnnotationWrapper<LuaMethod.LuaArgument, ? extends Annotation> transformer = argumentTransformers.get(annotation.annotationType());
				if (transformer != null) {
					transformer.transform(arg, annotation);
				}
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
