package test;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.osgi.util.promise.Failure;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Success;

import junit.framework.TestCase;

public class PromiseTest extends TestCase {
	static Timer timer = new Timer();

	/**
	 * Test if the chain calls can be deferred.
	 */

	public void testDeferredChain() throws Exception {
		Deferred<String> r = new Deferred<String>();
		final Promise<String> p1 = r.getPromise();
		final Semaphore s = new Semaphore(0);

		// Single callback. Will add one to s

		Success<String, String> doubler = new Success<String, String>() {

			@Override
			public Promise<String> call(String value)
					throws Exception {
				System.out.println("get : " + value);
				s.release();
				return Deferred.getDirectPromise(value + value);
			}
		};

		// Create a promise that is deferred

		final Promise<String> p2 = p1.hold();

		// Resolve it with some value. So normally we would
		// get callbacks immediately

		r.resolve("x");

		// Start a chain with 3 doublers. if they would
		// callback immediate (since the promise is resolved)
		// they would set the semaphore to 3

		p2.then(doubler).then(doubler).then(doubler);

		// See if this really has not happened.

		assertEquals(0, s.availablePermits());

		// Now execute any deferred callbacks.

		p2.launch();

		// And see if they have happend

		assertEquals(3, s.availablePermits());
	}

	/**
	 * Test that the onresolved callback is not executed until launch when in
	 * deferred mode.
	 */
	public void testDeferred() throws Exception {
		Deferred<String> r = new Deferred<String>();
		Promise<String> p1 = r.getPromise();
		final Semaphore s = new Semaphore(0);

		p1.hold().onresolve(new Runnable() {

			@Override
			public void run() {
				s.release();
			}
		});

		assertEquals(0, s.availablePermits());
		r.resolve("done");
		assertEquals(0, s.availablePermits());
		p1.launch();
		assertEquals(1, s.availablePermits());
		assertEquals("done", p1.get());
	}

	/**
	 * Test if we can parallelize the Promises
	 * 
	 * We use the same promise since that is perfectly possible.
	 */
	public void testParallel() throws Exception {
		Deferred<String> r = new Deferred<String>();
		Promise<String> p1 = r.getPromise();

		Promise<String[]> parallel = Promises.parallel(p1, p1, p1, p1, p1, p1);

		r.resolve("x");
		assertEquals("[x, x, x, x, x, x]", Arrays.toString(parallel.get()));
	}

	/**
	 * Test if we can get the errors when there is a chain. The idea is that you
	 * only specify the failure callback on the last
	 * {@link Promise#then(Success,Failure)} method. Any failures will bubble
	 * up.
	 * 
	 * @throws Exception
	 */
	public void testErrorsChain() throws Exception {
		Deferred<String> r = new Deferred<String>();
		final Promise<String> p1 = r.getPromise();
		final Semaphore s = new Semaphore(0);

		Success<String, String> doubler = new Success<String, String>() {

			@Override
			public Promise<String> call(String value)
					throws Exception {
				System.out.println(value);
				return Deferred.getDirectPromise(value + value);
			}
		};
		final Promise<String> p2 = p1.then(doubler).then(doubler).then(doubler);

		p2.onresolve(new Runnable() {

			@Override
			public void run() {
				try {
					if (p2.getError() != null)
						s.release();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		Exception e = new Exception("Y");
		assertEquals(0, s.availablePermits());
		r.fail(e);
		assertEquals(1, s.availablePermits());

		assertEquals(e, p2.getError());
	}

	/**
	 * Check if errors are properly transferred to the callbacks.
	 * 
	 * @throws Exception
	 */

	public void testErrors() throws Exception {
		Deferred<String> r = new Deferred<String>();
		final Promise<String> p1 = r.getPromise();
		final Semaphore s = new Semaphore(0);

		//
		// Check a chain bubble up
		//
		p1.then(null, new Failure<String>() {

			@Override
			public void fail(Promise<String> promise) throws Exception {
				s.release();
			}

		});

		//
		// And the normal resolve
		//
		p1.onresolve(new Runnable() {

			@Override
			public void run() {
				try {
					if (p1.getError() != null)
						s.release();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		// not resolved yet
		assertEquals(0, s.availablePermits());

		// resolve it with an error
		r.fail(new Exception("X"));
		assertEquals(2, s.availablePermits());
	}

	/**
	 * Check if a promise can be called after it has already called back. This
	 * is a common use case for promises. I.e. you create a promise and
	 * whenever someone needs the value he uses 'then' instead of directly getting
	 * the value. Does take some getting used to.
	 */
	public void testRepeat() throws Exception {
		Deferred<String> r = new Deferred<String>();
		Promise<String> p1 = r.getPromise();
		r.resolve("10");
		final Semaphore s = new Semaphore(0);

		p1.onresolve(new Runnable() {

			@Override
			public void run() {
				s.release();
			}
		});

		assertEquals(1, s.availablePermits());
		p1.onresolve(new Runnable() {

			@Override
			public void run() {
				s.release();
			}
		});

		assertEquals(2, s.availablePermits());

		Promise<Integer> p2 = p1.then(new Success<Integer, String>() {

			@Override
			public Promise<Integer> call(String value)
					throws Exception {
				s.release();

				return Deferred.getDirectPromise(Integer.parseInt(value));
			}
		});
		assertEquals(Integer.valueOf(10), p2.get());
	}

	/**
	 * Test the basic chaining functionality.
	 */
	public void testThen() throws Exception {
		Deferred<String> r = new Deferred<String>();
		Promise<String> p1 = r.getPromise();
		Promise<Integer> p2 = p1.then(new Success<Integer, String>() {

			@Override
			public Promise<Integer> call(final String value)
					throws Exception {
				return async(value);
			}

		});

		assertFalse(p1.isDone());
		assertFalse(p2.isDone());

		r.resolve("20");
		assertTrue(p1.isDone());
		assertFalse(p2.isDone());

		assertEquals(new Integer(20), p2.get());
	}

	private Promise<Integer> async(final String value) {
		final Deferred<Integer> n = new Deferred<>();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				try {
					n.resolve(Integer.parseInt(value));
				} catch (Exception e) {
					n.fail(e);
				}
			}
		}, 500);

		return n.getPromise();
	}

	/**
	 * Most simple basic test
	 */
	public void testSimple() throws Exception {
		Deferred<String> r = new Deferred<String>();
		Promise<String> p = r.getPromise();
		final Semaphore s = new Semaphore(0);

		p.onresolve(new Runnable() {

			@Override
			public void run() {
				s.release();
			}
		});

		assertEquals(0, s.availablePermits());
		assertFalse(p.isDone());

		r.resolve("Hello");
		assertTrue(p.isDone());
		assertEquals(1, s.availablePermits());
		assertEquals("Hello", p.get());
	}
}
