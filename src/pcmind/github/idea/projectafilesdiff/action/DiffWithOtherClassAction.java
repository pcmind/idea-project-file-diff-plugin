package pcmind.github.idea.projectafilesdiff.action;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.intellij.diff.DiffManager;
import com.intellij.diff.DiffRequestFactory;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.GotoActionBase;
import com.intellij.ide.util.gotoByName.ChooseByNameFilter;
import com.intellij.ide.util.gotoByName.ChooseByNameLanguageFilter;
import com.intellij.ide.util.gotoByName.ChooseByNamePopup;
import com.intellij.ide.util.gotoByName.GotoClassModel2;
import com.intellij.ide.util.gotoByName.GotoClassSymbolConfiguration;
import com.intellij.lang.Language;
import com.intellij.navigation.AnonymousElementProvider;
import com.intellij.navigation.ChooseByNameRegistry;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;

/**
 * Created by honore.vasconcelos on 2/24/2017.
 */
public class DiffWithOtherClassAction extends GotoActionBase {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        super.actionPerformed(e);
    }

    @Override
    public void gotoActionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            return;
        }
        final VirtualFile currentFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if(currentFile == null) {
            return;
        }
        FeatureUsageTracker.getInstance().triggerFeatureUsed("diff.with.a.class");

        PsiDocumentManager.getInstance(project).commitAllDocuments();

        final GotoClassModel2 model = new GotoClassModel2(project);
        showNavigationPopup(e, model, new GotoActionCallback<Language>() {
            @Override
            protected ChooseByNameFilter<Language> createFilter(@NotNull ChooseByNamePopup popup) {
                return new ChooseByNameLanguageFilter(popup, model, GotoClassSymbolConfiguration.getInstance(project), project);
            }

            @Override
            public void elementChosen(ChooseByNamePopup popup, Object element) {
                ApplicationManager.getApplication().runReadAction(() -> {
                    if (element instanceof PsiElement && ((PsiElement)element).isValid()) {
                        PsiElement psiElement = getElement(((PsiElement)element), popup);
                        psiElement = psiElement.getNavigationElement();
                        VirtualFile file = PsiUtilCore.getVirtualFile(psiElement);
                        if (file != null) {
                            DiffManager.getInstance().showDiff(project, DiffRequestFactory.getInstance().createFromFiles(project, currentFile, file));
                        }
                    }
                });
            }
        }, IdeBundle.message("go.to.class.toolwindow.title"), true);
    }

    @NotNull
    private static PsiElement getElement(@NotNull PsiElement element, ChooseByNamePopup popup) {
        final String path = popup.getPathToAnonymous();
        if (path != null) {
            final String[] classes = path.split("\\$");
            List<Integer> indexes = new ArrayList<>();
            for (String cls : classes) {
                if (cls.isEmpty()) continue;
                try {
                    indexes.add(Integer.parseInt(cls) - 1);
                }
                catch (Exception e) {
                    return element;
                }
            }
            PsiElement current = element;
            for (int index : indexes) {
                final PsiElement[] anonymousClasses = getAnonymousClasses(current);
                if (index >= 0 && index < anonymousClasses.length) {
                    current = anonymousClasses[index];
                }
                else {
                    return current;
                }
            }
            return current;
        }
        return element;
    }

    @NotNull
    private static PsiElement[] getAnonymousClasses(@NotNull PsiElement element) {
        for (AnonymousElementProvider provider : Extensions.getExtensions(AnonymousElementProvider.EP_NAME)) {
            final PsiElement[] elements = provider.getAnonymousElements(element);
            if (elements.length > 0) {
                return elements;
            }
        }
        return PsiElement.EMPTY_ARRAY;
    }

    @Override
    protected boolean hasContributors(DataContext dataContext) {
        return ChooseByNameRegistry.getInstance().getClassModelContributors().length > 0;
    }
}
