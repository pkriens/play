package play.promises.server;

import play.promises.service.Foo;

public class SyncFoo implements Foo {

	private String id;

	public SyncFoo(String id) {
		this.id = id;
	}

	public String foo(int delay) {
		try {
			System.out.println("doing " + delay + " for " + id);
			Thread.sleep(delay * 1000);
			System.out.println("done " + id);
		} catch (InterruptedException e) {
		}
		return id;
	}

}
