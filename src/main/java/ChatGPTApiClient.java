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
        HttpClient client = HttpClient.newHttpClient();
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

    private static void updateTokenUsage(String jsonResponse, TokenTracker tokenTracker) {
        JSONObject responseObject = new JSONObject(jsonResponse);
        if (responseObject.has("usage")) {
            JSONObject usage = responseObject.getJSONObject("usage");
            int totalTokens = usage.getInt("total_tokens");
            tokenTracker.recordTokenUsage(totalTokens);
        }
    }

    public static String getServiceName() {
        return SERVICE_NAME;
    }

    private static String getApiKey() {
        CredentialAttributes attributes = new CredentialAttributes(SERVICE_NAME);
        return PasswordSafe.getInstance().getPassword(attributes);
    }

    private static void saveApiKey(String apiKey) {
        CredentialAttributes attributes = new CredentialAttributes(SERVICE_NAME);
        PasswordSafe.getInstance().setPassword(attributes, apiKey);
    }

    private static String promptUserForApiKey() {
        return JOptionPane.showInputDialog("Enter OpenAI API Key:");
    }


    private static String formatResponse(String jsonResponse) {
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


    private static String createRequestBody(String prompt) {
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
