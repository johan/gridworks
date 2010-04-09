package com.metaweb.gridworks;

/**
 * Centralized configuration facility.
 */
public class Configurations {

    public static String get(final String name) {
        return System.getProperty(name);
    }
    
    public static String get(final String name, final String def) {
        final String val = get(name);
        return (val == null) ? def : val;
    }

    public static boolean getBoolean(final String name, final boolean def) {
        final String val = get(name);
        return (val == null) ? def : Boolean.parseBoolean(val);
    }

    public static int getInteger(final String name, final int def) {
        final String val = get(name);
        try {
            return (val == null) ? def : Integer.parseInt(val);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Could not parse '" + val + "' as an integer number.", e);
        }
    }

    public static float getFloat(final String name, final float def) {
        final String val = get(name);
        try {
            return (val == null) ? def : Float.parseFloat(val);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Could not parse '" + val + "' as a floating point number.", e);
        }
    }
    
}
