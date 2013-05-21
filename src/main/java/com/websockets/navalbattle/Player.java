package com.websockets.navalbattle;

import java.util.List;

import com.google.common.collect.Lists;

public class Player {
	
	private final String id;

	private final String name;

	private final List<Ship> ships;

	public Player(String id, String name) {
		this.id = id;
		this.name = name;
		ships = Lists.newArrayList();
	}

	public void addShips(Ship... ships) {
		this.ships.addAll(Lists.newArrayList(ships));
	}
	
	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public List<Ship> getShips() {
		return ships;
	}

	public Ship getShip(int index) {
		return ships.get(index);
	}
	
	public Ship hasShipInPosition(int x, int y) {
		for (Ship ship : ships) {
			if (ship.hasPosition(x, y)) {
				return ship;
			}
		}
		return null;
	}

}