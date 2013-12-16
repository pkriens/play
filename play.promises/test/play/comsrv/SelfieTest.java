package play.comsrv;

import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;

import junit.framework.TestCase;

/**
 * This test uses the asynchronous Selfie server that is rather self centered,
 * it only sends messages to itself. It returns a promise on send that is
 * resolved when the message reaches the receiver. Kind of silly.
 */
public class SelfieTest extends TestCase {

	public void testSelfie() throws Exception {
		Selfie selfie = new Selfie();
		selfie.activate();

		Promise<String> send = selfie.send("Hello");
		assertEquals("HELLO", send.get());

		selfie.deactivate();
	}

	public void testSelfie2() throws Exception {
		final Selfie selfie = new Selfie();
		selfie.activate();
		Success<String, String> triple = new Success<String, String>() {

			@Override
			public Promise<String> call(Promise<String> promise)
					throws Exception {
				return selfie.send("***" + promise.get() + "***");
			}
		};
		String s = selfie.send("Hello").then(triple).then(triple).get();

		assertEquals("******HELLO******", s);
		selfie.deactivate();
	}
}
