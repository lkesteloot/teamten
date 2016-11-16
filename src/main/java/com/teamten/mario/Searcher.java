/*
 *
 *    Copyright 2016 Lawrence Kesteloot
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

// Copyright 2011 Lawrence Kesteloot

package com.teamten.mario;

import java.awt.Point;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Implements the A* search algorithm to find the best move.
 */
public class Searcher {
    private static final int mDebug = 0;
    private static final boolean USE_DISTANCE = true;
    private static final boolean ALLOW_JUMPING = true;

    /**
     * Search results.
     */
    public static class Results {
        private final Deque<Input> mInputs;
        private final List<Point> mPath;
        private final List<Point> mExplored;
        private final long mElapsed;

        public Results(Deque<Input> inputs, List<Point> path, List<Point> explored, long elapsed) {
            mInputs = inputs;
            mPath = path;
            mExplored = explored;
            mElapsed = elapsed;
        }

        public Deque<Input> getInputs() {
            return mInputs;
        }

        public List<Point> getPath() {
            return mPath;
        }

        public List<Point> getExplored() {
            return mExplored;
        }

        public long getElapsed() {
            return mElapsed;
        }
    }

    /**
     * Search node.
     */
    private static class Node implements Comparable<Node> {
        private final World mWorld;
        private double mPathCost;
        private double mTotalCost;
        private Node mParent;
        private Input mInput;
        private int mGeneration;

        private Node(World world, double pathCost, double totalCost, Node parent, Input input,
                int generation) {

            mWorld = world;
            mPathCost = pathCost;
            mTotalCost = totalCost;
            mParent = parent;
            mInput = input;
            mGeneration = generation;
        }

        /**
         * Start node.
         */
        public Node(World world) {
            this(world, 0.0, 0.0, null, null, 0);
        }

        public World getWorld() {
            return mWorld;
        }

        public double getPathCost() {
            return mPathCost;
        }

        public void setTotalCost(double totalCost) {
            mTotalCost = totalCost;
        }

        public double getTotalCost() {
            return mTotalCost;
        }

        public void setParent(Node parent) {
            mParent = parent;
        }

        public Node getParent() {
            return mParent;
        }

        public void setInput(Input input) {
            mInput = input;
        }

        public Input getInput() {
            return mInput;
        }

        /**
         * Return all neighbors of this node. The costs are not populated.
         */
        private List<Node> makeNeighbors() {
            List<Node> neighbors = new ArrayList<Node>();

            boolean isTouchingFloor = getWorld().getEnv().isTouchingFloor(getWorld().getPlayer());
            if (isTouchingFloor) {
                if (ALLOW_JUMPING) {
                    neighbors.add(makeNeighbor(Input.JUMP));
                    neighbors.add(makeNeighbor(Input.LEFT_JUMP));
                    neighbors.add(makeNeighbor(Input.RIGHT_JUMP));
                }
                neighbors.add(makeNeighbor(Input.LEFT));
                neighbors.add(makeNeighbor(Input.RIGHT));
            } else {
                neighbors.add(makeNeighbor(Input.NOTHING));
            }

            return neighbors;
        }

        private Node makeNeighbor(Input input) {
            World newWorld = getWorld().step(input);

            double travelCost;
            if (USE_DISTANCE) {
                travelCost = getWorld().getPlayer().distanceTo(newWorld.getPlayer());

                // Penalize later moves.
                /// travelCost += mGeneration;

                // Get rid of this factor, for testing.
                /// travelCost = 0.0;

                // Make it want to get these sooner.
                travelCost += 0.1;
            } else {
                travelCost = 1.0; // XXX Now in units of ticks.
            }

            return new Node(newWorld, getPathCost() + travelCost, 0.0, this,
                    input, mGeneration + 1);
        }

        /**
         * Compute lower bound of cost of getting this world to the target.
         */
        private double estimateCostToTarget(Point target) {
            // Straight-line distance to target.
            double distance = getDistanceToTarget(target);

            if (USE_DISTANCE) {
                return distance;
            } else {
                // Best time to travel distance would be to accelerate for half the distance
                // and decelerate for the other half. The increase in speed is 1 per tick,
                // and we're returning the cost in ticks.
                //
                // d = 0.5 a t^2
                // t = sqrt(2 d / a)
                final double A = 1;

                // XXX Note that this ignores any existing acceleration.
                double time = 2 * Math.sqrt(distance / A);

                return time;
            }
        }

        public double getDistanceToTarget(Point target) {
            // Straight-line distance to target.
            Player player = getWorld().getPlayer();

            int dx = target.x - player.getX();
            int dy = target.y - player.getY();

            return Math.hypot(dx, dy);
        }

        public String getKeySequence() {
            StringBuilder builder = new StringBuilder();

            Node node = this;
            while (node.getParent() != null) {
                builder.append(node.getInput());
                node = node.getParent();
            }

            builder.reverse();

            return builder.toString();
        }

        @Override // Comparable
        public int compareTo(Node other) {
            return Double.compare(getTotalCost(), other.getTotalCost());
        }

        @Override // Object
        public int hashCode() {
            return mWorld.hashCode();
        }

        @Override // Object
        public boolean equals(Object other) {
            if (!(other instanceof Node)) {
                return false;
            }

            Node otherNode = (Node) other;

            return getWorld().equals(otherNode.getWorld());
        }
    }

    public Results findBestMove(World world, Point target) {
        long before = System.currentTimeMillis();

        if (!ALLOW_JUMPING) {
            // 1-D problem.
            target.y = world.getPlayer().getY();
        }

        // Nodes left to investigate.
        PriorityQueue<Node> openQueue = new PriorityQueue<Node>();
        Map<Node,Node> openSet = new HashMap<Node,Node>();

        // Nodes we're not interested in investigating anymore.
        Map<Node,Node> closedSet = new HashMap<Node,Node>();

        // Start node.
        Node startNode = new Node(world);
        startNode.setTotalCost(startNode.estimateCostToTarget(target));
        openQueue.add(startNode);
        openSet.put(startNode, startNode);

        int searchedNodeCount = 0;
        Node bestNode = startNode;
        List<Point> explored = new ArrayList<Point>();
        while (!openQueue.isEmpty()) {
            // Get the best node.
            Node node = openQueue.poll();
            openSet.remove(node);
            explored.add(node.getWorld().getPlayer().getPoint());

            if (mDebug >= 3) {
                System.out.printf("Pulled out dist=%f, total=%f (%f + %f), %s%n",
                        node.getDistanceToTarget(target),
                        node.getTotalCost(),
                        node.getPathCost(),
                        node.getTotalCost() - node.getPathCost(),
                        node.getKeySequence());
            }
            if (node.getDistanceToTarget(target) < bestNode.getDistanceToTarget(target)) {
                bestNode = node;
            }

            double distance = node.getDistanceToTarget(target);
            double speed = node.getWorld().getPlayer().getSpeed();
            if (distance < 5 /* && speed < 1 */) {
                // Done.
                break;
            }

            closedSet.put(node, node);
            searchedNodeCount++;
            if (searchedNodeCount == 50000) {
                // XXX use time limit instead.
                break;
            }
            if (mDebug >= 2) {
                System.out.printf("Cost: %f%n", node.getTotalCost());
            }

            // Generate neighbor nodes.
            List<Node> neighbors = node.makeNeighbors();
            for (Node neighbor : neighbors) {
                // Skip neighbor if in closed set.
                if (closedSet.containsKey(neighbor)) {
                    continue;
                }

                // Calculate tentative cost: path + guess
                double estimatedCostToTarget = neighbor.estimateCostToTarget(target);
                double totalCost = neighbor.getPathCost() + estimatedCostToTarget;
                neighbor.setTotalCost(totalCost);

                Node existingNeighbor = openSet.get(neighbor);
                if (existingNeighbor != null) {
                    // See if we've found a better path.
                    if (neighbor.getPathCost() < existingNeighbor.getPathCost()) {
                        // Remove first.
                        openQueue.remove(existingNeighbor);  // O(n)
                        // Put it back with new cost, parent, input, etc. 
                        openQueue.add(neighbor);
                        openSet.put(neighbor, neighbor);
                    } else {
                        // New cost higher, ignore.
                    }
                } else {
                    openQueue.add(neighbor);
                    openSet.put(neighbor, neighbor);
                }
            }
        }

        if (mDebug >= 1) {
            System.out.printf("Searched %d nodes.%n", searchedNodeCount);
        }

        Deque<Input> inputs = new LinkedList<Input>();
        int pathLength = 0;
        List<Point> path = new ArrayList<Point>();
        while (bestNode.getParent() != null) {
            path.add(bestNode.getWorld().getPlayer().getPoint());
            inputs.addFirst(bestNode.getInput());
            if (mDebug >= 1) {
                System.out.printf("%s (%f %f %d), ",
                        bestNode.getInput(),
                        bestNode.getPathCost(),
                        bestNode.getTotalCost(),
                        bestNode.getWorld().getPlayer().getX());
            }
            bestNode = bestNode.getParent();
            pathLength++;
        }
        if (mDebug >= 1) {
            System.out.println();
            System.out.printf("Path length: %d%n", pathLength);
        }

        long after = System.currentTimeMillis();

        // We didn't actually hit the goal, but this is the best we can do.
        return new Results(inputs, path, explored, after - before);
    }
}
