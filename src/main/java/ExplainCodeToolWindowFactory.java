import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import org.jetbrains.annotations.NotNull;

public class ExplainCodeToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ExplainCodeToolWindowContentFactory contentFactory = project.getService(ExplainCodeToolWindowContentFactory.class);
        if (contentFactory != null) {
            toolWindow.getContentManager().addContent(contentFactory.createContent());
        }
    }
}
