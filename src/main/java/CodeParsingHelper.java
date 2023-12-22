import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.*;

import java.util.HashSet;
import java.util.Set;

public class CodeParsingHelper {
    public static PyFunction getSelectedFunction(Editor editor, Project project) {
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (psiFile == null) {
            return null;
        }

        int offset = editor.getCaretModel().getOffset();
        PsiElement elementAtCaret = psiFile.findElementAt(offset);
        if (elementAtCaret == null) {
            return null;
        }

        return PsiTreeUtil.getParentOfType(elementAtCaret, PyFunction.class, false);
    }

    public static String prepareFunctionContext(PyFunction function) {
        StringBuilder contextBuilder = new StringBuilder();
        PyClass containingClass = PsiTreeUtil.getParentOfType(function, PyClass.class);

        String functionText;
        if (containingClass != null) {
            contextBuilder.append("Class Context:\n").append(containingClass.getText()).append("\n\n");
            functionText = function.getName();
        } else {
            functionText = function.getText();
        }

        contextBuilder.append("Function to Explain:\n").append(functionText).append("\n\n");

        PsiFile containingFile = function.getContainingFile();
        if (containingFile instanceof PyFile) {
            addUsedGlobalReferences((PyFile) containingFile, function, contextBuilder);
        }

        return contextBuilder.toString();
    }

    private static void addUsedGlobalReferences(PyFile pyFile, PyFunction function, StringBuilder contextBuilder) {
        Set<String> usedReferences = new HashSet<>();
        collectUsedReferences(function, usedReferences);

        for (PyImportStatementBase importStatement : pyFile.getImportBlock()) {
            if (isImportUsed(importStatement, usedReferences)) {
                contextBuilder.append(importStatement.getText()).append("\n");
            }
        }

        for (PsiElement element : pyFile.getChildren()) {
            if ((element instanceof PyAssignmentStatement || element instanceof PyFunction) && isElementUsed(element, usedReferences)) {
                contextBuilder.append(element.getText()).append("\n");
            }
        }
    }

    private static void collectUsedReferences(PsiElement element, Set<String> usedReferences) {
        if (element instanceof PyReferenceExpression) {
            usedReferences.add(((PyReferenceExpression) element).getReferencedName());
        }
        for (PsiElement child : element.getChildren()) {
            collectUsedReferences(child, usedReferences);
        }
    }

    private static boolean isImportUsed(PyImportStatementBase importStatement, Set<String> usedReferences) {
        if (importStatement instanceof PyImportStatement) {
            for (PyImportElement importElement : importStatement.getImportElements()) {
                if (usedReferences.contains(importElement.getVisibleName())) {
                    return true;
                }
            }
        }
        else if (importStatement instanceof PyFromImportStatement fromImport) {
            for (PyImportElement importElement : fromImport.getImportElements()) {
                if (usedReferences.contains(importElement.getVisibleName())) {
                    return true;
                }
            }
        }
        return false;
    }


    private static boolean isElementUsed(PsiElement element, Set<String> usedReferences) {
        if (element instanceof PyAssignmentStatement assignment) {
            PyExpression[] targets = assignment.getTargets();
            for (PyExpression target : targets) {
                if (target instanceof PyTargetExpression) {
                    String targetName = target.getName();
                    if (usedReferences.contains(targetName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
