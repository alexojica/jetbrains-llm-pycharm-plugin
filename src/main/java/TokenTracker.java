import java.util.Deque;
import java.util.LinkedList;

public class TokenTracker {
    private static final long TIME_WINDOW_MS = 60000;
    private final Deque<TokenRecord> tokenRecords = new LinkedList<>();
    private int currentTokenCount = 0;

    public synchronized void recordTokenUsage(int tokenCount) {
        long currentTime = System.currentTimeMillis();
        currentTokenCount += tokenCount;
        tokenRecords.addLast(new TokenRecord(tokenCount, currentTime));
        removeExpiredRecords(currentTime);
    }

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

    private void removeExpiredRecords(long currentTime) {
        while (!tokenRecords.isEmpty() && currentTime - tokenRecords.getFirst().timestamp > TIME_WINDOW_MS) {
            TokenRecord expired = tokenRecords.removeFirst();
            currentTokenCount -= expired.tokenCount;
        }
    }

    public synchronized int getCurrentTokenCount() {
        removeExpiredRecords(System.currentTimeMillis());
        return currentTokenCount;
    }

    private static class TokenRecord {
        int tokenCount;
        long timestamp;

        TokenRecord(int tokenCount, long timestamp) {
            this.tokenCount = tokenCount;
            this.timestamp = timestamp;
        }
    }
}
