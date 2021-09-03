package de.lukaspanneke.bachelor.help;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Timeouts for Callables returning when interrupted.
 * The Callable will have to return shortly after being interrupted.
 */
public class InteruptableTask<V> implements Runnable {
	private final Callable<V> task;
	boolean done;
	private V value;
	private Throwable e;

	private InteruptableTask(Callable<V> task) {
		this.task = task;
	}

	@Override
	public void run() {
		this.done = false;
		this.e = null;
		this.value = null;
		try {
			this.value = this.task.call();
		} catch (Throwable e) {
			this.e = e;
		} finally {
			synchronized (this) {
				this.done = true;
			}
		}
	}

	public V getValue() throws ExecutionException {
		synchronized (this) {
			if (!this.done) {
				throw new IllegalStateException();
			}
		}
		if (this.e != null) {
			throw new ExecutionException(this.e);
		}
		return this.value;
	}

	public synchronized boolean isDone() {
		return this.done;
	}

//	public static <V> V callWithTimeout(Callable<V> task, Duration duration) throws
//			ExecutionException, /* if the task threw an exception */
//			InterruptedException /* if the current thread is interrupted, while waiting for the task to finish */ {
//		InteruptableTask<V> timeTask = new InteruptableTask<>(task);
//		Thread thread = new Thread(timeTask);
//		thread.start();
//		/* first wait for the task to finish because the task is completed */
//		thread.join(duration.toMillis());
//		/* if the task does not complete, interrupt it. The task will have to give an incomplete answer */
//		if (!timeTask.isDone()) {
//			thread.interrupt();
//		}
//		/* wait for the task give the incomplete answer. this should not take long! */
//		thread.join();
//		/* not the task is done. either because of normal termination, or because it was asked to give an incomplete answer. in both cases an answer is given. */
//		return timeTask.getValue();
//	}

	public static <V> V interruptAfterTimeout(Callable<V> task, Duration duration) throws
			ExecutionException /* if the task threw an exception */ {
		InteruptableTask<V> timeTask = new InteruptableTask<>(task);
		final Thread runner = Thread.currentThread();
		Thread interrupter = new Thread(() -> {
			try {
				Thread.sleep(duration.toMillis());
				if (!timeTask.isDone()) {
					runner.interrupt();
				}
			} catch (InterruptedException interruptedException) {
				/* the interrupter thread can be interrupted, to signal, that the task is done and need not be interrupted anymore. */
			}
		});
		interrupter.start();
		timeTask.run();
		interrupter.interrupt();

		return timeTask.getValue();
	}
}
