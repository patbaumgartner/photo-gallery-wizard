package com.pabaumgartner.photogallery.wizard.config;

import java.util.List;

import dev.tamboui.tui.bindings.BindingSets;
import org.jline.utils.InfoCmp;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A missing native-image hint only shows up when the released binary refuses to start, so
 * the registrations are checked here instead.
 */
class NativeImageRuntimeHintsTest {

	private static final List<String> OPTIONAL_JLINE_TYPES = List.of("org.jline.terminal.impl.ffm.FfmTerminalProvider",
			"org.jline.terminal.impl.jni.JniTerminalProvider", "org.jline.nativ.JLineNativeLoader",
			"org.jline.nativ.Kernel32", "org.jline.terminal.impl.ffm.NativeWinSysTerminal",
			"org.jline.terminal.impl.ffm.NativeWinConsoleWriter");

	private RuntimeHints register() {
		RuntimeHints hints = new RuntimeHints();
		new NativeImageRuntimeHints().registerHints(hints, getClass().getClassLoader());
		return hints;
	}

	@Test
	void terminalResourcesAreRegistered() {
		RuntimeHints hints = register();

		assertThat(RuntimeHintsPredicates.resource().forResource("dev/tamboui/tui/bindings/default.properties"))
			.accepts(hints);
		assertThat(RuntimeHintsPredicates.resource().forResource("org/jline/utils/capabilities.txt")).accepts(hints);
		assertThat(RuntimeHintsPredicates.resource().forResource("META-INF/services/org/jline/terminal/provider/exec"))
			.accepts(hints);
	}

	@Test
	void reflectivelyUsedTypesAreRegistered() {
		RuntimeHints hints = register();

		assertThat(RuntimeHintsPredicates.reflection().onType(BindingSets.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onType(InfoCmp.class)).accepts(hints);
	}

	@Test
	void everyOptionalTypeThatCanBeLoadedIsRegistered() {
		RuntimeHints hints = register();

		List<String> loadable = OPTIONAL_JLINE_TYPES.stream().filter(this::classIsPresent).toList();

		assertThat(loadable).as("no optional JLine type resolves at all, so the hints below are untested").isNotEmpty();
		for (String type : loadable) {
			assertThat(RuntimeHintsPredicates.reflection().onType(TypeReference.of(type))).as("hint for %s", type)
				.accepts(hints);
		}
	}

	private boolean classIsPresent(String name) {
		try {
			Class.forName(name, false, getClass().getClassLoader());
			return true;
		}
		catch (ClassNotFoundException | LinkageError ex) {
			// Mirrors how Spring decides whether the hint applies: JLine ships some
			// providers as preview class files that the current JDK cannot load.
			return false;
		}
	}

}
