package net.rtxyd.fallen.lib.service;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AsmAnnotationData implements AnnotationData {

    private final AnnotationNode node;
    private Map<String, Object> cache;

    AsmAnnotationData(AnnotationNode node) {
        this.node = node;
    }

    @Override
    public String name() {
        return node.desc;
    }

    @Override
    public Object get(String name) {
        ensureParsed();
        return cache.get(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getWithDefaut(String e, T defauit) {
        Object result = get(e);
        return result != null ? (T) result : defauit;
    }

    // no cache.
    public Object[] getEachExact(String... names) {
        if (node.values == null || node.values.size() != names.length * 2) {
            return new Object[0];
        }

        Object[] result = new Object[names.length];
        for (int i = 0; i < names.length; i++) {
            Object key = node.values.get(i * 2);
            if (!names[i].equals(key)) {
                return new Object[0];
            }
            result[i] = node.values.get(i * 2 + 1);
        }
        return result;
    }

    @Override
    public boolean has(String element) {
        ensureParsed();
        return cache.containsKey(element);
    }

    private void ensureParsed() {
        if (cache != null) return;
        cache = new HashMap<>();
        if (node.values == null) return;
        for (int i = 0; i < node.values.size(); i += 2) {
            String key = (String) node.values.get(i);
            Object raw = node.values.get(i + 1);
            cache.put(key, adapt(raw));
        }
    }

    private Object adapt(Object v) {
        // node
        if (v instanceof AnnotationNode an) {
            return new AsmAnnotationData(an);
        }

        // array
        if (v instanceof List<?> list) {
            List<Object> result = new ArrayList<>(list.size());
            for (Object item : list) {
                result.add(adapt(item));
            }
            return result;
        }

        // enum
        if (v instanceof String[] e) {
            return e[1];
        }

        // class
        if (v instanceof Type t) {
            return t.getClassName();
        }

        // others
        return v;
    }
}