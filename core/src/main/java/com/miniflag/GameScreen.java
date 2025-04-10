package com.miniflag;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad.TouchpadStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class GameScreen implements Screen {
    private MainGame game;
    private SpriteBatch batch;
    private Stage stage;
    private Touchpad touchpad;
    private NetworkManager conn;  // Usamos conn (o network) para enviar los datos

    // MAPA
    private Texture backgroundTexture;

    // PROPIEDADES DEL PLAYER LOCAL
    private float cubeX, cubeY;
    private float cubeSize = 50f;
    // Para evitar envíos constantes
    private float lastSentX = -1;
    private float lastSentY = -1;

    private ShapeRenderer shapeRenderer;

    // LLAVE (u objeto)
    private Texture itemTexture;
    private float itemX, itemY;
    private float itemSize = 200f;

    // Fuente e interfaz
    private BitmapFont font;
    private TextButton exitButton;

    public GameScreen(MainGame game) {
        this.game = game;
        this.conn = game.network; // Se asume que la instancia de NetworkManager está en game.network
    }

    // Nuevo método que transforma los valores del joystick en una dirección
    protected String virtualJoystickControl() {
        // Obtiene valores normalizados (-1 a 1) del Touchpad
        //System.out.println(touchpad.getKnobPercentX());
        float x = touchpad.getKnobPercentX();
        float y = touchpad.getKnobPercentY();
        // Se define un umbral para descartar movimientos muy pequeños
        float threshold = 0.2f;
        if (Math.abs(x) < threshold && Math.abs(y) < threshold) {
            return "none";
        }
        // Se decide la dirección en función del mayor valor absoluto
        if (Math.abs(x) > Math.abs(y)) {
            return x > 0 ? "right" : "left";
        } else if (Math.abs(x) < Math.abs(y)) {
            return y > 0 ? "up" : "down";
        } else {
            return "none";
        }
    }

    // Lógica del juego: envía el mensaje de dirección obtenido del joystick
    private void gameLogic() {
        String direction = virtualJoystickControl();
        System.out.println(direction);
        // Envía solo si la dirección es distinta a "none"

            conn.sendData("{\"type\":\"direction\", \"value\":\"" + direction + "\"}");

    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Skin skin = new Skin(Gdx.files.internal("skin/flat-earth-ui.json"));

        // Configuración de la fuente usando FreeType
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Pixel_font.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = (int) (Gdx.graphics.getWidth() * 0.025f);
        font = generator.generateFont(parameter);
        generator.dispose();

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.up = skin.getDrawable("button-c");
        buttonStyle.down = skin.getDrawable("button-p");

        exitButton = new TextButton("Menu", buttonStyle);
        exitButton.setPosition(Gdx.graphics.getWidth() * 0.9f, Gdx.graphics.getHeight() * 0.9f);
        stage.addActor(exitButton);

        // Configuración del Touchpad (joystick)
        TextureRegion touchpadKnob = skin.getRegion("touchpad-knob");
        // Crear drawable transparente para el fondo del touchpad
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 0);
        pixmap.fill();
        Texture transparentTexture = new Texture(pixmap);
        pixmap.dispose();
        TextureRegionDrawable transparentBackgroundDrawable = new TextureRegionDrawable(new TextureRegion(transparentTexture));

        // Configuración del knob
        TextureRegionDrawable knobDrawable = new TextureRegionDrawable(touchpadKnob);
        knobDrawable.setMinWidth(touchpadKnob.getRegionWidth() * 5.5f);
        knobDrawable.setMinHeight(touchpadKnob.getRegionHeight() * 5.5f);

        TouchpadStyle touchpadStyle = new TouchpadStyle();
        touchpadStyle.background = transparentBackgroundDrawable;
        touchpadStyle.knob = knobDrawable;

        touchpad = new Touchpad(10, touchpadStyle);
        touchpad.setBounds(90, 90, Gdx.graphics.getWidth() * 0.15f, Gdx.graphics.getWidth() * 0.15f);
        stage.addActor(touchpad);

        // Cargar el fondo
        backgroundTexture = new Texture("game_assets/map/background.png");

        // Inicializar el player local al centro de la pantalla
        cubeX = Gdx.graphics.getWidth() / 2f - cubeSize / 2f;
        cubeY = Gdx.graphics.getHeight() / 2f - cubeSize / 2f;

        shapeRenderer = new ShapeRenderer();

        // Cargar la textura del objeto (ej. llave u orbe)
        itemTexture = new Texture("game_assets/items/flag.png");
        get_initial_key();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        // --- Actualiza la posición del player local usando el joystick ---
//        float touchpadX = touchpad.getKnobPercentX();
//        float touchpadY = touchpad.getKnobPercentY();
//        float moveSpeed = 5f;
//        cubeX += touchpadX * moveSpeed;
//        cubeY += touchpadY * moveSpeed;
        // Límite de pantalla
        cubeX = Math.max(0, Math.min(cubeX, Gdx.graphics.getWidth() - cubeSize));
        cubeY = Math.max(0, Math.min(cubeY, Gdx.graphics.getHeight() - cubeSize));

        // Llama a la lógica del juego para enviar la dirección al servidor según joystick
        gameLogic();

        // --- DIBUJO DEL MAPA ---
        batch.begin();
        batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        // Dibuja los jugadores con ShapeRenderer
        if (conn.gameState.has("players")) {

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.BLUE);

            for (JsonValue player : conn.gameState.get("players")) {
                cubeX = player.getFloat("x") * Gdx.graphics.getWidth();
                cubeY = (1f - player.getFloat("y")) * Gdx.graphics.getHeight();
              //  System.out.println("Estoy pintando: " + cubeX + " " +  cubeY);
                shapeRenderer.rect(cubeX, cubeY, cubeSize, cubeSize);
            }

            shapeRenderer.end();
        }

        // --- DIBUJO DE OBJETOS (por ejemplo, llave u orbe) ---
        batch.begin();
        batch.draw(itemTexture, itemX, itemY, itemSize, itemSize + 50);
        batch.end();

        // Actualiza y dibuja la interfaz de usuario (UI)
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();

        if (exitButton.isPressed()) {
            game.startMenu();
        }

        // Enviar posición actual (opcional, según lógica de tu juego)
        if (conn != null && conn.isConnected()) {
            if (cubeX != lastSentX || cubeY != lastSentY) {
                String message = String.format(
                    "{\"type\":\"position\", \"x\": %.2f, \"y\": %.2f}",
                    cubeX, cubeY
                );
                conn.sendData(message);
                lastSentX = cubeX;
                lastSentY = cubeY;
            }
        }
    }

    // Método para solicitar la posición inicial de un objeto (por ejemplo, llave) desde el servidor
    public void get_initial_key() {
        System.out.println("Solicitando la posición de la llave...");
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        String url = "https://" + conn.address + "/item-position";
        Net.HttpRequest request = requestBuilder.newRequest()
            .method(Net.HttpMethods.GET)
            .url(url)
            .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                String response = httpResponse.getResultAsString();
                System.out.println("Posición de la llave: " + response);
                try {
                    JsonReader jsonReader = new JsonReader();
                    JsonValue jsonResponse = jsonReader.parse(response);
                    itemX = jsonResponse.getFloat("x");
                    itemY = jsonResponse.getFloat("y");
                    System.out.println("Posición X: " + itemX + ", Posición Y: " + itemY);
                } catch (Exception e) {
                    System.out.println("Error al procesar la respuesta: " + e.getMessage());
                }
            }

            @Override
            public void failed(Throwable t) {
                System.out.println("Error al solicitar la posición de la llave: " + t.getMessage());
            }

            @Override
            public void cancelled() {
                System.out.println("Petición cancelada.");
            }
        });
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        batch.dispose();
        stage.dispose();
        backgroundTexture.dispose();
        shapeRenderer.dispose();
        itemTexture.dispose();
        font.dispose();
    }
}
