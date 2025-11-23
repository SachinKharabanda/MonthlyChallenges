package Challenges;

/**
 * Central place for challenge IDs and goals, plus helper methods.
 */
public final class Challenges {

    // Challenge IDs – must match config.yml "challenge-id" values
    public static final int CHEST_CHALLENGE_ID = 1;
    public static final int CRIMINAL_CHALLENGE_ID = 2;
    public static final int COPS_CHALLENGE_ID = 3;
    public static final int PLAYER_CHALLENGE_ID = 4;
    public static final int BALANCE_CHALLENGE_ID = 5;

    // Goals
    public static final int CHEST_GOAL = 1000;
    public static final int CRIMINAL_GOAL = 1000;
    public static final int COPS_GOAL = 100;
    public static final int PLAYER_GOAL = 50;
    public static final double BALANCE_GOAL = 50000.0D;

    private Challenges() {
        // Utility class
    }

    public static boolean isChestChallengeComplete(int current) {
        return current >= CHEST_GOAL;
    }

    public static boolean isCriminalChallengeComplete(int current) {
        return current >= CRIMINAL_GOAL;
    }

    public static boolean isCopsChallengeComplete(int current) {
        return current >= COPS_GOAL;
    }

    public static boolean isPlayerChallengeComplete(int current) {
        return current >= PLAYER_GOAL;
    }

    public static boolean isBalanceChallengeComplete(double balance) {
        return balance >= BALANCE_GOAL;
    }
}
