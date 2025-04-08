package com.miniflag;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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

    //MAPA
    private Texture backgroundTexture;

    // PERSONAJE
    private float cubeX, cubeY;
    private float cubeSize = 50;
    // PERSONAJE - POSICION
    private float lastSentX = -1;
    private float lastSentY = -1;


    private ShapeRenderer shapeRenderer;

    // LLAVE
    private Texture itemTexture;
    private float itemX, itemY;
    private float itemSize = 150;

    // Para la fuente
    private BitmapFont font;
    private TextButton exitButton;

    //CONEXION
    private NetworkManager network;

    public GameScreen(MainGame game) {

        this.game = game;
        this.network = game.network;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Skin skin = new Skin(Gdx.files.internal("skin/flat-earth-ui.json"));

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
        exitButton.setPosition(Gdx.graphics.getWidth()* 0.9f , Gdx.graphics.getHeight()* 0.9f);

        stage.addActor(exitButton);

        // Touchpad para el player
        TextureRegion touchpadBackground = skin.getRegion("touchpad");
        TextureRegion touchpadKnob = skin.getRegion("touchpad-knob");

        TextureRegionDrawable backgroundDrawable = new TextureRegionDrawable(touchpadBackground);
        TextureRegionDrawable knobDrawable = new TextureRegionDrawable(touchpadKnob);
        knobDrawable.setMinWidth(touchpadKnob.getRegionWidth() * 5.5f);
        knobDrawable.setMinHeight(touchpadKnob.getRegionHeight() * 5.5f);

        TouchpadStyle touchpadStyle = new TouchpadStyle();
        touchpadStyle.background = backgroundDrawable;
        touchpadStyle.knob = knobDrawable;

        touchpad = new Touchpad(10, touchpadStyle);
        touchpad.setBounds(90, 90, Gdx.graphics.getWidth() * 0.15f, Gdx.graphics.getWidth() * 0.15f);

        stage.addActor(touchpad);

        //MAPA
        backgroundTexture = new Texture("game_assets/map/mapa_estatico.jpg");

        // Personaje
        cubeX = Gdx.graphics.getWidth() / 2f - cubeSize / 2f;
        cubeY = Gdx.graphics.getHeight() / 2f - cubeSize / 2f;

        shapeRenderer = new ShapeRenderer();

        // Llave
        itemTexture = new Texture("game_assets/items/game_key.png");
        get_initial_key();

    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        // Movimiento jugador
        float touchpadX = touchpad.getKnobPercentX();
        float touchpadY = touchpad.getKnobPercentY();

        float moveSpeed = 5f;  // Velocidad jugador
        cubeX += touchpadX * moveSpeed;
        cubeY += touchpadY * moveSpeed;

        // LIMITES PANTALLA
        cubeX = Math.max(0, Math.min(cubeX, Gdx.graphics.getWidth() - cubeSize));
        cubeY = Math.max(0, Math.min(cubeY, Gdx.graphics.getHeight() - cubeSize));

        // DIBUJAR MAPA
        batch.begin();
        batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        // DIBUJAR PLAYER(Temporal)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.BLUE);
        shapeRenderer.rect(cubeX, cubeY, cubeSize, cubeSize);
        shapeRenderer.end();

        // DIBUJAR ITEM
        batch.begin();
        batch.draw(itemTexture, itemX, itemY, itemSize, itemSize);
        batch.end();

        // UPDATE SCENARIO
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();

        if (exitButton.isPressed()) {
            game.startMenu();
        }

        if (network != null && network.isConnected()) {
            if (cubeX != lastSentX || cubeY != lastSentY) {
                String message = String.format(
                    "{\"type\":\"position\", \"x\": %.2f, \"y\": %.2f}",
                    cubeX, cubeY
                );
                network.sendData(message);

                lastSentX = cubeX;
                lastSentY = cubeY;
            }
        }
    }
    public void get_initial_key() {
        System.out.println("Solicitando la posición de la llave...");

        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        String url = "http://" + network.address + ":" + network.port + "/item-position";

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
        stage.getViewport().update(width, height, true);  // Ajusta el viewport cuando la ventana cambia de tamaño
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

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
