package com.metaweb.gridworks.commands.info;

 import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.exporters.Exporter;
import com.metaweb.gridworks.exporters.HtmlTableExporter;
import com.metaweb.gridworks.exporters.TripleloaderExporter;
import com.metaweb.gridworks.exporters.TsvExporter;
import com.metaweb.gridworks.exporters.XlsExporter;
import com.metaweb.gridworks.model.Project;

public class ExportRowsCommand extends Command {
    
    static final protected Map<String, Exporter> s_formatToExporter = new HashMap<String, Exporter>();
    
    static {
        s_formatToExporter.put("tripleloader", new TripleloaderExporter());
        s_formatToExporter.put("html", new HtmlTableExporter());
        s_formatToExporter.put("xls", new XlsExporter());
    }
    
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            Engine engine = getEngine(request, project);
            String format = request.getParameter("format");
            
            Exporter exporter = s_formatToExporter.get(format.toLowerCase());
            if (exporter == null){
                exporter = new TsvExporter();
            }
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", exporter.getContentType());
            
            if (exporter.takeWriter()) {
                PrintWriter writer = response.getWriter();
                exporter.export(project, new Properties(), engine, writer);
            } else {
                exporter.export(project, new Properties(), engine, response.getOutputStream());
            }
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
