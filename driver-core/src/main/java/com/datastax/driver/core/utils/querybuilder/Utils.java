package com.datastax.driver.core.utils.querybuilder;

import java.net.InetAddress;
import java.util.*;
import java.util.regex.Pattern;

// Static utilities private to the query builder
abstract class Utils {

    private static final Pattern cnamePattern = Pattern.compile("\\w+(?:\\[.+\\])?", Pattern.CASE_INSENSITIVE);
    private static final Pattern fctsPattern = Pattern.compile("(?:count|writetime|ttl|token)\\(.*", Pattern.CASE_INSENSITIVE);

    static StringBuilder joinAndAppend(StringBuilder sb, String separator, String[] values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0)
                sb.append(separator);
            sb.append(values[i]);
        }
        return sb;
    }

    static StringBuilder joinAndAppend(BuiltStatement stmt, StringBuilder sb, String separator, Appendeable[] values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0)
                sb.append(separator);
            values[i].appendTo(sb);
            if (stmt != null)
                stmt.maybeAddRoutingKey(values[i].name(), values[i].firstValue());
        }
        return sb;
    }

    static StringBuilder joinAndAppendNames(StringBuilder sb, String separator, String[] values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0)
                sb.append(separator);
            appendName(values[i], sb);
        }
        return sb;
    }

    static StringBuilder joinAndAppendValues(StringBuilder sb, String separator, Object[] values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0)
                sb.append(separator);
            appendValue(values[i], sb);
        }
        return sb;
    }

    static StringBuilder appendValue(Object value, StringBuilder sb) {
        return appendValue(value, sb, false);
    }

    static StringBuilder appendFlatValue(Object value, StringBuilder sb) {
        appendFlatValue(value, sb, false);
        return sb;
    }

    private static StringBuilder appendValue(Object value, StringBuilder sb, boolean rawValue) {
        // That is kind of lame but lacking a better solution
        if (appendValueIfLiteral(value, sb))
            return sb;

        if (appendValueIfCollection(value, sb, rawValue))
            return sb;

        if (rawValue)
            return sb.append(value.toString());
        else
            return appendValueString(value.toString(), sb);
    }

    private static void appendFlatValue(Object value, StringBuilder sb, boolean rawValue) {
        if (appendValueIfLiteral(value, sb))
            return;

        if (rawValue)
            sb.append(value.toString());
        else
            appendValueString(value.toString(), sb);
    }

    private static boolean appendValueIfLiteral(Object value, StringBuilder sb) {
        if (value instanceof Integer || value instanceof Long || value instanceof Float || value instanceof Double || value instanceof UUID) {
            sb.append(value);
            return true;
        } else if (value instanceof InetAddress) {
            sb.append(((InetAddress)value).getHostAddress());
            return true;
        } else if (value instanceof Date) {
            sb.append(((Date)value).getTime());
            return true;
        } else {
            return false;
        }
    }

    private static boolean appendValueIfCollection(Object value, StringBuilder sb, boolean rawValue) {
        if (value instanceof List) {
            appendList((List)value, sb, rawValue);
            return true;
        } else if (value instanceof Set) {
            appendSet((Set)value, sb, rawValue);
            return true;
        } else if (value instanceof Map) {
            appendMap((Map)value, sb, rawValue);
            return true;
        } else {
            return false;
        }
    }

    static StringBuilder appendCollection(Object value, StringBuilder sb) {
        boolean wasCollection = appendValueIfCollection(value, sb, false);
        assert wasCollection;
        return sb;
    }

    static StringBuilder appendList(List l, StringBuilder sb) {
        return appendList(l, sb, false);
    }

    private static StringBuilder appendList(List l, StringBuilder sb, boolean rawValue) {
        sb.append("[");
        for (int i = 0; i < l.size(); i++) {
            if (i > 0)
                sb.append(",");
            appendFlatValue(l.get(i), sb, rawValue);
        }
        sb.append("]");
        return sb;
    }

    static StringBuilder appendSet(Set s, StringBuilder sb) {
        return appendSet(s, sb, false);
    }

    private static StringBuilder appendSet(Set s, StringBuilder sb, boolean rawValue) {
        sb.append("{");
        boolean first = true;
        for (Object elt : s) {
            if (first) first = false; else sb.append(",");
            appendFlatValue(elt, sb, rawValue);
        }
        sb.append("}");
        return sb;
    }

    static StringBuilder appendMap(Map<Object, Object> m, StringBuilder sb) {
        return appendMap(m, sb, false);
    }

    private static StringBuilder appendMap(Map<Object, Object> m, StringBuilder sb, boolean rawValue) {
        sb.append("{");
        boolean first = true;
        for (Map.Entry entry : m.entrySet()) {
            if (first)
                first = false;
            else
                sb.append(",");
            appendFlatValue(entry.getKey(), sb, rawValue);
            sb.append(":");
            appendFlatValue(entry.getValue(), sb, rawValue);
        }
        sb.append("}");
        return sb;
    }

    private static StringBuilder appendValueString(String value, StringBuilder sb) {
        return sb.append("'").append(value.replace("'", "''")).append("'");
    }

    static String toRawString(Object value) {
        return appendValue(value, new StringBuilder(), true).toString();
    }

    static StringBuilder appendName(String name, StringBuilder sb) {
        name = name.trim();
        if (cnamePattern.matcher(name).matches() || name.startsWith("\"") || fctsPattern.matcher(name).matches())
            sb.append(name);
        else
            sb.append("\"").append(name).append("\"");
        return sb;
    }

    static abstract class Appendeable {
        abstract void appendTo(StringBuilder sb);

        abstract String name();
        abstract Object firstValue();
    }
}
