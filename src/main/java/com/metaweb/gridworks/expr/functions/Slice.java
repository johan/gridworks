package com.metaweb.gridworks.expr.functions;

import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.gel.Function;

public class Slice implements Function {

    public Object call(Properties bindings, Object[] args) {
        if (args.length > 1 && args.length <= 3) {
            Object v = args[0];
            Object from = args[1];
            Object to = (args.length == 3) ? args[2] : null;
            
            if (v != null && from != null && from instanceof Number && (to == null || to instanceof Number)) {
                if (v.getClass().isArray() || v instanceof List<?>) {
                    int length = v.getClass().isArray() ? 
                            ((Object[]) v).length :
                            ExpressionUtils.toObjectList(v).size();
                    
                    int start = ((Number) from).intValue();
                    int end = (to != null && to instanceof Number) ? ((Number) to).intValue() : length;
                                
                    if (start < 0) {
                        start = length + start;
                    }
                    start = Math.min(length, Math.max(0, start));
                    
                    if (end < 0) {
                        end = length + end;
                    }
                    end = Math.min(length, Math.max(start, end));
                    
                    if (v.getClass().isArray()) {
                        Object[] a2 = new Object[end - start];
                        
                        System.arraycopy((Object[]) v, start, a2, 0, end - start);
                        
                        return a2;
                    } else {
                        return ExpressionUtils.toObjectList(v).subList(start, end);
                    }
                } else {
                    String s = (v instanceof String) ? (String) v : v.toString();
                    
                    int start = ((Number) from).intValue();
                    if (start < 0) {
                        start = s.length() + start;
                    }
                    start = Math.min(s.length(), Math.max(0, start));
                    
                    if (to != null && to instanceof Number) {
                        int end = ((Number) to).intValue();
                        if (end < 0) {
                            end = s.length() + end;
                        }
                        end = Math.min(s.length(), Math.max(start, end));
                        
                        return s.substring(start, end);
                    } else {
                        return s.substring(start);
                    }
                }
            }
        }
        return null;
    }

    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value(
            "If o is an array, returns o[from, to]. " +
            "if o is a string, returns o.substring(from, to)"
        );
        writer.key("params"); writer.value("o, number from, optional number to");
        writer.key("returns"); writer.value("Depends on actual arguments");
        writer.endObject();
    }
}
