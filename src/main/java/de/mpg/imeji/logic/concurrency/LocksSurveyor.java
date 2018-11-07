package de.mpg.imeji.logic.concurrency;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Thread checking periodically if some {@link Lock} needs to be unlocked
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class LocksSurveyor extends Thread {
	private static final Logger LOGGER = LogManager.getLogger(LocksSurveyor.class);
	private boolean signal = false;
	private boolean running = false;

	@Override
	public void run() {
		LOGGER.info("Lock Surveyor started: " + getName());
		Locks.reset();
		running = true;
		while (!signal) {
			try {
				synchronized (LOGGER) {
					final List<Lock> list = new ArrayList<Lock>(Locks.getExpiredLocks());
					if (!Locks.getExpiredLocks().isEmpty()) {
						LOGGER.info("Unlocking dead locks...");
						for (final Lock l : Locks.getExpiredLocks()) {
							list.add(l);
						}
						for (final Lock l : list) {
							LOGGER.info("on " + l.getUri() + " by " + l.getEmail());
							Locks.unLock(l);
						}
					}
					// wait a bit...
					LOGGER.wait(10000);
					// Thread.sleep(10000);
				}
			} catch (final NegativeArraySizeException e) {
				Locks.reset();
				LOGGER.error("Locks have been reinitialized. All locks have been released: ", e);
			} catch (final Exception e) {
				LOGGER.error("Locks Surveyor encountered a problem: ", e);
			}
		}
		LOGGER.warn("Lock Surveyor stopped. It should not occurs if application is still running!");
		running = false;
	}

	/**
	 * End the {@link Thread}
	 */
	public void terminate() {
		LOGGER.warn("Locks surveyor signaled to terminate!");
		signal = true;
		while (running) {
			LOGGER.debug("Waiting for LocksSurveyor to stop...");
		}
	}
}
