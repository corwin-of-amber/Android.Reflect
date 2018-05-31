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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        server.start();
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
