package com.miniflag;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad.TouchpadStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import org.w3c.dom.Text;
import java.util.ArrayList;
import com.badlogic.gdx.graphics.g2d.Animation;


public class GameScreen implements Screen {
    private MainGame game;
    private SpriteBatch batch;
    private Stage stage;
    private Touchpad touchpad;
    private NetworkManager conn;
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
    // Para evitar envíos constantes
    private float lastSentX = -1;
    private float lastSentY = -1;

    private ShapeRenderer shapeRenderer;

    // LLAVE (u objeto)
    private Texture itemTexture;
    private float itemX, itemY;
    private float itemSize = 250f;

    private final String[] COLORS = {"green", "blue", "darkgreen"};
    private final String[] DIRECTIONS = {"down", "up", "left", "right"};
    private final float FRAME_DURATION = 0.1f;

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

        // *********************
        // Cargar el fondo
        backgroundTexture = new Texture("game_assets/map/background.png");

        // Characters load
        idleCharacters = new ArrayList<>();
        runCharacters = new ArrayList<>();
        idleAnimations = new Animation[4][4];
        runAnimations = new Animation[4][4];
        for (int i = 0; i < COLORS.length; i++) {
            String color = COLORS[i];

            // Carga las texturas de sprites
            Texture idleTexture = new Texture("game_assets/sprites/orc_"+ color + "_idle_full.png");
            Texture runTexture = new Texture("game_assets/sprites/orc_"+ color + "_walk_full.png");

            idleCharacters.add(idleTexture);
            runCharacters.add(runTexture);


            TextureRegion[][] idleTmp = TextureRegion.split(idleTexture, 64, 64);
            TextureRegion[][] runTmp = TextureRegion.split(runTexture, 64, 64);

            for (int j = 0; j < 4; j++) { // Para cada dirección (fila)
                // Para idle: 4 frames por fila
                TextureRegion[] idleFrames = new TextureRegion[4];
                for (int k = 0; k < 4; k++) { // 4 columnas en idle
                    idleFrames[k] = idleTmp[j][k];
                }
                idleAnimations[i][j] = new Animation<>(FRAME_DURATION, idleFrames);

                // Para correr: 6 frames por fila
                TextureRegion[] runFrames = new TextureRegion[6];
                for (int k = 0; k < 6; k++) { // 6 columnas en run
                    runFrames[k] = runTmp[j][k];
                }
                runAnimations[i][j] = new Animation<>(FRAME_DURATION, runFrames);
            }
        }

        // Inicializar el player local al centro de la pantalla ??
        cubeX = Gdx.graphics.getWidth() / 2f - cubeSize / 2f;
        cubeY = Gdx.graphics.getHeight() / 2f - cubeSize / 2f;

        shapeRenderer = new ShapeRenderer();
    }

    @Override
    public void render(float delta) {

        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        stateTime += Gdx.graphics.getDeltaTime();


        // Límite de pantalla ??
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
             batch.begin();
//            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
//            shapeRenderer.setColor(Color.BLUE);
//
//            for (JsonValue player : conn.gameState.get("players")) {
//                cubeX = player.getFloat("x") * Gdx.graphics.getWidth();
//                cubeY = (1f - player.getFloat("y")) * Gdx.graphics.getHeight();
//              //  System.out.println("Estoy pintando: " + cubeX + " " +  cubeY);
//                shapeRenderer.rect(cubeX, cubeY, cubeSize, cubeSize);
//            }
//
//            shapeRenderer.end();
            JsonValue players = conn.gameState.get("players");
            for(int i = 0; i < players.size; i++) {
                JsonValue player = players.get(i);
                drawPlayer(player, COLORS[i]);

            }
            batch.end();
        }


        if(conn.gameState.has("flagPos")) {
            batch.begin();
           // System.out.println(conn.gameState);
            itemX = conn.gameState.get("flagPos").getFloat("dx") * Gdx.graphics.getWidth();
            itemY = (1f - conn.gameState.get("flagPos").getFloat("dy")) * Gdx.graphics.getHeight();

            itemTexture = new Texture("game_assets/items/flag.png");

            batch.draw(itemTexture, itemX, itemY, itemSize, itemSize + 50);
            batch.end();
        }

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

    private void drawPlayer(JsonValue player, String color) {
        if(!player.has("direction") || !player.has("moving")) {
            return;
        }
        float playerX = player.getFloat("x") * Gdx.graphics.getWidth();
        float playerY = (1f - player.getFloat("y")) * Gdx.graphics.getHeight();
        String direction = player.getString("direction");
        boolean moving = player.getBoolean("moving");
        TextureRegion currentFrame = null;
        for(int i = 0; i < COLORS.length; i++) {
            for(int j = 0; j < DIRECTIONS.length; j++) {
                if(DIRECTIONS[j].equals(direction) && COLORS[i].equals(color)) {
                    if(moving) {
                        currentFrame = runAnimations[i][j].getKeyFrame(stateTime, true);
                    }else {
                        currentFrame = idleAnimations[i][j].getKeyFrame(stateTime, true);
                    }
                }
            }
        }
        if(currentFrame == null) {
            return;
        }
        batch.draw(currentFrame, playerX, playerY, cubeSize, cubeSize);

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
