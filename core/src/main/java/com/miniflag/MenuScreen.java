package com.miniflag;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.graphics.Color;

public class MenuScreen implements Screen {
    private final MainGame game;
    private SpriteBatch batch;
    private BitmapFont titleFont;
    private Stage stage;
    private Skin skin;
    private Label playersLabel;

    public MenuScreen(MainGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        // Crear fuente pixelada personalizada
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Pixel_font.ttf"));

        // Fuente titulo
        FreeTypeFontGenerator.FreeTypeFontParameter titleParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        titleParam.size = (int) (Gdx.graphics.getWidth() * 0.08f);
        titleFont = generator.generateFont(titleParam);

        // Fuente boton
        FreeTypeFontGenerator.FreeTypeFontParameter buttonParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        buttonParam.size = (int) (Gdx.graphics.getWidth() * 0.025f);
        BitmapFont buttonFont = generator.generateFont(buttonParam);

        generator.dispose();

        // Stage y Skin
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("skin/flat-earth-ui.json"));

        // Estilo de botón personalizado
        TextButtonStyle buttonStyle = new TextButtonStyle();
        buttonStyle.up = skin.getDrawable("button-c");
        buttonStyle.down = skin.getDrawable("button-p");
        buttonStyle.over = skin.getDrawable("button-h");
        buttonStyle.font = buttonFont;
        buttonStyle.fontColor = Color.WHITE;

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = buttonFont;
        labelStyle.fontColor = Color.WHITE;

        // Label con número de jugadores
        Label playersLabel = new Label("Current players: --", labelStyle);

        // Posición del label (encima del botón)
        float labelWidth = Gdx.graphics.getWidth() * 0.4f;
        float labelHeight = Gdx.graphics.getHeight() * 0.05f;
        playersLabel.setSize(labelWidth, labelHeight);
        playersLabel.setPosition(
            Gdx.graphics.getWidth() / 2f - labelWidth / 3.5f,
            Gdx.graphics.getHeight() / 2f + labelHeight
        );
        stage.addActor(playersLabel);

        // Boton Start Game
        TextButton startButton = new TextButton("Start Game", buttonStyle);

        float buttonWidth = Gdx.graphics.getWidth() * 0.3f;
        float buttonHeight = Gdx.graphics.getHeight() * 0.12f;
        startButton.setSize(buttonWidth, buttonHeight);

        startButton.setPosition(
            Gdx.graphics.getWidth() / 2f - buttonWidth / 2f,
            Gdx.graphics.getHeight() / 2f - buttonHeight / 0.4f
        );

        startButton.getLabel().setWrap(true);
        startButton.getLabel().setFontScale(1.0f);

        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameScreen(game));
            }
        });

        stage.addActor(startButton);

        NetworkManager network = new NetworkManager();
        network.testHttpConnection();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        batch.begin();
        String title = "Mini Flag";

        // 🔹 Título más a la izquierda y adaptado al tamaño de pantalla
        float x = Gdx.graphics.getWidth() * 0.35f;
        float y = Gdx.graphics.getHeight() * 0.9f;
        titleFont.draw(batch, title, x, y);
        batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }


    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        batch.dispose();
        titleFont.dispose();
        stage.dispose();
        skin.dispose();
    }
}
