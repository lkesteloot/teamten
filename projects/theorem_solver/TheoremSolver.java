// Copyright 2011 Lawrence Kesteloot

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * Takes simple algebraic facts and tries to derive new ones. Was written to solve this
 * problem: draw two lines, semi-parallel, pick three points on each, and draw a line
 * between each point and the two non-matching points on the other line. The six
 * lines will intersect in three points. Those three points are in a line.
 *
 * Currently fails to prove this theorem. Possible reasons: I'm missing some basic
 * facts the program needs; I'm not deriving the right set of equations; the theorem
 * is not solvable using angles (perhaps only using point positions).
 */
public class TheoremSolver {
    private static final String[] TERMS = {
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P",
        "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "" };
    private static final int NUM_TERMS = TERMS.length;
    private static final String[] SUBSTITUTIONS = "BT,AU,DP,ES,GR,HO,JY,XK,WL,MQ,NV".split(",");
    private static final int MAX_COEFFICIENT = 2;
    private static final int MAX_COEFFICIENT_COUNT = 10;
    private final Map<Fact,Fact> mFacts = new HashMap<Fact,Fact>();
    private final Random mRandom = new Random();

    public static void main(String[] args) {
        new TheoremSolver().run(args);
    }

    private void run(String[] args) {
        /// runTests();
        addBasicFact("A + B + Q + K + J = 180");
        addBasicFact("A + F + K = 180");
        addBasicFact("B + C + D + E = 180");
        addBasicFact("I + H + O = 180");
        addBasicFact("J + I + H + G = 180");
        addBasicFact("C + D + P = 180");
        addBasicFact("F + E + S = 180");
        addBasicFact("F + G + R = 180");
        addBasicFact("N + M + L = 180");
        addBasicFact("I + Y + X + W = 180");
        addBasicFact("C + T + U + V = 180");
        addBasicFact("Y + X + W = J + K + L");
        addBasicFact("T + U + V = A + B + N");
        addBasicFact("X + W = R + O");
        addBasicFact("X + W = A + K + J");
        addBasicFact("U + V = P + S");
        addBasicFact("U + V = A + B + K");
        addBasicFact("B + C = M + Y");
        addBasicFact("J + I = M + T");
        addBasicFact("E + 2F + G + S + R = 360");
        addBasicFact("D + P + Q + O + H + M = 360");

        // Give it what we want to find out.
        /// addBasicFact("E + F + G = 180");

        for (int i = 0; i < 1000000; i++) {
            generateNewFact();
        }

        /// checkProven("H + O + U = K + L + W");
        checkProven("E + F + G = 180");
        checkProven("E + F + G = S + F + R");
        checkProven("S + F + R = 180");
        checkProven("G = S");
        checkProven("E = R");
        checkProven("E + G = S + R");
    }

    private void checkProven(String qed) {
        Fact fact = Fact.parseFact(qed);
        Fact foundFact = mFacts.get(fact);
        if (foundFact != null) {
            System.out.println(foundFact + "  Q.E.D.");
            foundFact.printTrace(0, 0);
        }
    }

    private void addBasicFact(String expression) {
        Fact fact = Fact.parseFact(expression);

        System.out.println("Adding fact \"" + fact + "\" (from expression \""
            + expression + "\")");

        addFactAndMirror(fact);
    }

    private void generateNewFact() {
        // Compute the total interestingness of the set.
        double totalInterestingness = 0.0;
        Iterator<Fact> itr = mFacts.keySet().iterator();
        while (itr.hasNext()) {
            Fact fact = itr.next();
            totalInterestingness += fact.getInterestingness();
        }

        // Pick two random facts.
        double index1 = mRandom.nextDouble() * totalInterestingness;
        double index2 = mRandom.nextDouble() * totalInterestingness;
        Fact fact1 = null;
        Fact fact2 = null;

        double interestingness = 0.0;
        itr = mFacts.keySet().iterator();
        while (itr.hasNext()) {
            Fact fact = itr.next();

            interestingness += fact.getInterestingness();

            if (fact1 == null && interestingness >= index1) {
                fact1 = fact;
            }
            if (fact2 == null && interestingness >= index2) {
                fact2 = fact;
            }
            if (fact1 != null && fact2 != null) {
                break;
            }
        }
        if (fact1 == null || fact2 == null) {
            return;
        }

        Fact newFact = Fact.sum(fact1, fact2, 1, mRandom.nextInt(2)*2 - 1);

        if (true || (newFact.getHighestCoefficient() <= MAX_COEFFICIENT
                && newFact.getCoefficientCount() <= MAX_COEFFICIENT_COUNT)) {

            addFactAndMirror(newFact);
        }
    }

    private void addFactAndMirror(Fact fact) {
        addFact(fact);

        // Flip to make equivalent statement.
        Fact mirrorFact = fact.getMirror();
        if (!mirrorFact.equals(fact)) {
            addFact(mirrorFact);
        }
    }

    private void addFact(Fact fact) {
        Fact existingFact = mFacts.get(fact);
        if (existingFact != null) {
            if (existingFact.getGeneration() <= fact.getGeneration()) {
                System.out.println("Fact \"" + fact + "\" was already known");
            } else {
                System.out.printf("Fact \"%s\" was already known but less directly (%d vs. %d)%n",
                        fact, existingFact.getGeneration(), fact.getGeneration());
                mFacts.put(fact, fact);
            }
        } else {
            System.out.printf("Fact \"%s\" was unknown%n", fact);
            mFacts.put(fact, fact);
        }
    }

    private Fact silentlyIntern(Fact fact) {
        Fact internedFact = mFacts.get(fact);
        if (internedFact != null) {
            return internedFact;
        } else {
            mFacts.put(fact, fact);
            return fact;
        }
    }

    private void runTests() {
        testParser("A = 0");
        testParser("A + B = 0");
        testParser("A + 52B = 0");
        testParser("5A + 52B = 5");
        testParser("-5A - 52B = -5");
    }

    private void testParser(String expression) {
        Fact fact = Fact.parseFact(expression);
        System.out.println(expression + " -> " + fact);
    }

    /**
     * A set of coefficients describing a degree-one polynomial. The terms add up to zero.
     */
    private static class Fact {
        private final int[] mTerms;
        private final int mGeneration;
        private final Fact mSourceFact1;
        private final Fact mSourceFact2;
        private final int mMultiplier1;
        private final int mMultiplier2;
        private final double mInterestingness;

        // For the parser.
        private static enum State {
            EXPECTING_TERM,
            PARSING_NUMBER,
        }

        private Fact(int[] terms, int generation, Fact sourceFact1, Fact sourceFact2,
                int multiplier1, int multiplier2) {

            // Normalize the terms.
            for (int i = 0; i < NUM_TERMS; i++) {
                if (terms[i] != 0) {
                    if (terms[i] < 0) {
                        for (int j = 0; j < NUM_TERMS; j++) {
                            terms[j] = -terms[j];
                        }
                    }
                    break;
                }
            }

            mTerms = terms;
            mGeneration = generation;
            mSourceFact1 = sourceFact1;
            mSourceFact2 = sourceFact2;
            mMultiplier1 = multiplier1;
            mMultiplier2 = multiplier2;

            mInterestingness = computeInterestingness();
        }

        private double computeInterestingness() {
            double interestingness = 1.0;

            for (int i = 0; i < NUM_TERMS; i++) {
                if (mTerms[i] != 0) {
                    if (TERMS[i].isEmpty()) {
                        interestingness /= Math.abs(mTerms[i])/90;
                    } else {
                        interestingness /= Math.abs(mTerms[i])*2;
                    }
                }
            }

            // Avoid the 0 = 0 equation.
            if (interestingness == 1.0) {
                interestingness = 0;
            }

            return interestingness;
        }

        /**
         * 0 represents the initial (given by user) facts, then they grow from there
         * as they're generated from each other.
         */
        public int getGeneration() {
            if (mSourceFact1 != null && mSourceFact2 != null) {
                return Math.max(mSourceFact1.getGeneration(), mSourceFact2.getGeneration()) + 1;
            } else {
                return mGeneration;
            }
        }

        /**
         * Return the index of the term for a character like 'A'. Returns the constant
         * index for ch = 0.
         *
         * @throws IllegalArgumentException if not found.
         */
        private static int getTermIndex(char ch) {
            if (ch == 0) {
                for (int i = 0; i < TERMS.length; i++) {
                    if (TERMS[i].isEmpty()) {
                        return i;
                    }
                }

                throw new IllegalArgumentException("Constant term not found");
            } else {
                for (int i = 0; i < TERMS.length; i++) {
                    if (!TERMS[i].isEmpty() && ch == TERMS[i].charAt(0)) {
                        return i;
                    }
                }

                throw new IllegalArgumentException("Term '" + ch + "' not found");
            }
        }

        /**
         * Parses a line of the form "A + 2B = 180".
         */
        public static Fact parseFact(String line) {
            int[] terms = new int[NUM_TERMS];
            State state = State.EXPECTING_TERM;
            int value = 1;
            int multiplier = 1;
            int globalMultiplier = 1;

            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);

                // Skip spaces.
                if (ch != ' ') {
                    switch (state) {
                        case EXPECTING_TERM:
                            if (ch == '+') {
                                multiplier = 1;
                            } else if (ch == '-') {
                                multiplier = -1;
                            } else if (ch == '=') {
                                globalMultiplier = -1;
                            } else if (ch >= '0' && ch <= '9') {
                                value = (int) (ch - '0');
                                state = State.PARSING_NUMBER;
                            } else if (ch >= 'A' && ch <= 'Z') {
                                int j = getTermIndex(ch);
                                terms[j] = globalMultiplier*multiplier;
                                multiplier = 1;
                            }
                            break;

                        case PARSING_NUMBER:
                            if (ch >= '0' && ch <= '9') {
                                value = value*10 + (int) (ch - '0');
                            } else if (ch >= 'A' && ch <= 'Z') {
                                int j = getTermIndex(ch);
                                terms[j] = globalMultiplier*multiplier*value;
                                multiplier = 1;
                                state = State.EXPECTING_TERM;
                            } else {
                            }
                            break;
                    }
                }
            }

            if (state == State.PARSING_NUMBER) {
                int j = getTermIndex((char) 0);
                terms[j] = globalMultiplier*multiplier*value;
            }

            return new Fact(terms, 0, null, null, 0, 0);
        }

        /**
         * Compute the GCD of two numbers. Uses Euclid's algorithm.
         */
        private static int dualGcd(int i, int j) {
            while (j != 0) {
                int remainder = i % j;
                i = j;
                j = remainder;
            }

            return i;
        }

        /**
         * Compute the GCD of a list of numbers.
         */
        private static int multiGcd(int[] values) {
            int gcd = values[0];

            for (int i = 1; i < values.length; i++) {
                gcd = dualGcd(gcd, values[i]);
            }

            if (gcd == 0) {
                gcd = 1;
            }

            return gcd;
        }

        public static Fact sum(Fact f1, Fact f2, int m1, int m2) {
            int[] terms = new int[NUM_TERMS];

            for (int i = 0; i < NUM_TERMS; i++) {
                terms[i] = m1*f1.mTerms[i] + m2*f2.mTerms[i];
            }

            // Reduce.
            int gcd = multiGcd(terms);
            if (gcd != 1) {
                for (int i = 0; i < NUM_TERMS; i++) {
                    terms[i] /= gcd;
                }
            }

            return new Fact(terms, 0, f1, f2, m1, m2);
        }

        @Override // Object
        public String toString() {
            StringBuilder builder = new StringBuilder();

            builder.append(addTerms(1));
            builder.append(" = ");
            builder.append(addTerms(-1));

            builder.append(" (");
            builder.append(getGeneration());
            builder.append(")");

            return builder.toString();
        }

        private String addTerms(int multiplier) {
            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < NUM_TERMS; i++) {
                int value = multiplier*mTerms[i];

                if (value > 0) {
                    if (builder.length() != 0) {
                        builder.append(" + ");
                    }

                    if (TERMS[i].isEmpty()) {
                        builder.append(value);
                    } else {
                        if (value != 1) {
                            builder.append(value);
                        }
                        builder.append(TERMS[i]);
                    }
                }
            }

            if (builder.length() == 0) {
                return "0";
            } else {
                return builder.toString();
            }
        }

        @Override // Object
        public int hashCode() {
            int hashCode = 17;

            for (int i = 0; i < NUM_TERMS; i++) {
                hashCode = hashCode*37 + mTerms[i];
            }

            return hashCode;
        }

        @Override // Object
        public boolean equals(Object o) {
            if (!(o instanceof Fact)) {
                return false;
            }

            Fact of = (Fact) o;

            for (int i = 0; i < NUM_TERMS; i++) {
                if (mTerms[i] != of.mTerms[i]) {
                    return false;
                }
            }

            return true;
        }

        /**
         * Make equivalent fact given substitutions.
         */
        public Fact getMirror() {
            int[] terms = new int[NUM_TERMS];

            for (int i = 0; i < NUM_TERMS; i++) {
                if (TERMS[i].isEmpty()) {
                    terms[i] = mTerms[i];
                } else {
                    char ch = TERMS[i].charAt(0);

                    for (String substitution : SUBSTITUTIONS) {
                        if (ch == substitution.charAt(0)) {
                            ch = substitution.charAt(1);
                            break;
                        }
                        if (ch == substitution.charAt(1)) {
                            ch = substitution.charAt(0);
                            break;
                        }
                    }
                    int j = getTermIndex(ch);
                    terms[j] = mTerms[i];
                }
            }

            /*
            Fact fact1Mirror = null;
            Fact fact2Mirror = null;
            if (mSourceFact1 != null) {
                fact1Mirror = silentlyIntern(mSourceFact1.getMirror());
                fact2Mirror = silentlyIntern(mSourceFact2.getMirror());
            }

            return new Fact(terms, mGeneration, fact1Mirror, fact2Mirror,
                    mMultiplier1, mMultiplier2);
                    */
            return new Fact(terms, mGeneration, null, null, // XXX
                    mMultiplier1, mMultiplier2);
        }

        /**
         * Returns the highest absolute value of any coefficient (except the constant).
         */
        public int getHighestCoefficient() {
            int highestCoefficient = 0;

            for (int i = 0; i < NUM_TERMS; i++) {
                if (!TERMS[i].isEmpty()) {
                    int coefficient = mTerms[i];
                    if (coefficient < 0) {
                        coefficient = -coefficient;
                    }
                    highestCoefficient = Math.max(highestCoefficient, coefficient);
                }
            }

            return highestCoefficient;
        }

        /**
         * Return the number of non-zero coefficients, including the constant.
         */
        public int getCoefficientCount() {
            int coefficientCount = 0;

            for (int i = 0; i < NUM_TERMS; i++) {
                if (mTerms[i] != 0) {
                    coefficientCount++;
                }
            }

            return coefficientCount;
        }

        public void printTrace(int indent, int multiplier) {
            for (int i = 0; i < indent; i++) {
                System.out.print(" ");
            }
            if (multiplier != 0) {
                System.out.print(multiplier + "*(");
            }
            System.out.print(toString());
            if (multiplier != 0) {
                System.out.print(")");
            }
            System.out.println();

            if (mSourceFact1 != null && mSourceFact2 != null) {
                mSourceFact1.printTrace(indent + 4, mMultiplier1);
                mSourceFact2.printTrace(indent + 4, mMultiplier2);
            }
        }

        public double getInterestingness() {
            return mInterestingness;
        }
    }
}
