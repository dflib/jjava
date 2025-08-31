package org.dflib.jjava.jupyter.kernel.magic.registry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Magics {
    private final Map<String, LineMagicFunction<?>> lineMagics;
    private final Map<String, CellMagicFunction<?>> cellMagics;

    public Magics() {
        this.lineMagics = new HashMap<>();
        this.cellMagics = new HashMap<>();
    }

    // Magic application

    public <T> T applyLineMagic(String name, List<String> args) throws Exception {
        @SuppressWarnings("unchecked")
        LineMagicFunction<T> magic = (LineMagicFunction<T>) this.lineMagics.get(name);

        if (magic == null)
            throw new UndefinedMagicException(name, true);

        return magic.execute(args);
    }

    public <T> T applyCellMagic(String name, List<String> args, String body) throws Exception {
        @SuppressWarnings("unchecked")
        CellMagicFunction<T> magic = (CellMagicFunction<T>) this.cellMagics.get(name);

        if (magic == null)
            throw new UndefinedMagicException(name, false);

        return magic.execute(args, body);
    }

    // Magic registration

    public void registerLineMagic(String name, LineMagicFunction<?> magic) {
        this.lineMagics.put(name, magic);
    }

    public void registerCellMagic(String name, CellMagicFunction<?> magic) {
        this.cellMagics.put(name, magic);
    }

    public <T extends LineMagicFunction<?>&CellMagicFunction<?>> void registerLineCellMagic(String name, T magic) {
        this.lineMagics.put(name, magic);
        this.cellMagics.put(name, magic);
    }

    // Reflective magic registration

    public void registerMagics(Object magics) {
        registerMagics(magics.getClass(), magics);
    }

    public void registerMagics(Class<?> magicsClass) {
        registerMagics(magicsClass, null);
    }

    private void registerMagics(Class<?> magicsClass, Object magics) {
        for (Method method : magicsClass.getDeclaredMethods()) {
            LineMagic lineMagic = method.getAnnotation(LineMagic.class);
            CellMagic cellMagic = method.getAnnotation(CellMagic.class);

            if (lineMagic == null && cellMagic == null) continue;

            if (method.getParameterCount() == 0) {
                // Magic function with no arguments
                registerNoArgsReflectionMagic(magics, method, lineMagic, cellMagic);
            } else if (lineMagic != null && cellMagic != null) {
                // Line cell magic with some arguments
                registerLineCellReflectionMagic(magics, method, lineMagic, cellMagic);
            } else if (lineMagic != null) {
                // Just line magic
                registerLineReflectionMagic(magics, method, lineMagic);
            } else {
                // Just cell magic
                registerCellReflectionMagic(magics, method, cellMagic);
            }
        }
    }

    private static Object invoke(Method m, Object instance, Object... args) throws Exception {
        try {
            return m.invoke(instance, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception)
                throw ((Exception) cause);
            throw new RuntimeException(cause.getMessage(), cause);
        }
    }

    private static class NoArgsReflectionMagicFunction implements LineMagicFunction<Object>, CellMagicFunction<Object> {
        private final Object instance;
        private final Method method;

        NoArgsReflectionMagicFunction(Object instance, Method method) {
            this.instance = instance;
            this.method = method;
        }

        @Override
        public Object execute(List<String> args, String body) throws Exception {
            return invoke(method, instance);
        }

        @Override
        public Object execute(List<String> args) throws Exception {
            return invoke(method, instance);
        }
    }

    private static class LineCellReflectionMagicFunction implements LineMagicFunction<Object>, CellMagicFunction<Object> {
        private final Object instance;
        private final Method method;

        LineCellReflectionMagicFunction(Object instance, Method method) {
            this.instance = instance;
            this.method = method;
        }

        @Override
        public Object execute(List<String> args, String body) throws Exception {
            return invoke(method, instance, args, body);
        }

        @Override
        public Object execute(List<String> args) throws Exception {
            return invoke(method, instance, args, null);
        }
    }

    private static class LineReflectionMagicFunction implements LineMagicFunction<Object> {
        private final Object instance;
        private final Method method;

        LineReflectionMagicFunction(Object instance, Method method) {
            this.instance = instance;
            this.method = method;
        }

        @Override
        public Object execute(List<String> args) throws Exception {
            return invoke(method, instance, args);
        }
    }

    private static class CellReflectionMagicFunction implements CellMagicFunction<Object> {
        private final Object instance;
        private final Method method;

        CellReflectionMagicFunction(Object instance, Method method) {
            this.instance = instance;
            this.method = method;
        }

        @Override
        public Object execute(List<String> args, String body) throws Exception {
            return invoke(method, instance, args, body);
        }
    }

    private boolean isValidBodyParam(Parameter param) {
        return !param.getType().isAssignableFrom(String.class);
    }

    private boolean isValidArgsParam(Parameter param) {
        if (!param.getType().isAssignableFrom(List.class)) return true;

        Type parameterizedType = param.getParameterizedType();
        if (parameterizedType instanceof ParameterizedType) {
            Type genericType = ((ParameterizedType) parameterizedType).getActualTypeArguments()[0];
            return !String.class.equals(genericType);
        }

        return false;
    }

    private void registerLineMagic(Method method, LineMagic lineMagic, LineMagicFunction<?> func) {
        registerLineMagic(lineMagic.value().isEmpty() ? method.getName() : lineMagic.value(), func);
        for (String alias : lineMagic.aliases())
            registerLineMagic(alias, func);
    }

    private void registerCellMagic(Method method, CellMagic cellMagic, CellMagicFunction<?> func) {
        registerCellMagic(cellMagic.value().isEmpty() ? method.getName() : cellMagic.value(), func);
        for (String alias : cellMagic.aliases())
            registerCellMagic(alias, func);
    }

    private void registerNoArgsReflectionMagic(Object instance, Method method, LineMagic lineMagic, CellMagic cellMagic) {
        NoArgsReflectionMagicFunction func = new NoArgsReflectionMagicFunction(instance, method);

        if (lineMagic != null)
            registerLineMagic(method, lineMagic, func);

        if (cellMagic != null)
            registerCellMagic(method, cellMagic, func);
    }

    private void registerLineCellReflectionMagic(Object instance, Method method, LineMagic lineMagic, CellMagic cellMagic) {
        Parameter[] params = method.getParameters();
        if (params.length != 2 || isValidArgsParam(params[0]) || isValidBodyParam(params[1]))
            throw new IllegalArgumentException("Line-cell magic must accept a List<String> and String as parameters. (Magic arguments and possible cell body)");

        LineCellReflectionMagicFunction func = new LineCellReflectionMagicFunction(instance, method);
        registerLineMagic(method, lineMagic, func);
        registerCellMagic(method, cellMagic, func);
    }

    private void registerLineReflectionMagic(Object instance, Method method, LineMagic lineMagic) {
        Parameter[] params = method.getParameters();
        if (params.length != 1 || isValidArgsParam(params[0]))
            throw new IllegalArgumentException("Line magic must accept a List<String> as a parameter. (Magic arguments)");

        registerLineMagic(method, lineMagic, new LineReflectionMagicFunction(instance, method));
    }

    private void registerCellReflectionMagic(Object instance, Method method, CellMagic cellMagic) {
        Parameter[] params = method.getParameters();
        if (params.length != 2 || isValidArgsParam(params[0]) || isValidBodyParam(params[1]))
            throw new IllegalArgumentException("Cell magic must accept a List<String> and String as parameters. (Magic arguments and cell body)");

        registerCellMagic(method, cellMagic, new CellReflectionMagicFunction(instance, method));
    }

    public Set<String> getAllCellMagicNames() {
        return this.cellMagics.keySet();
    }

    public Set<String> getAllLineMagicNames() {
        return this.lineMagics.keySet();
    }
}
