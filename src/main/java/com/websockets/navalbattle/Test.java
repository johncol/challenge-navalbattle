package com.websockets.navalbattle;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsOutbound;

@WebServlet(urlPatterns = "/test")
public class Test extends WebSocketServlet {

	private static final long serialVersionUID = -4639805760793121651L;

	@Override
	protected StreamInbound createWebSocketInbound(String arg0, HttpServletRequest arg1) {
		return new WebSocketConnection();
	}
	
	private static class WebSocketConnection extends MessageInbound {

		@Override
		protected void onBinaryMessage(ByteBuffer arg0) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void onTextMessage(CharBuffer data) throws IOException {
			String returnMessage = String.format("Message received! '%s'", data.toString());
			System.out.println(returnMessage);
			getWsOutbound().writeTextMessage(CharBuffer.wrap(returnMessage));
		}

		@Override
		protected void onClose(int status) {
			super.onClose(status);
			System.out.println("Websocket closed width status code " + status);
		}
		
		@Override
		protected void onOpen(WsOutbound outbound) {
			super.onOpen(outbound);
			System.out.println("Websocket opened");
		}
		
	}

}