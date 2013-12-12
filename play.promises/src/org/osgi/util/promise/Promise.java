package org.osgi.util.promise;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public interface Promise<T> {
	
	<R> Promise<R> then(Success<R,T> success, Failure<T> failure) throws Exception;
	<R> Promise<R> then(Success<R,T> success) throws Exception;
	void onresolve(Runnable done) throws Exception;
	
	/**
	 * Returns <tt>true</tt> if this task completed.
	 * 
	 * Completion may be due to normal termination, an exception, or
	 * cancellation -- in all of these cases, this method will return
	 * <tt>true</tt>.
	 * 
	 * @return <tt>true</tt> if this task completed
	 */
	boolean isDone();

	/**
	 * Waits if necessary for the computation to complete, and then retrieves
	 * its result.
	 * 
	 * @return the computed result
	 * @throws CancellationException
	 *             if the computation was cancelled
	 * @throws ExecutionException
	 *             if the computation threw an exception
	 * @throws InterruptedException
	 *             if the current thread was interrupted while waiting
	 */
	T get() throws Exception;
	
	Throwable getError() throws IllegalStateException, InterruptedException;

}
