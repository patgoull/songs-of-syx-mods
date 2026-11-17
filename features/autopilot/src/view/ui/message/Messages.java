package view.ui.message;

import java.io.IOException;

import game.GAME;
import game.save.Savable;
import game.time.TIME;
import init.constant.C;
import init.paths.PATHS;
import init.sprite.SPRITES;
import init.sprite.UI.Icon;
import init.sprite.UI.UI;
import snake2d.MButt;
import snake2d.Renderer;
import snake2d.SPRITE_RENDERER;
import snake2d.util.color.COLOR;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.DIR;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
import snake2d.util.file.Json;
import snake2d.util.file.JsonE;
import snake2d.util.gui.GUI_BOX;
import snake2d.util.gui.GuiSection;
import snake2d.util.gui.Hoverable.HOVERABLE;
import snake2d.util.gui.clickable.CLICKABLE;
import snake2d.util.gui.renderable.RENDEROBJ;
import snake2d.util.misc.ACTION;
import snake2d.util.sets.ArrayList;
import snake2d.util.sets.KeyMap;
import snake2d.util.sprite.text.Text;
import util.data.GETTER;
import util.gui.misc.GBox;
import util.gui.misc.GButt;
import util.gui.misc.GStat;
import util.gui.misc.GText;
import util.gui.panel.GPanel;
import util.gui.table.GTableBuilder;
import util.gui.table.GTableBuilder.GRowBuilder;
import util.text.D;
import util.text.Dic;
import util.text.DicTime;
import view.interrupter.IDebugPanel;
import view.interrupter.InterManager;
import view.interrupter.Interrupter;
import view.main.VIEW;

public final class Messages {

	private final ArrayList<Message> all = new ArrayList<Message>(256);
	private int unread = 0;
	private final List list;
	private final IMessage imess;
	private final InterManager manager;
	private final KeyMap<String> hideMap = new KeyMap<>();
	private final ArrayList<Message> queued = new ArrayList<>(16);
	
	public Messages(InterManager manager){
		this.manager = manager;
		list = new List();
		imess = new IMessage();
		IDebugPanel.add("Send message", new ACTION() {
			@Override
			public void exe() {
				debug();
			}
		});
		read();
		GAME.saver().add(new Savable("MESSAGES") {
			
			@Override
			public void save(FilePutter f) {
				f.mark(this);
				f.i(all.size());
				for (Message m : all)
					f.object(m);
			}
			
			@Override
			public void load(FileGetter f) throws IOException {
				f.check(this);
				unread = 0;
				
				int am = f.i();
				
				for (int i = 0; i < am; i++) {
					Message m = (Message) f.object(true);
					if (m != null) {
						all.add(m);
						if (!m.isRead)
							unread++;
					}
				}
				
				
			}
		});
		
	}
	
	private static final String fn = "BlockedMessages";
	
	private static CharSequence ¤¤delete = "¤Delete all read messages.";
	private static CharSequence ¤¤PauseD = "¤When selected, pauses the game and shows this type of message upon arrival.";
	private static CharSequence ¤¤title = "¤Title";
	private static CharSequence ¤¤Arrived = "¤Arrived";
	private static CharSequence ¤¤Messages = "¤Messages";
	static {
		D.ts(Messages.class);
	}
	
	private void read() {
		
		if (!PATHS.local().PROFILE.exists(fn))
			return;
		try {
			for (String s : new Json(PATHS.local().PROFILE.gets(fn)).values("M")) {
				if (s != null && !s.equals("null"))
					hideMap.put(s, s);
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
			hideMap.clear();
		}
		
	}
	
	private void flush() {
		
		try {
			String[] vv = new String[hideMap.all().size()];
			int i = 0;
			for (String s : hideMap.all()) {
				vv[i++] = s;
			}
			JsonE j = new JsonE();
			j.add("M", vv);
			
			if (!PATHS.local().PROFILE.exists(fn))
				PATHS.local().PROFILE.create(fn);
			j.save(PATHS.local().PROFILE.get(fn));
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void debug() {
		
		MessageText t = new MessageText("Debugging");
		t.paragraph("hello!");
		t.paragraph("goodbye!");
		t.send();
		
		t = new MessageText("Debugging Long");
		t.paragraph("hello again!");
		for (int i = 0; i < 10; i++)
			t.paragraph("You're taking too long to make this game. I'm so glad you're making this game Jake, isn't it a lot of fun! Could you add Godzilla monsters please, I so much want them. Until next time, ta ta!");
		
		t.send();
	}
	
	boolean add(Message m) {
		
		if (!all.hasRoom()) {
			remove(0);
		}
		

		all.add(m);
		m.section = m.makeSection();
		unread ++;
		
		if (hideMap.containsKey(m.key)) {
			m.isRead = true;
			unread--;
			imess.sound.play(true);
			return false;
		}
		
		if (goul.autopilot.AutoChoice.on()) { // GOUL: autopilot test tool, the message window would stop the game until closed
			m.isRead = true;
			unread--;
			if (m instanceof view.ui.diplomacy.UIDipMessDeal)
				goul.autopilot.AutoChoice.later("offer \"" + m.title() + "\", accepted", () -> ((view.ui.diplomacy.UIDipMessDeal) m).goulAccept());
			else
				snake2d.LOG.ln("GOUL auto: message \"" + m.title() + "\", not opened");
			return true;
		}

		if (imess.isActivated()) {
			if (queued.hasRoom())
				queued.add(m);
		}else
			imess.act(m);
		return true;
	}
	
	private void remove(int index) {
		
		if (index < 0 || index >= all.size())
			return;
		if (!all.get(index).isRead)
			unread --;
		
		all.removeOrdered(index);
	}
	
	GButt.Panel getButt() {
		GButt.Panel b = new GButt.Panel(SPRITES.icons().m.openscroll) {
			private Text nr = new Text(UI.FONT().M, 10);
			@Override
			protected void render(SPRITE_RENDERER r, float ds, boolean isActive, boolean isSelected,
					boolean isHovered) {
				isActive = all.size() > 0;
				activeSet(isActive);
				isSelected = list.isActivated();
				super.render(r, ds, isActive, isSelected, isHovered);
				
				if (unread > 0 && all.size() > 0) {
					nr.clear().add(unread).adjustWidth();
					if (!isHovered && TIME.currentSecond() - all.last().currentSecond < 3) {
						COLOR.WHITE2WHITE.bind();
						int x = body().x1() + (body.width() - init.sprite.UI.Icon.M)/2;
						int y = body().y1() + (body.height() - init.sprite.UI.Icon.M)/2;
						SPRITES.icons().m.openscroll.render(r, x, y);
					}
					
					int x = body().x1() + (body.width() - nr.width())/2;
					int y = body().y1() + (body.height() - nr.height())/2;
					COLOR.WHITE100.bind();
					nr.render(r, x-1, y-1);
					COLOR.RED50.bind();
					nr.render(r, x, y);
					COLOR.unbind();
				}
			}
			
			@Override
			protected void clickA() {
				if (all.size() > 0)
					list.act();
			}
		};
		b.hoverInfoSet(¤¤Messages);
		return b;
	}
	
	public int size() {
		return all.size();
	}
	
	public boolean activated() {
		return list.isActivated() || imess.isActivated();
	}
	
	public int unread() {
		return unread;
	}
	
	public double currentSecond() {
		if (all.size() == 0)
			return 0;
		return all.last().currentSecond;
	}
	
	public void activate() {
		if (all.size() > 0)
			list.act();
	}
	
	public void hide() {
		imess.close.exe();
	}
	
	public void hideAll() {
		while (imess.isActivated())
			imess.close.exe();
		list.hide();
	}

	public void reopen(Message mess) {
		imess.act(mess);
	}

	
	private class List extends Interrupter {

		private final GuiSection section;
		private boolean removed = false;

		
		protected List() {
			super("MESSAGE_LIST_OPEN");
			
			GTableBuilder builder = new GTableBuilder() {
				
				@Override
				public int nrOFEntries() {
					return all.size();
				}
				
				@Override
				public void click(int index) {
					if (removed) {
						removed = false;
						return;
					}
					Message message = all.get(all.size()-1-index);
					if (message != null)
						imess.act(message);
				}
			};
			
			builder.column(¤¤title, 200, new GRowBuilder() {
				@Override
				public RENDEROBJ build(GETTER<Integer> ier) {
					return new GStat() {
						
						@Override
						public void update(GText text) {
							Message m = all.get(all.size()-1-ier.get());
							if (m == null)
								return;
							if (m.title().length() >= 20) {
								text.add(m.title(), 0, 20);
								text.add('.').add('.').add('.');
							}else {
								text.add(m.title());
							}
							if (m.isRead) {
								text.color(COLOR.WHITE65);
							}else {
								text.color(COLOR.WHITE100);
							}
						}
					}.r(DIR.NW);
				}
			});
			
			builder.column(¤¤Arrived, 200, new GRowBuilder() {
				@Override
				public RENDEROBJ build(GETTER<Integer> ier) {
					return new GStat() {
						
						@Override
						public void update(GText text) {
							Message m = all.get(all.size()-1-ier.get());
							if (m == null)
								return;
							int t = (int) (TIME.currentSecond() - m.currentSecond);
							DicTime.setAgo(text, t);

							if (m.isRead) {
								text.color(COLOR.WHITE65);
							}else {
								text.color(COLOR.WHITE100);
							}
						}
					}.r(DIR.NW);
				}
			});
			
			builder.column("", (int) (Icon.M*1.5), new GRowBuilder() {
				@Override
				public RENDEROBJ build(GETTER<Integer> ier) {
					return new GButt.ButtPanel(SPRITES.icons().s.cancel) {
						@Override
						protected void clickA() {
							Message m = all.get(all.size()-1-ier.get());
							if (m == null)
								return;
							
							remove(all.size()-1-ier.get());
							removed = true;
						}
					}.hoverInfoSet(Dic.¤¤remove);
				}
			});
			
			builder.column("", (int) (Icon.M*1.5), new GRowBuilder() {
				@Override
				public RENDEROBJ build(GETTER<Integer> ier) {
					return new GButt.ButtPanel(SPRITES.icons().s.clock) {
						@Override
						protected void clickA() {
							Message m = all.get(all.size()-1-ier.get());
							if (m == null)
								return;
							
							if (hideMap.containsKey(m.key)) {
								hideMap.remove(m.key);
							}else {
								hideMap.put(m.key, m.key);
							}
							flush();
						}
						
						@Override
						protected void renAction() {
							Message m = all.get(all.size()-1-ier.get());
							selectedSet(m != null && !hideMap.containsKey(m.key));
						};
						
					}.hoverInfoSet(¤¤PauseD);
				}
			});
	
			
			section = builder.create(15, true);
			
			CLICKABLE b = new GButt.ButtPanel(¤¤delete) {
				@Override
				protected void clickA() {
					for (int i = 0; i < all.size(); i++) {
						if (all.get(i).isRead) {
							all.removeOrdered(i);
							i--;
						}
					}
				}
				
				@Override
				protected void renAction() {
					activeSet(all.size() - unread > 0);
				}
			};
			b.body().centerX(section).moveY1(section.body().y2()+10);
			section.add(b);
			
			GPanel p = new GPanel();
			p.set(section.body());
			
			p.setCloseAction(new ACTION() {
				@Override
				public void exe() {
					hide();
				}
			});
			p.body().centerY(C.DIM());
			p.body().centerX(C.WIDTH()/2, C.WIDTH());
			section.body().centerIn(p);
			section.add(p);
			section.moveLastToBack();

			p.setTitle(¤¤Messages, UI.FONT().H2);
			
			
			
		}

		void act(){
			show(manager);
		}
		
		@Override
		protected boolean hover(COORDINATE mCoo, boolean mouseHasMoved) {
			section.hover(mCoo);
			return true;
		}

		@Override
		protected void mouseClick(MButt button) {
			if (button == MButt.LEFT)
				section.click();
			if (button == MButt.RIGHT)
				hide();
		}

		@Override
		public void hide() {
			// TODO Auto-generated method stub
			super.hide();
		}
		
		@Override
		protected void hoverTimer(GBox text) {
			section.hoverInfoGet(text);
		}

		@Override
		protected boolean render(Renderer r, float ds) {
			section.render(r, ds);
			return true;
		}

		@Override
		protected boolean update(float ds) {
			// TODO Auto-generated method stub
			return true;
		}
		
	}
	
	private class IMessage extends Interrupter {

		protected IMessage() {
			super("MESSAGE_OPEN");
		}

		private final GPanel panel = new GPanel().setBig();
		private Message m;
		
		
		private final GButt.ButtPanel show = new GButt.ButtPanel(UI.FONT().S.getText(Dic.¤¤Alert)) {
			
			@Override
			public void hoverInfoGet(GUI_BOX text) {
				text.text(¤¤PauseD);
			};
			
			@Override
			protected void renAction() {
				selectedSet(!hideMap.containsKey(m.key));
			};
			
			@Override
			protected void clickA() {
				if (hideMap.containsKey(m.key)) {
					hideMap.remove(m.key);
				}else {
					hideMap.put(m.key, m.key);
				}
				flush();
			};
		}.icon(UI.icons().s.clock);
		
		ACTION close = new ACTION() {
			@Override
			public void exe() {
				hide();
				if (m == null)
					return;
				if (!m.isRead)
					unread--;
				m.isRead = true;
				if (!queued.isEmpty()) {
					act(queued.removeLast());
				}
			}
		};
		

		void act(Message m){
			if (VIEW.b().isActive())
				return;
			this.m = m;
			if (m.section == null)
				m.section = m.makeSection();
			panel.inner().set(m.section);
			panel.inner().setWidth(Math.max(panel.inner().width(), 500));
			panel.inner().incrH(20);
			panel.body().centerIn(C.DIM());
			m.section.body().centerX(panel.inner());
			m.section.body().moveY1(panel.inner().y1());
			show.body.moveX2(m.section.body().x2()-8);
			show.body.moveY1(panel.inner().y2()+2);
			panel.setCloseAction(close);
			if (m.title() != null && m.title().length() > 0)
				panel.setTitle(m.title());
			else
				panel.setTitle(Dic.¤¤Clear);
			
			show(manager);
		}
		
		@Override
		protected boolean hover(COORDINATE mCoo, boolean mouseHasMoved) {
			panel.hover(mCoo);
			if (m.section instanceof HOVERABLE)
				((HOVERABLE)m.section).hover(mCoo);
			show.hover(mCoo);
			return true;
		}

		@Override
		protected void mouseClick(MButt button) {
			if (button == MButt.LEFT) {
				panel.click();
				if (m.section instanceof CLICKABLE)
					((CLICKABLE)m.section).click();
				show.click();
			}else if (button == MButt.RIGHT)
				close.exe();
		}

		@Override
		protected void hoverTimer(GBox text) {
			if (panel.hoveredIs())
				panel.hoverInfoGet(text);
			if (m.section instanceof HOVERABLE)
				((HOVERABLE)m.section).hoverInfoGet(text);
			if (show.hoveredIs())
				show.hoverInfoGet(text);
		}

		@Override
		protected boolean render(Renderer r, float ds) {
			panel.render(r, ds);
			m.section.render(r, ds);
			show.render(r, ds);
			return true;
		}

		@Override
		protected boolean update(float ds) {
			return false;
		}
		
	}
	
}
