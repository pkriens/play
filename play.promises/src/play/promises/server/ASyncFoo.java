package play.promises.server;

import java.lang.reflect.Method;

import org.osgi.service.async.Async;
import org.osgi.service.async.AsyncDelegate;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import play.promises.service.Foo;
import aQute.bnd.annotation.component.Reference;

public class ASyncFoo extends SyncFoo implements AsyncDelegate<Foo> {
	Async async;

	public ASyncFoo(String id) {
		super(id);
	}


	@Reference
	public void setAsync(Async async) {
		this.async = async;
	}

	@Override
	public Promise<?> async(final Method m, final Object[] args) throws Exception {
		final Deferred<Object> deferred = new Deferred<>();
		System.out.println("Creating thread for " + id);
		Thread t = new Thread() {
			public void run() {
				try {
					System.out.println("Calling foo inside thread " + id);
					deferred.resolve(m.invoke(ASyncFoo.this, args));
				} catch (Throwable e) {
					deferred.fail(e);
				}
			}
		};
		t.start();

		return deferred.getPromise();
	}

}
