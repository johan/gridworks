package com.metaweb.gridworks;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.commands.auth.AuthorizeCommand;
import com.metaweb.gridworks.commands.auth.CheckAuthorizationCommand;
import com.metaweb.gridworks.commands.auth.DeAuthorizeCommand;
import com.metaweb.gridworks.commands.auth.GetUserBadgesCommand;
import com.metaweb.gridworks.commands.edit.AddColumnCommand;
import com.metaweb.gridworks.commands.edit.AnnotateOneRowCommand;
import com.metaweb.gridworks.commands.edit.AnnotateRowsCommand;
import com.metaweb.gridworks.commands.edit.ApplyOperationsCommand;
import com.metaweb.gridworks.commands.edit.CreateProjectCommand;
import com.metaweb.gridworks.commands.edit.DeleteProjectCommand;
import com.metaweb.gridworks.commands.edit.DenormalizeCommand;
import com.metaweb.gridworks.commands.edit.EditOneCellCommand;
import com.metaweb.gridworks.commands.edit.ExportProjectCommand;
import com.metaweb.gridworks.commands.edit.ExtendDataCommand;
import com.metaweb.gridworks.commands.edit.ImportProjectCommand;
import com.metaweb.gridworks.commands.edit.JoinMultiValueCellsCommand;
import com.metaweb.gridworks.commands.edit.MQLReadCommand;
import com.metaweb.gridworks.commands.edit.MQLWriteCommand;
import com.metaweb.gridworks.commands.edit.MassEditCommand;
import com.metaweb.gridworks.commands.edit.RemoveColumnCommand;
import com.metaweb.gridworks.commands.edit.RemoveRowsCommand;
import com.metaweb.gridworks.commands.edit.RenameColumnCommand;
import com.metaweb.gridworks.commands.edit.SaveProtographCommand;
import com.metaweb.gridworks.commands.edit.SplitColumnCommand;
import com.metaweb.gridworks.commands.edit.SplitMultiValueCellsCommand;
import com.metaweb.gridworks.commands.edit.TextTransformCommand;
import com.metaweb.gridworks.commands.edit.UndoRedoCommand;
import com.metaweb.gridworks.commands.edit.UploadDataCommand;
import com.metaweb.gridworks.commands.info.ComputeClustersCommand;
import com.metaweb.gridworks.commands.info.ComputeFacetsCommand;
import com.metaweb.gridworks.commands.info.ExportRowsCommand;
import com.metaweb.gridworks.commands.info.GetAllProjectMetadataCommand;
import com.metaweb.gridworks.commands.info.GetColumnsInfoCommand;
import com.metaweb.gridworks.commands.info.GetExpressionHistoryCommand;
import com.metaweb.gridworks.commands.info.GetHistoryCommand;
import com.metaweb.gridworks.commands.info.GetModelsCommand;
import com.metaweb.gridworks.commands.info.GetOperationsCommand;
import com.metaweb.gridworks.commands.info.GetProcessesCommand;
import com.metaweb.gridworks.commands.info.GetProjectMetadataCommand;
import com.metaweb.gridworks.commands.info.GetRowsCommand;
import com.metaweb.gridworks.commands.info.GetScatterplotCommand;
import com.metaweb.gridworks.commands.recon.ReconDiscardJudgmentsCommand;
import com.metaweb.gridworks.commands.recon.ReconJudgeOneCellCommand;
import com.metaweb.gridworks.commands.recon.ReconJudgeSimilarCellsCommand;
import com.metaweb.gridworks.commands.recon.ReconMarkNewTopicsCommand;
import com.metaweb.gridworks.commands.recon.ReconMatchBestCandidatesCommand;
import com.metaweb.gridworks.commands.recon.ReconMatchSpecificTopicCommand;
import com.metaweb.gridworks.commands.recon.ReconcileCommand;
import com.metaweb.gridworks.commands.util.CancelProcessesCommand;
import com.metaweb.gridworks.commands.util.GetExpressionLanguageInfoCommand;
import com.metaweb.gridworks.commands.util.GuessTypesOfColumnCommand;
import com.metaweb.gridworks.commands.util.LogExpressionCommand;
import com.metaweb.gridworks.commands.util.PreviewExpressionCommand;
import com.metaweb.gridworks.commands.util.PreviewExtendDataCommand;
import com.metaweb.gridworks.commands.util.PreviewProtographCommand;

public class GridworksServlet extends HttpServlet {
    
    private static final long serialVersionUID = 2386057901503517403L;
    
    static final protected Map<String, Command> _commands = new HashMap<String, Command>();
    
    // timer for periodically saving projects
    static protected Timer _timer;

    final Logger logger = LoggerFactory.getLogger("servlet");
    
    static {
        _commands.put("create-project-from-upload", new CreateProjectCommand());
        _commands.put("import-project", new ImportProjectCommand());
        _commands.put("export-project", new ExportProjectCommand());
        _commands.put("export-rows", new ExportRowsCommand());
        
        _commands.put("get-project-metadata", new GetProjectMetadataCommand());
        _commands.put("get-all-project-metadata", new GetAllProjectMetadataCommand());

        _commands.put("delete-project", new DeleteProjectCommand());
        
        _commands.put("get-models", new GetModelsCommand());
        _commands.put("get-rows", new GetRowsCommand());
        _commands.put("get-processes", new GetProcessesCommand());
        _commands.put("get-history", new GetHistoryCommand());
        _commands.put("get-operations", new GetOperationsCommand());
        _commands.put("get-columns-info", new GetColumnsInfoCommand());
        _commands.put("get-scatterplot", new GetScatterplotCommand());
        
        _commands.put("undo-redo", new UndoRedoCommand());
        _commands.put("apply-operations", new ApplyOperationsCommand());
        _commands.put("cancel-processes", new CancelProcessesCommand());
        
        _commands.put("compute-facets", new ComputeFacetsCommand());
        _commands.put("compute-clusters", new ComputeClustersCommand());
        
        _commands.put("edit-one-cell", new EditOneCellCommand());
        _commands.put("text-transform", new TextTransformCommand());
        _commands.put("mass-edit", new MassEditCommand());
        _commands.put("join-multi-value-cells", new JoinMultiValueCellsCommand());
        _commands.put("split-multi-value-cells", new SplitMultiValueCellsCommand());
        
        _commands.put("add-column", new AddColumnCommand());
        _commands.put("remove-column", new RemoveColumnCommand());
        _commands.put("rename-column", new RenameColumnCommand());
        _commands.put("split-column", new SplitColumnCommand());
        _commands.put("extend-data", new ExtendDataCommand());
        
        _commands.put("denormalize", new DenormalizeCommand());
        
        _commands.put("reconcile", new ReconcileCommand());
        _commands.put("recon-match-best-candidates", new ReconMatchBestCandidatesCommand());
        _commands.put("recon-mark-new-topics", new ReconMarkNewTopicsCommand());
        _commands.put("recon-discard-judgments", new ReconDiscardJudgmentsCommand());
        _commands.put("recon-match-specific-topic-to-cells", new ReconMatchSpecificTopicCommand());
        _commands.put("recon-judge-one-cell", new ReconJudgeOneCellCommand());
        _commands.put("recon-judge-similar-cells", new ReconJudgeSimilarCellsCommand());
        
        _commands.put("annotate-one-row", new AnnotateOneRowCommand());
        _commands.put("annotate-rows", new AnnotateRowsCommand());
        _commands.put("remove-rows", new RemoveRowsCommand());
        
        _commands.put("save-protograph", new SaveProtographCommand());
        
        _commands.put("get-expression-language-info", new GetExpressionLanguageInfoCommand());
        _commands.put("get-expression-history", new GetExpressionHistoryCommand());
        _commands.put("log-expression", new LogExpressionCommand());
        
        _commands.put("preview-expression", new PreviewExpressionCommand());
        _commands.put("preview-extend-data", new PreviewExtendDataCommand());
        _commands.put("preview-protograph", new PreviewProtographCommand());
        
        _commands.put("guess-types-of-column", new GuessTypesOfColumnCommand());
        
        _commands.put("check-authorization", new CheckAuthorizationCommand());
        _commands.put("authorize", new AuthorizeCommand());
        _commands.put("deauthorize", new DeAuthorizeCommand());
        _commands.put("user-badges", new GetUserBadgesCommand());

        _commands.put("upload-data", new UploadDataCommand());
        _commands.put("mqlread", new MQLReadCommand());
        _commands.put("mqlwrite", new MQLWriteCommand());
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.trace("> initialize");
        
        ProjectManager.initialize();
                
        if (_timer == null) {
            _timer = new Timer();            
        }
        
        long period = 1000 * 60 * 5; // 5 minutes
        _timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                ProjectManager.singleton.save(false); // quick, potentially incomplete save
            }
        }, period, period);
        
        logger.trace("< initialize");
    }
    
    @Override
    public void destroy() {
        logger.trace("> destroy");

        // cancel automatic periodic saving and force a complete save. 
        if (_timer != null) {
            _timer.cancel();
            _timer = null;
        }
        if (ProjectManager.singleton != null) {
            ProjectManager.singleton.save(true); // complete save
            ProjectManager.singleton = null;
        }
        
        super.destroy();

        logger.trace("< destroy");
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String commandName = getCommandName(request);
        Command command = _commands.get(commandName);
        if (command != null) {
            logger.trace("> GET {}", commandName);
            command.doGet(request, response);
            logger.trace("< GET {}", commandName);
        } else {
            response.sendError(404);
        }
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String commandName = getCommandName(request);
        Command command = _commands.get(commandName);
        if (command != null) {
            logger.trace("> POST {}", commandName);
            command.doPost(request, response);
            logger.trace("< POST {}", commandName);
        } else {
            response.sendError(404);
        }
    }
        
    protected String getCommandName(HttpServletRequest request) {
        // Remove extraneous path segments that might be there for other purposes,
        // e.g., for /export-rows/filename.ext, export-rows is the command while
        // filename.ext is only for the browser to prompt a convenient filename. 
        String commandName = request.getPathInfo().substring(1);
        int slash = commandName.indexOf('/');
        return slash > 0 ? commandName.substring(0, slash) : commandName;
    }
}

