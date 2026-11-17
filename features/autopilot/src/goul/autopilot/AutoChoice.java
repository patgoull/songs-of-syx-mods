package goul.autopilot;

import java.util.ArrayList;

import snake2d.LOG;

/**
 * Popups that stop the game until something is clicked (battles, offers) take their default choice here instead, one
 * update later as a click would come. Only with -Dgoul.autopilot; used by the copied vanilla classes.
 */
public final class AutoChoice {

	private static final boolean ON = System.getProperty("goul.autopilot") != null;
	private static final ArrayList<Runnable> pending = new ArrayList<>();

	private AutoChoice() {

	}

	public static boolean on() {
		return ON;
	}

	/** Queues the choice and logs it. Returns false without the autopilot: the caller then shows its popup. */
	public static boolean later(String what, Runnable choice) {
		if (!ON)
			return false;
		LOG.ln("GOUL auto: " + what);
		pending.add(choice);
		return true;
	}

	public static boolean busy() {
		return !pending.isEmpty();
	}

	/** Called by the autopilot every update: runs one queued choice */
	static void run() {
		if (pending.isEmpty())
			return;
		pending.remove(0).run();
	}

}
