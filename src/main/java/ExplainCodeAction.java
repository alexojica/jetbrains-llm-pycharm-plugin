import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.jetbrains.python.psi.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.util.Objects;

public class ExplainCodeAction extends AnAction {
    private static final int MAX_TOKEN_LIMIT = 7000;
    private static final String CONTEXT_BATCH_PROMPT = "compress the following text in a way that fits in a tweet (ideally) and such that you (GPT-4) can reconstruct the intention of the human who wrote text as close as possible to the original intention. This is for yourself. It does not need to be human readable or understandable. Abuse of language mixing, abbreviations, symbols (unicode and emoji), or any other encodings or internal representations is all permissible, as long as it, if pasted in a new inference cycle, will yield near-identical results as the original text: ";
    private static final String SUMMARY_PROMPT = "decode the following summaries that you encoded and create an overall summary of them: ";
    private static final Integer MAX_TOKENS_PER_MINUTE = 9500;
    private final TokenTracker tokenTracker = new TokenTracker();
    private static final int MAX_WAIT_TIME_SECONDS = 10;


    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        Project project = e.getProject();
        if (editor == null || project == null) return;

        PyFunction selectedFunction = CodeParsingHelper.getSelectedFunction(editor, project);
        if (selectedFunction == null) {
            JOptionPane.showMessageDialog(null, "No Python function selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow("Explain Method ChatGPT");
        if (toolWindow != null) {
            toolWindow.show(() -> {});
            ExplainCodeToolWindowContentFactory contentFactory = project.getService(ExplainCodeToolWindowContentFactory.class);
            if (contentFactory == null) return;

            contentFactory.updateCode(selectedFunction.getText(), Objects.requireNonNull(Language.findLanguageByID("Python")));

            fetchExplanationAsync(selectedFunction, contentFactory);
        }
    }

    private void fetchExplanationAsync(PyFunction selectedFunction, ExplainCodeToolWindowContentFactory contentFactory) {
        SwingUtilities.invokeLater(() -> contentFactory.updateExplanation("Loading"));

        Timer loadingTimer = getLoadingTimer(contentFactory);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String explanation = getResponseFromLLM(selectedFunction);

            SwingUtilities.invokeLater(() -> {
                loadingTimer.stop();
                contentFactory.updateExplanation(explanation);
            });
        });
    }

    private String getResponseFromLLM(PyFunction function) {
        String context = CodeParsingHelper.prepareFunctionContext(function);
        context = CodeCompressor.compressCode(context);
        if (CodeCompressor.estimateTokenCount(context) > MAX_TOKEN_LIMIT) {
            String[] contextLines = context.split("\n");
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append(CONTEXT_BATCH_PROMPT);
            StringBuilder summaryBuilder = new StringBuilder();
            summaryBuilder.append(SUMMARY_PROMPT);

            for (String line : contextLines) {
                contextBuilder.append(line).append("\n");
                if (CodeCompressor.estimateTokenCount(contextBuilder.toString()) >= MAX_TOKEN_LIMIT) {
                int tokens = tokenTracker.getCurrentTokenCount();
                    try {
                        summaryBuilder.append(sendRequestToOpenAI(contextBuilder.toString()));
                    } catch (IOException | InterruptedException e) {
                        return "Error: " + e.getMessage() + "\n" + CodeCompressor.estimateTokenCount(contextBuilder.toString()) + "\n" + tokens + "\n" + summaryBuilder;
                    }
                    contextBuilder = new StringBuilder();
                    contextBuilder.append(CONTEXT_BATCH_PROMPT);
                }
            }
            int tokens = tokenTracker.getCurrentTokenCount();
            try {
                summaryBuilder.append(sendRequestToOpenAI(contextBuilder.toString()));
                return sendRequestToOpenAI(summaryBuilder.toString());
            } catch (IOException | InterruptedException e) {
                return "Error: " + e.getMessage()+ "\n" + CodeCompressor.estimateTokenCount(contextBuilder.toString()) + "\n" + tokens + "\n" + summaryBuilder;
            }
        } else {
            try {
                return sendRequestToOpenAI(context);
            } catch (IOException | InterruptedException e) {
                return "Error: " + e.getMessage();
            }
        }
    }

    private String sendRequestToOpenAI(String request) throws IOException, InterruptedException {
        int estimatedTokens = CodeCompressor.estimateTokenCount(request);
        int waitTime = tokenTracker.getRemainingWaitTime();

        if (tokenTracker.getCurrentTokenCount() + estimatedTokens > MAX_TOKENS_PER_MINUTE) {
            if (waitTime > MAX_WAIT_TIME_SECONDS) {
                throw new IOException("Token limit exceeded, and wait time is too long: " + waitTime + " seconds.");
            }
            try {
                Thread.sleep(waitTime * 1000L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted while waiting for token availability", ex);
            }
        }

        return ChatGPTApiClient.getExplanationFromLLM(request, tokenTracker);
    }

    @NotNull
    private static Timer getLoadingTimer(ExplainCodeToolWindowContentFactory contentFactory) {
        Timer loadingTimer = new Timer(300, null);
        loadingTimer.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                String currentText = contentFactory.getExplanationText();
                if (currentText.endsWith("...")) {
                    contentFactory.updateExplanation("Loading");
                } else {
                    contentFactory.updateExplanation(currentText + ".");
                }
            });
        });
        loadingTimer.setRepeats(true);
        loadingTimer.start();
        return loadingTimer;
    }
}
