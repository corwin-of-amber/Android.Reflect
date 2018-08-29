package amber.corwin.androidreflect.reflect;

import java.lang.reflect.Array;



public class ValueRender {

    /**
     * Library-independent HTML escaping function (https://stackoverflow.com/a/25228492/37639)
     */
    public static String escapeHTML(String s) {
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
                out.append("&#");
                out.append((int) c);
                out.append(';');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    public String renderObject(Object o) {
        if (o == null) {
            return "null";
        }
        if (o.getClass().isArray()) {
            return renderArray(o);
        }
        else return escapeHTML(o.toString());
    }

    protected String renderArray(Object arr) {
        int length = Array.getLength(arr);
        StringBuffer b = new StringBuffer();
        b.append("[");
        for (int i = 0; i < length; i++) {
            if (i > 0) b.append(", ");
            Object el = Array.get(arr, i);
            b.append("<span class=\"array-element\">");
            b.append(renderObject(el));
            b.append("</span>");
        }
        return b.toString();
    }
}
