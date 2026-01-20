package sdfrpe.github.io.ptcclans.Utils;

public class EloCalculator {
    private static final int K_FACTOR_NORMAL = 32;
    private static final int K_FACTOR_FORFEIT = 16;
    private static final int MIN_ELO = 0;
    private static final int MAX_ELO = 5000;

    public static int calculateEloChange(int winnerElo, int loserElo, boolean wasForfeit) {
        int kFactor = wasForfeit ? K_FACTOR_FORFEIT : K_FACTOR_NORMAL;
        double expectedWinner = getExpectedScore(winnerElo, loserElo);
        int eloChange = (int) Math.round(kFactor * (1.0 - expectedWinner));
        return Math.max(1, eloChange);
    }

    public static double getExpectedScore(int playerElo, int opponentElo) {
        return 1.0 / (1.0 + Math.pow(10.0, (opponentElo - playerElo) / 400.0));
    }

    public static int clampElo(int elo) {
        return Math.max(MIN_ELO, Math.min(MAX_ELO, elo));
    }

    public static EloResult calculateEloChanges(int winnerElo, int loserElo, boolean wasForfeit) {
        int eloChange = calculateEloChange(winnerElo, loserElo, wasForfeit);
        int newWinnerElo = clampElo(winnerElo + eloChange);
        int newLoserElo = clampElo(loserElo - eloChange);
        return new EloResult(newWinnerElo, newLoserElo, eloChange);
    }

    public static class EloResult {
        private final int newWinnerElo;
        private final int newLoserElo;
        private final int eloChange;

        public EloResult(int newWinnerElo, int newLoserElo, int eloChange) {
            this.newWinnerElo = newWinnerElo;
            this.newLoserElo = newLoserElo;
            this.eloChange = eloChange;
        }

        public int getNewWinnerElo() {
            return newWinnerElo;
        }

        public int getNewLoserElo() {
            return newLoserElo;
        }

        public int getEloChange() {
            return eloChange;
        }
    }
}