package play.promises.server;

import play.promises.service.Foo;

public class SyncFoo implements Foo {

	protected String id;

	public SyncFoo(String id) {
		this.id = id;
	}

	public String foo(int delay) {
		try {
			System.out.println("foo("+id+") " + delay );
			Thread.sleep(delay * 1000);
			System.out.println("foo("+id+") " + delay + " done ");
		} catch (InterruptedException e) {
		}
		return id;
	}

}
