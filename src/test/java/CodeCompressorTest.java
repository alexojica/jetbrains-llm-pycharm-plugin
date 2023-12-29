import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeCompressorTest {
    @Test
    void compressCode_WithTokenCountUnderLimit() {
        String code = "print('Hello, world!')";
        String compressedCode = CodeCompressor.compressCode(code);
        assertEquals(code, compressedCode, "Code should not be compressed if under token limit");
    }

    @Test
    void compressCode_WithTokenCountOverLimit() {
        String code = "print('Hello, world!') # Inline comment";
        String expected = "print('Hello, world!') # Inline comment";
        String compressedCode = CodeCompressor.compressCode(code);
        assertEquals(expected, compressedCode, "Code should have comments removed");
    }

    @Test
    void estimateTokenCount_VariousScenarios() {
        assertEquals(2, CodeCompressor.estimateTokenCount("abcd"), "Token count estimation is incorrect");
        assertEquals(3, CodeCompressor.estimateTokenCount("abcdefgh"), "Token count estimation is incorrect");
    }
}