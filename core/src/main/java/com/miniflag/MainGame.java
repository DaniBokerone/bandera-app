package com.miniflag;

import com.badlogic.gdx.Game;

public class MainGame extends Game {

    public NetworkManager network;
    public int currentPlayerCount = 0;

    @Override
    public void create() {
        network = NetworkManager.getInstance();

        network.setPlayerCountListener(count -> {
            currentPlayerCount = count;
        });

        this.setScreen(new MenuScreen(this));
    }

    public void startMenu() {
        this.setScreen(new MenuScreen(this));
    }

    public void startGame() {

        this.setScreen(new GameScreen(this));
    }
}
