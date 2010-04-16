package com.metaweb.gridworks.exporters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class TsvExporter implements Exporter {
    public String getContentType() {
        return "application/x-unknown";
    }
    
    public boolean takeWriter() {
        return true;
    }
    
    public void export(Project project, Properties options, Engine engine,
            OutputStream outputStream) throws IOException {
        throw new RuntimeException("Not implemented");
    }
    
    public void export(Project project, Properties options, Engine engine, Writer writer) throws IOException {
        boolean first = true;
        for (Column column : project.columnModel.columns) {
            if (first) {
                first = false;
            } else {
                writer.write("\t");
            }
            writer.write(column.getName());
        }
        writer.write("\n");
        
        {
            RowVisitor visitor = new RowVisitor() {
                Writer writer;
                
                public RowVisitor init(Writer writer) {
                    this.writer = writer;
                    return this;
                }
                
                public boolean visit(Project project, int rowIndex, Row row, boolean contextual, boolean includeDependent) {
                    boolean first = true;
                    try {
                        for (Column column : project.columnModel.columns) {
                            if (first) {
                                first = false;
                            } else {
                                writer.write("\t");
                            }
                            
                            int cellIndex = column.getCellIndex();
                            if (cellIndex < row.cells.size()) {
                                Cell cell = row.cells.get(cellIndex);
                                if (cell != null && cell.value != null) {
                                    Object v = cell.value;
                                    String s = v instanceof String ? ((String) v) : v.toString();
                                    
                                    s = s.replace("\\", "\\\\")
                                        .replace("\n", "\\n")
                                        .replace("\r", "\\r")
                                        .replace("\t", "\\t");
                                    
                                    writer.write(s);
                                }
                            }
                        }
                        writer.write("\n");
                    } catch (IOException e) {
                        // ignore
                    }
                    return false;
                }
            }.init(writer);
            
            FilteredRows filteredRows = engine.getAllFilteredRows(true);
            filteredRows.accept(project, visitor);
        }
    }

}
