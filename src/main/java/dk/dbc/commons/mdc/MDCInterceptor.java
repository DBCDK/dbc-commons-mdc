/*
 * Copyright (C) 2020 DBC A/S (http://dbc.dk/)
 *
 * This is part of mdc
 *
 * mdc is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mdc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.commons.mdc;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Across bean boundary interceptor annotation
 * <p>
 * This allows for adding parameter values to MDC
 * <p>
 * This is added via the {@link MDCExtension} to methods that are
 * annotated with {@link LogAs} or {@link GenerateTrackingId}. This should not
 * be used directly.
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@MDCInterceptorBinding
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 1)
@SuppressWarnings("PMD.UnusedPrivateMethod")
class MDCInterceptor {

    private static final Logger log = LoggerFactory.getLogger(MDCInterceptor.class);

    private static final HashMap<Method, Invoker> WRAPPERS = new HashMap<>();
    // Do noting "wrapper"
    private static final Invoker DEFAULT_WRAPPER = InvocationContext::proceed;

    @FunctionalInterface
    private interface Invoker {

        Object call(InvocationContext ic) throws Exception;
    }

    @AroundInvoke
    private Object methodInvocation(InvocationContext context) throws Exception {
        return WRAPPERS.getOrDefault(context.getMethod(), DEFAULT_WRAPPER)
                .call(context);
    }

    /**
     * Store in the global wrappers object a wrapper for this method
     *
     * @param method The method that is annotated with {@link LogAs}
     * @return an error message or null
     */
    static String wrapMethod(Method method) {
        String methodName = method.toGenericString();
        try {
            Stream.Builder<Consumer<Object[]>> builder = Stream.builder();
            Parameter[] parameters = method.getParameters();
            for (int i = 0 ; i < parameters.length ; i++) {
                Parameter parameter = parameters[i];
                Class<?> type = parameter.getType();
                if (parameter.isAnnotationPresent(GenerateTrackingId.class)) {
                    if (!type.equals(String.class))
                        throw new IllegalArgumentException("@GenerateTrackingId can only be used upon String types");
                    builder.add(makeTrackingIdSetter(i));
                }
                LogAs mdc = parameter.getAnnotation(LogAs.class);
                if (mdc != null) {
                    if (mdc.value().isEmpty())
                        throw new IllegalArgumentException("An MDC field needs a name");
                    if (!mdc.value().replaceAll("[-_.a-zA-Z0-9]", "").isEmpty())
                        throw new IllegalArgumentException("MDC field name contains invalid characters (a-zA-Z0-9-_.)");
                    if (cannotBecomeString(type))
                        log.warn("Argument of type: {} probably doesn't convert to a useful string", type);
                    builder.add(makeMDCSetter(i, mdc.value(), mdc.includeNull(), type));
                }
            }
            Consumer<Object[]>[] functions = builder.build().toArray(Consumer[]::new);
            WRAPPERS.put(method, makeInvoker(functions));
            log.info("Wrapped {} for mdc logging", methodName);
        } catch (RuntimeException ex) {
            return ex.getMessage() + " for " + methodName;
        }
        return null;
    }

    private static Invoker makeInvoker(Consumer<Object[]>[] functions) {
        return (ic) -> {
            Map<String, String> oldMdc = MDC.getCopyOfContextMap();
            try {
                Object[] params = ic.getParameters();
                for (Consumer<Object[]> function : functions) {
                    function.accept(params);
                }
                return ic.proceed();
            } finally {
                if (oldMdc == null)
                    MDC.clear();
                else
                    MDC.setContextMap(oldMdc);
            }
        };
    }

    /**
     * Make a Consumer that ensures a value in a parameter
     *
     * @param pos parameter position
     * @return function
     */
    static Consumer<Object[]> makeTrackingIdSetter(int pos) {
        return params -> {
            if (params[pos] == null || ( (String) params[pos] ).trim().isEmpty())
                params[pos] = UUID.randomUUID().toString();
        };
    }

    /**
     * Create a Consumer, that copies a parameter value to the MDC object
     *
     * @param pos         parameter position
     * @param field       name of MDC field
     * @param includeNull if parameter is null should it be included as "null"?
     * @param type        the formal type of the parameter (needed for primitive
     *                    arrays)
     * @return function
     */
    static Consumer<Object[]> makeMDCSetter(int pos, String field, boolean includeNull, Class<?> type) {
        if (type.isArray()) {
            type = type.getComponentType();
            if (type.isArray()) {
                return makeMDCArraySetter(pos, field, includeNull, o -> Arrays.deepToString((Object[]) o));
            } else if (type.isPrimitive()) {
                switch (type.getName()) {
                    case "double":
                        return makeMDCArraySetter(pos, field, includeNull, o -> Arrays.toString((double[]) o));
                    case "float":
                        return makeMDCArraySetter(pos, field, includeNull, o -> Arrays.toString((float[]) o));
                    case "long":
                        return makeMDCArraySetter(pos, field, includeNull, o -> Arrays.toString((long[]) o));
                    case "int":
                        return makeMDCArraySetter(pos, field, includeNull, o -> Arrays.toString((int[]) o));
                    case "short":
                        return makeMDCArraySetter(pos, field, includeNull, o -> Arrays.toString((short[]) o));
                    case "char":
                        return makeMDCArraySetter(pos, field, includeNull, o -> Arrays.toString((char[]) o));
                    case "byte":
                        return makeMDCArraySetter(pos, field, includeNull, o -> Arrays.toString((byte[]) o));
                    case "boolean":
                        return makeMDCArraySetter(pos, field, includeNull, o -> Arrays.toString((boolean[]) o));
                    default:
                        throw new IllegalStateException("Type: " + type.getName() + " is primitive but not (yet) supported");
                }
            } else {
                if (cannotBecomeString(type))
                    log.warn("Argument of type: {} probably doesn't convert to a useful string", type);
                return makeMDCArraySetter(pos, field, includeNull, o -> Arrays.toString((Object[]) o));
            }
        } else {
            return params -> {
                Object param = params[pos];
                if (param != null || includeNull) {
                    MDC.put(field, String.valueOf(param));
                }
            };
        }
    }

    private static Consumer<Object[]> makeMDCArraySetter(int pos, String field, boolean includeNull, Function<Object, String> toString) {
        return params -> {
            Object param = params[pos];
            if (param != null) {
                MDC.put(field, toString.apply(param));
            } else if (includeNull) {
                MDC.put(field, "null");
            }
        };
    }

    /**
     * Check if a type cannot be converted to a meaningful string
     *
     * @param type class definition
     * @return if {@link java.lang.String#valueOf(java.lang.Object)} will
     *         give a
     *         nondescript result
     */
    private static boolean cannotBecomeString(Class<?> type) {
        if (type.isPrimitive())
            return false;
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals("toString") && method.getParameterCount() == 0)
                return false;
        }
        return true;
    }
}
