// Copyright 2011 Lawrence Kesteloot

package com.teamten.mario;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Side-scroller that uses AI to get to its goal.
 */
public class Mario extends JFrame {
    private static final int ANIMATION_MS = 40;
    private static final int COMPUTATION_MS = 40;
    private World mWorld;
    private WorldDrawer mWorldDrawer;
    private volatile Input mInput;
    private boolean mRunning = true;
    private boolean mAutomatic = false;
    private final Searcher mSearcher;
    private volatile Point mTarget = null;
    private Future<Searcher.Results> mResults = null;
    private Deque<Input> mInputs = new LinkedList<Input>();
    private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
    private int mPulledInputs = 0;
    private boolean mDebug = false;

    public static void main(String[] args) {
        new Mario();
    }

    private Mario() {
        resetGame();
        mWorldDrawer = new WorldDrawer(mWorld);
        mInput = Input.NOTHING;
        mSearcher = new Searcher();

        makeUi(mWorldDrawer);
        hookUpInput(mWorldDrawer);
        startAnimationTimer();
    }

    private void resetGame() {
        Env env = Env.makeEnv();
        Player player = new Player(Env.WIDTH/2, Env.HEIGHT - Floor.HEIGHT - Player.INITIAL_RADIUS);
        mWorld = new World(env, player);
        if (mWorldDrawer != null) {
            mWorldDrawer.setWorld(mWorld);
        }
        mInput = Input.NOTHING;
    }

    private void makeUi(WorldDrawer worldDrawer) {
        JPanel content = (JPanel) getContentPane();
        content.setLayout(new BorderLayout());
        content.add(worldDrawer, BorderLayout.CENTER);

        setSize(Env.WIDTH*4, Env.HEIGHT*4);
        setVisible(true);
    }

    private void startAnimationTimer() {
        Timer timer = new Timer(ANIMATION_MS, new ActionListener() {
            @Override // ActionListener
            public void actionPerformed(ActionEvent actionEvent) {
                // Take snapshot of mouse position for computation thread.
                // Use the WorldDrawer's component, not this JPanel's,
                // because the latter includes the title bar of the window.
                Point target = mWorldDrawer.getMousePosition();
                if (target == null) {
                    mTarget = null;
                } else {
                    mTarget = mWorldDrawer.reverseTransform(target);
                }

                mWorldDrawer.setTarget(mTarget);

                if (mRunning || true) {
                    if (mAutomatic || true) {
                        if (mResults != null && mResults.isDone()) {
                            Searcher.Results results;
                            try {
                                results = mResults.get();
                            } catch (Exception e) {
                                System.out.println(e);
                                // Various interrupted and execution exceptions.
                                return;
                            }
                            mInputs = results.getInputs();
                            while (mPulledInputs > 1) {
                                Input input = mInputs.poll();
                                /// System.out.println("Proactively pulling: " + input);
                                mPulledInputs--;
                            }
                            mPulledInputs = 0;
                            if (mDebug) {
                                mWorldDrawer.setPath(results.getPath(), results.getElapsed()/200.);
                                mWorldDrawer.setExplored(results.getExplored());
                            } else {
                                mWorldDrawer.setPath(null, 0);
                                mWorldDrawer.setExplored(null);
                            }
                            mWorldDrawer.setWorld(mWorld);
                            mResults = null;
                        }

                        Input input = mInputs.poll();
                        if (input == null) {
                            input = Input.NOTHING;
                        } else {
                            /// System.out.println("Pulling: " + input);
                            mPulledInputs++;
                        }

                        mWorld = mWorld.step(input);

                        if (mResults == null && mTarget != null) {
                            mResults = computePath(mTarget, mWorld);
                        }
                    } else {
                        mWorld = mWorld.step(mInput);
                    }
                    mWorldDrawer.setWorld(mWorld);
                } else {
                    mTarget = null;
                }
            }
        });
        timer.start();
    }

    private Future<Searcher.Results> computePath(final Point target, final World world) {
        Callable<Searcher.Results> callable = new Callable<Searcher.Results>() {
            @Override // Callable
            public Searcher.Results call() {
                return mSearcher.findBestMove(world, target);
            }
        };

        return mExecutorService.submit(callable);
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

            @Override // KeyListener
            public void keyTyped(KeyEvent keyEvent) {
                switch (keyEvent.getKeyChar()) {
                    case 's':
                        mRunning = false;
                        mAutomatic = false;
                        computePath(mTarget, mWorld);
                        break;

                    case 'a':
                        mRunning = true;
                        mAutomatic = true;
                        break;

                    case 'm':
                        mRunning = true;
                        mAutomatic = false;
                        break;

                    case 'd':
                        mDebug = !mDebug;
                        break;

                    case 'x':
                        resetGame();
                        break;
                }
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
        });
    }
}
