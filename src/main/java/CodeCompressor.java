public class CodeCompressor {
    private static final int MAX_TOKEN_LIMIT = 7000;

    public static String compressCode(String code) {
        if (estimateTokenCount(code) <= MAX_TOKEN_LIMIT) return code; // No need to compress

        return removeNonEssentialComments(code);
    }

    private static String removeNonEssentialComments(String code) {
        return code.replaceAll("(?m)^\\s*#.*", ""); // Regex to remove full-line comments
    }

    public static int estimateTokenCount(String text) {
        String delimiters = "\\s+|\\.|,|:|;|\\?|!|-|\\(|\\)|\\[|\\]|\\{|\\}|'|\"|&|\\*|%|\\$|#|\n|\r|\t";
        String[] tokens = text.split(delimiters);

        int tokenCount = 0;
        for (String token : tokens) {
            int length = token.length();
            tokenCount += length / 4;
            if (length % 4 != 0) {
                tokenCount++;
            }
        }

        // Apply a correction factor to adjust the total count (reducing by 10%)
        double correctionFactor = 1.11;
        tokenCount = (int) Math.ceil(tokenCount * correctionFactor);

        return tokenCount;
    }
}
