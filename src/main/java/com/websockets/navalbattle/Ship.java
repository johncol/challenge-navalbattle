package com.websockets.navalbattle;

public class Ship {

	private final Integer positionX;

	private final Integer positionY;

	private boolean alive;

	public Ship(int x, int y) {
		alive = true;
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

	public boolean isAlive() {
		return alive;
	}

	public void sink() {
		alive = false;
	}
	
	@Override
	public String toString() {
		return String.format("Ship [%d, %d]", positionX.intValue(), positionY.intValue());
	}

}