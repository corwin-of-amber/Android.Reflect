package amber.corwin.androidreflect;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

import amber.corwin.androidreflect.reflect.ObjectStore;
import nanohttpd.NanoHTTPD;

import static amber.corwin.androidreflect.reflect.MethodCall_jdk_lt_8.methodSimpleSignature;
import static nanohttpd.NanoHTTPD.MIME_HTML;
import static nanohttpd.NanoHTTPD.MIME_PLAINTEXT;


public class MainActivity extends Activity {

    ReflectServer server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        setContentView(R.layout.activity_main);

        loadMethods();

        server = new ReflectServer(Toast.class) {
            @Override
            protected void log(String msg, Exception error) {
                Log.e("Reflect.Server", msg, error);
            }
        };

        // Since application files are unavailable, register a resource provider
        server.registerStaticResources(new ReflectServer.StaticResourceProvider() {
            @Override
            public InputStream open(String path) {
                if (path.equals("/js/reflect.js")) return getResources().openRawResource(R.raw.reflect);
                else return null;
            }
        });

        server.setWorker(new ReflectServer.Worker() {

            @Override
            public <V> V delegate(Callable<V> code) throws Exception {
                final Semaphore s = new Semaphore(0);

                final Box<V> box = new Box<>();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            box.ret = code.call();
                        }
                        catch (Exception e) { box.err = e; }
                        finally { s.release(); }
                    }
                });

                while (true) {
                    try {
                        s.acquire();
                    }
                    catch (InterruptedException e) { continue; }
                    if (box.err != null) throw box.err;
                    else return box.ret;
                }
            }
        });

        // Make Activity instance available
        ObjectStore store = server.getObjectStore();
        try {
            store.persist(store.add(this), "$0");
        }
        catch (ObjectStore.NoSuchObjectException e) { assert(false); }

        server.start();
    }

    static class Box<V> {
        Exception err = null;
        V ret = null;
    }

    public void loadMethods() {
        Method[] methods = Toast.class.getMethods();
        List<String> entries = new ArrayList<>();
        for (Method m : methods) {
            entries.add(methodSimpleSignature(m));
            //Log.d("Reflect.Core", m.toGenericString());
        }

        ArrayAdapter<Object> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, entries.toArray());

        ListView lv = (ListView)findViewById(R.id.methods_list);
        lv.setAdapter(adapter);
    }


}
