package play.promises.server;

import org.osgi.service.async.Async;
import org.osgi.util.promise.Resolver;

import aQute.bnd.annotation.component.Reference;

public class ASyncFoo extends SyncFoo {
	Async async;

	public ASyncFoo( String id) {
		super(id);
	}

	@Override
	public String foo(final int delay) {
		final Resolver<String> resolver = async.getResolver();
		if (resolver == null)
			return super.foo(delay);

		Thread t = new Thread() {
			public void run() {
				try {
					String n = ASyncFoo.super.foo(delay);
					resolver.resolve(n);
				} catch (Exception e) {
					resolver.fail(e);
				}
			}
		};
		t.start();

		return null;
	}

	@Reference
	void setAsync(Async async) {
		this.async = async;
	}

}
