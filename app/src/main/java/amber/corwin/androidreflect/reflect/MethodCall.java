package amber.corwin.androidreflect.reflect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;



public class MethodCall {
    public String name;
    public List<String> parameterTypes;
    public List<String> argumentValues;

	static MethodCall fromString(String spec) {
        String[] split = spec.split("[?]", 1);
        return fromStrings(split[0], split.length > 1 ? split[1] : "");
    }
    
    public static MethodCall fromStrings(String name, String params) {
        MethodCall q = new MethodCall();

        q.name = name;
    		q.parameterTypes = new ArrayList<>();
    		q.argumentValues = new ArrayList<>();
        if (!params.equals("")) {
        		 for (String pa : Arrays.asList(params.split("&"))) {
        			 String[] splt = pa.split("=", 2);
        			 q.parameterTypes.add(splt[0]);
        			 q.argumentValues.add(splt.length > 1 ? splt[1] : null);            				 
        		 }
        }

        return q;
    }

    public Class<?>[] parameterClasses() throws ClassNotFoundException {
        List<Class<?>> l = new ArrayList<>();
        for (String p : parameterTypes) {
            l.add(typeForName(p));
        }
        return l.toArray(new Class<?>[0]);
    }
    
    public Object[] argumentActualValues(ValueParser parser) throws ValueParser.ValueFormatError {
    		List<Object> vals = new ArrayList<>(argumentValues.size());
    		for (int i = 0; i < parameterTypes.size(); ++i) {
    			vals.add(parser.parseValue(argumentValues.get(i), parameterTypes.get(i)));
    		}
    		return vals.toArray();
    }
    
    /**
     * Looks for a class, including primitives.
     * Uses the map PRIMITIVES.
     * @throws ClassNotFoundException
     */
    private Class<?> typeForName(String name) throws ClassNotFoundException {
    		Class<?> c = PRIMITIVES.get(name);
    		return c != null ? c : Class.forName(name);
    }
    
    static private Map<String, Class<?>> PRIMITIVES = new TreeMap<>();
    static {  // SLI5
    		for (Class<?> c : new Class<?>[] { int.class, byte.class, boolean.class, short.class, long.class, float.class, double.class, char.class, void.class })
    			PRIMITIVES.put(c.getName(), c);
    }
    
}
