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

            neighbors.add(makeNeighbor(Input.NOTHING));
            neighbors.add(makeNeighbor(Input.JUMP));
            neighbors.add(makeNeighbor(Input.LEFT));
            neighbors.add(makeNeighbor(Input.RIGHT));

            return neighbors;
        }

        private Node makeNeighbor(Input input) {
            World newWorld = getWorld().step(input);
            double distanceTraveled = getWorld().getPlayer().distanceTo(newWorld.getPlayer());
            distanceTraveled = 1.0; // XXX Now in units of ticks.
            return new Node(newWorld, getPathCost() + distanceTraveled, 0.0, this, input);
        }

        /**
         * Compute lower bound of cost of getting this world to the target.
         */
        private double estimateCostToTarget(Point target) {
            // Straight-line distance to target.
            double distance = getDistanceToTarget(target);

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

        public double getDistanceToTarget(Point target) {
            // Straight-line distance to target.
            Player player = getWorld().getPlayer();

            int dx = target.x - player.getX();
            int dy = target.y - player.getY();

            // XXX Force dy to zero for now since we can't jump.
            /// dy = 0;

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

            bestNode = node;

            double distance = node.getDistanceToTarget(target);
            double speed = node.getWorld().getPlayer().getSpeed();
            if (distance < 1 && speed < 1) {
                // Done.
                break;
            }

            closedSet.put(node, node);
            searchedNodeCount++;
            if (searchedNodeCount == 100) {
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

        Input input = Input.NOTHING;
        int pathLength = 0;
        while (bestNode.getParent() != null) {
            input = bestNode.getInput();
            if (mDebug >= 1) {
                System.out.printf("%s (%f %f %d), ",
                        input,
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

        // We didn't actually hit the goal, but this is the best we can do.
        return input;
    }
}
