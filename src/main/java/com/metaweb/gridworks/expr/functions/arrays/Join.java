package com.metaweb.gridworks.expr.functions.arrays;

import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.gel.ControlFunctionRegistry;
import com.metaweb.gridworks.gel.Function;

public class Join implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2) {
            Object v = args[0];
            Object s = args[1];
            
            if (v != null && (v.getClass().isArray() || v instanceof List<?>) &&
                s != null && s instanceof String) {
                
                String separator = (String) s;
                
                StringBuffer sb = new StringBuffer();
                if (v.getClass().isArray()) {
	                for (Object o : (Object[]) v) {
	                    if (o != null) {
	                        if (sb.length() > 0) {
	                            sb.append(separator);
	                        }
	                        sb.append(o.toString());
	                    }
	                }
                } else {
	                for (Object o : ExpressionUtils.toObjectList(v)) {
	                    if (o != null) {
	                        if (sb.length() > 0) {
	                            sb.append(separator);
	                        }
	                        sb.append(o.toString());
	                    }
	                }
                }
                
                return sb.toString();
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects an array and a string");
    }

    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns the string obtained by joining the array a with the separator sep");
        writer.key("params"); writer.value("array a, string sep");
        writer.key("returns"); writer.value("string");
        writer.endObject();
    }
}
