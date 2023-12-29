import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.intellij.openapi.diagnostic.Logger;

class ChatGPTApiClientTest {

    private MockedStatic<Logger> mockedLogger;
    private MockedStatic<PasswordSafe> mockedPasswordSafe;

    @BeforeEach
    void setUp() {
        // Mock the Logger static methods
        mockedLogger = Mockito.mockStatic(Logger.class);
        Logger mockLoggerInstance = Mockito.mock(Logger.class);
        mockedLogger.when(() -> Logger.getInstance(any(Class.class))).thenReturn(mockLoggerInstance);
        mockedPasswordSafe = Mockito.mockStatic(PasswordSafe.class);
        PasswordSafe mockPasswordSafeInstance = Mockito.mock(PasswordSafe.class);
        mockedPasswordSafe.when(PasswordSafe::getInstance).thenReturn(mockPasswordSafeInstance);
        Mockito.when(mockPasswordSafeInstance.getPassword(any(CredentialAttributes.class))).thenReturn("mock-api-key");
    }

    @AfterEach
    void tearDown() {
        // Close the static mock
        mockedLogger.close();
        mockedPasswordSafe.close();
    }

    @Test
    void getExplanationFromLLMSuccessfulResponse() throws IOException, InterruptedException {
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse mockResponse = mock(HttpResponse.class);

        Mockito.when(mockHttpClient.send(any(HttpRequest.class), any())).thenReturn(mockResponse);
        Mockito.when(mockResponse.statusCode()).thenReturn(200);
        Mockito.when(mockResponse.body()).thenReturn("{\"choices\": [{\"message\": {\"content\": \"Test explanation\"}}]}");

        TokenTracker mockTokenTracker = mock(TokenTracker.class);

        ChatGPTApiClient.setHttpClient(mockHttpClient);

        String result = ChatGPTApiClient.getExplanationFromLLM("test prompt", mockTokenTracker);

        assertEquals("Test explanation", result);
    }

    @Test
    void getExplanationFromLLMErrorResponse() throws IOException, InterruptedException {
        HttpClient mockHttpClient = Mockito.mock(HttpClient.class);
        HttpResponse mockResponse = mock(HttpResponse.class);

        Mockito.when(mockHttpClient.send(any(HttpRequest.class), any())).thenReturn(mockResponse);
        Mockito.when(mockResponse.statusCode()).thenReturn(400);
        Mockito.when(mockResponse.body()).thenReturn("Error message");

        TokenTracker mockTokenTracker = Mockito.mock(TokenTracker.class);
        PasswordSafe mockPasswordSafe = Mockito.mock(PasswordSafe.class);

        Mockito.when(mockPasswordSafe.getPassword(any(CredentialAttributes.class))).thenReturn("mock-api-key");

        ChatGPTApiClient.setHttpClient(mockHttpClient);
        ChatGPTApiClient.setPasswordSafe(mockPasswordSafe);

        IOException exception = assertThrows(IOException.class, () ->
            ChatGPTApiClient.getExplanationFromLLM("test prompt", mockTokenTracker)
        );

        String expectedMessage = "Received non-200 response from ChatGPT API: Error message";
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void updateTokenUsage() {
        String jsonResponse = "{\"usage\": {\"total_tokens\": 100}}";
        TokenTracker tokenTracker = mock(TokenTracker.class);

        ChatGPTApiClient.updateTokenUsage(jsonResponse, tokenTracker);

        Mockito.verify(tokenTracker).recordTokenUsage(100);
    }

    @Test
    void updateTokenUsageNoUsageField() {
        String jsonResponse = "{}";
        TokenTracker mockTokenTracker = mock(TokenTracker.class);

        ChatGPTApiClient.updateTokenUsage(jsonResponse, mockTokenTracker);

        Mockito.verify(mockTokenTracker, Mockito.never()).recordTokenUsage(anyInt());
    }

    @Test
    void formatResponseValidResponse() {
        String jsonResponse = "{\"choices\": [{\"message\": {\"content\": \"Test explanation\"}}]}";
        String result = ChatGPTApiClient.formatResponse(jsonResponse);
        assertEquals("Test explanation", result);
    }

    @Test
    void formatResponseNoChoices() {
        String jsonResponse = "{\"choices\": []}";
        String result = ChatGPTApiClient.formatResponse(jsonResponse);
        assertEquals("No response found.", result);
    }

    @Test
    void createRequestBody() {
        String prompt = "test prompt";
        JSONObject expectedJson = new JSONObject();
        expectedJson.put("model", "gpt-4");
        expectedJson.put("messages", new JSONArray().put(new JSONObject().put("role", "user").put("content", prompt)));
        expectedJson.put("temperature", 1);
        expectedJson.put("top_p", 1);
        expectedJson.put("n", 1);
        expectedJson.put("stream", false);
        expectedJson.put("max_tokens", 1000);
        expectedJson.put("presence_penalty", 0);
        expectedJson.put("frequency_penalty", 0);

        String actualBody = ChatGPTApiClient.createRequestBody(prompt);
        JSONObject actualJson = new JSONObject(actualBody);

        assertEquals(expectedJson.toString(), actualJson.toString());
    }
}
