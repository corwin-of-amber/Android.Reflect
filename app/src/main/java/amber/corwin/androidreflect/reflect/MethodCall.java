package amber.corwin.androidreflect.reflect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import amber.corwin.androidreflect.reflect.ValueParser.ValueFormatError;



public class MethodCall {
	public String className;
    public String name;
    public List<String> parameterTypes;
    public List<String> argumentValues;
    public String thisArg;

	static MethodCall fromString(String spec) throws ValueFormatError {
        String[] split = spec.split("[?]", 2);
        return fromStrings(split[0], split.length > 1 ? split[1] : "");
    }
    
    public static MethodCall fromStrings(String name, String params) throws ValueFormatError {
        MethodCall q = new MethodCall();

        String[] splitName = name.split("/", 2);
        
        if (splitName.length > 1) {
        	q.className = splitName[0];
        	q.name = splitName[1];
        }
        else
        	throw new ValueParser.ValueFormatError(); // "class name is missing for method call " + name
        q.parameterTypes = new ArrayList<>();
		q.argumentValues = new ArrayList<>();
        if (!params.equals("")) {
            for (String pa : Arrays.asList(params.split("&"))) {
            	String[] splt = pa.split("=", 2);
            	if (splt[0].equals("this"))
            	    q.thisArg = splt.length > 1 ? splt[1] : null;
            	else {
            		q.parameterTypes.add(splt[0]);
            		q.argumentValues.add(splt.length > 1 ? splt[1] : null);
            	}
            }
        }

        return q;
    }

    public Class<?> class_() throws ClassNotFoundException {
        return ValueParser.typeForName(className);
    }
    
	public Class<?>[] parameterClasses() throws ClassNotFoundException {
		List<Class<?>> l = new ArrayList<>();
		for (String p : parameterTypes) {
			l.add(ValueParser.typeForName(p));
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
	
	public Object thisArgActual(ValueParser parser) throws ValueParser.ValueFormatError {
	    return parser.parseReference(thisArg, className);
	}
    
}
