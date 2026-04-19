package net.rtxyd.fallen.lib.engine;

import java.util.*;

public class ClassIndex {

    private final Map<String, ClassInfo> classHierarchy = new HashMap<>();

    String add(String name, ClassInfo info) {
        return classHierarchy.put(name, info) == null ? null : name;
    }

    public boolean contains(String className) {
        return classHierarchy.containsKey(className);
    }

    public boolean isSubclassOf(String className, Set<String> superClasses) {
        String current = className;
        Set<String> visited = new HashSet<>();

        while (current != null && visited.add(current)) {
            ClassInfo info = classHierarchy.get(current);
            if (info == null) break;

            if (info.superName != null && superClasses.contains(info.superName)) return true;

            for (String iface : info.interfaces) {
                if (superClasses.contains(iface)) return true;
            }

            current = info.superName;
        }

        return false;
    }

    public Set<String> getAllClasses() {
        return Collections.unmodifiableSet(classHierarchy.keySet());
    }

    ClassInfo get(String className) {
        return classHierarchy.get(className);
    }

    public List<String> getNestMembers(String className) {
        ClassInfo info = classHierarchy.get(className);
        if (info != null) {
            return Collections.unmodifiableList(info.nestMembers);
        }
        return List.of();
    }
}