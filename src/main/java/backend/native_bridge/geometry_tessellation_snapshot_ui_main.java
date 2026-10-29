package backend.native_bridge;

import ui.workspace.drafting.features.entity_lineage_contract_ui_main.EntityRef;

public final class geometry_tessellation_snapshot_ui_main {
    private final long revision;
    private final float[] positions;
    private final int[] indices;
    private final EntityRef[] triangleEntities;
    private final int vertexCount;
    private final int triangleCount;

    public geometry_tessellation_snapshot_ui_main(long revision, float[] positions, int[] indices,
                                                  EntityRef[] triangleEntities, int vertexCount, int triangleCount) {
        this.revision = revision;
        this.positions = positions != null ? positions.clone() : new float[0];
        this.indices = indices != null ? indices.clone() : new int[0];
        this.triangleEntities = triangleEntities != null ? triangleEntities.clone() : new EntityRef[0];
        this.vertexCount = vertexCount;
        this.triangleCount = triangleCount;
    }

    public long getRevision() { return revision; }
    public float[] getPositions() { return positions.clone(); }
    public int[] getIndices() { return indices.clone(); }
    public EntityRef[] getTriangleEntities() { return triangleEntities.clone(); }
    public EntityRef getEntityForTriangle(int triIdx) {
        if (triIdx >= 0 && triIdx < triangleEntities.length) return triangleEntities[triIdx];
        return null;
    }
    public int getVertexCount() { return vertexCount; }
    public int getTriangleCount() { return triangleCount; }
}
