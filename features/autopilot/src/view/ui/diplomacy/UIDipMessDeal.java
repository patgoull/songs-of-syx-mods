package view.ui.diplomacy;

import game.faction.FACTIONS;
import game.faction.diplomacy.DIP;
import game.faction.diplomacy.deal.Deal;
import game.faction.diplomacy.deal.DealSave;
import game.faction.royalty.opinion.ROPINION;
import game.time.TIME;
import init.sprite.UI.UI;
import snake2d.util.color.COLOR;
import snake2d.util.datatypes.DIR;
import snake2d.util.gui.GUI_BOX;
import snake2d.util.gui.GuiSection;
import util.gui.misc.GBox;
import util.gui.misc.GButt;
import util.gui.misc.GStat;
import util.gui.misc.GText;
import util.info.GFORMAT;
import util.text.D;
import util.text.Dic;
import view.main.VIEW;
import view.ui.diplomacy.UIDipMess.MessIntro;
import view.ui.message.MessageSection;

public final class UIDipMessDeal extends MessageSection{
	
	static CharSequence ¤¤noLonger = "¤This offer is no longer valid.";
	static CharSequence ¤¤accepted = "¤You have accepted this offer.";
	static CharSequence ¤¤declined = "¤You have declined this offer.";
	static CharSequence ¤¤Time = "¤Inform us of your decision within a day.";
	
	static CharSequence ¤¤AcceptD = "¤Accepting this offer will change the faction's opinion of you by:";
	static CharSequence ¤¤DeclineD = "¤Declining this offer will change the faction's opinion of you by:";
	
	private static CharSequence ¤¤Exp = "The agreement has expired.";
	private static CharSequence ¤¤Power = "Since this agreement was drafted too much has changed.";
	
	private static CharSequence ¤¤Inspect = "¤Inspect Faction.";

	static {
		D.ts(UIDipMessDeal.class);
	}
	private static final long serialVersionUID = 1L;
	private final double powerF;
	private final double powerP;
	private final boolean peace;
	private final double time;
	private final DealSave save;
	private final String message;
	private final String desc;
	private byte aa = 0;
	private final double happiness;
	private final double decline;
	private final MessIntro intro;

	
	public UIDipMessDeal(CharSequence title, CharSequence message, CharSequence desc, Deal deal, double happiness, double declineP) {
		super(title);
		peace = deal.bools.PEACE.is();
		powerF = deal.npc.npc().offensivePower();
		powerP = FACTIONS.player().offensivePower();
		time = TIME.currentSecond();
		save = new DealSave(deal);
		this.message = ""+message;
		this.desc = desc == null ? null : ""+desc;
		this.happiness = happiness;
		this.decline = declineP;
		deal.npc.npc().request.set(declineP, title);
		intro = new MessIntro(deal.npc.npc());
	}
	
//	public UIDipMessDeal(CharSequence title, CharSequence desc, Deal deal, double happiness, double declineP) {
//		this(title, desc, null, deal, happiness, declineP);
//	}
	
	@Override
	protected void make(GuiSection section) {
		
		

		paragraph(message);

		section.addRelBody(16, DIR.S, new UIDealListSaved(save, 250));
		
		
		
		section.addRelBody(16, DIR.S, new GStat() {

			@Override
			public void update(GText text) {
				CharSequence pa = pactive(); 
				if (pa != null) {
					text.warnify();
					text.add(pa);
					text.setMaxWidth(WIDTH);
				}else {
					text.color(COLOR.WHITE85);
					text.add(¤¤Time);
				}
			}
			
		}.r(DIR.N));
		
		GuiSection s = new GuiSection();
		s.addRightC(0, new GButt.ButtPanel(UI.icons().m.crossair) {
			
			@Override
			protected void renAction() {
				activeSet(save.f() != null);
			}
			
			@Override
			protected void clickA() {
				VIEW.world().UI.factions.open(save.f());
			}
			
		}.hoverTitleSet(¤¤Inspect));
		s.addRightC(0, new GButt.ButtPanel(Dic.¤¤Accept) {
			
			@Override
			protected void renAction() {
				activeSet(pactive() == null);
			}
			
			@Override
			protected void clickA() {
				Deal d = DIP.TMP();
				save.set(d);
				if (pactive() != null)
					return;
				
				d.execute(false);
				save.f().request.clear();
				ROPINION.GIFTS().makeDeal(d.npc.npc(), happiness);
				aa = 1;
				VIEW.messages().hide();
			}
			
			@Override
			public void hoverInfoGet(GUI_BOX text) {
				GBox b = (GBox) text;
				b.text(¤¤AcceptD);
				b.NL();
				b.add(GFORMAT.f0(b.text(), happiness));
			}
			
		});
		
		s.addRightC(0, new GButt.ButtPanel(Dic.¤¤Decline) {
			
			@Override
			protected void renAction() {
				boolean a = true;
				if (aa == 1)
					a = false;
				if (aa == -1)
					a = false;
				if (Math.abs(TIME.currentSecond()-time) > TIME.secondsPerDay())
					a = false;
				if (save.f() == null)
					a = false;
				activeSet(a);
			}
			
			@Override
			protected void clickA() {
				aa = -1;
				save.f().request.expire();
				VIEW.messages().hide();
			}
			
			@Override
			public void hoverInfoGet(GUI_BOX text) {
				GBox b = (GBox) text;
				b.text(¤¤DeclineD);
				b.NL();
				b.add(GFORMAT.f0(b.text(), decline));
				
			}
			
		});
		
		section.addRelBody(8, DIR.S, s);
		
		{
			section.addRelBody(8, DIR.N, intro.make());
		}
		
		if (desc != null) {
			section.addRelBody(8, DIR.S, new GText(UI.FONT().M, desc).lablifySub().setMaxWidth(WIDTH));
		}
	}
	

	
	/** GOUL: what the Accept button does, for the autopilot (no window is open, so nothing to hide) */
	public void goulAccept() {
		Deal d = DIP.TMP();
		save.set(d);
		if (pactive() != null)
			return;

		d.execute(false);
		save.f().request.clear();
		ROPINION.GIFTS().makeDeal(d.npc.npc(), happiness);
		aa = 1;
	}
	
	private CharSequence pactive() {

		if (aa == 1)
			return ¤¤accepted;
		if (aa == -1)
			return ¤¤declined;
		if (Math.abs(TIME.currentSecond()-time) > TIME.secondsPerDay())
			return ¤¤Exp;
		CharSequence p = save.problem();
		if (p != null)
			return p;
		
		if (peace) {
			if (Math.abs((powerF+10000.0)/(save.f().offensivePower()+10000)-1) > 0.25) {
				return ¤¤Power;
			}
			
			if (Math.abs((powerP+10000.0)/(FACTIONS.player().offensivePower()+10000)-1) > 0.25) {
				return ¤¤Power;
			}
		}
		
		
		return null;
	}
	
	
}