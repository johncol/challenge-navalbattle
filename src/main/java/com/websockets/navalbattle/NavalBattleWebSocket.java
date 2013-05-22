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
	
	private static boolean gameCompleted = false; 

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
				if (morePlayersAllowed()) {
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
			log("Player " + player + " connected");
			if (player1 == null) {
				player1 = player;
			} else {
				player2 = player;
			}
			if (!morePlayersAllowed()) {
				sendMessage(getSocket(player1), ALL_PLAYERS_ONLINE);
				sendMessage(getSocket(player2), ALL_PLAYERS_ONLINE);
				log("All players connected");
			}
		}

		@Override
		protected void onTextMessage(CharBuffer data) throws IOException {
			String message = data.toString();
			Player player = getPlayer();
			log("Player " + player + " sent message -> " + message);
			String[] coords = message.split(MESSAGE_SEPARATOR);
			if (isCoordsDefinition(message)) {
				log("Coordinates definition");
				savePlayerShips(player, coords);
				setUpPlayers++;
				if (playersReadyWithShips()) {
					sendPlayersReadyMessage();
					playerPlays(player1);
				}
			} else if (isAttack(message)) {
				log("Attack:");
				Player otherPlayer = getOtherPlayer();
				boolean endGame = attack(player, otherPlayer, coords);
				if (!endGame) {
					playerPlays(otherPlayer);
				} else {
					sendMessage(getSocket(player), WINNER);
					sendMessage(getSocket(otherPlayer), LOSER);
					gameCompleted = true;
					getSocket(player).close(GAME_COMPLETED, null);
				}
			}
		}

		private WsOutbound getSocket(Player player) {
			return connections.get(player).getWsOutbound();
		}

		private void playerPlays(Player player) throws IOException {
			Player otherPlayer = getOtherPlayer(player);
			sendMessage(getSocket(player), YOU_PLAY);
			sendMessage(getSocket(otherPlayer), YOU_WAIT);
		}
		
		private void savePlayerShips(Player player, String[] message) {
			for (int i = 1; i < message.length; i++) {
				String coord = message[i];
				int x = Integer.parseInt(String.valueOf(coord.charAt(0)));
				int y = Integer.parseInt(String.valueOf(coord.charAt(1)));
				Ship ship = new Ship(x, y);
				player.addShips(ship);
				log("Player " + player + " has new ship: " + ship);
			}
		}

		private void sendPlayersReadyMessage() throws IOException {
			for (NavalBattleConnection connection : connections.values()) {
				sendMessage(connection.getWsOutbound(), ALL_PLAYERS_HAVE_SHIPS);
			}
			log("All players have their ships");
		}

		private boolean attack(Player player, Player otherPlayer, String[] coords) throws IOException {
			String coord = coords[1];
			log(String.format("Player '%s' is attacking player '%s' in position %s", player.getName(), otherPlayer.getName(), coord));
			int x = Integer.parseInt(String.valueOf(coord.charAt(0)));
			int y = Integer.parseInt(String.valueOf(coord.charAt(1)));
			Ship ship = otherPlayer.hasShipInPosition(x, y);
			String resultPlayer, resultOtherPlayer;
			boolean endBattle = false;
			if (ship != null && ship.isAlive()) {
				ship.sink();
				resultPlayer = createAttackResponseMessage(true, false, coord);
				resultOtherPlayer = createAttackResponseMessage(true, true, coord);
				log("Ship down: " + ship);
				endBattle = gameOver(otherPlayer);
			} else {
				resultPlayer = createAttackResponseMessage(false, false, coord);
				resultOtherPlayer = createAttackResponseMessage(false, true, coord);
				log("Shot missed");
			}
			log(resultPlayer);
			log(resultOtherPlayer);
			sendMessage(getSocket(player), resultPlayer);
			sendMessage(getSocket(otherPlayer), resultOtherPlayer);
			return endBattle;
		}

		private String createAttackResponseMessage(boolean hit, boolean target, String coord) {
			String shot = hit ? HIT : MISSED;
			String rol = target ? YOU_ARE_THE_TARGET : YOU_ARE_NOT_THE_TARGET;
			return shot + "-" + rol + "-" + coord;
		}

		private boolean gameOver(Player player) {
			return !player.hasShipsAlive();
		}

		private void sendMessage(WsOutbound outbound, String message) throws IOException {
			outbound.writeTextMessage(CharBuffer.wrap(message));
		}

		private boolean playersReadyWithShips() {
			return setUpPlayers == 2;
		}

		private boolean morePlayersAllowed() {
			return player1 == null || player2 == null;
		}

		private Player getPlayer() {
			return player1.getId() == connectionId ? player1 : player2;
		}

		private Player getOtherPlayer() {
			return player1.getId() == connectionId ? player2 : player1;
		}

		private Player getOtherPlayer(Player player) {
			return player.equals(player1) ? player2 : player1;
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
			if (!gameCompleted) {
				try {
					Player player = getPlayer();
					Player otherPlayer = getOtherPlayer();
					log("Player " + player + " left the game");
					sendMessage(getSocket(otherPlayer), ENEMY_LEFT);
				} catch (IOException e) {
					log("Error sending close message");
				} catch (NullPointerException ignore) {
				}
			}
			connections.clear();
			setUpPlayers = 0;
			gameCompleted = false;
			player1 = null;
			player2 = null;
		}

		public void log(String message) {
			System.out.println("LOG: " + message);
		}

	}

}