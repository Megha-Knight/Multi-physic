#include <stdio.h>
#include <assert.h>
#include <string.h>
#include <math.h>
#include "astra_geometry.h"

int main(void) {
    printf("=== Testing Astra Geometry ABI Contract & Milestone 3 Extrude ===\n");

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
    printf("  [PASS] Entity roles enum verified\n");

    /* 4. Runtime Model Lifecycle */
    astra_geom_model_handle *model = NULL;
    astra_geom_status st = astra_geom_model_create(&model);
    assert(st == ASTRA_GEOM_OK);
    assert(model != NULL);
    printf("  [PASS] astra_geom_model_create succeeded\n");

    /* 5. Evaluate Sketch Profile (Square: 0,0 to 10,10) */
    astra_id128 prof_id = { .high = 0x11111111ULL, .low = 0x22222222ULL };
    double square_uv[8] = { 0.0, 0.0,  10.0, 0.0,  10.0, 10.0,  0.0, 10.0 };
    uint32_t loop_offsets[2] = { 0, 4 };

    astra_geom_feature_request prof_req;
    memset(&prof_req, 0, sizeof(prof_req));
    prof_req.struct_size = (uint32_t)sizeof(astra_geom_feature_request);
    prof_req.operation_kind = ASTRA_OP_SKETCH_PROFILE;
    prof_req.feature_id = prof_id;
    prof_req.parameters.sketch_profile.struct_size = (uint32_t)sizeof(astra_geom_sketch_profile_params);
    prof_req.parameters.sketch_profile.loop_count = 1;
    prof_req.parameters.sketch_profile.point_count = 4;
    prof_req.parameters.sketch_profile.plane_origin_xyz_mm[0] = 0.0;
    prof_req.parameters.sketch_profile.plane_origin_xyz_mm[1] = 0.0;
    prof_req.parameters.sketch_profile.plane_origin_xyz_mm[2] = 0.0;
    prof_req.parameters.sketch_profile.plane_u_axis_xyz[0] = 1.0;
    prof_req.parameters.sketch_profile.plane_u_axis_xyz[1] = 0.0;
    prof_req.parameters.sketch_profile.plane_u_axis_xyz[2] = 0.0;
    prof_req.parameters.sketch_profile.plane_v_axis_xyz[0] = 0.0;
    prof_req.parameters.sketch_profile.plane_v_axis_xyz[1] = 1.0;
    prof_req.parameters.sketch_profile.plane_v_axis_xyz[2] = 0.0;
    prof_req.parameters.sketch_profile.coordinates_uv_mm = square_uv;
    prof_req.parameters.sketch_profile.loop_offsets = loop_offsets;

    astra_geom_result_handle *prof_res = NULL;
    st = astra_geom_evaluate_feature(model, &prof_req, &prof_res);
    assert(st == ASTRA_GEOM_OK);
    assert(prof_res != NULL);
    printf("  [PASS] astra_geom_evaluate_feature(SKETCH_PROFILE) succeeded\n");
    astra_geom_result_release(prof_res);

    /* 6. Evaluate Extrude (Height = 25.0 mm along +Z) */
    astra_id128 ext_id = { .high = 0x33333333ULL, .low = 0x44444444ULL };
    astra_geom_feature_request ext_req;
    memset(&ext_req, 0, sizeof(ext_req));
    ext_req.struct_size = (uint32_t)sizeof(astra_geom_feature_request);
    ext_req.operation_kind = ASTRA_OP_EXTRUDE;
    ext_req.feature_id = ext_id;
    ext_req.parameters.extrude.struct_size = (uint32_t)sizeof(astra_geom_extrude_params);
    ext_req.parameters.extrude.profile_feature_id = prof_id;
    ext_req.parameters.extrude.direction_xyz[0] = 0.0;
    ext_req.parameters.extrude.direction_xyz[1] = 0.0;
    ext_req.parameters.extrude.direction_xyz[2] = 1.0;
    ext_req.parameters.extrude.distance_mm = 25.0;

    astra_geom_result_handle *ext_res = NULL;
    st = astra_geom_evaluate_feature(model, &ext_req, &ext_res);
    assert(st == ASTRA_GEOM_OK);
    assert(ext_res != NULL);
    printf("  [PASS] astra_geom_evaluate_feature(EXTRUDE) succeeded\n");

    /* 7. Verify Tessellation View */
    astra_tessellation_view view;
    memset(&view, 0, sizeof(view));
    st = astra_geom_result_tessellation(ext_res, &view);
    assert(st == ASTRA_GEOM_OK);
    assert(view.vertex_count == 8);    /* 4 bottom, 4 top */
    assert(view.triangle_count == 12);  /* 2 start cap + 2 end cap + 8 lateral */
    assert(view.positions_xyz != NULL);
    assert(view.triangle_indices != NULL);
    assert(view.triangle_entities != NULL);
    printf("  [PASS] Tessellation view verified: 8 vertices, 12 triangles\n");

    /* 8. Verify Entity References Resolution */
    astra_entity_ref start_cap_query;
    memset(&start_cap_query, 0, sizeof(start_cap_query));
    start_cap_query.role = ASTRA_ROLE_END_CAP_START;
    start_cap_query.feature_id = ext_id;
    start_cap_query.sub_index = 0;
    uint32_t kind = 0;
    st = astra_geom_result_resolve_entity(ext_res, &start_cap_query, &kind);
    assert(st == ASTRA_GEOM_OK);
    assert(kind == 1); /* Face */
    printf("  [PASS] Start cap entity resolved\n");

    astra_entity_ref lat_query;
    memset(&lat_query, 0, sizeof(lat_query));
    lat_query.role = ASTRA_ROLE_LATERAL_FACE;
    lat_query.feature_id = ext_id;
    lat_query.sub_index = 2; /* 3rd profile edge */
    st = astra_geom_result_resolve_entity(ext_res, &lat_query, &kind);
    assert(st == ASTRA_GEOM_OK);
    printf("  [PASS] Lateral face entity (sub_index 2) resolved\n");

    /* 9. Verify Fail-Closed for Unsupported Operation (e.g. Boolean) */
    astra_geom_feature_request bool_req;
    memset(&bool_req, 0, sizeof(bool_req));
    bool_req.struct_size = (uint32_t)sizeof(astra_geom_feature_request);
    bool_req.operation_kind = ASTRA_OP_BOOLEAN;
    astra_geom_result_handle *bool_res = NULL;
    st = astra_geom_evaluate_feature(model, &bool_req, &bool_res);
    assert(st == ASTRA_GEOM_UNSUPPORTED_OPERATION);
    assert(bool_res == NULL);
    printf("  [PASS] Fail-closed verification for Boolean operation\n");

    /* 10. Clean Cleanup */
    astra_geom_result_release(ext_res);
    astra_geom_model_release(model);
    printf("  [PASS] Cleanup completed cleanly\n");

    printf("=== ALL ASTRA GEOMETRY MILESTONE 3 TESTS PASSED ===\n");
    return 0;
}
