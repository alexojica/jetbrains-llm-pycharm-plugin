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
    /**
     * Retrieves the Python function under the caret in the given editor.
     * It uses the project's document manager to find the PsiFile and then locates
     * the Python function at the caret's current position.
     *
     * @param editor  The editor instance where the caret's position is considered.
     * @param project The current open project in the IDE.
     * @return PyFunction instance if a function is found under the caret, null otherwise.
     */
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

    /**
     * Prepares a contextual string representation of a given Python function.
     * This includes the class context (if any) and the text of the function itself.
     * It also includes any global references used by the function.
     *
     * @param function The Python function to generate context for.
     * @return String representation of the function's context.
     */
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

    /**
     * Adds global references used in the Python file to the context builder.
     * This includes imports and other elements like assignments and functions
     * that are used within the specified Python function.
     *
     * @param pyFile         The Python file to search for global references.
     * @param function       The Python function for which the references are collected.
     * @param contextBuilder The StringBuilder to append the found references.
     */
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

    /**
     * Collects the names of referenced elements used in a Python element.
     * It recursively searches through all child elements.
     *
     * @param element         The Python element to start the search from.
     * @param usedReferences  The set to which the referenced names are added.
     */
    private static void collectUsedReferences(PsiElement element, Set<String> usedReferences) {
        if (element instanceof PyReferenceExpression) {
            usedReferences.add(((PyReferenceExpression) element).getReferencedName());
        }
        for (PsiElement child : element.getChildren()) {
            collectUsedReferences(child, usedReferences);
        }
    }

    /**
     * Determines whether an import statement is used in a Python function.
     * Checks if any of the visible names from the import statement are in the set of used references.
     *
     * @param importStatement The import statement to check.
     * @param usedReferences  The set of names that are used in the function.
     * @return true if the import is used, false otherwise.
     */
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

    /**
     * Checks if an element (like an assignment or function) is used in a Python file.
     * For assignments, it checks if the target names are in the set of used references.
     *
     * @param element         The element to check (like an assignment or function).
     * @param usedReferences  The set of names that are used in the function.
     * @return true if the element is used, false otherwise.
     */
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
