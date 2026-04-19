package net.rtxyd.fallen.lib.engine;

import java.util.Collections;
import java.util.List;

public final class ClassInfo {
    public final String superName;
    public final String version;
    public final List<String> interfaces;
    public final List<String> nestMembers;

    public ClassInfo(String superName, String version, List<String> interfaces, List<String> nestMembers) {
        this.superName = superName;
        this.version = version;
        this.interfaces = interfaces != null ? interfaces : Collections.emptyList();
        this.nestMembers = nestMembers != null ? nestMembers : Collections.emptyList();
    }

    public List<String> getNestMembers() {
        return Collections.unmodifiableList(nestMembers);
    }
}