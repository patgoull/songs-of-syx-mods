package view.sett.ui.room.construction;

import init.constant.C;
import init.sprite.UI.Icon;
import init.sprite.UI.UI;
import settlement.room.main.placement.RoomChecklist;
import snake2d.SPRITE_RENDERER;
import snake2d.util.color.COLOR;
import snake2d.util.gui.GUI_BOX;
import snake2d.util.gui.Hoverable.HOVERABLE;
import util.colors.GCOLOR;
import util.gui.misc.GBox;
import util.gui.misc.GStat;
import util.gui.misc.GText;
import util.text.D;
import view.main.VIEW;

/**
 * GOUL: a line in the construction panel saying whether the room can be constructed, and a second one with the
 * warnings, if any. Hovering them lists every requirement, met or not.
 */
final class SChecklist {

	private static CharSequence ¤¤ready = "Ready to construct";
	private static CharSequence ¤¤warning = "1 warning";
	private static CharSequence ¤¤warnings = "{0} warnings";
	private static CharSequence ¤¤problems = "{0} requirements not met";

	static {
		D.ts(SChecklist.class);
	}

	private final RoomChecklist list = new RoomChecklist();
	// the checks walk the whole room, a few times a second is enough for the line
	private double next;

	private static final int LINE = Icon.S+C.SG*4;

	final HOVERABLE row = new HOVERABLE.HoverableAbs(C.SG*250, LINE*2) {

		final GStat stat = new GStat() {
			@Override
			public void update(GText text) {
				if (list.problems() > 0)
					text.errorify().add(¤¤problems).insert(0, list.problems());
				else
					text.normalify2().add(¤¤ready);
			}
		};

		final GStat warn = new GStat() {
			@Override
			public void update(GText text) {
				if (list.warnings() == 1)
					text.warnify().add(¤¤warning);
				else
					text.warnify().add(¤¤warnings).insert(0, list.warnings());
			}
		};

		@Override
		protected void render(SPRITE_RENDERER r, float ds, boolean isHovered) {
			if (VIEW.renderSecond() > next) {
				list.check(null);
				next = VIEW.renderSecond() + 0.25;
			}
			if (list.problems() > 0) {
				GCOLOR.T().ERROR.bind();
				UI.icons().s.cancel.render(r, body().x1(), body().y1());
			}else {
				GCOLOR.T().IGREAT.bind();
				UI.icons().s.allRight.render(r, body().x1(), body().y1());
			}
			COLOR.unbind();
			stat.render(r, body().x1()+Icon.S+C.SG*4, body().y1());
			if (list.warnings() > 0) {
				GCOLOR.T().WARNING.bind();
				UI.icons().s.alert.render(r, body().x1(), body().y1()+LINE);
				COLOR.unbind();
				warn.render(r, body().x1()+Icon.S+C.SG*4, body().y1()+LINE);
			}
		}

		@Override
		public void hoverInfoGet(GUI_BOX text) {
			list.check((GBox) text);
		}
	};

}
