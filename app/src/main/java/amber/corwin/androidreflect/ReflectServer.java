package amber.corwin.androidreflect;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

import amber.corwin.androidreflect.reflect.MethodCall;
import amber.corwin.androidreflect.reflect.ValueParser;
import nanohttpd.NanoHTTPD;

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
                    return methodsPage();
                else if (q != null && q.startsWith("call"))
                    return methodCallPage(path, q.substring(4));
                else
                    return super.serve(session);
            }
        };

        try {
            httpd.start();
        }
        catch (IOException e) {
            log("server start failed;", e);
        }
    }

    public void stop() {
        httpd.stop();
    }

    protected void log(String msg, Exception error) {
        System.err.println(msg);
        if (error != null) System.err.println(error);
    }

    private NanoHTTPD.Response methodsPage() {
        StringBuilder payload = new StringBuilder();
        for (Method m : root.getMethods()) {
            String links = (Modifier.isStatic(m.getModifiers())) ?
                    String.format("<a href=\"%s\">call</a>", methodCallUrl(m)) : "";
            payload.append(String.format("<li>%s %s</li>\n",
                    methodSimpleSignature(m), links));
        }
        return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, MIME_HTML,
                String.format(BASE_TEMPLATE, String.format("<ul>%s</ul>\n", payload.toString())));
    }
    
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
    
    /**
     * Supposed to produce a succinct, compact representation of the method.
     * Does not have to be super-precise, emphasis on easy readability.
     */
    private String methodSimpleSignature(Method m) {
    		StringBuilder b = new StringBuilder();
    		
    		int mod = m.getModifiers();
    		if (Modifier.isStatic(mod)) b.append("static ");
    		b.append(m.getReturnType().getSimpleName() + " ");
    		b.append(m.getName());
    		b.append("(");
    		boolean first = true;
    		for (Parameter p : m.getParameters()) {
    			if (first) first = false; else b.append(", ");
    			b.append(p.getType().getSimpleName() + " " + p.getName());
    		}
    		b.append(")");
    		
    		return b.toString();
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
