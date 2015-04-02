package org.squiddev.luaj.api.conversion;

import org.objectweb.asm.MethodVisitor;
import org.squiddev.luaj.api.builder.APIBuilder;

/**
 * A bytecode item that can be injected into a {@link MethodVisitor}
 */
public interface IInjector {
	/**
	 * An injector that adds no bytecode
	 */
	IInjector VOID = new IInjector() {
		@Override
		public void inject(MethodVisitor mv, APIBuilder builder) {

		}
	};

	/**
	 * Inject this into a visitor
	 *
	 * @param mv      The visitor to inject into
	 * @param builder The API builder we are injecting with. This allows us to get class writing properties
	 */
	void inject(MethodVisitor mv, APIBuilder builder);
}
