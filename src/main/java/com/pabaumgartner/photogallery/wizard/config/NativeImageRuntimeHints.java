package com.pabaumgartner.photogallery.wizard.config;

import dev.tamboui.tui.bindings.BindingSets;
import org.jline.terminal.impl.DumbTerminalProvider;
import org.jline.terminal.impl.exec.ExecTerminalProvider;
import org.jline.utils.InfoCmp;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class NativeImageRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		// TamboUI key-binding property files
		hints.resources().registerPattern("dev/tamboui/tui/bindings/*.properties");

		// JLine terminal capabilities and properties
		hints.resources().registerPattern("org/jline/utils/*.caps");
		hints.resources().registerPattern("org/jline/utils/*.txt");
		hints.resources().registerPattern("org/jline/jansi/*");
		hints.resources().registerPattern("org/jline/nativ/**");

		// JLine terminal provider service discovery
		hints.resources().registerPattern("META-INF/services/org/jline/terminal/provider/*");

		// JLine and TamboUI classes that use reflection
		hints.reflection().registerType(BindingSets.class);
		hints.reflection().registerType(InfoCmp.class);

		// JLine terminal providers (instantiated via reflection during provider
		// discovery)
		hints.reflection().registerType(DumbTerminalProvider.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		hints.reflection().registerType(ExecTerminalProvider.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		hints.reflection()
			.registerTypeIfPresent(classLoader, "org.jline.terminal.impl.ffm.FfmTerminalProvider",
					MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		hints.reflection()
			.registerTypeIfPresent(classLoader, "org.jline.terminal.impl.jni.JniTerminalProvider",
					MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

		// JLine native library loader (uses reflection for platform detection)
		hints.reflection()
			.registerTypeIfPresent(classLoader, "org.jline.nativ.JLineNativeLoader",
					MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.ACCESS_DECLARED_FIELDS);

		// JLine JNI Kernel32 bindings (Windows terminal support via JNI)
		hints.reflection()
			.registerTypeIfPresent(classLoader, "org.jline.nativ.Kernel32", MemberCategory.ACCESS_DECLARED_FIELDS);

		// JLine FFM Windows terminal support
		hints.reflection()
			.registerTypeIfPresent(classLoader, "org.jline.terminal.impl.ffm.NativeWinSysTerminal",
					MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_METHODS);
		hints.reflection()
			.registerTypeIfPresent(classLoader, "org.jline.terminal.impl.ffm.NativeWinConsoleWriter",
					MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
	}

}
