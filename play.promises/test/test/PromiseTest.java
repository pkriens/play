package test;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;
import org.osgi.util.promise.Resolver;
import org.osgi.util.promise.Success;

import junit.framework.TestCase;

public class PromiseTest extends TestCase {
	static Timer timer = new Timer();

	
	
	public void testParallel() throws Exception {
		Resolver<String> r = new Resolver<String>();
		Promise<String> p1 = r.getPromise();
		
		Promise<String[]> parallel = Promises.parallel(p1,p1,p1,p1,p1,p1);
		
		r.resolve("x");
		assertEquals("[x, x, x, x, x, x]", Arrays.toString(parallel.get()));
	}
	
	
	
	public void testErrorsChain() throws Exception {
		Resolver<String> r = new Resolver<String>();
		final Promise<String> p1 = r.getPromise();
		final Semaphore s = new Semaphore(0);

		Success<String, String> doubler = new Success<String, String>() {

			@Override
			public Promise<String> call(Promise<String> promise)
					throws Exception {
				System.out.println(promise.get());
				return Resolver.getDirectPromise(promise.get() + promise.get());
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

		assertEquals(0, s.availablePermits());
		r.fail(new Exception("Y"));
		assertEquals(1, s.availablePermits());
	}

	public void testErrors() throws Exception {
		Resolver<String> r = new Resolver<String>();
		final Promise<String> p1 = r.getPromise();
		final Semaphore s = new Semaphore(0);

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
		assertEquals(0, s.availablePermits());
		r.fail(new Exception("X"));
		assertEquals(1, s.availablePermits());
	}

	public void testRepeat() throws Exception {
		Resolver<String> r = new Resolver<String>();
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
			public Promise<Integer> call(Promise<String> promise)
					throws Exception {
				s.release();

				return Resolver.getDirectPromise(Integer.parseInt(promise.get()));
			}
		});
		assertEquals(Integer.valueOf(10), p2.get());
	}

	public void testThen() throws Exception {
		Resolver<String> r = new Resolver<String>();
		Promise<String> p1 = r.getPromise();
		Promise<Integer> p2 = p1.then(new Success<Integer, String>() {

			@Override
			public Promise<Integer> call(final Promise<String> promise)
					throws Exception {
				return async(promise.get());
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
		final Resolver<Integer> n = new Resolver<>();
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

	public void testSimple() throws Exception {
		Resolver<String> r = new Resolver<String>();
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
