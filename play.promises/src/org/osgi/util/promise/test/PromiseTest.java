package org.osgi.util.promise.test;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Resolver;
import org.osgi.util.promise.Success;

import junit.framework.TestCase;

public class PromiseTest extends TestCase {
	static Timer timer = new Timer();
	
	public void testThen() throws Exception {
		Resolver<String> r = new Resolver<String>();
		Promise<String> p1 = r.getPromise();
		Promise<Integer> p2 = p1.then( new Success<Integer,String>(){

			@Override
			public Promise<Integer> call(final Promise<String> promise)
					throws Exception {
				return async(promise.get());
			}

		});
		
		assertFalse( p1.isDone());
		assertFalse( p2.isDone());
		
		r.resolve("20");
		assertTrue( p1.isDone());
		assertFalse( p2.isDone());

		assertEquals( new Integer(20), p2.get());
	}
	
	private Promise<Integer> async(final String value) {
		final Resolver<Integer> n = new Resolver<>();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				try {
					n.resolve( Integer.parseInt(value));
				} catch (Exception e) {
					n.fail(e);
				}
			}}, 500);
		
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
