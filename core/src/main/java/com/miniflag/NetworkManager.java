package com.miniflag;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.WebSocketListener;
import com.github.czyzby.websocket.WebSockets;

public class NetworkManager {
    // Dirección del host y puerto
    private String address = "bandera5.ieti.site";
    private int port = 443;
    private WebSocket socket;
    private boolean isConnected = false;

    public NetworkManager() {
        System.out.println("Iniciando NetworkManager...");

        // Construimos la URL con el esquema ws:// (no wss://) ya que el servidor no tiene TLS,
        // y se conecta a la ruta '/test'
        String wsUrl = "wss://" + address + "/test";
        System.out.println("Conectando a: " + wsUrl);

        // Se crea el socket utilizando la URL de WebSocket configurada
        socket = WebSockets.newSocket(wsUrl);

        socket.setSendGracefully(false);
        socket.addListener(new MyWebSocketListener());

        socket.connect();
    }

    public void sendData(String data) {
        if (isConnected && socket != null && socket.isOpen()) {
            socket.send(data);
            System.out.println("Datos enviados: " + data);
        } else {
            System.out.println("No conectado. No se pueden enviar datos.");
        }
    }

    public void disconnect() {
        if (socket != null && socket.isOpen()) {
            socket.close();
            System.out.println("Desconectado del servidor.");
        }
        isConnected = false;
    }

    public boolean isConnected() {
        return isConnected;
    }

    // Método para probar la conexión HTTP hacia el servidor en /test
    // Nota: Si estás usando Android 9 (API 28) o superior, asegúrate de configurar
    // el tráfico en texto plano (HTTP) en el archivo de seguridad de red, o usar HTTPS.
    public void testHttpConnection() {
        System.out.println("Intentando conexión HTTP...");
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        String fullUrl = "https://" + address + ":" + port + "/test";

        Net.HttpRequest request = requestBuilder.newRequest()
            .method(Net.HttpMethods.GET)
            .url(fullUrl)
            .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                String response = httpResponse.getResultAsString();
                System.out.println("Respuesta del servidor HTTP: " + response);
            }

            @Override
            public void failed(Throwable t) {
                System.out.println("Error al conectar con el servidor HTTP: " + t.getMessage());
            }

            @Override
            public void cancelled() {
                System.out.println("Petición HTTP cancelada.");
            }
        });
    }

    // Listener para gestionar eventos en la conexión WebSocket
    private class MyWebSocketListener implements WebSocketListener {
        @Override
        public boolean onOpen(WebSocket webSocket) {
            System.out.println("Conexión WebSocket abierta.");
            isConnected = true;
            // Envía un mensaje inicial al establecer la conexión
            socket.send("Hola servidor desde MiniFlag!");
            return true;
        }

        @Override
        public boolean onClose(WebSocket webSocket, int closeCode, String reason) {
            System.out.println("Conexión cerrada. Código: " + closeCode + ", Razón: " + reason);
            isConnected = false;
            return true;
        }

        @Override
        public boolean onMessage(WebSocket webSocket, String packet) {
            System.out.println("Mensaje recibido: " + packet);
            return true;
        }

        @Override
        public boolean onMessage(WebSocket webSocket, byte[] packet) {
            System.out.println("Mensaje recibido (bytes). Longitud: " + packet.length);
            return true;
        }

        @Override
        public boolean onError(WebSocket webSocket, Throwable error) {
            System.out.println("ERROR en WebSocket: " + error.getMessage());
            return true;
        }
    }
}
