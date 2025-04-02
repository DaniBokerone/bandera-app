package com.miniflag;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

public class MenuScreen implements Screen {
    private MainGame game;
    private SpriteBatch batch;
    private Texture image;
    private NetworkManager networkManager;

    public MenuScreen(MainGame game) {
        this.game = game;
        this.networkManager = new NetworkManager();
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");

        //Conectar con el servidor(Ejemplo siempre funciona)
        networkManager.connect();

    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        batch.begin();
        batch.draw(image, 140, 210);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {}

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
        image.dispose();
    }
}
