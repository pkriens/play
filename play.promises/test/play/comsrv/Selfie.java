package play.comsrv;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Deactivate;

/**
 * A Rather stupid example that is using real 
 *
 */
public class Selfie extends Thread {
	DataOutputStream dout;
	DataInputStream din;
	AtomicInteger i = new AtomicInteger(1000);
	private Map<Integer, Deferred<String>> map = new ConcurrentHashMap<>();

	@Activate
	void activate() throws IOException {
		PipedInputStream in = new PipedInputStream();
		PipedOutputStream out = new PipedOutputStream(in);
		dout = new DataOutputStream(out);
		din = new DataInputStream(in);
		start();
	}

	@Deactivate
	void deactivate() throws IOException {
		interrupt();
	}

	/**
	 * There is an assumption that the buffer size of the streams >>>> the
	 * message.
	 * 
	 * @param s the message
	 * @return a Promise
	 * @throws Exception
	 */
	public synchronized Promise<String> send(String s) throws Exception {
		int id = i.getAndIncrement();
		Deferred<String> d = new Deferred<>();
		map.put(id, d);
		dout.writeInt(id);
		dout.writeUTF(s);
		dout.flush();
		return d.getPromise();
	}

	public void run() {
		try {
			while (!isInterrupted()) {
				int id = din.readInt();
				String s = din.readUTF();
				Deferred<String> d = map.remove(id);
				d.resolve(s.toUpperCase());
			}
		} catch (InterruptedIOException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
