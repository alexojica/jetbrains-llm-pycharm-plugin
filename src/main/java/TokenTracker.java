import java.util.Deque;
import java.util.LinkedList;

/**
 * A class for tracking token usage over a time window.
 */
public class TokenTracker {
    private static final long TIME_WINDOW_MS = 60000;
    private final Deque<TokenRecord> tokenRecords = new LinkedList<>();
    private int currentTokenCount = 0;

    /**
     * Records the usage of tokens and adds a new token record to the deque.
     *
     * @param tokenCount The number of tokens used.
     */
    public synchronized void recordTokenUsage(int tokenCount) {
        long currentTime = System.currentTimeMillis();
        currentTokenCount += tokenCount;
        tokenRecords.addLast(new TokenRecord(tokenCount, currentTime));
        removeExpiredRecords(currentTime);
    }

    /**
     * Calculates and returns the remaining wait time in seconds based on the oldest token record.
     *
     * @return The remaining wait time in seconds.
     */
    public synchronized int getRemainingWaitTime() {
        long currentTime = System.currentTimeMillis();
        removeExpiredRecords(currentTime);
        if (!tokenRecords.isEmpty()) {
            long oldestTimestamp = tokenRecords.getFirst().timestamp;
            long timePassed = currentTime - oldestTimestamp;
            return (int) ((TIME_WINDOW_MS - timePassed) / 1000);
        }
        return 0;
    }

    /**
     * Removes expired token records from the deque and updates the current token count.
     *
     * @param currentTime The current time in milliseconds.
     */
    private void removeExpiredRecords(long currentTime) {
        while (!tokenRecords.isEmpty() && currentTime - tokenRecords.getFirst().timestamp > TIME_WINDOW_MS) {
            TokenRecord expired = tokenRecords.removeFirst();
            currentTokenCount -= expired.tokenCount;
        }
    }

    /**
     * Retrieves the current total token count, taking into account expired token records.
     *
     * @return The current total token count.
     */
    public synchronized int getCurrentTokenCount() {
        removeExpiredRecords(System.currentTimeMillis());
        return currentTokenCount;
    }

    /**
     * A private inner class representing a token usage record with token count and timestamp.
     */
    private static class TokenRecord {
        int tokenCount;
        long timestamp;

        /**
         * Constructs a TokenRecord with the given token count and timestamp.
         *
         * @param tokenCount The number of tokens used.
         * @param timestamp  The timestamp when the tokens were used.
         */
        TokenRecord(int tokenCount, long timestamp) {
            this.tokenCount = tokenCount;
            this.timestamp = timestamp;
        }
    }
}
