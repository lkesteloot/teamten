// Copyright 2011 Lawrence Kesteloot

package com.teamten.mario;

import java.awt.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Implements the A* search algorithm to find the best move.
 */
public class Searcher {
    private static final int mDebug = 0;

    /**
     * Search node.
     */
    private static class Node implements Comparable<Node> {
        private final World mWorld;
        private double mCost;
        private Node mParent;
        private Input mInput;

        private Node(World world, double cost, Node parent, Input input) {
            mWorld = world;
            mCost = cost;
            mParent = parent;
            mInput = input;
        }

        public Node(World world) {
            this(world, 0.0, null, null);
        }

        public Node(World world, Node parent, Input input) {
            this(world, 0.0, parent, input);
        }

        public World getWorld() {
            return mWorld;
        }

        public void setCost(double cost) {
            mCost = cost;
        }

        public double getCost() {
            return mCost;
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

            neighbors.add(makeNeighbor(Input.LEFT));
            neighbors.add(makeNeighbor(Input.RIGHT));

            return neighbors;
        }

        private Node makeNeighbor(Input input) {
            return new Node(getWorld().step(input), this, input);
        }

        @Override // Comparable
        public int compareTo(Node other) {
            return Double.compare(getCost(), other.getCost());
        }

        @Override // Object
        public int hashCode() {
            return mWorld.hashCode();
        }

        @Override // Object
        public boolean equals(Object other) {
            return mWorld.equals(other);
        }
    }

    public Input findBestMove(World world, Point target) {
        // Nodes left to investigate.
        PriorityQueue<Node> openQueue = new PriorityQueue<Node>();
        Map<Node,Node> openSet = new HashMap<Node,Node>();

        // Nodes we're not interested in investigating anymore.
        Map<Node,Node> closedSet = new HashMap<Node,Node>();

        // Start node.
        Node startNode = new Node(world);
        startNode.setCost(calculateWorldCost(world, target));
        openQueue.add(startNode);
        openSet.put(startNode, startNode);

        int searchedNodeCount = 0;
        Node bestNode = startNode;
        while (!openQueue.isEmpty()) {
            // Get the best node.
            Node node = openQueue.poll();
            openSet.remove(node);
            searchedNodeCount++;
            if (searchedNodeCount == 100) {
                // XXX
                break;
            }
            if (mDebug >= 2) {
                System.out.printf("Cost: %f%n", node.getCost());
            }

            // Generate neighbor nodes.
            List<Node> neighbors = node.makeNeighbors();
            for (Node neighbor : neighbors) {
                // Skip neighbor if in closed set.
                if (closedSet.containsKey(neighbor)) {
                    continue;
                }

                bestNode = neighbor;

                // Calculate tentative cost: g(node) + guess
                double cost = calculateWorldCost(node.getWorld(), target);
                neighbor.setCost(cost);

                Node existingNeighbor = openSet.get(neighbor);
                if (existingNeighbor != null) {
                    if (cost < existingNeighbor.getCost()) {
                        // Remove first.
                        openQueue.remove(neighbor);  // O(n)
                        // Put it back with new cost. 
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

        Input input = Input.NOTHING;
        int pathLength = 0;
        while (bestNode.getParent() != null) {
            input = bestNode.getInput();
            bestNode = bestNode.getParent();
            pathLength++;
        }
        if (mDebug >= 1) {
            System.out.printf("Path length: %d%n", pathLength);
        }

        // We didn't actually hit the goal, but this is the best we can do.
        return input;
    }

    /**
     * Compute the cost of this world relative to the target.
     */
    private double calculateWorldCost(World world, Point target) {
        Player player = world.getPlayer();

        int dx = target.x - player.getX();
        int dy = target.y - player.getY();

        return Math.hypot(dx, dy);
    }
}
