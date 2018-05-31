package amber.corwin.androidreflect;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import amber.corwin.androidreflect.reflect.MethodCall;
import amber.corwin.androidreflect.reflect.ValueParser;
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
                if (path.equals("/"))
                    return membersPage();
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

    private NanoHTTPD.Response membersPage() {
        StringBuilder payload = new StringBuilder();
        for (Method m : root.getMethods()) {
            String links = (Modifier.isStatic(m.getModifiers()) ?
                    String.format("<a href=\"%s\" data-href=\"%s\">call</a>", methodCallUrl(m), methodCallUrl(m)) : "") +
            		(m.getParameterCount() > 0 ? "<input />" : "");
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
                String.format(BASE_TEMPLATE, String.format("%s<ul>%s</ul>\n", HEAD, payload.toString())));
    }
    
    String HEAD = "<script src=\"js/reflect.js\"></script>";
    
    private String methodCallUrl(Method m) {
		// "/{name}?call&{param1-type}&{param2-type}&..."
		StringBuilder b = new StringBuilder();
		b.append("/");
		b.append(m.getName());
		b.append("?call");
		for (Class<?> c : m.getParameterTypes())
			b.append("&" + c.getName());
		return b.toString();
    }
    
    private String fieldGetUrl(Field f) {
    	// "/{name}?get"
    	return "/" + f.getName() + "?get";
    }
    
    private String fieldSimpleSignature(Field f) {
    	return f.getType().toGenericString() + " " + f.getName();
    }
    
    private NanoHTTPD.Response methodCallPage(String path, String queryString) {
		// Remove leading "&" from query string
		path = removeLeading(path, "/");
		queryString = removeLeading(queryString, "&");

        // Parse method call
        MethodCall q = MethodCall.fromStrings(path, queryString);
        try {
            Method method = root.getMethod(q.name, q.parameterClasses());
            Object ret = null;
            Object err = null;
            
            try {
            	ret = method.invoke(null, q.argumentActualValues(parser));
            }
            catch (InvocationTargetException e) { err = e.getCause(); }
            catch (Exception e) { err = e; }
            
            return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, MIME_HTML,
                    String.format(BASE_TEMPLATE, method.toGenericString() + (err == null ? " = " + ret : " !! " + err)));
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
    
    private NanoHTTPD.Response fieldGetPage(String path, String queryString) {
    	path = removeLeading(path, "/");
    	
    	try {
    		Field field = root.getField(path);
            Object ret = null;
            Object err = null;
    		
    		try {
    			ret = field.get(null);
    		}
    		catch (Exception e) { err = e; }
    		
            return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, MIME_HTML,
                    String.format(BASE_TEMPLATE, field.toGenericString() + (err == null ? " = " + ret : " !! " + err)));
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

    // -----------
    // Parser Part
    // -----------
    static ValueParser parser = new ValueParser();
	
	
    public static void main(String[] args) {
        ReflectServer s = new ReflectServer(System.class);
        s.start();
    }
}
