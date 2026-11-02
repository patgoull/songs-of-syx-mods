package view.sett.ui.room.construction;

import game.time.TIME;
import init.constant.C;
import init.resources.RESOURCE;
import init.resources.RESOURCES;
import init.sprite.UI.Icon;
import init.sprite.UI.UI;
import settlement.environment.Foundation;
import settlement.main.SETT;
import settlement.maintenance.ROOM_DEGRADER;
import snake2d.SPRITE_RENDERER;
import snake2d.util.gui.GUI_BOX;
import snake2d.util.gui.GuiSection;
import snake2d.util.gui.Hoverable.HOVERABLE;
import util.colors.GCOLOR;
import util.gui.misc.GBox;
import util.gui.misc.GButt;
import util.gui.misc.GStat;
import util.gui.misc.GText;
import util.info.GFORMAT;
import util.text.D;
import util.text.Dic;
import util.text.DicTime;

final class SStats {

	private final HOVERABLE[] statStats = new HOVERABLE[8];
	private final HOVERABLE statResourcesStructure;
	private final HOVERABLE statResourcesCave;
	private final HOVERABLE[] statResources = new HOVERABLE[8];
	private final HOVERABLE foundation;
	private final GuiSection stats = new GuiSection();
	private final State s;
	private final SChecklist checklist = new SChecklist(); // GOUL
	
	private static CharSequence ¤¤expense = "The room does not have enough support as indicated by yellow tiles. It can be built, but will require extra materials and maintenance. Shape the room thinner, or remove room tiles in the center to allow for more support and less costs.";
	private static CharSequence ¤¤foundation = "This is a heavy room and relies on its foundation. Poor foundation will increase building and maintenance cost slightly, while good will decrease it.";
	private static CharSequence ¤¤isolation = "The room will be poorly insulated, and maintenance need will be higher as a consequence. Toggle the automatic building of walls, and use as few doorways as possible to improve it.";
	
	
	static {
		D.ts(SStats.class);
		
	}
			
	SStats(State s){
		this.s = s;
		statResourcesStructure = new HOVERABLE.HoverableAbs((int) (Icon.M*2.5), Icon.M) {
			final GStat stat = new GStat() {
				
				@Override
				public void update(GText text) {
					int am = s.placement.placer.structure.roofs()*s.placement.placer.structure.get().structure.resAmount;
					am += s.placement.placer.structure.walls()*s.placement.placer.structure.get().structure.resAmount;
					GFORMAT.i(text, am);
				}
			};
			@Override
			protected void render(SPRITE_RENDERER r, float ds, boolean isHovered) {
				if (s.placement.placer.structure.get().structure.resource != null) {
					s.placement.placer.structure.get().structure.resource.icon().render(r, body().x1(), body().y1());
					stat.render(r, body().x1() + Icon.M+C.SG*2, body().y1()+(body().height()-stat.height())/2);
				}
				
			}
			
			@Override
			public void hoverInfoGet(GUI_BOX text) {
				
				if (s.placement.placer.structure.get().structure.resource != null) {
					text.text(s.placement.placer.structure.get().structure.resource.name);
					text.NL();
					text.text(s.placement.placer.structure.get().structure.nameCeiling);
					
				}
			}
		};
		statResourcesCave = new HOVERABLE.HoverableAbs((int) (Icon.M*2.5), Icon.M) {
			final GStat stat = new GStat() {
				
				@Override
				public void update(GText text) {
					int am = s.placement.placer.structure.mountainWalls()*SETT.JOBS().clearss.caveFill.resAmount();
					GFORMAT.i(text, am);
				}
			};
			@Override
			protected void render(SPRITE_RENDERER r, float ds, boolean isHovered) {
				if (s.placement.placer.structure.mountainWalls() == 0)
					return;
				RESOURCES.STONE().icon().render(r, body().x1(), body().y1());
				stat.render(r, body().x1() + Icon.M+C.SG*2, body().y1()+(body().height()-stat.height())/2);
			}
			
			@Override
			public void hoverInfoGet(GUI_BOX text) {
				text.text(RESOURCES.STONE().name);
				text.NL();
				text.text(SETT.JOBS().clearss.caveFill.placer().name());
			}
		};
		

		for (int i = 0; i < 8; i++) {
			final int k = i;
			statResources[i] = new HOVERABLE.HoverableAbs((int) (Icon.M*2.5), Icon.M) {
				final GStat stat = new GStat() {
					
					@Override
					public void update(GText text) {
						double am = SETT.ROOMS().placement.placer.resNeeded(k);
						GFORMAT.i(text, (int)Math.ceil(am));
						if (SETT.ROOMS().placement.placer.cost().total() > 0) {
							text.warnify();
							text.add('*');
						}else if (SETT.ROOMS().placement.placer.blueprint().constructor().needsIsolation() && s.placement.placer.isolation() < 1) {
							text.warnify();
						}else if (SETT.ROOMS().placement.placer.cost().total() < 0) {
							text.color(GCOLOR.T().IGREAT);
						}
						
					}
				};
				@Override
				protected void render(SPRITE_RENDERER r, float ds, boolean isHovered) {
					if (SETT.ROOMS().placement.placer.resNeeded(k) <= 0)
						return;
					s.b.constructor().resource(k).icon().render(r, body().x1(), body().y1());
					stat.render(r, body().x1() + Icon.M+C.SG*2, body().y1()+(body().height()-stat.height())/2);
				}
				
				@Override
				public void hoverInfoGet(GUI_BOX text) {
					if (SETT.ROOMS().placement.placer.resNeeded(k) <= 0)
						return;
					GBox b = (GBox) text;
					RESOURCE res = s.b.constructor().resource(k);
					b.title(res.name);
					
					b.add(b.text().lablify().add(Dic.¤¤Cost).s().add('(').add(Dic.¤¤construction).add(')'));
					b.tab(7);
					{
						GText t = b.text();
						t.add(SETT.ROOMS().placement.placer.resNeededNoCost(k));
						b.add(t);
						
						int n = SETT.ROOMS().placement.placer.resNeededOnlyCost(k);
						if (n != 0) {
							t = b.text();
							GFORMAT.iIncr(t, n);
							if (n > 0)
								t.warnify();
							if (n < 0)
								t.color(GCOLOR.T().IGREAT);
							b.add(t);
						}
					}
					
					
					
					
					
					b.NL(8);
					double deg = SETT.ROOMS().placement.placer.blueprint().degradeRate();
					if (deg > 0) {
						b.add(b.text().lablify().add(Dic.¤¤Maintenance).s().add('(').add(DicTime.¤¤Year).add(')'));
						b.tab(7);
						deg *= 1 + SETT.ROOMS().placement.placer.cost().total();
						double iso = s.placement.placer.isolation();
						if (SETT.ROOMS().placement.placer.blueprint().constructor().needsIsolation()) {
							deg *= 1 + (1-s.placement.placer.isolation())*2;
						}else {
							iso = 1.0;
						}
						double none = ROOM_DEGRADER.rateResource(SETT.MAINTENANCE().speed(), SETT.ROOMS().placement.placer.blueprint().degradeRate(), iso, SETT.ROOMS().placement.placer.resNeededNoCost(k));
						double am = ROOM_DEGRADER.rateResource(SETT.MAINTENANCE().speed(), SETT.ROOMS().placement.placer.blueprint().degradeRate(), iso, SETT.ROOMS().placement.placer.resNeeded(k)) - none;
						none *= TIME.years().bitConversion(TIME.days());
						am *= TIME.years().bitConversion(TIME.days());
						{
							GText t = b.text();
							t.add(none, 2);
							b.add(t);
							
							if (am != 0) {
								t = b.text();
								GFORMAT.f0(t, am);
								if (am > 0)
									t.warnify();
								else
									t.color(GCOLOR.T().IGREAT);
								b.add(t);
							}
							
						}
						b.NL();
					}
					
					b.sep();
					
					b.textLL(Dic.¤¤SupportRoom);
					b.tab(7);
					b.add(GFORMAT.perc(b.text(), 1.0-SETT.ROOMS().placement.placer.cost().support()));
					b.NL();
					if (SETT.ROOMS().placement.placer.cost().support() > 0) {
						b.add(b.text().warnify().add(¤¤expense));
						b.NL();
						
					}
					b.NL(8);
					
					if (SETT.ROOMS().placement.placer.blueprint().constructor().isHeavy()) {
						b.textLL(Foundation.¤¤name);
						b.tab(7);
						double d = SETT.ROOMS().placement.placer.cost().foundation();
						b.add(GFORMAT.percInc(b.text(), -d, 2));
						b.NL();
						if (d >= 1) {
							b.add(b.text().normalify2().add(¤¤foundation));
							b.NL();
							
						}else {
							b.add(b.text().warnify().add(¤¤foundation));
							b.NL();
						}
						b.NL(8);
					}
					
					
					if (SETT.ROOMS().placement.placer.blueprint().constructor().needsIsolation()) {
						b.textLL(SETT.ROOMS().isolation.info.name);
						b.tab(7);
						b.add(GFORMAT.perc(b.text(), s.placement.placer.isolation()));
						b.NL();
						if (s.placement.placer.isolation() < 1) {
							b.add(b.text().warnify().add(¤¤isolation));
							b.NL();
						}
						
					}
					b.NL(8);
					
				}
			};

			statStats[i] = new HOVERABLE.HoverableAbs(C.SG*250, C.SG*16) {
				final GStat stat = new GStat() {
					@Override
					public void update(GText text) {
						s.b.constructor().stats().get(k).format(text, s.placement.placer.itemStats(k));
					}
				};
				final GStat title = new GStat() {
					@Override
					public void update(GText text) {
						text.lablify().add(s.b.constructor().stats().get(k).name());
					}
				};
				
				@Override
				protected void render(SPRITE_RENDERER r, float ds, boolean isHovered) {
					stat.render(r, body().x1()+190, body().y1());
					title.render(r, body().x1(), body().y1());
					
				}
				
				@Override
				public void hoverInfoGet(GUI_BOX text) {
					text.text(s.b.constructor().stats().get(k).desc());
				}
			};
			
		}
		
		GuiSection f = new GuiSection();
		f.add(new GButt.ButtPanel(UI.icons().s.eye) {
			
			@Override
			protected void renAction() {
				selectedSet(SETT.ROOMS().placement.placer.showFoundation.is());
			}
			
			@Override
			protected void clickA() {
				SETT.ROOMS().placement.placer.showFoundation.toggle();
			}
			
		});
		f.addRightC(6, new GText(UI.FONT().S, SETT.OVERLAY().FOUNDATION.name).lablify());
		
		f.addRightCAbs(190-32, new GStat() {
			
			@Override
			public void update(GText text) {
				GFORMAT.percInc(text, -SETT.ROOMS().placement.placer.cost().foundation(), 2);
			}
		});
		f.hoverInfoSet(¤¤foundation);
		foundation = f;
		
	}
	
	GuiSection get() {
		stats.clear();
		int k = 0;
		
		for (int i = 0; i <s. b.constructor().stats().size(); i++) {
			stats.addDown(0, statStats[i]);
		}
		
		if (s.b.constructor().isHeavy()) {
			stats.addDown(0, foundation);
		}
		
		int w = statResources[0].body().width();
		int h = statResources[0].body().height();
		int y1 = 84+18;
		
		for (int i = 0; i < s.b.constructor().resources(); i++) {
			stats.add(statResources[i], (k%3)*w, y1 + (k/3)*h);
			k++;
		}
		if (s.b.constructor().mustBeIndoors()) {
			k++;
			stats.add(statResourcesStructure, (k%3)*w, y1+ (k/3)*h);
			k++;
			stats.add(statResourcesCave, (k%3)*w, y1 + (k/3)*h);
		}
		if (s.b.constructor().usesArea())
			stats.add(checklist.row, 0, stats.body().y2()+4); // GOUL: can it be constructed, every requirement on hover
		stats.body().incrH(16);
		return stats;
	}
	
}
