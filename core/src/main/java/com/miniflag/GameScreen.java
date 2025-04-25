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
    private boolean flagVisible = true;

    // OBJETO (BUILDING)
    private Texture buildingTexture;
    private float buildingX, buildingY;
    private float buildingSize = 450f;
    private boolean buildingVisible = true;


    private final String[] COLORS = {"green", "blue", "darkgreen"};
    private final String[] DIRECTIONS = {"down", "up", "left", "right"};
    private final float FRAME_DURATION = 0.1f;
    private final float SCREEN_WIDTH = 1920f;
    private final float SCREEN_HEIGHT = 1080f;
    private final float WORLD_WIDTH = 4000f;
    private final float WORLD_HEIGHT = 3000f;

    // FUENTE E INTERFAZ
    private BitmapFont font;
    private TextButton exitButton;

    public GameScreen(MainGame game) {
        this.game = game;
        this.conn = game.network;
        camera = new OrthographicCamera();
        viewport = new FitViewport(SCREEN_WIDTH, SCREEN_HEIGHT, camera);
        viewport.apply();
    }

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

    private void gameLogic() {
        String direction = virtualJoystickControl();
        //System.out.println(direction);
        conn.sendData("{\"type\":\"direction\", \"value\":\"" + direction + "\"}");
    }

    private void onFlagTouched() {
        System.out.println("¡Bandera tocada!");
    }

    private void winGame(String playerId) {
        System.out.println("¡Juego ganado!");
        if (conn != null && conn.isConnected()) {
            if (cubeX != lastSentX || cubeY != lastSentY) {
                String msg = String.format(
                    "{\"type\":\"endGame\"}",
                    playerId
                );
                conn.sendData(msg);
            }
        }
    }

    private boolean isPlayerTouchingFlag(float playerX, float playerY, float playerSize,
                                         float flagX, float flagY, float flagSize) {
        return playerX < flagX + flagSize &&
            playerX + playerSize > flagX &&
            playerY < flagY + flagSize &&
            playerY + playerSize > flagY;
    }


    private boolean isFlagInBuilding(
        float playerX, float playerY, float playerSize,
        float buildingX, float buildingY, float buildingSize) {
        // DEBUG: imprime todos los valores y el resultado de cada condición
        boolean cond1 = playerX <  buildingX + buildingSize;
        boolean cond2 = playerX + playerSize > buildingX;
        boolean cond3 = playerY <  buildingY + buildingSize;
        boolean cond4 = playerY + playerSize > buildingY;

        System.out.printf(
            "isFlagInBuilding → pX=%.1f, pY=%.1f, pS=%.1f, bX=%.1f, bY=%.1f, bS=%.1f | "
                + "cond1(pX < bX+bS)=%b, cond2(pX+pS > bX)=%b, "
                + "cond3(pY < bY+bS)=%b, cond4(pY+pS > bY)=%b%n",
            playerX, playerY, playerSize,
            buildingX, buildingY, buildingSize,
            cond1, cond2, cond3, cond4
        );

        return cond1 && cond2 && cond3 && cond4;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0);
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        hudStage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(hudStage);

        FileHandle f = Gdx.files.internal("skin/flat-earth-ui.json");
        Skin skin = new Skin(f);

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
            Gdx.files.internal("fonts/Pixel_font.ttf")
        );
        FreeTypeFontGenerator.FreeTypeFontParameter parameter =
            new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = (int) (Gdx.graphics.getWidth() * 0.025f);
        font = generator.generateFont(parameter);
        generator.dispose();

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.up = skin.getDrawable("button-c");
        buttonStyle.down = skin.getDrawable("button-p");

        exitButton = new TextButton("Menu", buttonStyle);
        exitButton.setPosition(
            Gdx.graphics.getWidth() * 0.9f,
            Gdx.graphics.getHeight() * 0.9f
        );
        hudStage.addActor(exitButton);

        TextureRegion touchpadKnob = skin.getRegion("touchpad-knob");
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 0);
        pixmap.fill();
        Texture transparentTexture = new Texture(pixmap);
        pixmap.dispose();
        TextureRegionDrawable transparentBackgroundDrawable =
            new TextureRegionDrawable(new TextureRegion(transparentTexture));

        TextureRegionDrawable knobDrawable = new TextureRegionDrawable(touchpadKnob);
        knobDrawable.setMinWidth(touchpadKnob.getRegionWidth() * 5.5f);
        knobDrawable.setMinHeight(touchpadKnob.getRegionHeight() * 5.5f);

        Touchpad.TouchpadStyle touchpadStyle = new Touchpad.TouchpadStyle();
        touchpadStyle.background = transparentBackgroundDrawable;
        touchpadStyle.knob = knobDrawable;

        touchpad = new Touchpad(10, touchpadStyle);
        touchpad.setBounds(
            90, 90,
            Gdx.graphics.getWidth() * 0.15f,
            Gdx.graphics.getWidth() * 0.15f
        );
        hudStage.addActor(touchpad);

        backgroundTexture = new Texture("game_assets/map/background.png");

        idleCharacters = new ArrayList<>();
        runCharacters = new ArrayList<>();
        idleAnimations = new Animation[4][4];
        runAnimations = new Animation[4][4];
        for (int i = 0; i < COLORS.length; i++) {
            String color = COLORS[i];
            Texture idleTexture = new Texture(
                "game_assets/sprites/orc_" + color + "_idle_full.png"
            );
            Texture runTexture = new Texture(
                "game_assets/sprites/orc_" + color + "_walk_full.png"
            );
            idleCharacters.add(idleTexture);
            runCharacters.add(runTexture);
            TextureRegion[][] idleTmp = TextureRegion.split(idleTexture, 64, 64);
            TextureRegion[][] runTmp = TextureRegion.split(runTexture, 64, 64);
            for (int j = 0; j < 4; j++) {
                TextureRegion[] idleFrames = new TextureRegion[4];
                for (int k = 0; k < 4; k++) {
                    idleFrames[k] = idleTmp[j][k];
                }
                idleAnimations[i][j] = new Animation<>(FRAME_DURATION, idleFrames);

                TextureRegion[] runFrames = new TextureRegion[6];
                for (int k = 0; k < 6; k++) {
                    runFrames[k] = runTmp[j][k];
                }
                runAnimations[i][j] = new Animation<>(FRAME_DURATION, runFrames);
            }
        }

        cubeX = Gdx.graphics.getWidth() / 2f - cubeSize / 2f;
        cubeY = Gdx.graphics.getHeight() / 2f - cubeSize / 2f;
        shapeRenderer = new ShapeRenderer();
    }

    @Override
    public void render(float delta) {
//        WebSockets.update();
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        stateTime += delta;

        if(conn.gameState != null) {
            if(conn.gameState.has("started")) {
                if(!conn.gameState.getBoolean("started")) {
                    game.setScreen(new MenuScreen((MainGame) game));
                }
            }
        }

        // 1) Encuentra tu jugador en el gameState
        float desiredX = cubeX + cubeSize/2;
        float desiredY = cubeY + cubeSize/2;
        if (conn.gameState != null && conn.gameState.has("players")) {
            JsonValue players = conn.gameState.get("players");
            for (int i = 0; i < players.size; i++) {
                JsonValue p = players.get(i);
                if (p.getString("id").equals(conn.playerId)) {
                    desiredX = p.getFloat("x") * WORLD_WIDTH;
                    desiredY = (1f - p.getFloat("y")) * WORLD_HEIGHT;
                    break;
                }
            }
        }
        // 2) Clampear la cámara para que no salga del mapa
        float halfW = viewport.getWorldWidth() * 0.5f;
        float halfH = viewport.getWorldHeight() * 0.5f;
        float camX = Math.min(Math.max(desiredX, halfW), WORLD_WIDTH  - halfW);
        float camY = Math.min(Math.max(desiredY, halfH), WORLD_HEIGHT - halfH);
        camera.position.set(camX, camY, 0);
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        cubeX = desiredX - cubeSize/2;
        cubeY = desiredY - cubeSize/2;

        // 3) Resto del render (sin tocar cámara en drawPlayer)
        gameLogic();

        batch.begin();
        batch.draw(backgroundTexture, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.end();

        if (conn.gameState != null && conn.gameState.has("players")) {
            batch.begin();
            JsonValue players = conn.gameState.get("players");
            for (int i = 0; i < players.size; i++) {
                JsonValue player = players.get(i);
                // clamp: no salirse de 0 .. COLORS.length-1
                int colorIndex = i;
                if (colorIndex >= COLORS.length) {
                    colorIndex = COLORS.length - 1;
                }
                drawPlayer(player, COLORS[colorIndex]);
            }

            batch.end();
        }

        if (conn.gameState != null && conn.gameState.has("flagPos") && flagVisible) {
            batch.begin();
            itemX = conn.gameState.get("flagPos").getFloat("dx") * WORLD_WIDTH;
            itemY = (1f - conn.gameState.get("flagPos").getFloat("dy")) * WORLD_HEIGHT;
            if (itemTexture == null) {
                itemTexture = new Texture("game_assets/items/flag.png");
            }
            batch.draw(itemTexture, itemX, itemY, itemSize, itemSize + 50);
            batch.end();

            if (isPlayerTouchingFlag(cubeX, cubeY, cubeSize, itemX, itemY, itemSize)) {
                onFlagTouched();
            }
        }

        if (conn.gameState != null && conn.gameState.has("buildings") && buildingVisible) {


            if (conn.gameState.get("buildings").size > 0) {
                batch.begin();
                for (int i = 0; i < conn.gameState.get("buildings").size; i++) {


                    buildingX = conn.gameState.get("buildings").get(i).getFloat("dx") * WORLD_WIDTH;
                    buildingY = (1f - conn.gameState.get("buildings").get(i).getFloat("dy")) * WORLD_HEIGHT;
                    if (buildingTexture == null) {
                        buildingTexture = new Texture("game_assets/items/building.png");
                    }
                    batch.draw(buildingTexture, buildingX, buildingY, buildingSize, buildingSize + 50);

                    if (isFlagInBuilding(cubeX, cubeY, cubeSize, buildingX, buildingY, buildingSize)) {
                        System.out.println("¡Juego ganado!");
                        // winGame(conn.playerId);
                    }
                }
                batch.end();


            }
        }


        hudStage.act(Math.min(delta, 1/30f));
        hudStage.draw();

        if (exitButton.isPressed()) {
            game.startMenu();
        }

        // Envío de posición como antes...
        if (conn != null && conn.isConnected()) {
            if (cubeX != lastSentX || cubeY != lastSentY) {
                String msg = String.format(
                    "{\"type\":\"position\",\"x\":%.2f,\"y\":%.2f}",
                    cubeX, cubeY
                );
                conn.sendData(msg);
                lastSentX = cubeX;
                lastSentY = cubeY;
            }
        }
    }

    private void drawPlayer(JsonValue player, String color) {
        if (!player.has("direction") || !player.has("moving")) return;

        // Simplemente dibujamos al jugador; NO TOCAMOS la cámara aquí
        float px = player.getFloat("x") * WORLD_WIDTH;
        float py = (1f - player.getFloat("y")) * WORLD_HEIGHT;
        boolean moving = player.getBoolean("moving");
        String dir = player.getString("direction");

        TextureRegion frame = null;
        for (int i = 0; i < COLORS.length; i++) {
            if (!COLORS[i].equals(color)) continue;
            for (int j = 0; j < DIRECTIONS.length; j++) {
                if (DIRECTIONS[j].equals(dir)) {
                    frame = moving
                        ? runAnimations[i][j].getKeyFrame(stateTime, true)
                        : idleAnimations[i][j].getKeyFrame(stateTime, true);
                    break;
                }
            }
            if (frame != null) break;
        }

        if (frame != null) {
            float drawX = px - cubeSize * 0.5f;
            float drawY = py - cubeSize * 0.5f;
            batch.draw(frame, drawX, drawY, cubeSize, cubeSize);
        }
    }


    @Override
    public void resize(int w, int h) {
        viewport.update(w, h);
        hudStage.getViewport().update(w, h);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        batch.dispose();
        hudStage.dispose();
        backgroundTexture.dispose();
        shapeRenderer.dispose();
        if (itemTexture != null) itemTexture.dispose();
        font.dispose();
    }
}
