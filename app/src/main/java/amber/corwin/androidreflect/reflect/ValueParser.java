package amber.corwin.androidreflect.reflect;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.UUID;

public class ValueParser {

	private ObjectStore store;

	public ValueParser(ObjectStore store) {
		this.store = store;
	}
	
    public Object parseValue(String text, String type) throws ValueFormatError {
		if (type.equals("int"))
			return Integer.parseInt(text);
		else if (type.equals("java.lang.String") || type.equals("java.lang.CharSequence"))
			return text;
		else
			return parseReference(text, type);
    }
    
    public Object parseReference(String ref, String type) throws ValueFormatError {
    	if (store == null)
    		throw new NoSuchElementException("store uninitialized");
    	else if (ref.startsWith("[")) {
    		Object obj = store.get(parseUUID(ref));
    		if (obj == null) throw new NoSuchElementException("undefined or expired " + ref);
    		return obj;
    	}
    	else if (ref.startsWith("$")) {
            Object obj = store.get(ref);
            if (obj == null) throw new NoSuchElementException("undefined or expired " + ref);
            return obj;
    	}
		else
			throw new ValueFormatError();
	}
    
    public UUID parseUUID(String ref) throws ValueFormatError {
        try {
            return UUID.fromString(ref.substring(1, ref.length() - 1));
        }
        catch (IllegalArgumentException e) {
            throw new ValueFormatError();
        }
    }
	
    /**
     * Looks for a class, including primitives.
     * Uses the map PRIMITIVES.
     * @throws ClassNotFoundException
     */
    public static Class<?> typeForName(String name) throws ClassNotFoundException {
    		Class<?> c = PRIMITIVES.get(name);
    		return c != null ? c : Class.forName(name);
    }
    
    static private Map<String, Class<?>> PRIMITIVES = new TreeMap<>();
    static {  // SLI5
    		for (Class<?> c : new Class<?>[] { int.class, byte.class, boolean.class, short.class, long.class, float.class, double.class, char.class, void.class })
    			PRIMITIVES.put(c.getName(), c);
    }
    

    @SuppressWarnings("serial")
	public static class ValueFormatError extends Exception { }

}
