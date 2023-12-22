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

    public ExplainCodeToolWindowContentFactory(Project project) {
        this.project = project;
        this.explanationArea = new JTextArea();
        this.codeEditor = createCodeEditor();
        this.explanationArea.setText("Explanation will appear here...");
        initializeComponents();
        initializeApiKeyButton();
    }

    private void initializeComponents() {
        explanationArea.setEditable(false);
        explanationArea.setLineWrap(true);
        explanationArea.setWrapStyleWord(true);
        explanationArea.setFont(new Font("Arial", Font.PLAIN, 16)); // Set a better font for explanation
        explanationArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add some padding
        explanationArea.setBackground(Gray._43); // Dark background
        explanationArea.setForeground(new Color(169, 183, 198)); // Light font color for readability
    }

    private void initializeApiKeyButton() {
        apiKeyButton = new JButton("Update API Key");
        apiKeyButton.addActionListener(e -> promptAndUpdateApiKey());
    }

    private void promptAndUpdateApiKey() {
        String newApiKey = JOptionPane.showInputDialog("Enter new OpenAI API Key:");
        if (newApiKey != null && !newApiKey.trim().isEmpty()) {
            saveApiKey(newApiKey);
            JOptionPane.showMessageDialog(null, "API Key updated successfully.");
        }
    }

    private void saveApiKey(String apiKey) {
        CredentialAttributes attributes = new CredentialAttributes(ChatGPTApiClient.getServiceName());
        PasswordSafe.getInstance().setPassword(attributes, apiKey);
    }

    private EditorEx createCodeEditor() {
        EditorFactory editorFactory = EditorFactory.getInstance();
        Document document = editorFactory.createDocument("");
        EditorEx editor = (EditorEx) editorFactory.createViewer(document, project);
        editor.setEmbeddedIntoDialogWrapper(true);

        // Apply a border to the editor to visually distinguish it
        editor.getComponent().setBorder(BorderFactory.createLineBorder(JBColor.GRAY, 1));

        EditorSettings settings = editor.getSettings();
        settings.setLineNumbersShown(true);

        // Apply the default editor scheme for colors and fonts
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        editor.setColorsScheme(scheme);
        editor.setBackgroundColor(scheme.getDefaultBackground()); // Ensure background matches the theme

        return editor;
    }

    public Content createContent() {
        // Main panel to contain the code editor and explanation area
        JBSplitter splitter = getJbSplitter();

        // Create a panel for the API key button
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(apiKeyButton, BorderLayout.EAST); // Position the button to the right

        // Main container panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(splitter, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH); // Add the button panel at the bottom

        // Create and return content for the tool window
        ContentFactory contentFactory = ContentFactory.getInstance();
        return contentFactory.createContent(mainPanel, "", false);
    }

    @NotNull
    private JBSplitter getJbSplitter() {
        JBSplitter splitter = new JBSplitter(true, 0.5f); // Create a splitter with initial proportion

        // Add the code editor to the top of the splitter
        JScrollPane codeScrollPane = new JBScrollPane(codeEditor.getComponent());
        splitter.setFirstComponent(codeScrollPane);

        // Explanation area inside a scroll pane
        JScrollPane explanationScrollPane = new JBScrollPane(explanationArea);
        splitter.setSecondComponent(explanationScrollPane);
        return splitter;
    }


    public void updateCode(String code, Language language) {
        // Get the file type for the provided language extension
        FileType fileType = language.getAssociatedFileType();
        // Access the document directly from the editor
        Document document = codeEditor.getDocument();

        // Use the document to set the new text
        WriteCommandAction.runWriteCommandAction(project, () -> document.replaceString(0, document.getTextLength(), code));

        // Set the syntax highlighter based on the language
        EditorHighlighter highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType);
        codeEditor.setHighlighter(highlighter);
    }


    public void updateExplanation(String explanation) {
        explanationArea.setText(explanation);
    }

    // Make sure to dispose of the editor when it's no longer needed
    public void dispose() {
        EditorFactory.getInstance().releaseEditor(codeEditor);
    }

    public String getExplanationText() {
        return explanationArea.getText();
    }
}
