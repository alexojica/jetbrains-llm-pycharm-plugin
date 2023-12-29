import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import org.jetbrains.annotations.NotNull;

public class ExplainCodeToolWindowFactory implements ToolWindowFactory {
    /**
     * Creates the content for the ExplainCode tool window and adds it to the specified tool window.
     *
     * @param project     The Project associated with the tool window.
     * @param toolWindow  The ToolWindow to which the content is added.
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ExplainCodeToolWindowContentFactory contentFactory = project.getService(ExplainCodeToolWindowContentFactory.class);
        if (contentFactory != null) {
            toolWindow.getContentManager().addContent(contentFactory.createContent());
        }
    }
}
