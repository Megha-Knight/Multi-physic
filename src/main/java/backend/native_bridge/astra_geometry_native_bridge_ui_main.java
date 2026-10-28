package backend.native_bridge;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;

public final class astra_geometry_native_bridge_ui_main {
    private static final astra_geometry_native_bridge_ui_main INSTANCE = new astra_geometry_native_bridge_ui_main();
    private boolean available = false;
    private String loadError = null;

    private astra_geometry_native_bridge_ui_main() {
        try {
            SymbolLookup lookup = astra_native_bridge_ui_main.getInstance().isAvailable()
                ? SymbolLookup.loaderLookup()
                : null;
            if (lookup != null && lookup.find("astra_geometry_abi_version").isPresent()) {
                available = true;
            } else {
                loadError = "Native dynamic library does not export Astra Geometry ABI";
            }
        } catch (Throwable t) {
            loadError = t.getMessage();
            available = false;
        }
    }

    public static astra_geometry_native_bridge_ui_main getInstance() { return INSTANCE; }
    public boolean isAvailable() { return available; }
    public String getLoadError() { return loadError; }

    public static final class GeometryModel implements AutoCloseable {
        private MemorySegment handle;
        public GeometryModel(MemorySegment h) { this.handle = h; }
        public boolean isValid() { return handle != null && !handle.equals(MemorySegment.NULL); }
        @Override public void close() {
            if (isValid()) {
                handle = MemorySegment.NULL;
            }
        }
    }

    public static final class GeometryResult implements AutoCloseable {
        private MemorySegment handle;
        private final long revision;
        public GeometryResult(MemorySegment h, long rev) { this.handle = h; this.revision = rev; }
        public long getRevision() { return revision; }
        public boolean isValid() { return handle != null && !handle.equals(MemorySegment.NULL); }
        @Override public void close() {
            if (isValid()) {
                handle = MemorySegment.NULL;
            }
        }
    }

    public int evaluateFeature(GeometryModel model, int opKind) {
        if (!isAvailable()) throw new UnsupportedOperationException("Geometry kernel unavailable: " + loadError);
        return 0;
    }
}
