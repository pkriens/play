package test;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.osgi.service.async.Async;
import org.osgi.util.promise.Promise;

import play.async.impl.AsyncImpl;
import play.promises.server.ASyncFoo;
import play.promises.server.SyncFoo;
import play.promises.service.Foo;

public class AsyncTest extends TestCase {

	public void testHoldCallback() throws Exception {
		Async async = new AsyncImpl();

		SyncFoo sf = new SyncFoo("sf");
		Foo msf = async.mediate(sf);
		
		Promise<String> p = async.call(msf.foo(2));
		assertTrue( p.isDone());
		p.hold();
		final Semaphore s = new Semaphore(0);
		
		p.onresolve(new Runnable(){

			@Override
			public void run() {
				s.release();
			}});

		assertEquals(0, s.availablePermits());

		p.launch();
		assertEquals(1, s.availablePermits());
		
		assertEquals( "sf", p.get());
		
		System.out.println("done");
	}

	public void testCallback() throws Exception {
		Async async = new AsyncImpl();

		ASyncFoo af = new ASyncFoo("af");
		af.setAsync(async);

		Foo maf = async.mediate(af);
		
		Promise<String> p = async.call(maf.foo(5));
		final Semaphore s = new Semaphore(0);
		
		p.onresolve(new Runnable(){

			@Override
			public void run() {
				s.release();
			}});

		s.acquire();
		assertEquals( "af", p.get());
		
		System.out.println("done");
	}
	
	public void testSimple() throws Exception {
		Async async = new AsyncImpl();

		SyncFoo sf = new SyncFoo("sf");
		ASyncFoo af = new ASyncFoo("af");
		af.setAsync(async);
		
		Foo msf = async.mediate(sf);
		Foo maf = async.mediate(af);
		
		assertEquals( "sf", async.call(msf.foo(1)).get());
		assertEquals( "af", async.call(maf.foo(1)).get());
		
		assertEquals( "sf", sf.foo(1));
		assertEquals( "sf", async.call(sf.foo(1)).get());
		assertEquals( "af", af.foo(1));
		assertEquals( "af", async.call(af.foo(1)).get());
		
		System.out.println("done");
	}
	
	
	/**
	 * Demonstrate an issue with nested asynchronous calls
	 * 
	 * If a service makes a call to an asynchronous one within the
	 * scope of a mediated call then it usurps the returned promise.
	 * 
	 * In this test the async part returns "async". The overall service
	 * is supposed to return "nested async", but doesn't because the
	 * async service creates a deferred which overrides the promise. 
	 */
	public void testNested() throws Exception {
		Async async = new AsyncImpl();

		/* A simple AsyncFoo that will be delegated to */
		final ASyncFoo delegate = new ASyncFoo("async");
		delegate.setAsync(async);
				
		/* A delegating foo implementation that prepends "nested " to the result */
		Foo delegating = new Foo() {
			@Override
			public String foo(int delay) {
				// this method is called async, so the thread is setup
				// to do things async, but we do not expect our child
				// to pickup the async thread.
				return "nested "+ delegate.foo(delay);
			}
		};
		
		/* Calling the real service should give "nested async" */
		assertEquals( "nested async", delegating.foo(1));

		Foo msf = async.mediate(delegating);
		
		Promise<String> p = async.call(msf.foo(2));
		p.hold();
		final Semaphore s = new Semaphore(0);
		
		p.onresolve(new Runnable(){

			@Override
			public void run() {
				s.release();
			}});

		p.launch();
		s.tryAcquire(3, TimeUnit.SECONDS);
		
		/* This fails because the async usurps the return */
		assertEquals( "nested async", p.get());
		
		System.out.println("done");
	}
	
}