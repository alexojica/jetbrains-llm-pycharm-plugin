import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.jetbrains.python.psi.PyFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import javax.swing.*;

import static org.mockito.Mockito.*;

class ExplainCodeActionTest {
    @InjectMocks
    private ExplainCodeAction explainCodeAction;

    @Mock
    private AnActionEvent mockActionEvent;

    @Mock
    private Editor mockEditor;

    @Mock
    private Project mockProject;

    @Mock
    private ToolWindow mockToolWindow;

    @Mock
    private ExplainCodeToolWindowContentFactory mockContentFactory;

    @Mock
    private CodeParsingHelper codeParsingHelper;

    @Mock
    private PsiDocumentManager psiDocumentManager;

    @Mock
    private PsiFile psiFile;

    @Captor
    private ArgumentCaptor<Editor> editorCaptor;

    @Captor
    private ArgumentCaptor<Project> projectCaptor;
    @Mock
    private CaretModel mockCaretModel;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(mockActionEvent.getData(CommonDataKeys.EDITOR)).thenReturn(mockEditor);
        when(mockActionEvent.getProject()).thenReturn(mockProject);

        PsiDocumentManager psiDocumentManagerInstance = Mockito.mock(PsiDocumentManager.class);
        when(PsiDocumentManager.getInstance(mockProject)).thenReturn(psiDocumentManagerInstance);

        Document mockDocument = Mockito.mock(Document.class);
        when(mockEditor.getDocument()).thenReturn(mockDocument);
        when(psiDocumentManagerInstance.getPsiFile(mockDocument)).thenReturn(psiFile);

        when(mockCaretModel.getOffset()).thenReturn(0);
        when(mockEditor.getCaretModel()).thenReturn(mockCaretModel);

        PyFunction mockFunction = Mockito.mock(PyFunction.class);
        when(codeParsingHelper.getSelectedFunction(mockEditor, mockProject)).thenReturn(mockFunction);

        // Mock ToolWindowManager
        ToolWindowManager mockToolWindowManager = Mockito.mock(ToolWindowManager.class);
        when(mockProject.getService(ToolWindowManager.class)).thenReturn(mockToolWindowManager);

        when(mockToolWindowManager.getToolWindow("Explain Method ChatGPT")).thenReturn(mockToolWindow);
        when(mockProject.getService(ExplainCodeToolWindowContentFactory.class)).thenReturn(mockContentFactory);
    }

    @Test
    public void testActionPerformed_WithSelectedFunction() {
        Application mockApplication = Mockito.mock(Application.class);

        MockedStatic<ApplicationManager> mockApplicationManager = Mockito.mockStatic(ApplicationManager.class);
        mockApplicationManager.when(ApplicationManager::getApplication).thenReturn(mockApplication);
        PyFunction mockFunction = Mockito.mock(PyFunction.class);
        when(CodeParsingHelper.getSelectedFunction(mockEditor, mockProject)).thenReturn(mockFunction);

        explainCodeAction.actionPerformed(mockActionEvent);

        verify(mockToolWindow).show(any());
    }

    @Test
    public void testActionPerformed_NoSelectedFunction() {
        MockedStatic<JOptionPane> mockJOptionPane = Mockito.mockStatic(JOptionPane.class);

        when(CodeParsingHelper.getSelectedFunction(mockEditor, mockProject)).thenReturn(null);
        explainCodeAction.actionPerformed(mockActionEvent);

        verifyNoInteractions(mockToolWindow);
        verifyNoInteractions(mockContentFactory);
    }

}
