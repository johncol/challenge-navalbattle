package com.websockets.navalbattle;

import static com.websockets.navalbattle.constants.Constants.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsOutbound;

import com.google.common.collect.Maps;

@WebServlet(urlPatterns = "/game")
public class NavalBattleWebSocket extends WebSocketServlet {

	private static final long serialVersionUID = 4698957388632877568L;

	private static Player player1;

	private static Player player2;

	private static Map<Player, NavalBattleConnection> connections = Maps.newHashMap();
	
	private static int setUpPlayers = 0; 

	@Override
	protected StreamInbound createWebSocketInbound(String subProtocol, HttpServletRequest request) {
		String name = request.getParameter("username");
		String id = request.getSession().getId();
		return new NavalBattleConnection(id, name);
	}

	private static class NavalBattleConnection extends MessageInbound {

		private final String connectionId;

		private final String username;

		public NavalBattleConnection(String connectionId, String username) {
			super();
			this.username = username;
			this.connectionId = connectionId;
		}

		@Override
		protected void onOpen(WsOutbound websocket) {
			try {
				if (playerAllowed()) {
					registerPlayer(websocket);
				} else {
					websocket.close(NO_MORE_PLAYERS_ALLOWED, null);
					log("No more players allowed");
				}
			} catch (IOException e) {
				log("Error writing message with websocket");
			}
		}

		private void registerPlayer(WsOutbound outbound) throws IOException {
			Player player = new Player(connectionId, username);
			connections.put(player, this);
			log("Player " + player.getName() + " connected");
			if (player1 == null) {
				player1 = player;
			} else {
				player2 = player;
				sendMessage(outbound, ALL_PLAYERS_ONLINE);
				sendMessage(connections.get(player1).getWsOutbound(), ALL_PLAYERS_ONLINE);
				log("All players connected");
			}
		}

		@Override
		protected void onTextMessage(CharBuffer data) throws IOException {
			String message = data.toString();
			Player player = getPlayer();
			log("Player " + player.getName() + " sent message -> " + message);
			String[] coords = message.split(MESSAGE_SEPARATOR);
			if (isCoordsDefinition(message)) {
				log("Coordinates definition");
				savePlayerShips(player, coords);
				playersReadyWithShips();
			} else if (isAttack(message)) {
				log("Attack");
				sendAtackMessage(player, coords);
			}
//			Player otherPlayer = getOtherPlayer();
//			sendMessage(connections.get(otherPlayer).getWsOutbound(), message);
		}
		
		private void savePlayerShips(Player player, String[] message) {
			for (int i = 1; i < message.length; i++) {
				String coord = message[i];
				int x = Integer.parseInt(String.valueOf(coord.charAt(0)));
				int y = Integer.parseInt(String.valueOf(coord.charAt(1)));
				Ship ship = new Ship(x, y);
				player.addShips(ship);
				log("Player " + player.getName() + " has new ship: " + ship);
			}
		}

		private void playersReadyWithShips() throws IOException {
			if (++setUpPlayers == 2) {
				for (NavalBattleConnection connection : connections.values()) {
					sendMessage(connection.getWsOutbound(), ALL_PLAYERS_HAVE_SHIPS);
				}
				log("All players have their ships");
			}
		}

		private void sendAtackMessage(Player player, String[] coords) {
			Player otherPlayer = getOtherPlayer();
			System.out.println(String.format("Player '%s' is attacking player '%s'", player.getName(), otherPlayer.getName()));
			String coord = coords[1];
			int x = Integer.parseInt(String.valueOf(coord.charAt(0)));
			int y = Integer.parseInt(String.valueOf(coord.charAt(1)));
			Ship ship = player.hasShipInPosition(x, y);
			if (ship != null) {
				System.out.println("ship down");
				System.out.println(ship);
			} else {
				System.out.println("you missed");
			}
		}

		private boolean playerAllowed() {
			return player1 == null || player2 == null;
		}

		private void sendMessage(WsOutbound outbound, String message) throws IOException {
			outbound.writeTextMessage(CharBuffer.wrap(message));
		}

		private Player getPlayer() {
			return player1.getId() == connectionId ? player1 : player2.getId() == connectionId ? player2 : null;
		}

		private Player getOtherPlayer() {
			return player1.getId() == connectionId ? player2 : player2.getId() == connectionId ? player1 : null;
		}

		private boolean isCoordsDefinition(String message) {
			return message.charAt(0) == COORDINATES_DEFINITION;
		}

		private boolean isAttack(String message) {
			return message.charAt(0) == ATTACK;
		}

		@Override
		protected void onBinaryMessage(ByteBuffer data) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void onClose(int status) {
			Player player = getPlayer();
			connections.remove(player);
			log("Player " + player.getName() + " left the game");
			if (player1.getId() == connectionId) {
				player1 = null;
			} else {
				player2 = null;
			}
		}
		
		public void log(String message) {
			System.out.println("LOG: " + message);
		}

	}

}