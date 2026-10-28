#include <stdio.h>
#include <assert.h>
#include <string.h>
#include "astra_geometry.h"

int main(void) {
    printf("=== Testing Astra Geometry ABI Contract Declarations ===\n");

    /* 1. ABI Version */
    assert(ASTRA_GEOMETRY_ABI_VERSION == 1u);
    printf("  [PASS] ASTRA_GEOMETRY_ABI_VERSION == 1\n");

    /* 2. Status codes */
    assert(ASTRA_GEOM_OK == 0);
    assert(ASTRA_GEOM_INVALID_ARGUMENT == -100);
    assert(ASTRA_GEOM_UNSUPPORTED_OPERATION == -101);
    assert(ASTRA_GEOM_DEGENERATE_INPUT == -102);
    assert(ASTRA_GEOM_INVALID_TOPOLOGY == -103);
    assert(ASTRA_GEOM_ENTITY_NOT_FOUND == -104);
    assert(ASTRA_GEOM_ENTITY_CONSUMED == -105);
    assert(ASTRA_GEOM_ENTITY_AMBIGUOUS == -106);
    assert(ASTRA_GEOM_OUT_OF_MEMORY == -107);
    assert(ASTRA_GEOM_INTERNAL_ERROR == -108);
    assert(ASTRA_GEOM_BUFFER_TOO_SMALL == -109);
    assert(ASTRA_GEOM_ABI_MISMATCH == -110);
    printf("  [PASS] Status codes enum verified\n");

    /* 3. Entity Roles */
    assert(ASTRA_ROLE_UNSPECIFIED == 0);
    assert(ASTRA_ROLE_END_CAP_START == 1);
    assert(ASTRA_ROLE_END_CAP_END == 2);
    assert(ASTRA_ROLE_LATERAL_FACE == 3);
    assert(ASTRA_ROLE_INTERSECTION_EDGE == 4);
    assert(ASTRA_ROLE_REVOLVE_START_CAP == 5);
    assert(ASTRA_ROLE_REVOLVE_END_CAP == 6);
    assert(ASTRA_ROLE_SWEEP_SIDE == 7);
    assert(ASTRA_ROLE_SHELL_INNER_FACE == 8);
    assert(ASTRA_ROLE_HOLE_CYLINDRICAL_WALL == 9);
    assert(ASTRA_ROLE_HOLE_ENTRY_RIM == 10);
    printf("  [PASS] Entity roles enum verified\n");

    /* 4. Lineage Outcomes */
    assert(ASTRA_LINEAGE_PRESERVED == 1);
    assert(ASTRA_LINEAGE_SPLIT == 2);
    assert(ASTRA_LINEAGE_MERGED == 3);
    assert(ASTRA_LINEAGE_CONSUMED == 4);
    assert(ASTRA_LINEAGE_REPLACED == 5);
    assert(ASTRA_LINEAGE_UNRESOLVED == 6);
    printf("  [PASS] Lineage outcomes enum verified\n");

    /* 5. Struct definitions and field presence */
    astra_id128 id = { .high = 0x1234567890ABCDEFull, .low = 0xFEDCBA0987654321ull };
    assert(id.high == 0x1234567890ABCDEFull);
    assert(id.low == 0xFEDCBA0987654321ull);

    astra_entity_ref ref;
    memset(&ref, 0, sizeof(ref));
    ref.struct_size = (uint32_t)sizeof(astra_entity_ref);
    ref.role = ASTRA_ROLE_LATERAL_FACE;
    ref.feature_id = id;
    ref.sub_index = 3;
    assert(ref.sub_index == 3);
    assert(ref.role == ASTRA_ROLE_LATERAL_FACE);
    printf("  [PASS] astra_entity_ref layout verified\n");

    astra_lineage_record rec;
    memset(&rec, 0, sizeof(rec));
    rec.struct_size = (uint32_t)sizeof(astra_lineage_record);
    rec.outcome = ASTRA_LINEAGE_SPLIT;
    rec.source = ref;
    rec.successor_count = 2;
    assert(rec.outcome == ASTRA_LINEAGE_SPLIT);
    assert(rec.successor_count == 2);
    printf("  [PASS] astra_lineage_record layout verified\n");

    astra_geom_feature_request req;
    memset(&req, 0, sizeof(req));
    req.struct_size = (uint32_t)sizeof(astra_geom_feature_request);
    req.operation_kind = ASTRA_OP_EXTRUDE;
    req.feature_id = id;
    req.parameters.extrude.distance_mm = 50.0;
    assert(req.operation_kind == ASTRA_OP_EXTRUDE);
    assert(req.parameters.extrude.distance_mm == 50.0);
    printf("  [PASS] astra_geom_feature_request layout verified\n");

    astra_tessellation_view view;
    memset(&view, 0, sizeof(view));
    view.struct_size = (uint32_t)sizeof(astra_tessellation_view);
    view.vertex_count = 100;
    view.triangle_count = 50;
    assert(view.triangle_count == 50);
    printf("  [PASS] astra_tessellation_view layout verified\n");

    printf("=== ALL ASTRA GEOMETRY ABI DECLARATION TESTS PASSED ===\n");
    return 0;
}
