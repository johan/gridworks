package com.metaweb.gridworks.operations;

import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.util.ParsingUtilities;

abstract public class EngineDependentOperation extends AbstractOperation {
    final private String _engineConfigString;
    
    transient protected JSONObject    _engineConfig;
    
    protected EngineDependentOperation(JSONObject engineConfig) {
        _engineConfig = engineConfig;
        _engineConfigString = engineConfig == null || engineConfig.length() == 0
        	? null : engineConfig.toString();
    }
    
    protected Engine createEngine(Project project) throws Exception {
        Engine engine = new Engine(project);
        engine.initializeFromJSON(getEngineConfig());
        return engine;
    }
    
    protected JSONObject getEngineConfig() {
        if (_engineConfig == null && _engineConfigString != null) {
            try {
                _engineConfig = ParsingUtilities.evaluateJsonStringToObject(_engineConfigString);
            } catch (JSONException e) {
                // ignore
            }
        }
        return _engineConfig;
    }
}
