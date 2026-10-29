package backend.native_bridge;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import ui.workspace.drafting.features.entity_lineage_contract_ui_main.EntityRef;
import ui.workspace.drafting.features.entity_lineage_contract_ui_main.Id128;

public final class astra_geometry_native_bridge_ui_main {
    private static final astra_geometry_native_bridge_ui_main INSTANCE = new astra_geometry_native_bridge_ui_main();
    private boolean available = false;
    private String loadError = null;
    private MethodHandle mhAbiVer, mhLastErr, mhModelCreate, mhModelRelease, mhEvaluateFeature, mhResultRelease;
    private MethodHandle mhResultRevision, mhResultTessellation, mhResolveEntity, mhLineageCount, mhLineageAt, mhLineageSucc;

    private astra_geometry_native_bridge_ui_main() { init(); }

    public static astra_geometry_native_bridge_ui_main getInstance() { return INSTANCE; }
    public boolean isAvailable() { return available; }
    public String getLoadError() { return loadError; }

    private void init() {
        try {
            Path dllPath = astra_native_bridge_ui_main.getInstance().resolveLibraryPath();
            if (dllPath == null || !Files.exists(dllPath)) { loadError = "Native dynamic library not found"; return; }
            SymbolLookup l = SymbolLookup.libraryLookup(dllPath, Arena.ofAuto());
            Linker lnk = Linker.nativeLinker();
            mhAbiVer = lnk.downcallHandle(l.find("astra_geometry_abi_version").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT));
            mhLastErr = lnk.downcallHandle(l.find("astra_geometry_last_error").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            mhModelCreate = lnk.downcallHandle(l.find("astra_geom_model_create").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            mhModelRelease = lnk.downcallHandle(l.find("astra_geom_model_release").orElseThrow(), FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
            mhEvaluateFeature = lnk.downcallHandle(l.find("astra_geom_evaluate_feature").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            mhResultRelease = lnk.downcallHandle(l.find("astra_geom_result_release").orElseThrow(), FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
            mhResultRevision = lnk.downcallHandle(l.find("astra_geom_result_revision").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            mhResultTessellation = lnk.downcallHandle(l.find("astra_geom_result_tessellation").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            mhResolveEntity = lnk.downcallHandle(l.find("astra_geom_result_resolve_entity").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            mhLineageCount = lnk.downcallHandle(l.find("astra_geom_result_lineage_count").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
            mhLineageAt = lnk.downcallHandle(l.find("astra_geom_result_lineage_at").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
            mhLineageSucc = lnk.downcallHandle(l.find("astra_geom_result_lineage_successor_at").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

            int ver = (int) mhAbiVer.invokeExact();
            if (ver != 1) { loadError = "ABI version mismatch: " + ver; return; }
            available = true;
        } catch (Throwable t) { loadError = t.getMessage(); available = false; }
    }

    private void ensureAvailable() { if (!available) throw new UnsupportedOperationException("Geometry kernel unavailable: " + loadError); }
    public int getAbiVersion() { ensureAvailable(); try { return (int) mhAbiVer.invokeExact(); } catch (Throwable t) { throw new RuntimeException(t); } }

    public String getLastError() {
        if (!available) return loadError;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(512); MemorySegment req = arena.allocate(ValueLayout.JAVA_INT);
            int rc = (int) mhLastErr.invokeExact(buf, 512, req);
            return rc == 0 ? buf.getString(0) : "Failed to retrieve last error (code " + rc + ")";
        } catch (Throwable t) { return "Error invoking astra_geometry_last_error: " + t.getMessage(); }
    }

    public GeometryModel createModel() {
        ensureAvailable();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment ptr = arena.allocate(ValueLayout.ADDRESS);
            int rc = (int) mhModelCreate.invokeExact(ptr);
            if (rc != 0) throw new RuntimeException("astra_geom_model_create failed (code " + rc + "): " + getLastError());
            return new GeometryModel(ptr.get(ValueLayout.ADDRESS, 0));
        } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public GeometryResult evaluateFeature(GeometryModel model, MemorySegment request) {
        ensureAvailable();
        if (model == null || !model.isValid()) throw new IllegalArgumentException("Invalid geometry model handle");
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outRes = arena.allocate(ValueLayout.ADDRESS);
            int rc = (int) mhEvaluateFeature.invokeExact(model.handle, request, outRes);
            if (rc != 0) throw new UnsupportedOperationException("evaluateFeature failed (code " + rc + "): " + getLastError());
            MemorySegment resHandle = outRes.get(ValueLayout.ADDRESS, 0);
            MemorySegment revPtr = arena.allocate(ValueLayout.JAVA_LONG);
            int rcRev = (int) mhResultRevision.invokeExact(resHandle, revPtr);
            return new GeometryResult(resHandle, revPtr.get(ValueLayout.JAVA_LONG, 0));
        } catch (UnsupportedOperationException e) { throw e; } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public geometry_tessellation_snapshot_ui_main copyTessellationSnapshot(GeometryResult res) {
        ensureAvailable();
        if (res == null || !res.isValid()) return null;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment view = arena.allocate(56); // sizeof(astra_tessellation_view)
            int rc = (int) mhResultTessellation.invokeExact(res.handle, view);
            if (rc != 0) throw new RuntimeException("astra_geom_result_tessellation failed: " + getLastError());
            long rev = view.get(ValueLayout.JAVA_LONG, 8);
            long vCount = view.get(ValueLayout.JAVA_LONG, 16);
            long tCount = view.get(ValueLayout.JAVA_LONG, 24);
            MemorySegment posPtr = view.get(ValueLayout.ADDRESS, 32);
            MemorySegment idxPtr = view.get(ValueLayout.ADDRESS, 40);
            MemorySegment entPtr = view.get(ValueLayout.ADDRESS, 48);

            float[] positions = new float[(int) vCount * 3];
            int[] indices = new int[(int) tCount * 3];
            EntityRef[] entities = new EntityRef[(int) tCount];

            MemorySegment posSeg = posPtr.reinterpret(positions.length * 4L);
            MemorySegment.copy(posSeg, ValueLayout.JAVA_FLOAT, 0, positions, 0, positions.length);

            MemorySegment idxSeg = idxPtr.reinterpret(indices.length * 4L);
            for (int i = 0; i < indices.length; i++) indices[i] = (int) idxSeg.get(ValueLayout.JAVA_INT, i * 4L);

            MemorySegment entSeg = entPtr.reinterpret(tCount * 48L); // sizeof(astra_entity_ref) = 48
            for (int i = 0; i < (int) tCount; i++) {
                long offset = i * 48L;
                int role = entSeg.get(ValueLayout.JAVA_INT, offset + 4L);
                long fHigh = entSeg.get(ValueLayout.JAVA_LONG, offset + 8L);
                long fLow = entSeg.get(ValueLayout.JAVA_LONG, offset + 16L);
                int subIdx = entSeg.get(ValueLayout.JAVA_INT, offset + 24L);
                int flags = entSeg.get(ValueLayout.JAVA_INT, offset + 28L);
                long rHigh = entSeg.get(ValueLayout.JAVA_LONG, offset + 32L);
                long rLow = entSeg.get(ValueLayout.JAVA_LONG, offset + 40L);
                entities[i] = new EntityRef(new Id128(fHigh, fLow), role, subIdx, flags, new Id128(rHigh, rLow));
            }
            return new geometry_tessellation_snapshot_ui_main(rev, positions, indices, entities, (int) vCount, (int) tCount);
        } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public boolean resolveEntity(GeometryResult res, EntityRef ref) {
        ensureAvailable();
        if (res == null || !res.isValid() || ref == null) return false;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment refSeg = arena.allocate(48);
            refSeg.set(ValueLayout.JAVA_INT, 0, 48);
            refSeg.set(ValueLayout.JAVA_INT, 4, ref.role());
            refSeg.set(ValueLayout.JAVA_LONG, 8, ref.featureId().high());
            refSeg.set(ValueLayout.JAVA_LONG, 16, ref.featureId().low());
            refSeg.set(ValueLayout.JAVA_INT, 24, ref.subIndex());
            refSeg.set(ValueLayout.JAVA_INT, 28, ref.flags());
            refSeg.set(ValueLayout.JAVA_LONG, 32, ref.relatedFeatureId().high());
            refSeg.set(ValueLayout.JAVA_LONG, 40, ref.relatedFeatureId().low());
            MemorySegment kindPtr = arena.allocate(ValueLayout.JAVA_INT);
            int rc = (int) mhResolveEntity.invokeExact(res.handle, refSeg, kindPtr);
            return rc == 0;
        } catch (Throwable t) { return false; }
    }

    public static final class GeometryModel implements AutoCloseable {
        private MemorySegment handle; private boolean closed = false;
        public GeometryModel(MemorySegment h) { this.handle = h; }
        public boolean isValid() { return !closed && handle != null && !handle.equals(MemorySegment.NULL); }
        public MemorySegment getHandle() { return handle; }
        @Override public synchronized void close() {
            if (!closed && isValid()) { closed = true; try { INSTANCE.mhModelRelease.invokeExact(handle); } catch (Throwable ignored) {} handle = MemorySegment.NULL; }
        }
    }

    public static final class GeometryResult implements AutoCloseable {
        private MemorySegment handle; private final long revision; private boolean closed = false;
        public GeometryResult(MemorySegment h, long rev) { this.handle = h; this.revision = rev; }
        public long getRevision() { return revision; }
        public boolean isValid() { return !closed && handle != null && !handle.equals(MemorySegment.NULL); }
        @Override public synchronized void close() {
            if (!closed && isValid()) { closed = true; try { INSTANCE.mhResultRelease.invokeExact(handle); } catch (Throwable ignored) {} handle = MemorySegment.NULL; }
        }
    }
}
