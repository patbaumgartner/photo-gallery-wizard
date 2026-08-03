package com.pabaumgartner.photogallery.wizard.tui;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Hands state changes from background workers to the render thread. Everything the wizard
 * mutates is confined to that one thread, so no wizard state needs locking.
 */
final class UiEventQueue {

	private final Queue<Runnable> pending = new ConcurrentLinkedQueue<>();

	void post(Runnable event) {
		pending.add(event);
	}

	void drain() {
		Runnable event;
		while ((event = pending.poll()) != null) {
			event.run();
		}
	}

}
