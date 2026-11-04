package settlement.room.main.copy;

import settlement.room.main.ROOMS;
import view.tool.PLACABLE;

public final class ROOM_COPY {

	private Copier copy;
	private Copier move; // GOUL
	public final CopierMass copier = new CopierMass();
	public final SavedPrintsPlacer savedPlacer;
	public final SavedPrints prints;
	
	public ROOM_COPY(ROOMS r){
		BSwap s = new BSwap(r);
		prints = new SavedPrints(r);
		savedPlacer = new SavedPrintsPlacer(s);
		copy = new Copier(s);
		move = new Copier(s, new RoomMover()); // GOUL
		
	}
	
	public void copy(int rx, int ry) {
		if (copy.isPlacable(rx, ry) == null)
			copy.placeFirst(rx, ry);
	}
	
	public PLACABLE copy() {
		return copy;
	}
	
	// GOUL: Move Room
	public PLACABLE move() {
		return move;
	}
}
