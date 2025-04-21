package com.miniflag;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.graphics.OrthographicCamera;
import java.util.ArrayList;

public class GameScreen implements Screen {
    private MainGame game;
    private SpriteBatch batch;
    // Usaremos dos stages: uno para el mundo (si fuera necesario) y otro para la UI (hudStage)
    // Como la mayoría de la funcionalidad de UI se basa en el stage, aquí usaremos hudStage.
    private Stage hudStage;
    private Touchpad touchpad;
    private NetworkManager conn;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private ArrayList<Texture> idleCharacters;
    private ArrayList<Texture> runCharacters;
    private Animation<TextureRegion>[][] idleAnimations;
    private Animation<TextureRegion>[][] runAnimations;

    // MAPA
    private Texture backgroundTexture;
    private float stateTime = 0;

    // PROPIEDADES DEL PLAYER LOCAL
    private float cubeX, cubeY;
    private float cubeSize = 250f;
    private float lastSentX = -1;
    private float lastSentY = -1;

    private ShapeRenderer shapeRenderer;

    // OBJETO (FLAG)
    private Texture itemTexture;
    private float itemX, itemY;
    private float itemSize = 250f;

    private final String[] COLORS = {"green", "blue", "darkgreen"};
    private final String[] DIRECTIONS = {"down", "up", "left", "right"};
    private final float FRAME_DURATION = 0.1f;
    private final float SCREEN_WIDTH = 1920f;
    private final float SCREEN_HEIGHT = 1080f;
    // Dimensiones del mundo (para posicionar fondo, jugadores, objetos)
    private final float WORLD_WIDTH = 4000f;
    private final float WORLD_HEIGHT = 3000f;

    // FUENTE E INTERFAZ
    private BitmapFont font;
    private TextButton exitButton;

    public GameScreen(MainGame game) {
        this.game = game;
        this.conn = game.network;
        // Crear la cámara y el viewport para el mundo
        camera = new OrthographicCamera();
        viewport = new FitViewport(SCREEN_WIDTH, SCREEN_HEIGHT, camera);
        viewport.apply();
    }

    // Método para transformar el valor del joystick en dirección
    protected String virtualJoystickControl() {
        float x = touchpad.getKnobPercentX();
        float y = touchpad.getKnobPercentY();
        float threshold = 0.2f;
        if (Math.abs(x) < threshold && Math.abs(y) < threshold) {
            return "none";
        }
        return (Math.abs(x) > Math.abs(y)) ? (x > 0 ? "right" : "left")
            : (y > 0 ? "up" : "down");
    }

    // Lógica del juego: envía la dirección obtenida
    private void gameLogic() {
        String direction = virtualJoystickControl();
        System.out.println(direction);

        conn.sendData("{\"type\":\"direction\", \"value\":\"" + direction + "\"}");

    }

    @Override
    public void show() {
        // Inicializamos el SpriteBatch para el mundo
        batch = new SpriteBatch();
        // Configuramos la cámara con una posición inicial centrada
        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0);
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        // Creamos un stage para la UI con un viewport fijo (ScreenViewport) que no se mueve
        hudStage = new Stage(new ScreenViewport());
        // Establece el stage de entrada para la UI
        Gdx.input.setInputProcessor(hudStage);

        FileHandle f = Gdx.files.internal("skin/flat-earth-ui.json");
        Skin skin = new Skin(f);

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
        hudStage.addActor(exitButton);

        // Configuración del Touchpad (joystick)
        TextureRegion touchpadKnob = skin.getRegion("touchpad-knob");
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 0);
        pixmap.fill();
        Texture transparentTexture = new Texture(pixmap);
        pixmap.dispose();
        TextureRegionDrawable transparentBackgroundDrawable = new TextureRegionDrawable(new TextureRegion(transparentTexture));

        TextureRegionDrawable knobDrawable = new TextureRegionDrawable(touchpadKnob);
        knobDrawable.setMinWidth(touchpadKnob.getRegionWidth() * 5.5f);
        knobDrawable.setMinHeight(touchpadKnob.getRegionHeight() * 5.5f);

        Touchpad.TouchpadStyle touchpadStyle = new Touchpad.TouchpadStyle();
        touchpadStyle.background = transparentBackgroundDrawable;
        touchpadStyle.knob = knobDrawable;

        touchpad = new Touchpad(10, touchpadStyle);
        // Posicionar el joystick en coordenadas de pantalla (fijas)
        touchpad.setBounds(90, 90, Gdx.graphics.getWidth() * 0.15f, Gdx.graphics.getWidth() * 0.15f);
        hudStage.addActor(touchpad);

        // Cargar el fondo (mapa)
        backgroundTexture = new Texture("game_assets/map/background.png");

        // Cargar personajes y animaciones
        idleCharacters = new ArrayList<>();
        runCharacters = new ArrayList<>();
        idleAnimations = new Animation[4][4];
        runAnimations = new Animation[4][4];
        // No re–inicializamos la cámara ni el viewport para el mundo aquí
        for (int i = 0; i < COLORS.length; i++) {
            String color = COLORS[i];
            Texture idleTexture = new Texture("game_assets/sprites/orc_" + color + "_idle_full.png");
            Texture runTexture = new Texture("game_assets/sprites/orc_" + color + "_walk_full.png");
            idleCharacters.add(idleTexture);
            runCharacters.add(runTexture);
            TextureRegion[][] idleTmp = TextureRegion.split(idleTexture, 64, 64);
            TextureRegion[][] runTmp = TextureRegion.split(runTexture, 64, 64);
            for (int j = 0; j < 4; j++) { // Por cada dirección (fila)
                TextureRegion[] idleFrames = new TextureRegion[4];
                for (int k = 0; k < 4; k++) { // 4 columnas en idle
                    idleFrames[k] = idleTmp[j][k];
                }
                idleAnimations[i][j] = new Animation<>(FRAME_DURATION, idleFrames);

                TextureRegion[] runFrames = new TextureRegion[6];
                for (int k = 0; k < 6; k++) { // 6 columnas en run
                    runFrames[k] = runTmp[j][k];
                }
                runAnimations[i][j] = new Animation<>(FRAME_DURATION, runFrames);
            }
        }

        // Posición inicial del jugador local
        cubeX = Gdx.graphics.getWidth() / 2f - cubeSize / 2f;
        cubeY = Gdx.graphics.getHeight() / 2f - cubeSize / 2f;

        shapeRenderer = new ShapeRenderer();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        stateTime += delta;

        // Actualizar la cámara (para el mundo) según la posición del jugador local, obtenida del gameState si está disponible
        if (conn.gameState != null && conn.gameState.has("players")) {
            JsonValue players = conn.gameState.get("players");
            for (int i = 0; i < players.size; i++) {
                JsonValue player = players.get(i);
                if (player.getString("id").equals(conn.playerId)) {
                    float localX = player.getFloat("x") * WORLD_WIDTH;
                    float localY = (1f - player.getFloat("y")) * WORLD_HEIGHT;
                    camera.position.set(localX, localY, 0);
                    break;
                }
            }
        } else {
            camera.position.set(cubeX + cubeSize / 2, cubeY + cubeSize / 2, 0);
        }
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        // Lógica del juego: enviar datos según joystick
        gameLogic();

        // Dibujo del mundo (fondo, jugadores, objeto) usando la cámara que sigue al jugador
        batch.begin();
        batch.draw(backgroundTexture, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.end();

        if (conn.gameState != null && conn.gameState.has("players")) {
            batch.begin();
            JsonValue players = conn.gameState.get("players");
            for (int i = 0; i < players.size; i++) {
                JsonValue player = players.get(i);
                drawPlayer(player, COLORS[i]);
            }
            batch.end();
        }

        if (conn.gameState != null && conn.gameState.has("flagPos")) {
            batch.begin();
            itemX = conn.gameState.get("flagPos").getFloat("dx") * WORLD_WIDTH;
            itemY = (1f - conn.gameState.get("flagPos").getFloat("dy")) * WORLD_HEIGHT;
            // Se recomienda cargar itemTexture solo una vez (por ejemplo, en show)
            if (itemTexture == null) {
                itemTexture = new Texture("game_assets/items/flag.png");
            }
            batch.draw(itemTexture, itemX, itemY, itemSize, itemSize + 50);
            batch.end();
        }

        // Dibujo de la interfaz (UI) usando el hudStage, que tiene un viewport fijo
        hudStage.act(Math.min(delta, 1 / 30f));
        hudStage.draw();

        if (exitButton.isPressed()) {
            game.startMenu();
        }

        // Enviar posición actual (si ha cambiado) para sincronización
        if (conn != null && conn.isConnected()) {
            if (cubeX != lastSentX || cubeY != lastSentY) {
                String message = String.format("{\"type\":\"position\", \"x\": %.2f, \"y\": %.2f}", cubeX, cubeY);
                conn.sendData(message);
                lastSentX = cubeX;
                lastSentY = cubeY;
            }
        }
    }

    private void drawPlayer(JsonValue player, String color) {
        if (!player.has("direction") || !player.has("moving")) {
            return;
        }
        float playerX = player.getFloat("x") * WORLD_WIDTH;
        float playerY = (1f - player.getFloat("y")) * WORLD_HEIGHT;

        if(player.getString("id").equals(conn.playerId)) {
            camera.position.set(playerX, playerY, 0);
            camera.update();
            batch.setProjectionMatrix(camera.combined);
        }

        String direction = player.getString("direction");
        boolean moving = player.getBoolean("moving");
        TextureRegion currentFrame = null;
        for (int i = 0; i < COLORS.length; i++) {
            for (int j = 0; j < DIRECTIONS.length; j++) {
                if (DIRECTIONS[j].equals(direction) && COLORS[i].equals(color)) {
                    currentFrame = moving
                        ? runAnimations[i][j].getKeyFrame(stateTime, true)
                        : idleAnimations[i][j].getKeyFrame(stateTime, true);
                }
            }
        }
        if (currentFrame == null) {
            return;
        }
        batch.draw(currentFrame, playerX, playerY, cubeSize, cubeSize);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        hudStage.getViewport().update(width, height);
    }

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void hide() {
        // Se pueden realizar limpiezas aquí si se desea, sin llamar a dispose() directamente
    }

    @Override
    public void dispose() {
        batch.dispose();
        hudStage.dispose();
        backgroundTexture.dispose();
        shapeRenderer.dispose();
        if (itemTexture != null) itemTexture.dispose();
        font.dispose();
    }
}
