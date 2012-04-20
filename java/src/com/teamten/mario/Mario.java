// Copyright 2011-2012 Lawrence Kesteloot

package com.teamten.mario;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
    private final Searcher mSearcher;
    private Future<Searcher.Results> mResults = null;
    private Deque<Input> mInputs = new LinkedList<Input>();
    private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
    /**
     * This represents how late the results are. When we spawn a task to calculate the
     * path, we reset this to 0. If the results come back by the beginning of the next
     * frame, then no frames have been skipped and we can use the input list as-is. But
     * if the results don't come back, then the results are based on old data and we must
     * keep track of the missed frames so we can skip them in the input queue.
     */
    private int mResultDelay = 0;
    private boolean mDebug = false;
    private boolean mMouseDown = false;
    private int mLevel = 0;

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
        Env env = Levels.makeLevel(mLevel);
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
                Point target;

                // The mouse button acts like the touchscreen being tapped.
                if (mMouseDown) {
                    // Take snapshot of mouse position for computation thread.
                    // Use the WorldDrawer's component, not this JPanel's,
                    // because the latter includes the title bar of the window.
                    Point componentTarget = mWorldDrawer.getMousePosition();
                    if (componentTarget == null) {
                        target = null;
                    } else {
                        target = mWorldDrawer.reverseTransform(componentTarget);
                    }
                } else {
                    target = null;
                }

                // Draw the target if there is one.
                mWorldDrawer.setTarget(target);

                // See if we got results from the other thread.
                if (mResults != null && mResults.isDone()) {
                    // Get the results from the searcher.
                    Searcher.Results results;
                    try {
                        results = mResults.get();
                    } catch (Exception e) {
                        System.out.println(e);
                        // Various interrupted and execution exceptions.
                        return;
                    }

                    // See what the searcher thinks we should do right now and in
                    // the near future.
                    mInputs = results.getInputs();

                    // If we missed some frames, skip their inputs.
                    while (mResultDelay > 0) {
                        mInputs.poll();
                        mResultDelay--;
                    }

                    // Optionally draw the ball's path and other debug info.
                    if (mDebug) {
                        mWorldDrawer.setPath(results.getPath(), results.getElapsed()/200.);
                        mWorldDrawer.setExplored(results.getExplored());
                    } else {
                        mWorldDrawer.setPath(null, 0);
                        mWorldDrawer.setExplored(null);
                    }

                    // We've processed these results.
                    mResults = null;
                }

                // Get the next thing we should be doing.
                Input input = mInputs.poll();
                if (input == null) {
                    input = Input.NOTHING;
                }

                // Run the simulation.
                mWorld = mWorld.step(input);

                // Keep track of how many frames old the results are.
                mResultDelay++;

                // Compute the best inputs in a different thread.
                if (mResults == null && target != null) {
                    mResults = computePath(target, mWorld);
                    mResultDelay = 0;
                }
                mWorldDrawer.setWorld(mWorld);
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
                    case 'd':
                        mDebug = !mDebug;
                        break;

                    case 'x':
                        resetGame();
                        break;

                    case '[':
                        if (mLevel > 0) {
                            mLevel--;
                            resetGame();
                        }
                        break;

                    case ']':
                        if (mLevel < Levels.LEVEL_COUNT - 1) {
                            mLevel++;
                            resetGame();
                        }
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

        worldDrawer.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mMouseDown = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mMouseDown = false;
            }
        });
    }
}
