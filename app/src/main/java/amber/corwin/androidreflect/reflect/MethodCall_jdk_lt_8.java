package amber.corwin.androidreflect.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

/**
 * Provides implementations that require Java >= 8.
 */

public class MethodCall_jdk_lt_8 {
    /**
     * Supposed to produce a succinct, compact representation of the method.
     * Does not have to be super-precise, emphasis on easy readability.
     */
    public static String methodSimpleSignature(Method m) {
        StringBuilder b = new StringBuilder();

        int mod = m.getModifiers();
        if (Modifier.isStatic(mod)) b.append("static ");
        b.append(m.getReturnType().getSimpleName() + " ");
        b.append(m.getName());
        b.append("(");
        boolean first = true;
        for (Class<?> p : m.getParameterTypes()) {
            if (first) first = false; else b.append(", ");
            b.append(p.getSimpleName());
        }
        b.append(")");

        return b.toString();
    }

}
