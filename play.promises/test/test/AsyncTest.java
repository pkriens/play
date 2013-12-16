package test;

import java.util.concurrent.Semaphore;

import junit.framework.TestCase;

import org.osgi.service.async.Async;
import org.osgi.util.promise.Promise;

import play.async.impl.AsyncImpl;
import play.promises.server.ASyncFoo;
import play.promises.server.SyncFoo;
import play.promises.service.Foo;

public class AsyncTest extends TestCase {

	public void testDeferredCallback() throws Exception {
		Async async = new AsyncImpl();

		SyncFoo sf = new SyncFoo("sf");
		Foo msf = async.mediate(sf);
		
		Promise<String> p = async.hold(msf.foo(2));
		assertTrue( p.isDone());
		
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
		
		assertEquals( "sf", sf.foo(1));
		assertEquals( "sf", async.call(sf.foo(1)).get());
		assertEquals( "af", af.foo(1));
		assertEquals( "af", async.call(af.foo(1)).get());
		assertEquals( "sf", async.call(msf.foo(1)).get());
		assertEquals( "af", async.call(maf.foo(1)).get());
		
		System.out.println("done");
	}
}
