package play.tracker;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;

public class Watch implements BundleActivator {
	private BundleTracker<Bundle> tracker;

	@Override
	public void start(BundleContext context) {
		tracker = new BundleTracker<Bundle>(context, -1, null) {
			@Override
			public Bundle addingBundle(Bundle b, BundleEvent e) {
				System.out.println("Tracking bundle " + b.getSymbolicName());
				return b;
			}
		};
		tracker.open();
	}

	@Override
	public void stop(BundleContext context) {}

}
