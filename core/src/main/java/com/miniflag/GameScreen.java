package com.miniflag;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad.TouchpadStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class GameScreen implements Screen {
    private MainGame game;
    private SpriteBatch batch;
    private Stage stage;
    private Touchpad touchpad;

    // PERSONAJE
    private float cubeX, cubeY;
    private float cubeSize = 50; // Tamaño del cubo


    private ShapeRenderer shapeRenderer;

    // LLAVE
    private Texture itemTexture;
    private float itemX, itemY;
    private float itemSize = 150;

    public GameScreen(MainGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Skin skin = new Skin(Gdx.files.internal("skin/flat-earth-ui.json"));

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

        // Personaje
        cubeX = Gdx.graphics.getWidth() / 2f - cubeSize / 2f;
        cubeY = Gdx.graphics.getHeight() / 2f - cubeSize / 2f;

        shapeRenderer = new ShapeRenderer();

        // Llave
        itemTexture = new Texture("game_assets/items/game_key.png");
        itemX = Gdx.graphics.getWidth() / 2f - itemSize / 2f;
        itemY = Gdx.graphics.getHeight() / 2f - itemSize / 2f;

        batch.begin();
        batch.draw(itemTexture, itemX, itemY, itemSize, itemSize);
        batch.end();
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

        // DIBUJAR PLAYER(Temporal)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.BLUE);
        shapeRenderer.rect(cubeX, cubeY, cubeSize, cubeSize);
        shapeRenderer.end();

        // DIBUJAR ITEM
        batch.begin();
        batch.draw(itemTexture, itemX, itemY, itemSize, itemSize);  // Dibuja la imagen en el centro
        batch.end();

        // UPDATE SCENARIO
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
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
        shapeRenderer.dispose();
        itemTexture.dispose();
    }
}
