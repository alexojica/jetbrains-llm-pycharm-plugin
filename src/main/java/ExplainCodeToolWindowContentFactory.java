import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.lang.Language;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class ExplainCodeToolWindowContentFactory {
    private final Project project;
    private final EditorEx codeEditor;
    private final JTextArea explanationArea;
    private JButton apiKeyButton;

    /**
     * Constructs an instance of ExplainCodeToolWindowContentFactory.
     *
     * @param project The Project associated with the content.
     */
    public ExplainCodeToolWindowContentFactory(Project project) {
        this.project = project;
        this.explanationArea = new JTextArea();
        this.codeEditor = createCodeEditor();
        this.explanationArea.setText("Explanation will appear here...");
        initializeComponents();
        initializeApiKeyButton();
    }

    /**
     * Initializes the components for the explanation area and code editor.
     */
    private void initializeComponents() {
        explanationArea.setEditable(false);
        explanationArea.setLineWrap(true);
        explanationArea.setWrapStyleWord(true);
        explanationArea.setFont(new Font("Arial", Font.PLAIN, 16));
        explanationArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        explanationArea.setBackground(Gray._43);
        explanationArea.setForeground(new Color(169, 183, 198));
    }

    /**
     * Initializes the API key update button.
     */
    private void initializeApiKeyButton() {
        apiKeyButton = new JButton("Update API Key");
        apiKeyButton.addActionListener(e -> promptAndUpdateApiKey());
    }

    /**
     * Prompts the user to enter and save a new API key.
     */
    private void promptAndUpdateApiKey() {
        String newApiKey = JOptionPane.showInputDialog("Enter new OpenAI API Key:");
        if (newApiKey != null && !newApiKey.trim().isEmpty()) {
            saveApiKey(newApiKey);
            JOptionPane.showMessageDialog(null, "API Key updated successfully.");
        }
    }

    /**
     * Saves the provided API key securely.
     *
     * @param apiKey The new API key to be saved.
     */
    private void saveApiKey(String apiKey) {
        CredentialAttributes attributes = new CredentialAttributes(ChatGPTApiClient.getServiceName());
        PasswordSafe.getInstance().setPassword(attributes, apiKey);
    }

    /**
     * Creates a code editor for displaying and editing code.
     *
     * @return The created code editor.
     */
    private EditorEx createCodeEditor() {
        EditorFactory editorFactory = EditorFactory.getInstance();
        Document document = editorFactory.createDocument("");
        EditorEx editor = (EditorEx) editorFactory.createViewer(document, project);
        editor.setEmbeddedIntoDialogWrapper(true);

        editor.getComponent().setBorder(BorderFactory.createLineBorder(JBColor.GRAY, 1));

        EditorSettings settings = editor.getSettings();
        settings.setLineNumbersShown(true);

        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        editor.setColorsScheme(scheme);
        editor.setBackgroundColor(scheme.getDefaultBackground());

        return editor;
    }

    /**
     * Creates the content for the ExplainCode tool window.
     *
     * @return The created content for the tool window.
     */
    public Content createContent() {
        JBSplitter splitter = getJbSplitter();

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(apiKeyButton, BorderLayout.EAST);

        // Main container panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(splitter, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        ContentFactory contentFactory = ContentFactory.getInstance();
        return contentFactory.createContent(mainPanel, "", false);
    }


    @NotNull
    private JBSplitter getJbSplitter() {
        JBSplitter splitter = new JBSplitter(true, 0.5f);

        JScrollPane codeScrollPane = new JBScrollPane(codeEditor.getComponent());
        splitter.setFirstComponent(codeScrollPane);

        JScrollPane explanationScrollPane = new JBScrollPane(explanationArea);
        splitter.setSecondComponent(explanationScrollPane);
        return splitter;
    }

    /**
     * Updates the code editor with the provided code and sets the syntax highlighter based on the language.
     *
     * @param code     The code to be displayed in the editor.
     * @param language The language associated with the code.
     */
    public void updateCode(String code, Language language) {
        FileType fileType = language.getAssociatedFileType();
        Document document = codeEditor.getDocument();

        WriteCommandAction.runWriteCommandAction(project, () -> document.replaceString(0, document.getTextLength(), code));

        EditorHighlighter highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType);
        codeEditor.setHighlighter(highlighter);
    }

    /**
     * Updates the explanation text displayed in the explanation area.
     *
     * @param explanation The explanation to be displayed.
     */
    public void updateExplanation(String explanation) {
        explanationArea.setText(explanation);
    }

    /**
     * Gets the text content of the explanation area.
     *
     * @return The text content of the explanation area.
     */
    public String getExplanationText() {
        return explanationArea.getText();
    }
}
