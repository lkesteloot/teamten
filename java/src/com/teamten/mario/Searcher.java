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
    private static final int mDebug = 2;

    /**
     * Search node.
     */
    private static class Node implements Comparable<Node> {
        private final World mWorld;
        private double mPathCost;
        private double mTotalCost;
        private Node mParent;
        private Input mInput;

        private Node(World world, double pathCost, double totalCost, Node parent, Input input) {
            mWorld = world;
            mPathCost = pathCost;
            mTotalCost = totalCost;
            mParent = parent;
            mInput = input;
        }

        /**
         * Start node.
         */
        public Node(World world) {
            this(world, 0.0, 0.0, null, null);
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

            neighbors.add(makeNeighbor(Input.LEFT));
            neighbors.add(makeNeighbor(Input.RIGHT));

            return neighbors;
        }

        private Node makeNeighbor(Input input) {
            World newWorld = getWorld().step(input);
            double distanceTraveled = getWorld().getPlayer().distanceTo(newWorld.getPlayer());
            return new Node(newWorld, getPathCost() + distanceTraveled, 0.0, this, input);
        }

        /**
         * Compute the cost of this world relative to the target.
         */
        private double estimateCostToTarget(Point target) {
            Player player = getWorld().getPlayer();

            int dx = target.x - player.getX();
            int dy = target.y - player.getY();

            return Math.hypot(dx, dy);
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

    public Input findBestMove(World world, Point target) {
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
        while (!openQueue.isEmpty()) {
            // Get the best node.
            Node node = openQueue.poll();
            openSet.remove(node);
            closedSet.put(node, node);
            System.out.printf("Adding %d to closed set (%d)%n", node.getWorld().getPlayer().getX(), node.hashCode());
            searchedNodeCount++;
            if (searchedNodeCount == 100) {
                // XXX
                break;
            }
            if (mDebug >= 2) {
                System.out.printf("Cost: %f%n", node.getTotalCost());
            }

            // Generate neighbor nodes.
            List<Node> neighbors = node.makeNeighbors();
            for (Node neighbor : neighbors) {
                // Skip neighbor if in closed set.
                System.out.printf("Checking %d in closed set (%d)%n", neighbor.getWorld().getPlayer().getX(), neighbor.hashCode());
                if (closedSet.containsKey(neighbor)) {
                    System.out.println("Skipping");
                    continue;
                }

                bestNode = neighbor;

                // Calculate tentative cost: path + guess
                double totalCost = neighbor.getPathCost() + neighbor.estimateCostToTarget(target);
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

        Input input = Input.NOTHING;
        int pathLength = 0;
        while (bestNode.getParent() != null) {
            input = bestNode.getInput();
            System.out.printf("%s (%f %d), ", input, bestNode.getPathCost(),
                    bestNode.getWorld().getPlayer().getX());
            bestNode = bestNode.getParent();
            pathLength++;
        }
        System.out.println();
        if (mDebug >= 1) {
            System.out.printf("Path length: %d%n", pathLength);
        }

        // We didn't actually hit the goal, but this is the best we can do.
        return input;
    }
}
