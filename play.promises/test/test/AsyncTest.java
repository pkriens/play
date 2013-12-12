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

	public void testCallback() throws Exception {
		Async async = new AsyncImpl();

		ASyncFoo af = new ASyncFoo("af");
		af.setAsync(async);
		
		Foo maf = async.mediate(af);
		Promise<String> p = async.invoke(maf.foo(5));
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
		assertEquals( "sf", async.invoke(sf.foo(1)).get());
		assertEquals( "af", af.foo(1));
		assertEquals( "af", async.invoke(af.foo(1)).get());
		assertEquals( "sf", async.invoke(msf.foo(1)).get());
		assertEquals( "af", async.invoke(maf.foo(1)).get());
		
		System.out.println("done");
	}
}
