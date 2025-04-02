package com.miniflag;

import com.badlogic.gdx.Application;
import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.WebSocketListener;
import com.github.czyzby.websocket.WebSockets;

public class NetworkManager {
    private WebSocket socket;
    private String address = "localhost";
    private int port = 8888;
    private boolean isConnected = false;

    public NetworkManager() {
        System.out.println("NetworkManager funcionando sin conexión real.");
        this.isConnected = false;
    }

    public void connect() {
        // Simulamos la conexion
        System.out.println("Simulando la conexión a: " + address + ":" + port);
        isConnected = true;
    }

    public void sendData(String data) {
        if (isConnected) {
            System.out.println("Datos enviados (simulados): " + data);
        } else {
            System.out.println("No hay conexión real. Datos no enviados.");
        }
    }

    public void disconnect() {
        System.out.println("Simulando desconexión...");
        isConnected = false;
    }

    private class MyWebSocketListener implements WebSocketListener {
        @Override
        public boolean onOpen(WebSocket webSocket) {
            System.out.println("Conexión abierta... (simulada)");
            return false;
        }

        @Override
        public boolean onClose(WebSocket webSocket, int closeCode, String reason) {
            System.out.println("Conexión cerrada... (simulada)");
            return false;
        }

        @Override
        public boolean onMessage(WebSocket webSocket, String packet) {
            System.out.println("Mensaje recibido: " + packet + " (simulado)");
            return false;
        }

        @Override
        public boolean onMessage(WebSocket webSocket, byte[] packet) {
            System.out.println("Mensaje recibido (bytes): " + packet + " (simulado)");
            return false;
        }

        @Override
        public boolean onError(WebSocket webSocket, Throwable error) {
            System.out.println("ERROR: " + error.toString() + " (simulado)");
            return false;
        }
    }
}
