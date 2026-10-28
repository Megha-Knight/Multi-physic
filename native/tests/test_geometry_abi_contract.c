#include <stdio.h>
#include <assert.h>
#include <string.h>
#include "astra_geometry.h"

int main(void) {
    printf("=== Testing Astra Geometry ABI Contract Declarations & Runtime ===\n");

    /* 1. ABI Version */
    assert(ASTRA_GEOMETRY_ABI_VERSION == 1u);
    assert(astra_geometry_abi_version() == 1u);
    printf("  [PASS] ASTRA_GEOMETRY_ABI_VERSION == 1 (macro and runtime function)\n");

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

    /* 5. Runtime Model Lifecycle */
    printf("  [INFO] sizeof(astra_geom_feature_request) = %zu\n", sizeof(astra_geom_feature_request));
    astra_geom_model_handle *model = NULL;
    astra_geom_status st = astra_geom_model_create(&model);
    assert(st == ASTRA_GEOM_OK);
    assert(model != NULL);
    printf("  [PASS] astra_geom_model_create succeeded\n");

    /* 6. Feature Evaluation Fail-Closed Verification */
    astra_geom_feature_request req;
    memset(&req, 0, sizeof(req));
    req.struct_size = (uint32_t)sizeof(astra_geom_feature_request);
    req.operation_kind = ASTRA_OP_EXTRUDE;
    astra_geom_result_handle *res = NULL;
    st = astra_geom_evaluate_feature(model, &req, &res);
    assert(st == ASTRA_GEOM_UNSUPPORTED_OPERATION);
    assert(res == NULL);
    printf("  [PASS] astra_geom_evaluate_feature returned ASTRA_GEOM_UNSUPPORTED_OPERATION\n");

    /* 7. Diagnostic Reporting */
    char err_buf[256];
    uint32_t req_bytes = 0;
    st = astra_geometry_last_error(err_buf, sizeof(err_buf), &req_bytes);
    assert(st == ASTRA_GEOM_OK);
    assert(req_bytes > 0);
    assert(strstr(err_buf, "B-rep evaluation not yet implemented") != NULL);
    printf("  [PASS] astra_geometry_last_error populated diagnostic text: '%s'\n", err_buf);

    /* 8. Small buffer diagnostic check */
    st = astra_geometry_last_error(err_buf, 4, &req_bytes);
    assert(st == ASTRA_GEOM_BUFFER_TOO_SMALL);
    printf("  [PASS] astra_geometry_last_error handles buffer-too-small correctly\n");

    /* 9. Model Release */
    astra_geom_model_release(model);
    printf("  [PASS] astra_geom_model_release completed cleanly\n");

    printf("=== ALL ASTRA GEOMETRY ABI DECLARATION & RUNTIME TESTS PASSED ===\n");
    return 0;
}
