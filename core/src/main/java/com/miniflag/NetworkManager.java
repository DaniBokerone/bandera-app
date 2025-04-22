package com.miniflag;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.WebSocketListener;
import com.github.czyzby.websocket.WebSockets;

public class NetworkManager {
    // Dirección del host y puerto
    String address = "bandera5.ieti.site";
    int port = 443;
//    String address = "10.0.2.2";
//    int port = 3000;
    private WebSocket socket;
    private boolean isConnected = false;
    public JsonValue gameState;
    private static NetworkManager instance;
    public String playerId;

    //MANEJAR CONTADOR JUGADORES
    public interface PlayerCountListener {
        void onPlayerCountUpdate(int count);
    }
    private PlayerCountListener playerCountListener;

    public void setPlayerCountListener(PlayerCountListener listener) {
        this.playerCountListener = listener;
    }

    private NetworkManager() {
        System.out.println("Iniciando NetworkManager...");


       // String wsUrl = "wss://" + address ;
        String wsUrl = "wss://" + address + "?role=player";
//        String wsUrl = "ws://" + address + ":" + port ;

        System.out.println("Conectando a: " + wsUrl);

        // Se crea el socket utilizando la URL de WebSocket configurada
        socket = WebSockets.newSocket(wsUrl);

        socket.setSendGracefully(false);
        socket.addListener(new MyWebSocketListener());

        socket.connect();
    }

    public static synchronized NetworkManager getInstance() {
           if (instance == null) {
                  instance = new NetworkManager();
           }
           return instance;
    }

//    public static NetworkManager getInstance() {
//        if(instance == null) {
//            instance = new NetworkManager();
//        }
//        return instance;
//    }


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

    public void testHttpConnection() {
        System.out.println("Intentando conexion HTTP...");
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        String fullUrl = "http://" + address +  "/test";
//        String fullUrl = "http://" + address + ":" + port + "/test";


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
            //socket.send("Hola servidor desde MiniFlag!");
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
//            System.out.println("Mensaje recibido: " + packet);
            JsonReader reader = new JsonReader();
            JsonValue response = reader.parse(packet);

            if(response.has("type")) {
                if(response.getString("type").equals("update")) {
                    if(response.has("gameState")) {
                        gameState = response.get("gameState");

                    }
                }else if(response.getString("type").equals("playerCount")) {
                    try {
                        int count = Integer.parseInt(packet.replaceAll("[^0-9]", ""));
                        if (playerCountListener != null) {
                            playerCountListener.onPlayerCountUpdate(count);
                        }
                    } catch (Exception e) {
                        System.out.println("Error al parsear playerCount: " + e.getMessage());
                    }
                }else if(response.getString("type").equals("welcome")) {
                    playerId = response.getString("id");
                    Gdx.app.log("ID", playerId);
                }
            }

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
