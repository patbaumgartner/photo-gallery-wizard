package com.pabaumgartner.photogallery.wizard.tui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UiEventQueueTest {

	@Test
	void postedEventsOnlyRunWhenTheOwningThreadDrains() {
		UiEventQueue queue = new UiEventQueue();
		List<String> applied = new ArrayList<>();

		queue.post(() -> applied.add("first"));
		queue.post(() -> applied.add("second"));

		assertThat(applied).isEmpty();

		queue.drain();

		assertThat(applied).containsExactly("first", "second");
	}

	@Test
	void drainIsANoOpWhenNothingWasPosted() {
		UiEventQueue queue = new UiEventQueue();
		List<String> applied = new ArrayList<>();

		queue.drain();

		assertThat(applied).isEmpty();
	}

	@Test
	void eventsPostedFromAWorkerThreadRunOnTheDrainingThreadInOrder() throws InterruptedException {
		UiEventQueue queue = new UiEventQueue();
		List<Integer> applied = new ArrayList<>();
		List<String> runningThreads = new ArrayList<>();
		CountDownLatch posted = new CountDownLatch(1);

		Thread producer = new Thread(() -> {
			for (int i = 0; i < 100; i++) {
				int value = i;
				queue.post(() -> {
					applied.add(value);
					runningThreads.add(Thread.currentThread().getName());
				});
			}
			posted.countDown();
		}, "test-worker");
		producer.start();

		assertThat(posted.await(5, TimeUnit.SECONDS)).isTrue();
		producer.join();
		queue.drain();

		assertThat(applied).hasSize(100).isSorted();
		assertThat(runningThreads).containsOnly(Thread.currentThread().getName());
	}

	@Test
	void drainAppliesEventsPostedWhileEarlierEventsAreStillRunning() {
		UiEventQueue queue = new UiEventQueue();
		List<String> applied = new ArrayList<>();

		queue.post(() -> {
			applied.add("outer");
			queue.post(() -> applied.add("inner"));
		});

		queue.drain();

		assertThat(applied).containsExactly("outer", "inner");
	}

}
