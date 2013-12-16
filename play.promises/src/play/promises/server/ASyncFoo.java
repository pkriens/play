package play.promises.server;

import org.osgi.service.async.Async;
import org.osgi.util.promise.Deferred;

import aQute.bnd.annotation.component.Reference;

public class ASyncFoo extends SyncFoo {
	Async async;

	public ASyncFoo( String id) {
		super(id);
	}

	@Override
	public String foo(final int delay) {
		final Deferred<String> resolver = async.createDeferred();
		if (resolver == null)
			return super.foo(delay);

		System.out.println("Creating thread for " + id);
		Thread t = new Thread() {
			public void run() {
				try {
					System.out.println("Calling foo inside thread " + id);
					String n = ASyncFoo.super.foo(delay);
					resolver.resolve(n);
				} catch (Exception e) {
					resolver.fail(e);
				}
			}
		};
		t.start();

		System.out.println("Async return for " + id);
		return null;
	}

	@Reference
	public
	void setAsync(Async async) {
		this.async = async;
	}

}
