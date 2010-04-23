package com.metaweb.gridworks.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metaweb.gridworks.Gridworks;
import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.ProjectMetadata;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.history.History;
import com.metaweb.gridworks.process.ProcessManager;
import com.metaweb.gridworks.protograph.Protograph;
import com.metaweb.gridworks.util.Pool;

public class Project {
    final public long            id;
    
    final public ColumnModel     columnModel = new ColumnModel();
    final public List<Row>       rows = new ArrayList<Row>();
    final public History         history;
    
    public Protograph            protograph;
    
    transient public ProcessManager processManager = new ProcessManager();
    transient public Date lastSave = new Date();
    
    final static Logger logger = LoggerFactory.getLogger("project");
    
    static public long generateID() {
        return System.currentTimeMillis() + Math.round(Math.random() * 1000000000000L);
    }
    
    public Project() {
        id = generateID();
        history = new History(this);
    }
    
    protected Project(long id) {
        this.id = id;
        this.history = new History(this);
    }
    
    public ProjectMetadata getMetadata() {
    	return ProjectManager.singleton.getProjectMetadata(id);
    }
    
    public void save() {
        synchronized (this) {
            File dir = ProjectManager.singleton.getProjectDir(id);
            
            File tempFile = new File(dir, "data.temp.zip");
            try {
                saveToFile(tempFile);
            } catch (Exception e) {
                e.printStackTrace();
                
                logger.warn("Failed to save project {}", id);
                return;
            }
            
            File file = new File(dir, "data.zip");
            File oldFile = new File(dir, "data.old.zip");
            
            if (file.exists()) {
                file.renameTo(oldFile);
            }
            
            tempFile.renameTo(file);
            if (oldFile.exists()) {
                oldFile.delete();
            }
            
            lastSave = new Date();
            
            logger.info("Saved project '{}'",id);
        }
    }
    
    protected void saveToFile(File file) throws Exception {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
        try {
            Pool pool = new Pool();
            
            out.putNextEntry(new ZipEntry("data.txt"));
            try {
                saveToOutputStream(out, pool);
            } finally {
                out.closeEntry();
            }
            
            out.putNextEntry(new ZipEntry("pool.txt"));
            try {
                pool.save(out);
            } finally {
                out.closeEntry();
            }
        } finally {
            out.close();
        }
    }
    
    protected void saveToOutputStream(OutputStream out, Pool pool) throws IOException {
        Writer writer = new OutputStreamWriter(out);
        try {
            Properties options = new Properties();
            options.setProperty("mode", "save");
            options.put("pool", pool);
            
            saveToWriter(writer, options);
        } finally {
            writer.flush();
        }
    }
    
    protected void saveToWriter(Writer writer, Properties options) throws IOException {
        writer.write(Gridworks.getVersion()); writer.write('\n');
        
        writer.write("columnModel=\n"); columnModel.save(writer, options);
        writer.write("history=\n"); history.save(writer, options);
        if (protograph != null) {
            writer.write("protograph="); protograph.save(writer, options); writer.write('\n');
        }
        
        writer.write("rowCount="); writer.write(Integer.toString(rows.size())); writer.write('\n');
        for (Row row : rows) {
            row.save(writer, options); writer.write('\n');
        }
    }
    
    static public Project load(File dir, long id) {
        try {
            File file = new File(dir, "data.zip");
            if (file.exists()) {
                return loadFromFile(file, id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        try {
            File file = new File(dir, "data.temp.zip");
            if (file.exists()) {
                return loadFromFile(file, id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        try {
            File file = new File(dir, "data.old.zip");
            if (file.exists()) {
                return loadFromFile(file, id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    static protected Project loadFromFile(
        File file, 
        long id
    ) throws Exception {
        ZipFile zipFile = new ZipFile(file);
        try {
            Pool pool = new Pool();
            ZipEntry poolEntry = zipFile.getEntry("pool.txt");
            if (poolEntry != null) {
                pool.load(new InputStreamReader(
                    zipFile.getInputStream(poolEntry)));
            } // else, it's a legacy project file
            
            return loadFromReader(
                new LineNumberReader(
                    new InputStreamReader(
                        zipFile.getInputStream(
                            zipFile.getEntry("data.txt")))),
                id,
                pool
            );
        } finally {
            zipFile.close();
        }
    }
    
    static protected Project loadFromReader(
        LineNumberReader reader, 
        long id,
        Pool pool
    ) throws Exception {
        long start = System.currentTimeMillis();
        
        /* String version = */ reader.readLine();
        
        Project project = new Project(id);
        int maxCellCount = 0;
        
        String line;
        while ((line = reader.readLine()) != null) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            String value = line.substring(equal + 1);
            
            if ("columnModel".equals(field)) {
                project.columnModel.load(reader);
            } else if ("history".equals(field)) {
                project.history.load(project, reader);
            } else if ("protograph".equals(field)) {
                project.protograph = Protograph.load(project, value);
            } else if ("rowCount".equals(field)) {
                int count = Integer.parseInt(value);
                
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                    	Row row = Row.load(line, pool);
                        project.rows.add(row);
                        maxCellCount = Math.max(maxCellCount, row.cells.size());
                    }
                }
            }
        }
        
        project.columnModel.setMaxCellIndex(maxCellCount - 1);
        
        logger.info(
            "Loaded project {} from disk in {} sec(s)",id,Long.toString((System.currentTimeMillis() - start) / 1000)
        );
        
        project.recomputeRowContextDependencies();
        
        return project;
    }

    
    static protected class Group {
        int[]   cellIndices;
        int     keyCellIndex;
    }
    
    public void recomputeRowContextDependencies() {
        List<Group> keyedGroups = new ArrayList<Group>();
        
        addRootKeyedGroup(keyedGroups);
        
        for (ColumnGroup group : columnModel.columnGroups) {
            if (group.keyColumnIndex >= 0) {
                Group keyedGroup = new Group();
                keyedGroup.keyCellIndex = columnModel.columns.get(group.keyColumnIndex).getCellIndex();
                keyedGroup.cellIndices = new int[group.columnSpan - 1];
                
                int c = 0;
                for (int i = 0; i < group.columnSpan; i++) {
                    int columnIndex = group.startColumnIndex + i;
                    if (columnIndex != group.keyColumnIndex) {
                        int cellIndex = columnModel.columns.get(columnIndex).getCellIndex();
                        keyedGroup.cellIndices[c++] = cellIndex;
                    }
                }
                
                keyedGroups.add(keyedGroup);
            }
        }
        
        Collections.sort(keyedGroups, new Comparator<Group>() {
            public int compare(Group o1, Group o2) {
                return o2.cellIndices.length - o1.cellIndices.length; // larger groups first
            }
        });
        
        int[] lastNonBlankRowsByGroup = new int[keyedGroups.size()];
        for (int i = 0; i < lastNonBlankRowsByGroup.length; i++) {
            lastNonBlankRowsByGroup[i] = -1;
        }
        
        int rowCount = rows.size();
        int groupCount = keyedGroups.size();
        
        int recordIndex = 0;
        for (int r = 0; r < rowCount; r++) {
            Row row = rows.get(r);
            row.contextRows = null;
            row.contextRowSlots = null;
            row.contextCellSlots = null;
            
            for (int g = 0; g < groupCount; g++) {
                Group group = keyedGroups.get(g);
                
                if (!ExpressionUtils.isNonBlankData(row.getCellValue(group.keyCellIndex))) {
                    int contextRowIndex = lastNonBlankRowsByGroup[g];
                    if (contextRowIndex >= 0) {
                        for (int dependentCellIndex : group.cellIndices) {
                            if (ExpressionUtils.isNonBlankData(row.getCellValue(dependentCellIndex))) {
                                setRowDependency(
                                    row, 
                                    dependentCellIndex, 
                                    contextRowIndex, 
                                    group.keyCellIndex
                                );
                            }
                        }
                    }
                } else {
                    lastNonBlankRowsByGroup[g] = r;
                }
            }
            
            if (row.contextRowSlots != null && row.contextRowSlots.length > 0) {
                row.recordIndex = -1;
                row.contextRows = new ArrayList<Integer>();
                for (int index : row.contextRowSlots) {
                    if (index >= 0) {
                        row.contextRows.add(index);
                    }
                }
                Collections.sort(row.contextRows);
                
                columnModel._hasDependentRows = true;
            } else {
                row.recordIndex = recordIndex++;
            }
        }
    }
    
    protected void addRootKeyedGroup(List<Group> keyedGroups) {
        int count = columnModel.getMaxCellIndex() + 1;
        if (count > 0 && columnModel.getKeyColumnIndex() < columnModel.columns.size()) {
            Group rootKeyedGroup = new Group();
            
            rootKeyedGroup.cellIndices = new int[count - 1];
            rootKeyedGroup.keyCellIndex = columnModel.columns.get(columnModel.getKeyColumnIndex()).getCellIndex();

            for (int i = 0; i < count; i++) {
                if (i < rootKeyedGroup.keyCellIndex) {
                    rootKeyedGroup.cellIndices[i] = i;
                } else if (i > rootKeyedGroup.keyCellIndex) {
                    rootKeyedGroup.cellIndices[i - 1] = i;
                }
            }
            keyedGroups.add(rootKeyedGroup);
        }
    }
    
    protected void setRowDependency(Row row, int cellIndex, int contextRowIndex, int contextCellIndex) {
        int count = columnModel.getMaxCellIndex() + 1;
        if (row.contextRowSlots == null || row.contextCellSlots == null) {
            row.contextRowSlots = new int[count];
            row.contextCellSlots = new int[count];
            
            for (int i = 0; i < count; i++) {
                row.contextRowSlots[i] = -1;
                row.contextCellSlots[i] = -1;
            }
        }
        
        row.contextRowSlots[cellIndex] = contextRowIndex;
        row.contextCellSlots[cellIndex] = contextCellIndex;
    }
}
