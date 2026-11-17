package goul.autopilot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import game.GAME;
import game.time.TIME;
import init.constant.C;
import init.paths.PATHS;
import script.SCRIPT;
import settlement.main.SETT;
import settlement.room.main.RoomBlueprint;
import settlement.room.main.RoomBlueprintIns;
import settlement.room.main.RoomInstance;
import snake2d.CORE;
import snake2d.CoreStats;
import snake2d.LOG;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
import view.main.VIEW;
import view.subview.GameWindow;

/**
 * Test only. When the game is started with -Dgoul.autopilot (run.ps1 -Auto), the menu loads a save right away and this
 * script plays a fixed tour of the city: zoom levels by day, night, a pan and high speed. After each step it logs the
 * frame rate and frame times ("GOUL auto:" lines) and saves a screenshot, then it quits the game without saving.
 * Without the setting it does nothing and isn't offered in the game.
 */
public final class Autopilot implements SCRIPT {

	private static final String MODE = System.getProperty("goul.autopilot");

	@Override
	public CharSequence name() {
		return "Goul autopilot (test)";
	}

	@Override
	public CharSequence desc() {
		return "Test only: plays a fixed camera tour and quits, when started with run.ps1 -Auto.";
	}

	@Override
	public boolean isSelectable() {
		return false;
	}

	@Override
	public boolean forceInit() {
		return MODE != null;
	}

	@Override
	public SCRIPT_INSTANCE createInstance() {
		return new Tour();
	}

	private static abstract class Step {
		final String name;
		final double seconds;
		final boolean shot;
		final int zoom;

		/** @param zoom kept for the whole step (the mouse wheel over the window would change it), or -1 */
		Step(String name, double seconds, boolean shot, int zoom) {
			this.name = name;
			this.seconds = seconds;
			this.shot = shot;
			this.zoom = zoom;
		}

		void start() {
		}

		/** @param part 0 to 1 through the step */
		void tick(double part) {
		}
	}

	private static final class Tour implements SCRIPT_INSTANCE {

		private final Step[] steps;
		private int current = -1;
		private long stepStart;
		private long last;
		private int frames;
		private long worst;
		private double stepGame;
		private int shots;
		/** time of the last update, read by the watchdog thread */
		private volatile long alive;
		private final Path out;

		Tour() {
			String o = System.getProperty("goul.autopilot.out");
			out = o == null ? null : Paths.get(o);
			if ("sim".equals(MODE)) {
				steps = new Step[] { new Step("settle", 5, false, 1) {
					@Override
					void start() {
						GAME.SPEED.speedSet(1);
						center();
					}
				}, simWarm(), simStep() };
				return;
			}
			if ("save".equals(MODE)) {
				steps = new Step[] { new Step("settle", 5, false, 1) {
					@Override
					void start() {
						GAME.SPEED.speedSet(1);
						center();
					}
				}, saveStep(1), saveStep(2), saveStep(3) };
				return;
			}
			if ("look".equals(MODE)) {
				// the same spot at fixed sun positions of one day, for comparing how the game looks
				steps = new Step[] { new Step("settle", 6, false, 1) {
					@Override
					void start() {
						GAME.SPEED.speedSet(1);
						zoom(1);
						findSun();
					}
				}, sunStep("look-1-sunrise", 0.03), sunStep("look-2-morning", 0.12), sunStep("look-3-noon", 0.5),
						sunStep("look-4-afternoon", 0.82), sunStep("look-5-sunset", 0.97), sunStep("look-6-twilight", 1.06),
						sunStep("look-7-night", -1), sunStep("look-8-night-zoom0", -1) };
				return;
			}
			if ("load".equals(MODE)) {
				// load only: the timing line, a short settle for the first frames, then quit
				steps = new Step[] { new Step("settle", 3, false, 1) {
					@Override
					void start() {
						center();
					}
				} };
				return;
			}
			steps = new Step[] {
				new Step("settle", 8, false, 1) {
					@Override
					void start() {
						GAME.SPEED.speedSet(1);
						zoom(1);
					}
				},
				zoomStep("day-zoom0", 0),
				zoomStep("day-zoom1", 1),
				zoomStep("day-zoom2", 2),
				zoomStep("day-zoom3", 3),
				new Step("night-zoom3", 6, true, 3) {
					@Override
					void start() {
						night();
						zoom(3);
					}
				},
				zoomStep("night-zoom2", 2),
				new Step("night-pan-zoom3", 10, true, 3) {
					@Override
					void start() {
						zoom(3);
					}

					@Override
					void tick(double part) {
						double a = part * Math.PI * 2;
						int r = 80 * C.TILE_SIZE;
						window().centerAt(centerX() + (int) (Math.cos(a) * r), centerY() + (int) (Math.sin(a) * r));
					}
				},
				new Step("night-speed3-zoom3", 10, false, 3) {
					@Override
					void start() {
						center();
						GAME.SPEED.speedSet(3);
					}
				},
				simWarm(),
				simStep(),
			};
		}

		/** Saves into the output folder, never the game's saves, the way the autosave does (minified), and logs the time */
		private Step saveStep(int i) {
			return new Step("save-" + i, 4, false, 1) {
				@Override
				void start() {
					Path dir = out != null ? out : Paths.get(".");
					long t = System.nanoTime();
					Path p = GAME.saver().save(dir, "goul-save-test", true);
					double ms = (System.nanoTime() - t) / 1e6;
					long kb = 0;
					try {
						kb = p == null ? 0 : Files.size(p) / 1024;
					} catch (IOException e) {
					}
					LOG.ln(String.format(java.util.Locale.ROOT, "GOUL auto: save %d took %.0f ms, %d KB%s", i, ms, kb, p == null ? " (failed)" : ""));
				}
			};
		}

		/** The game ramps up to the set speed one step per update, and slows down when it can't keep up */
		private static Step simWarm() {
			return new Step("sim-warmup", 6, false, 1) {
				@Override
				void start() {
					center();
					GAME.SPEED.speedSet(250);
				}
			};
		}

		/** Simulation at the max speed (250x), at zoom 1 where drawing is cheap: "game x" is the speed reached */
		private static Step simStep() {
			return new Step("sim-speed250-zoom1", 15, false, 1) {
			};
		}

		private static Step zoomStep(String name, int zoom) {
			return new Step(name, 5, true, zoom) {
				@Override
				void start() {
					zoom(zoom);
				}
			};
		}

		private static GameWindow window() {
			return VIEW.s().getWindow();
		}

		/** The middle of the map: in the test city most of the town is there, both the part under the mountain (north) and outside */
		private static void center() {
			window().centerAt(centerX(), centerY());
		}

		private static int centerX() {
			if (focusX >= 0)
				return focusX * C.TILE_SIZE + C.TILE_SIZEH;
			return SETT.TWIDTH * C.TILE_SIZE / 2;
		}

		private static int centerY() {
			if (focusX >= 0)
				return focusY * C.TILE_SIZE + C.TILE_SIZEH;
			return SETT.THEIGHT * C.TILE_SIZE / 2;
		}

		/** the tile to look at instead of the middle of the map, from -Dgoul.autopilot.focus, or -1 */
		private static int focusX = -1;
		private static int focusY = -1;

		/** -Dgoul.autopilot.focus=<room key> (e.g. REFINER_SMELTER): the room of that type with the most workers */
		private static void findFocus() {
			String key = System.getProperty("goul.autopilot.focus");
			if (key == null || key.isEmpty())
				return;
			RoomInstance best = null;
			int bestW = -1;
			try {
				for (RoomBlueprint b : SETT.ROOMS().all()) {
					if (!key.equals(b.key) || !(b instanceof RoomBlueprintIns<?>))
						continue;
					for (RoomInstance ins : ((RoomBlueprintIns<?>) b).all()) {
						int w = ins.employees() == null ? 0 : ins.employees().employed();
						if (w > bestW) {
							best = ins;
							bestW = w;
						}
					}
				}
			} catch (Exception e) {
				LOG.ln("GOUL auto: focus failed: " + e);
			}
			if (best == null) {
				LOG.ln("GOUL auto: no room " + key + ", looking at the middle of the map");
				return;
			}
			focusX = best.body().cX();
			focusY = best.body().cY();
			LOG.ln("GOUL auto: focus " + key + " at " + focusX + "," + focusY + ", " + bestW + " workers");
		}

		private static void zoom(int z) {
			window().setZoomout(z);
			center();
		}

		/** The middle of the next night (scans the next two days in 1% steps) */
		private static void night() {
			double day = TIME.secondsPerDay();
			double now = TIME.currentSecond();
			int first = -1;
			int end = -1;
			for (int k = 1; k <= 200; k++) {
				TIME.set(now + day * k / 100);
				boolean n = TIME.light().nightIs();
				if (n && first < 0)
					first = k;
				if (!n && first >= 0) {
					end = k;
					break;
				}
			}
			if (first < 0) {
				TIME.set(now);
				LOG.ln("GOUL auto: no night found");
				return;
			}
			if (end < 0)
				end = 200;
			TIME.set(now + day * ((first + end) / 2) / 100);
		}

		/** game seconds of the next sunrise and the sunset after it, set by findSun */
		private static double sunrise = -1;
		private static double sunset = -1;

		/** Finds the next sunrise and the sunset after it (scans the next two days in 0.25% steps) and goes back to now */
		private static void findSun() {
			double day = TIME.secondsPerDay();
			double now = TIME.currentSecond();
			boolean wasNight = TIME.light().nightIs();
			for (int k = 1; k <= 800 && sunset < 0; k++) {
				double t = now + day * k / 400;
				TIME.set(t);
				boolean n = TIME.light().nightIs();
				if (wasNight && !n)
					sunrise = t;
				else if (!wasNight && n && sunrise >= 0)
					sunset = t;
				wasNight = n;
			}
			TIME.set(now);
			if (sunset < 0)
				LOG.ln("GOUL auto: no sunrise and sunset found");
		}

		/**
		 * @param part of the daylight after the sunrise found by findSun (0 sunrise, 1 sunset, above 1 the night after it
		 *             by daylight lengths), or -1 for the middle of that night. Zoom 1, or 0 when the name ends with zoom0.
		 */
		private static Step sunStep(String name, double part) {
			int z = name.endsWith("zoom0") ? 0 : 1;
			return new Step(name, 3, true, z) {
				@Override
				void start() {
					if (sunset < 0)
						return;
					double light = sunset - sunrise;
					double t = part >= 0 ? sunrise + light * part : sunset + (TIME.secondsPerDay() - light) / 2;
					TIME.set(t);
					GAME.SPEED.speedSet(0);
					zoom(z);
				}
			};
		}

		/** A popup this script doesn't handle stops the updates: quit after 60 s rather than keep the screen */
		private void watchdog() {
			Thread t = new Thread(() -> {
				while (true) {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						return;
					}
					if (System.nanoTime() - alive > 60e9) {
						LOG.ln("GOUL auto: stuck, no update for 60 s (a popup?), quitting");
						System.exit(1);
					}
				}
			}, "goul-autopilot-watchdog");
			t.setDaemon(true);
			t.start();
		}

		@Override
		public void update(double ds) {
			long now = System.nanoTime();
			alive = now;
			AutoChoice.run();
			if (current < 0) {
				LOG.ln("GOUL auto: game ready, mode " + MODE);
				watchdog();
				timing();
				findFocus();
				next(now);
				return;
			}
			if (current >= steps.length)
				return;

			frames++;
			worst = Math.max(worst, now - last);
			last = now;

			Step s = steps[current];
			double part = (now - stepStart) / 1e9 / s.seconds;
			if (part < 1) {
				if (s.zoom >= 0 && window().zoomout() != s.zoom)
					window().setZoomout(s.zoom);
				s.tick(part);
				return;
			}

			double sec = (now - stepStart) / 1e9;
			LOG.ln(String.format(java.util.Locale.ROOT, "GOUL auto: %-20s fps %5.1f, updates/s %5.1f, worst gap %5.1f ms, game x%6.1f, ms update %5.2f render %5.2f flush %5.2f, zoom %d, night %b",
					s.name, CoreStats.FPS.ave, frames / sec, worst / 1e6, (TIME.currentSecond() - stepGame) / sec, ms(CoreStats.updatePercentage), ms(CoreStats.renderPercentage),
					ms(CoreStats.coreFlush), window().zoomout(), TIME.light().nightIs()));
			if (s.shot)
				shot(s.name);
			next(System.nanoTime());
		}

		/** startup: java start to the automatic Continue (includes the menu's logo); load: Continue to the first game update */
		private static void timing() {
			try {
				long start = java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime();
				long cont = Long.parseLong(System.getProperty("goul.autopilot.continue"));
				long now = System.currentTimeMillis();
				LOG.ln(String.format(java.util.Locale.ROOT, "GOUL auto: timing startup %.2f s, load %.2f s", (cont - start) / 1000.0, (now - cont) / 1000.0));
			} catch (Exception e) {
				LOG.ln("GOUL auto: timing failed: " + e);
			}
		}

		private void next(long now) {
			current++;
			if (current >= steps.length) {
				LOG.ln("GOUL auto: done, quitting");
				CORE.annihilate();
				return;
			}
			steps[current].start();
			stepGame = TIME.currentSecond();
			stepStart = now;
			last = now;
			frames = 0;
			worst = 0;
		}

		private static double ms(CoreStats.Value v) {
			return v.ave * 1000.0 / 60 / 100;
		}

		/** The game's own screenshot (as with Print Screen), moved to the output folder */
		private void shot(String name) {
			try {
				CORE.getGraphics().makeScreenShot();
				if (out == null)
					return;
				Path dir = PATHS.local().SCREENSHOT.get();
				Path newest = null;
				try (Stream<Path> files = Files.list(dir)) {
					for (Path p : (Iterable<Path>) files::iterator) {
						String f = "" + p.getFileName();
						if (f.startsWith("shot") && f.endsWith(".png")
								&& (newest == null || Files.getLastModifiedTime(p).compareTo(Files.getLastModifiedTime(newest)) > 0))
							newest = p;
					}
				}
				if (newest == null)
					return;
				Files.createDirectories(out);
				shots++;
				Files.move(newest, out.resolve(String.format("%02d-%s.png", shots, name)), StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception e) {
				LOG.ln("GOUL auto: screenshot failed: " + e);
			}
		}

		@Override
		public void save(FilePutter file) {
		}

		@Override
		public void load(FileGetter file) throws IOException {
		}
	}

}
