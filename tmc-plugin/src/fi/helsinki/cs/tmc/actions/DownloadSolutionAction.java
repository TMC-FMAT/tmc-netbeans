package fi.helsinki.cs.tmc.actions;

import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.utilities.ServerErrorHelper;
import fi.helsinki.cs.tmc.core.events.TmcEvent;
import fi.helsinki.cs.tmc.coreimpl.BridgingProgressObserver;
import fi.helsinki.cs.tmc.core.events.TmcEventBus;
import fi.helsinki.cs.tmc.model.CourseDb;
import fi.helsinki.cs.tmc.model.ProjectMediator;
import fi.helsinki.cs.tmc.model.TmcProjectInfo;
import fi.helsinki.cs.tmc.ui.ConvenientDialogDisplayer;
import fi.helsinki.cs.tmc.utilities.BgTask;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;

import com.google.common.base.Function;

import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DynamicMenuContent;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JMenuItem;

@ActionID(category = "TMC", id = "fi.helsinki.cs.tmc.actions.DownloadSolutionAction")
@ActionRegistration(displayName = "#CTL_DownloadSolutionAction", lazy = false)
@ActionReferences({@ActionReference(path = "Menu/TM&C", position = -35, separatorAfter = -30)})
@Messages("CTL_DownloadSolutionAction=Download suggested &solution")
public class DownloadSolutionAction extends AbstractExerciseSensitiveAction {

    private static final Logger logger = Logger.getLogger(DownloadSolutionAction.class.getName());
    private ProjectMediator projectMediator;
    private CourseDb courseDb;
    private ConvenientDialogDisplayer dialogs;
    private TmcEventBus eventBus;

    public DownloadSolutionAction() {
        this.projectMediator = ProjectMediator.getInstance();
        this.courseDb = CourseDb.getInstance();
        this.dialogs = ConvenientDialogDisplayer.getDefault();
        this.eventBus = TmcEventBus.getDefault();
    }

    @Override
    public String getName() {
        return "Download suggested &solution";
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean enabledFor(Exercise exercise) {
        return exercise.getSolutionDownloadUrl() != null;
    }

    @Override
    protected boolean enabledForMultipleProjects() {
        return true;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }

    @Override
    public JMenuItem getMenuPresenter() {
        return new ActionMenuItem();
    }

    private JMenuItem getOriginalMenuPresenter() {
        return super.getMenuPresenter();
    }

    @Override
    protected ProjectMediator getProjectMediator() {
        return projectMediator;
    }

    @Override
    protected CourseDb getCourseDb() {
        return courseDb;
    }

    @Override
    protected void performAction(Node[] nodes) {
        projectMediator.saveAllFiles();

        for (final Project project : projectsFromNodes(nodes)) {
            final Exercise ex = exerciseForProject(project);
            if (ex.getSolutionDownloadUrl() == null) {
                // We shouldn't be visible any more.
                // See https://github.com/testmycode/tmc-netbeans/issues/45
                this.setEnabled(false);
                return;
            }

            String question =
                    "Are you sure you want to OVERWRITE your copy of\n"
                            + ex.getName()
                            + " with the suggested solution?";
            String title = "Replace with solution?";
            dialogs.askYesNo(
                    question,
                    title,
                    new Function<Boolean, Void>() {
                        @Override
                        public Void apply(Boolean yes) {
                            if (yes) {
                                eventBus.post(new InvokedEvent(ex));
                                downloadSolution(ex, projectMediator.wrapProject(project));
                            }
                            return null;
                        }
                    });
        }
    }

    private void downloadSolution(final Exercise ex, final TmcProjectInfo proj) {
        Exercise exercise = exerciseForProject(proj.getProject());
        ProgressObserver observer = new BridgingProgressObserver();
        Callable<Exercise> dlModelSolutionTask = TmcCore.get().downloadModelSolution(observer, exercise);
        BgTask.start("Downloading suggested solution", dlModelSolutionTask, observer,
                new BgTaskListener<Object>() {
                    @Override
                    public void bgTaskReady(Object result) {
                        projectMediator.scanForExternalChanges(proj);
                    }

                    @Override
                    public void bgTaskCancelled() {}

                    @Override
                    public void bgTaskFailed(Throwable ex) {
                        logger.log(Level.INFO, "Failed to extract solution.", ex);
                        dialogs.displayError(
                                "Failed to extract solution.\n"
                                        + ServerErrorHelper.getServerExceptionMsg(ex));
                    }
                });
    }


    private class ActionMenuItem extends JMenuItem implements DynamicMenuContent {

        public ActionMenuItem() {
            super(DownloadSolutionAction.this);
        }

        @Override
        public JComponent[] getMenuPresenters() {
            if (DownloadSolutionAction.this.isEnabled()) {
                return new JComponent[] {getOriginalMenuPresenter()};
            } else {
                return new JComponent[0];
            }
        }

        @Override
        public JComponent[] synchMenuPresenters(JComponent[] jcs) {
            return getMenuPresenters();
        }
    }

    public static class InvokedEvent implements TmcEvent {

        public final Exercise exercise;

        public InvokedEvent(Exercise exercise) {
            this.exercise = exercise;
        }
    }
}
