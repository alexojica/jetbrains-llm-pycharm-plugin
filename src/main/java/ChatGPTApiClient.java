import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.intellij.openapi.diagnostic.Logger;
import javax.swing.JOptionPane;


public class ChatGPTApiClient {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final Logger LOG = Logger.getInstance(ChatGPTApiClient.class);
    private static final String SERVICE_NAME = "ChatGPTApiClientService";
    private static HttpClient httpClient;
    private static PasswordSafe passwordSafe;

    /**
     * Sets the PasswordSafe instance to be used for storing and retrieving API keys.
     *
     * @param ps The PasswordSafe instance.
     */
    public static void setPasswordSafe(PasswordSafe ps) {
        passwordSafe = ps;
    }

    /**
     * Gets the PasswordSafe instance used for storing and retrieving API keys. If not set, a new instance is created.
     *
     * @return The PasswordSafe instance.
     */
    private static PasswordSafe getPasswordSafe() {
        if (passwordSafe == null) {
            passwordSafe = PasswordSafe.getInstance();
        }
        return passwordSafe;
    }

    /**
     * Sets the HttpClient instance to be used for making HTTP requests.
     *
     * @param client The HttpClient instance.
     */
    public static void setHttpClient(HttpClient client) {
        httpClient = client;
    }

    /**
     * Gets the HttpClient instance used for making HTTP requests. If not set, a new instance is created.
     *
     * @return The HttpClient instance.
     */
    private static HttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = HttpClient.newHttpClient();
        }
        return httpClient;
    }

    /**
     * Gets an explanation from the Language Model for the given prompt using the ChatGPT API.
     *
     * @param prompt       The prompt for which an explanation is requested.
     * @param tokenTracker A TokenTracker instance to record token usage.
     * @return The explanation as a String.
     * @throws IOException          If an I/O error occurs during the HTTP request.
     * @throws InterruptedException If the HTTP request is interrupted.
     */
    public static String getExplanationFromLLM(String prompt, TokenTracker tokenTracker) throws IOException, InterruptedException {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = promptUserForApiKey();
            if (apiKey == null || apiKey.isEmpty()) {
                throw new RuntimeException("API key is required.");
            }
            saveApiKey(apiKey);
        }

        String requestBody = createRequestBody(prompt);
        HttpClient client = getHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            LOG.warn(response.statusCode() + " " + response.body());
            updateTokenUsage(response.body(), tokenTracker);
            return formatResponse(response.body());
        } else {
            String errorMessage = "Received non-200 response from ChatGPT API: " + response.body();
            LOG.error(errorMessage);
            throw new IOException(errorMessage);
        }
    }

     /**
     * Updates token usage information based on the JSON response from the ChatGPT API.
     *
     * @param jsonResponse   The JSON response from the ChatGPT API.
     * @param tokenTracker   A TokenTracker instance to record token usage.
     */
    static void updateTokenUsage(String jsonResponse, TokenTracker tokenTracker) {
        JSONObject responseObject = new JSONObject(jsonResponse);
        if (responseObject.has("usage")) {
            JSONObject usage = responseObject.getJSONObject("usage");
            int totalTokens = usage.getInt("total_tokens");
            tokenTracker.recordTokenUsage(totalTokens);
        }
    }

    /**
     * Gets the name of the service used for storing API keys securely.
     *
     * @return The service name.
     */
    public static String getServiceName() {
        return SERVICE_NAME;
    }

    /**
     * Gets the API key from the PasswordSafe, if it exists.
     *
     * @return The API key as a String, or null if not found.
     */
    private static String getApiKey() {
        CredentialAttributes attributes = new CredentialAttributes(SERVICE_NAME);
        return getPasswordSafe().getPassword(attributes);
    }

    /**
     * Saves the API key to the PasswordSafe for secure storage.
     *
     * @param apiKey The API key to be saved.
     */
    private static void saveApiKey(String apiKey) {
        CredentialAttributes attributes = new CredentialAttributes(SERVICE_NAME);
        getPasswordSafe().setPassword(attributes, apiKey);
    }

    /**
     * Prompts the user to enter their OpenAI API key.
     *
     * @return The user-entered API key as a String.
     */
    private static String promptUserForApiKey() {
        return JOptionPane.showInputDialog("Enter OpenAI API Key:");
    }

    /**
     * Formats the JSON response from the ChatGPT API into a readable explanation.
     *
     * @param jsonResponse The JSON response from the ChatGPT API.
     * @return The formatted explanation as a String.
     */
    static String formatResponse(String jsonResponse) {
        JSONObject responseObject = new JSONObject(jsonResponse);
        JSONArray choices = responseObject.getJSONArray("choices");

        if (!choices.isEmpty()) {
            JSONObject firstChoice = choices.getJSONObject(0);
            JSONObject message = firstChoice.getJSONObject("message");
            String content = message.getString("content");

            return content.replace("\\n", "\n");
        }

        return "No response found.";
    }

    /**
     * Creates a JSON request body for sending a prompt to the ChatGPT API.
     *
     * @param prompt The prompt to be sent to the API.
     * @return The JSON request body as a String.
     */
    static String createRequestBody(String prompt) {
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        JSONArray messages = new JSONArray();
        messages.put(userMessage);

        JSONObject data = new JSONObject();
        data.put("model", "gpt-4");
        data.put("messages", messages);
        data.put("temperature", 1);
        data.put("top_p", 1);
        data.put("n", 1);
        data.put("stream", false);
        data.put("max_tokens", 1000);
        data.put("presence_penalty", 0);
        data.put("frequency_penalty", 0);

        return data.toString();
    }
}
