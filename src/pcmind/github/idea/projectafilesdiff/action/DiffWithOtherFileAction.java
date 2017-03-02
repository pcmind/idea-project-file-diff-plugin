package pcmind.github.idea.projectafilesdiff.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;

import com.intellij.diff.DiffManager;
import com.intellij.diff.DiffRequestFactory;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.GotoActionBase;
import com.intellij.ide.actions.GotoFileItemProvider;
import com.intellij.ide.util.gotoByName.ChooseByNameFilter;
import com.intellij.ide.util.gotoByName.ChooseByNamePopup;
import com.intellij.ide.util.gotoByName.GotoFileConfiguration;
import com.intellij.ide.util.gotoByName.GotoFileModel;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;

/**
 * Created by honore.vasconcelos on 2/24/2017.
 */
public class DiffWithOtherFileAction extends GotoActionBase {

    @Override
    public void gotoActionPerformed(AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        if (project == null) return;

        FeatureUsageTracker.getInstance().triggerFeatureUsed("diff.with.a.file");
        final VirtualFile currentFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if(currentFile == null) {
            return;
        }
        final GotoFileModel gotoFileModel = new GotoFileModel(project);
        GotoActionCallback<FileType> callback = new GotoActionCallback<FileType>() {
            @Override
            protected ChooseByNameFilter<FileType> createFilter(@NotNull ChooseByNamePopup popup) {
                return new GotoFileFilter(popup, gotoFileModel, project);
            }

            @Override
            public void elementChosen(final ChooseByNamePopup popup, final Object element) {
                if (element == null) return;
                ApplicationManager.getApplication().assertIsDispatchThread();
                Navigatable n = (Navigatable)element;
                //this is for better cursor position
                if (element instanceof PsiFile) {
                    VirtualFile file = ((PsiFile)element).getVirtualFile();
                    if (file == null) return;
                    DiffManager.getInstance().showDiff(project, DiffRequestFactory.getInstance().createFromFiles(project, currentFile, file));
                }
            }
        };
        GotoFileItemProvider provider = new GotoFileItemProvider(project, getPsiContext(e), gotoFileModel);
        showNavigationPopup(e, gotoFileModel, callback, IdeBundle.message("go.to.file.toolwindow.title"), true, true, provider);
    }

    protected static class GotoFileFilter extends ChooseByNameFilter<FileType> {
        GotoFileFilter(final ChooseByNamePopup popup, GotoFileModel model, final Project project) {
            super(popup, model, GotoFileConfiguration.getInstance(project), project);
        }

        @Override
        @NotNull
        protected List<FileType> getAllFilterValues() {
            List<FileType> elements = new ArrayList<>();
            ContainerUtil.addAll(elements, FileTypeManager.getInstance().getRegisteredFileTypes());
            Collections.sort(elements, FileTypeComparator.INSTANCE);
            return elements;
        }

        @Override
        protected String textForFilterValue(@NotNull FileType value) {
            return value.getName();
        }

        @Override
        protected Icon iconForFilterValue(@NotNull FileType value) {
            return value.getIcon();
        }
    }

    /**
     * A file type comparator. The comparison rules are applied in the following order.
     * <ol>
     * <li>Unknown file type is greatest.</li>
     * <li>Text files are less then binary ones.</li>
     * <li>File type with greater name is greater (case is ignored).</li>
     * </ol>
     */
    static class FileTypeComparator implements Comparator<FileType> {
        /**
         * an instance of comparator
         */
        static final Comparator<FileType> INSTANCE = new FileTypeComparator();

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(final FileType o1, final FileType o2) {
            if (o1 == o2) {
                return 0;
            }
            if (o1 == FileTypes.UNKNOWN) {
                return 1;
            }
            if (o2 == FileTypes.UNKNOWN) {
                return -1;
            }
            if (o1.isBinary() && !o2.isBinary()) {
                return 1;
            }
            if (!o1.isBinary() && o2.isBinary()) {
                return -1;
            }
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    }
}
