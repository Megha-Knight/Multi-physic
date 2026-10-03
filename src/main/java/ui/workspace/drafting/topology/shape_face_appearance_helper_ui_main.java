package ui.workspace.drafting.topology;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * shape_face_appearance_helper_ui_main.java
 * External appearance override store for shape bodies and derived face regions.
 * Prevents inflating shape_item_ui_main beyond the 200-line limit.
 */
public final class shape_face_appearance_helper_ui_main {

    private static final Map<String, Map<String, topology_face_appearance_ui_main>> OVERRIDES = new LinkedHashMap<>();

    private shape_face_appearance_helper_ui_main() {}

    public static synchronized void setAppearance(String shapeId, String faceId, topology_face_appearance_ui_main app) {
        if (shapeId == null || faceId == null) return;
        if (app == null) {
            clearAppearance(shapeId, faceId);
            return;
        }
        OVERRIDES.computeIfAbsent(shapeId, k -> new LinkedHashMap<>()).put(faceId, app);
    }

    public static synchronized topology_face_appearance_ui_main getAppearance(String shapeId, String faceId) {
        if (shapeId == null || faceId == null) return null;
        Map<String, topology_face_appearance_ui_main> map = OVERRIDES.get(shapeId);
        return map != null ? map.get(faceId) : null;
    }

    public static synchronized void clearAppearance(String shapeId, String faceId) {
        if (shapeId == null || faceId == null) return;
        Map<String, topology_face_appearance_ui_main> map = OVERRIDES.get(shapeId);
        if (map != null) map.remove(faceId);
    }

    public static synchronized Map<String, topology_face_appearance_ui_main> getOverridesForShape(String shapeId) {
        if (shapeId == null) return Collections.emptyMap();
        Map<String, topology_face_appearance_ui_main> map = OVERRIDES.get(shapeId);
        return map != null ? Collections.unmodifiableMap(new LinkedHashMap<>(map)) : Collections.emptyMap();
    }

    public static synchronized void copyOverrides(String fromShapeId, String toShapeId) {
        if (fromShapeId == null || toShapeId == null) return;
        Map<String, topology_face_appearance_ui_main> map = OVERRIDES.get(fromShapeId);
        if (map != null) {
            Map<String, topology_face_appearance_ui_main> target = OVERRIDES.computeIfAbsent(toShapeId, k -> new LinkedHashMap<>());
            target.putAll(map);
        }
    }

    public static synchronized void clearShape(String shapeId) {
        if (shapeId != null) OVERRIDES.remove(shapeId);
    }

    public static synchronized void clearAll() {
        OVERRIDES.clear();
    }
}
