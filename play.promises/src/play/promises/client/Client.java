package play.promises.client;

import org.osgi.service.async.Async;
import org.osgi.util.promise.Failure;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;

import play.promises.server.ASyncFoo;
import play.promises.server.SyncFoo;
import play.promises.service.Foo;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

@Component
public class Client {
	Async async;
	Foo actualAsync;
	Foo unmediatedAsync;
	Foo actualSync;
	Foo unmediatedSync;
	Failure<String> failure = new Failure<String>() {

		@Override
		public void fail(Promise<String> promise) throws Exception {
			System.out.println("Shit happens " + promise.getError());
		}

	};

	public void activate() throws Exception {
		this.actualSync = async.mediate(unmediatedSync);
		this.actualAsync = async.mediate(unmediatedAsync);

		System.out.println("Before");
		Promise<String> p = async.invoke(actualAsync.foo(2));
		p.then(new Success<String, String>() {

			@Override
			public Promise<String> call(Promise<String> promise) throws Exception {
				System.out.println("In ");
				System.out.println( promise.get());
				return async.invoke(actualAsync.foo(4));
			}
		}, failure);
		System.out.println("After");
		System.out.println( "Get " + p.get());

	}

	@Reference
	void setAsync(Async async) {
		this.async = async;
	}

	@Reference
	void setSyncFoo(SyncFoo foo) {
		this.actualSync = foo;
	}

	@Reference
	void setASyncFoo(ASyncFoo foo) {
		this.actualAsync = foo;
	}
}
