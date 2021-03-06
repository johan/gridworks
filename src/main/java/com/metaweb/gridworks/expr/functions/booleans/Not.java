package com.metaweb.gridworks.expr.functions.booleans;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;

public class Not implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1) {
            return !objectToBoolean(args[0]);
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a boolean");
    }

    public static boolean objectToBoolean(Object o) {
        return o == null ? false : (
            (o instanceof Boolean) ? ((Boolean) o).booleanValue() : Boolean.parseBoolean(o.toString()));
    }
    
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns the opposite of b");
        writer.key("params"); writer.value("boolean b");
        writer.key("returns"); writer.value("boolean");
        writer.endObject();
    }
}
