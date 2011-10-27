// Copyright 2011 Lawrence Kesteloot

package com.teamten.mario;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Side-scroller that uses AI to get to its goal.
 */
public class Mario extends JFrame {
    private static final int ANIMATION_MS = 40;
    private World mWorld;
    private WorldDrawer mWorldDrawer;
    private Input mInput;

    public static void main(String[] args) {
        new Mario();
    }

    private Mario() {
        Env env = Env.makeEnv();
        Player player = new Player(Env.WIDTH/2, Env.HEIGHT/2);
        mWorld = new World(env, player);
        mWorldDrawer = new WorldDrawer(mWorld);
        mInput = new Input();

        makeUi(mWorldDrawer);
        hookUpInput(mWorldDrawer);
        startTimer();
    }

    private void makeUi(WorldDrawer worldDrawer) {
        JPanel content = (JPanel) getContentPane();
        content.setLayout(new BorderLayout());
        content.add(worldDrawer, BorderLayout.CENTER);

        setSize(Env.WIDTH*4, Env.HEIGHT*4);
        setVisible(true);
    }

    private void startTimer() {
        Timer timer = new Timer(ANIMATION_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mWorld = mWorld.step(mInput);
                mWorldDrawer.setWorld(mWorld);
            }
        });
        timer.start();
    }

    private void hookUpInput(WorldDrawer worldDrawer) {
        worldDrawer.addKeyListener(new KeyListener() {
            @Override // KeyListener
            public void keyPressed(KeyEvent keyEvent) {
                keyActuated(keyEvent, true);
            }

            @Override // KeyListener
            public void keyReleased(KeyEvent keyEvent) {
                keyActuated(keyEvent, false);
            }

            private void keyActuated(KeyEvent keyEvent, boolean pressed) {
                switch (keyEvent.getKeyCode()) {
                    case KeyEvent.VK_SPACE:
                        mInput = mInput.withJumpPressed(pressed);
                        break;

                    case KeyEvent.VK_LEFT:
                        mInput = mInput.withLeftPressed(pressed);
                        break;

                    case KeyEvent.VK_RIGHT:
                        mInput = mInput.withRightPressed(pressed);
                        break;
                }
            }

            @Override // KeyListener
            public void keyTyped(KeyEvent keyEvent) {
                // Nothing.
            }
        });
    }
}
