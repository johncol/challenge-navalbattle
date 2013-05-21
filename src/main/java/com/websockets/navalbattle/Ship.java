package com.websockets.navalbattle;

public class Ship {

	private final Integer positionX;

	private final Integer positionY;

	public Ship(int x, int y) {
		positionX = Integer.valueOf(x);
		positionY = Integer.valueOf(y);
	}

	public Integer getPositionX() {
		return positionX;
	}

	public Integer getPositionY() {
		return positionY;
	}

	public boolean hasPosition(int x, int y) {
		return positionX.intValue() == x && positionY.intValue() == y;
	}
	
	@Override
	public String toString() {
		return String.format("Ship [%d, %d]", positionX.intValue(), positionY.intValue());
	}

}