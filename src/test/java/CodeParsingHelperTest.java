import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.PyFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CodeParsingHelperTest {
    private Editor mockEditor;
    private Project mockProject;
    private PsiDocumentManager mockPsiDocumentManager;
    private Document mockDocument;

    @BeforeEach
    void setUp() {
        mockEditor = mock(Editor.class);
        mockProject = mock(Project.class);
        mockPsiDocumentManager = mock(PsiDocumentManager.class);
        mockDocument = mock(Document.class);
        CaretModel mockCaretModel = mock(CaretModel.class);

        when(mockEditor.getDocument()).thenReturn(mockDocument);
        when(mockEditor.getCaretModel()).thenReturn(mockCaretModel);
        when(mockCaretModel.getOffset()).thenReturn(0);
        when(PsiDocumentManager.getInstance(mockProject)).thenReturn(mockPsiDocumentManager);
    }

    @Test
    void getSelectedFunction_NullFile() {
        when(mockPsiDocumentManager.getPsiFile(mockDocument)).thenReturn(null);

        PyFunction result = CodeParsingHelper.getSelectedFunction(mockEditor, mockProject);
        assertNull(result, "Should return null if PsiFile is null");
    }

    @Test
    void getSelectedFunction_ValidFunction() {
        PsiFile mockPsiFile = mock(PsiFile.class);
        PsiElement mockElementAtCaret = mock(PsiElement.class);
        PyFunction mockPyFunction = mock(PyFunction.class);

        when(mockPsiDocumentManager.getPsiFile(mockDocument)).thenReturn(mockPsiFile);
        when(mockPsiFile.findElementAt(0)).thenReturn(mockElementAtCaret);
        when(PsiTreeUtil.getParentOfType(mockElementAtCaret, PyFunction.class, false)).thenReturn(mockPyFunction);

        PyFunction result = CodeParsingHelper.getSelectedFunction(mockEditor, mockProject);

        assertNotNull(result, "Should return a valid PyFunction");
        assertEquals(mockPyFunction, result, "Should return the correct PyFunction");
    }
}