package amber.corwin.androidreflect;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;

import amber.corwin.androidreflect.reflect.MethodCall;
import amber.corwin.androidreflect.reflect.ObjectStore;
import amber.corwin.androidreflect.reflect.ValueParser;
import amber.corwin.androidreflect.reflect.ValueParser.ValueFormatError;
import nanohttpd.NanoHTTPD;

import static amber.corwin.androidreflect.reflect.MethodCall_jdk_lt_8.methodSimpleSignature;
import static nanohttpd.NanoHTTPD.MIME_HTML;
import static nanohttpd.NanoHTTPD.MIME_PLAINTEXT;

/**
 * Sets up an HTTP server that provides the following functionality:
 * - Listing methods of a class
 * - Invoking a method by specifying its parameter types and providing actual values
 */

public class ReflectServer {

    private Class<?> root;
    private NanoHTTPD httpd;

    public ReflectServer(Class<?> rootClass) {
        this.root = rootClass;
    }

    // ----------------
    // HTTP Server Part
    // ----------------

    private static final String BASE_TEMPLATE = "<html><body>%s</body></html>";

    public void start() {
        httpd = new NanoHTTPD(8014) {
            @Override
            public Response serve(IHTTPSession session) {
                String path = session.getUri();
                String q = session.getQueryParameterString();
                if (path == "/")
                	return membersPage(root, q);
                else if (path.matches("/[^/]*/?"))
                    return membersPage(path, q);
                else if (q != null && q.startsWith("call"))
                    return methodCallPage(path, q.substring(4));
                else if (q != null && q.startsWith("get"))
                    return fieldGetPage(path, q.substring(3));
                else if (path.startsWith("/js/"))
                	return staticResource(path);
                else
                    return super.serve(session);
            }
        };

        try {
            httpd.start();
            log(String.format("server started at http://localhost:%d", httpd.getListeningPort()));
        }
        catch (IOException e) {
            log("server start failed;", e);
        }
    }

    public void stop() {
        httpd.stop();
    }

    protected void log(String msg) {
    	log(msg, null);
    }
    
    protected void log(String msg, Exception error) {
        System.err.println(msg);
        if (error != null) System.err.println(error);
    }

    // ----------
    // Pages Part
    // ----------
    
    String HEAD = "<script src=\"/js/reflect.js\"></script>";
    
    private NanoHTTPD.Response membersPage(String path, String thisRef) {
    	path = removeLeading(path, "/");
    	path = removeTrailing(path, "/");
    	try {
    		return membersPage(ValueParser.typeForName(path), thisRef);
    	}
    	catch (ClassNotFoundException e) {
    		return new NanoHTTPD.Response(NanoHTTPD.Response.Status.NOT_FOUND, MIME_PLAINTEXT,
    				e.toString());
    	}
    }
    
    private NanoHTTPD.Response membersPage(Class<?> root, String thisRef) {
        String header = String.format("<input class=\"this-ref\" value=\"%s\"/>", thisRef == null ? "" : thisRef);
        StringBuilder payload = new StringBuilder();
        for (Method m : root.getMethods()) {
            String links = 
                    String.format("<a href=\"%s\" data-href=\"%s\">call</a>", methodCallUrl(m, thisRef), methodCallUrl(m)) +
                    (Modifier.isStatic(m.getModifiers()) ? "" : String.format("<input class=\"this-arg\" value=\"%s\"/>", (thisRef == null ? "" : thisRef))) +
            		(m.getParameterTypes().length > 0 ? "<input />" : "");
            payload.append(String.format("<li>%s %s</li>\n",
                    methodSimpleSignature(m), links));
        }
        for (Field f : root.getFields()) {
            String links = (Modifier.isStatic(f.getModifiers())) ?
                    String.format("<a href=\"%s\">get</a>", fieldGetUrl(f)) : "";
            payload.append(String.format("<li>%s %s</li>\n",
                    fieldSimpleSignature(f), links));
        }
        return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, MIME_HTML,
                String.format(BASE_TEMPLATE, String.format("%s\n%s\n<ul>%s</ul>\n", HEAD, header, payload.toString())));
    }
    
    private String methodCallUrl(Method m) {
        return methodCallUrl(m, null);
    }
    
    private String methodCallUrl(Method m, String thisRef) {
		// "/{class}/{name}?call&{param1-type}&{param2-type}&..."
		StringBuilder b = new StringBuilder();
		b.append("/");
		b.append(m.getDeclaringClass().getName());
		b.append("/");
		b.append(m.getName());
		b.append("?call");
		if (!Modifier.isStatic(m.getModifiers())) {
		    b.append("&this");
		    if (thisRef != null) b.append("=" + thisRef);
		}
		for (Class<?> c : m.getParameterTypes())
			b.append("&" + c.getName());
		return b.toString();
    }
    
    private String fieldGetUrl(Field f) {
    	// "/{class}/{name}?get"
    	return "/" + f.getDeclaringClass().getName() + "/" + f.getName() + "?get";
    }
    
    private String objectRefUrl(UUID ref, String className) {
        // "/{class}?[{uuid}]
        return "/" + className + "?[" + ref + "]";
    }
    
    private String fieldSimpleSignature(Field f) {
        // TODO toGenericString if JDK supports
    	return f.getType().toString() + " " + f.getName();
    }
    
    private String objectRefLink(UUID ref, String className) {
        return String.format("<a href=\"%s\">[%s]</a>", objectRefUrl(ref, className), ref);
    }
    
    private String objectRefLink(UUID ref, Class<?> type) {
        return objectRefLink(ref, type.getName());
    }
    
    private NanoHTTPD.Response methodCallPage(String path, String queryString) {
		// Remove leading "&" from query string
		path = removeLeading(path, "/");
		queryString = removeLeading(queryString, "&");

        // Parse method call
		try {
	        MethodCall q = MethodCall.fromStrings(path, queryString);
	        Class<?> root = q.class_();
	        
	        try {
	            Method method = root.getMethod(q.name, q.parameterClasses());
	            Object ret = null;
	            Object err = null;
	            UUID uuid = null;
	            
	            try {
	            	ret = method.invoke(q.thisArgActual(parser), q.argumentActualValues(parser));
	            	if (ret != null)
	            	    uuid = store.add(ret);
	            }
	            catch (InvocationTargetException e) { err = e.getCause(); }
	            catch (Exception e) { err = e; }
	            
	            return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, MIME_HTML,
	                    String.format(BASE_TEMPLATE,  
	                    		String.format("<p>%s</p>", method.toGenericString() +
	                    			                       (err == null ? " = " + ret : " !! " + err)) +
	                    		String.format("<p>%s</p>", uuid == null ? "" : objectRefLink(uuid, ret.getClass()))));
	        }
	        catch (NoSuchMethodException e) {
	            return new NanoHTTPD.Response(NanoHTTPD.Response.Status.NOT_FOUND, MIME_PLAINTEXT,
	            		"Method not found: " + q.name);
	        }
	        catch (ClassNotFoundException e) {
	            return new NanoHTTPD.Response(NanoHTTPD.Response.Status.NOT_FOUND, MIME_PLAINTEXT,
	            		"Unknown parameter type: " + e.toString());
	        }
		}
		catch (ValueFormatError e) {
            return new NanoHTTPD.Response(NanoHTTPD.Response.Status.BAD_REQUEST, MIME_PLAINTEXT,
            		"Invalid method specification: " + path + "?" + queryString);
		}
        catch (ClassNotFoundException e) {
            return new NanoHTTPD.Response(NanoHTTPD.Response.Status.NOT_FOUND, MIME_PLAINTEXT,
                    "Unknown class: " + e.toString());
        }
    }
    
    private NanoHTTPD.Response fieldGetPage(String path, String queryString) {
    	path = removeLeading(path, "/");
    	
    	String[] split = path.split("/", 2);
    	
    	try {
    	    Class<?> root = ValueParser.typeForName(split[0]);
    		Field field = root.getField(split[1]);
    		Object obj = queryString != null && queryString.length() != 0 ?
    		        parser.parseReference(queryString, split[0]) : null;
    		
            Object ret = null;
            Object err = null;
            UUID uuid = null;

    		try {
    			ret = field.get(obj);
    			if (ret != null)
    			    uuid = store.add(ret);
    		}
    		catch (Exception e) { err = e; }
    		
            return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, MIME_HTML,
                    String.format(BASE_TEMPLATE, 
                            String.format("<p>%s</p>", field.toGenericString() + (err == null ? " = " + ret : " !! " + err))) + 
                            String.format("<p>%s</p>", uuid == null ? "" : objectRefLink(uuid, ret.getClass())));
    	}
        catch (ValueFormatError e) {
            return new NanoHTTPD.Response(NanoHTTPD.Response.Status.BAD_REQUEST, MIME_PLAINTEXT,
                    "Invalid object reference: " + queryString);
        }
        catch (ClassNotFoundException e) {
            return new NanoHTTPD.Response(NanoHTTPD.Response.Status.NOT_FOUND, MIME_PLAINTEXT,
                    "Unknown class: " + e.toString());
        }
    	catch (NoSuchFieldException e) {
            return new NanoHTTPD.Response(NanoHTTPD.Response.Status.NOT_FOUND, MIME_PLAINTEXT,
            		"Field not found: " + path);
    	}
    }
    
    private static final String STATIC_ROOT_JS = "app/src/main/js";
    
    private NanoHTTPD.Response staticResource(String path) {
    	try {
	    	if (path.startsWith("/js/"))
	    		return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK,
	    				"text/javascript",
	    				new FileInputStream(STATIC_ROOT_JS + path.substring(3)));
	    	else
	    		return new NanoHTTPD.Response(NanoHTTPD.Response.Status.NOT_FOUND, "", "");
    	}
    	catch (FileNotFoundException e) {
    		return new NanoHTTPD.Response(NanoHTTPD.Response.Status.NOT_FOUND, "", "");
    	}
    }
    
    private static String removeLeading(String s, String prefix) {
		while (s.startsWith(prefix)) s = s.substring(prefix.length());
		return s;
    }
    
    private static String removeTrailing(String s, String suffix) {
    	while (s.endsWith(suffix)) s = s.substring(0, s.length() - suffix.length());
    	return s;
    }

    // ----------------
    // ObjectStore Part
    // ----------------
    private ObjectStore store = new ObjectStore();
    
    // -----------
    // Parser Part
    // -----------
    private ValueParser parser = new ValueParser(store);
	
	
    public static void main(String[] args) {
        ReflectServer s = new ReflectServer(System.class);
        s.start();
    }
}
