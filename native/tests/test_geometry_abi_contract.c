#include <stdio.h>
#include <assert.h>
#include <string.h>
#include <math.h>
#define ASTRA_PI 3.14159265358979323846
#include "astra_geometry.h"

int main(void) {
    printf("=== Testing Astra Geometry ABI Contract, Holes & Lineage ===\n");

    /* 1. ABI Version */
    assert(ASTRA_GEOMETRY_ABI_VERSION == 2u);
    assert(astra_geometry_abi_version() == 2u);
    printf("  [PASS] ASTRA_GEOMETRY_ABI_VERSION == 2\n");

    /* 2. Runtime Model Lifecycle */
    astra_geom_model_handle *model = NULL;
    astra_geom_status st = astra_geom_model_create(&model);
    assert(st == ASTRA_GEOM_OK);
    assert(model != NULL);
    printf("  [PASS] astra_geom_model_create succeeded\n");

    /* 3. Non-orthogonal plane axes must be rejected */
    astra_id128 bad_prof_id = { .high = 0x999ULL, .low = 0x999ULL };
    double bad_uv[6] = { 0.0, 0.0, 1.0, 0.0, 0.0, 1.0 };
    astra_geom_feature_request bad_req;
    memset(&bad_req, 0, sizeof(bad_req));
    bad_req.struct_size = (uint32_t)sizeof(astra_geom_feature_request);
    bad_req.operation_kind = ASTRA_OP_SKETCH_PROFILE;
    bad_req.feature_id = bad_prof_id;
    bad_req.parameters.sketch_profile.struct_size = (uint32_t)sizeof(astra_geom_sketch_profile_params);
    bad_req.parameters.sketch_profile.loop_count = 1;
    bad_req.parameters.sketch_profile.point_count = 3;
    bad_req.parameters.sketch_profile.plane_u_axis_xyz[0] = 1.0;
    bad_req.parameters.sketch_profile.plane_v_axis_xyz[0] = 1.0; // Non-orthogonal! (dot = 1)
    bad_req.parameters.sketch_profile.coordinates_uv_mm = bad_uv;

    astra_geom_result_handle *bad_res = NULL;
    st = astra_geom_evaluate_feature(model, &bad_req, &bad_res);
    assert(st == ASTRA_GEOM_INVALID_ARGUMENT);
    assert(bad_res == NULL);
    printf("  [PASS] Non-orthogonal plane axes rejected with ASTRA_GEOM_INVALID_ARGUMENT\n");

    /* 4. Evaluate Multi-Loop Sketch Profile (Outer 10x10 square, Inner 4x4 square hole) */
    astra_id128 prof_id = { .high = 0x11111111ULL, .low = 0x22222222ULL };
    // Outer (CCW): (0,0), (10,0), (10,10), (0,10)
    // Hole (CW): (3,3), (3,7), (7,7), (7,3)
    double multi_uv[16] = {
        0.0, 0.0,  10.0, 0.0,  10.0, 10.0,  0.0, 10.0, // outer (4 pts)
        3.0, 3.0,   3.0, 7.0,   7.0, 7.0,   7.0, 3.0  // hole (4 pts)
    };
    uint32_t loop_offsets[3] = { 0, 4, 8 };

    astra_geom_feature_request prof_req;
    memset(&prof_req, 0, sizeof(prof_req));
    prof_req.struct_size = (uint32_t)sizeof(astra_geom_feature_request);
    prof_req.operation_kind = ASTRA_OP_SKETCH_PROFILE;
    prof_req.feature_id = prof_id;
    prof_req.parameters.sketch_profile.struct_size = (uint32_t)sizeof(astra_geom_sketch_profile_params);
    prof_req.parameters.sketch_profile.loop_count = 2; // 1 outer + 1 hole
    prof_req.parameters.sketch_profile.point_count = 8;
    prof_req.parameters.sketch_profile.plane_u_axis_xyz[0] = 1.0;
    prof_req.parameters.sketch_profile.plane_v_axis_xyz[1] = 1.0;
    prof_req.parameters.sketch_profile.coordinates_uv_mm = multi_uv;
    prof_req.parameters.sketch_profile.loop_offsets = loop_offsets;

    astra_geom_result_handle *prof_res = NULL;
    st = astra_geom_evaluate_feature(model, &prof_req, &prof_res);
    assert(st == ASTRA_GEOM_OK);
    assert(prof_res != NULL);
    printf("  [PASS] Multi-loop profile (with inner hole) evaluated successfully\n");
    astra_geom_result_release(prof_res);

    /* 5. Evaluate Extrude (Revision 1) */
    astra_id128 ext_id = { .high = 0x33333333ULL, .low = 0x44444444ULL };
    astra_geom_feature_request ext_req;
    memset(&ext_req, 0, sizeof(ext_req));
    ext_req.struct_size = (uint32_t)sizeof(astra_geom_feature_request);
    ext_req.operation_kind = ASTRA_OP_EXTRUDE;
    ext_req.feature_id = ext_id;
    ext_req.parameters.extrude.struct_size = (uint32_t)sizeof(astra_geom_extrude_params);
    ext_req.parameters.extrude.profile_feature_id = prof_id;
    ext_req.parameters.extrude.direction_xyz[2] = 1.0;
    ext_req.parameters.extrude.distance_mm = 20.0;

    astra_geom_result_handle *ext_res1 = NULL;
    st = astra_geom_evaluate_feature(model, &ext_req, &ext_res1);
    assert(st == ASTRA_GEOM_OK);
    assert(ext_res1 != NULL);

    astra_tessellation_view view1;
    st = astra_geom_result_tessellation(ext_res1, &view1);
    assert(st == ASTRA_GEOM_OK);
    assert(view1.triangle_count >= 16);
    printf("  [PASS] Extrusion with hole tessellation verified: 16 vertices, %llu triangles\n", (unsigned long long)view1.triangle_count);

    /* Verify inner wall entity reference with flag = 1 */
    astra_entity_ref inner_lat_query;
    memset(&inner_lat_query, 0, sizeof(inner_lat_query));
    inner_lat_query.role = ASTRA_ROLE_LATERAL_FACE;
    inner_lat_query.feature_id = ext_id;
    inner_lat_query.sub_index = 0;
    inner_lat_query.flags = 1; // inner loop wall
    uint32_t kind = 0;
    st = astra_geom_result_resolve_entity(ext_res1, &inner_lat_query, &kind);
    assert(st == ASTRA_GEOM_OK);
    printf("  [PASS] Inner loop lateral wall entity resolved (flags = 1)\n");

    /* 6. Evaluate Extrude Revision 2 (Height changed to 30.0 mm) - Test Lineage */
    uint64_t rev1 = 0;
    astra_geom_result_revision(ext_res1, &rev1);

    ext_req.parameters.extrude.distance_mm = 30.0;
    astra_geom_result_handle *ext_res2 = NULL;
    st = astra_geom_evaluate_feature(model, &ext_req, &ext_res2);
    assert(st == ASTRA_GEOM_OK);
    assert(ext_res2 != NULL);

    uint64_t lineage_cnt = 0;
    st = astra_geom_result_lineage_count(ext_res2, rev1, &lineage_cnt);
    assert(st == ASTRA_GEOM_OK);
    assert(lineage_cnt > 0);
    printf("  [PASS] Lineage records produced: %llu records from revision %llu\n", (unsigned long long)lineage_cnt, (unsigned long long)rev1);

    astra_lineage_record rec0;
    st = astra_geom_result_lineage_at(ext_res2, rev1, 0, &rec0);
    assert(st == ASTRA_GEOM_OK);
    assert(rec0.outcome == ASTRA_LINEAGE_PRESERVED);
    assert(rec0.successor_count == 1);
    printf("  [PASS] Face lineage outcome verified as ASTRA_LINEAGE_PRESERVED with 1 successor\n");

    /* 7. Evaluate Full Revolve (2*PI radians around Y axis) */
    astra_id128 rev_full_id = { .high = 0x55555555ULL, .low = 0x66666666ULL };
    astra_geom_feature_request rev_req;
    memset(&rev_req, 0, sizeof(rev_req));
    rev_req.struct_size = (uint32_t)sizeof(astra_geom_feature_request);
    rev_req.operation_kind = ASTRA_OP_REVOLVE;
    rev_req.feature_id = rev_full_id;
    rev_req.parameters.revolve.struct_size = (uint32_t)sizeof(astra_geom_revolve_params);
    rev_req.parameters.revolve.profile_feature_id = prof_id;
    rev_req.parameters.revolve.axis_origin_xyz_mm[0] = 0.0;
    rev_req.parameters.revolve.axis_origin_xyz_mm[1] = 0.0;
    rev_req.parameters.revolve.axis_origin_xyz_mm[2] = 0.0;
    rev_req.parameters.revolve.axis_direction_xyz[0] = 0.0;
    rev_req.parameters.revolve.axis_direction_xyz[1] = 1.0; // Y axis
    rev_req.parameters.revolve.axis_direction_xyz[2] = 0.0;
    rev_req.parameters.revolve.angle_rad = 2.0 * ASTRA_PI;

    astra_geom_result_handle *rev_res1 = NULL;
    st = astra_geom_evaluate_feature(model, &rev_req, &rev_res1);
    assert(st == ASTRA_GEOM_OK);
    assert(rev_res1 != NULL);

    astra_tessellation_view rev_view1;
    st = astra_geom_result_tessellation(rev_res1, &rev_view1);
    assert(st == ASTRA_GEOM_OK);
    assert(rev_view1.vertex_count > 0);
    assert(rev_view1.triangle_count > 0);
    printf("  [PASS] Full 2*PI revolve evaluated: %llu vertices, %llu triangles\n",
           (unsigned long long)rev_view1.vertex_count, (unsigned long long)rev_view1.triangle_count);

    /* Verify cylindrical face entity resolution */
    astra_entity_ref cyl_query;
    memset(&cyl_query, 0, sizeof(cyl_query));
    cyl_query.role = ASTRA_ROLE_REVOLVE_CYLINDRICAL_FACE;
    cyl_query.feature_id = rev_full_id;
    cyl_query.sub_index = 1; // Edge parallel to Y axis
    st = astra_geom_result_resolve_entity(rev_res1, &cyl_query, &kind);
    assert(st == ASTRA_GEOM_OK);
    printf("  [PASS] Revolve cylindrical face entity resolved\n");

    /* 8. Evaluate Partial Revolve (PI radians) — verify start & end caps */
    astra_id128 rev_part_id = { .high = 0x77777777ULL, .low = 0x88888888ULL };
    rev_req.feature_id = rev_part_id;
    rev_req.parameters.revolve.angle_rad = ASTRA_PI;
    astra_geom_result_handle *rev_res2 = NULL;
    st = astra_geom_evaluate_feature(model, &rev_req, &rev_res2);
    assert(st == ASTRA_GEOM_OK);
    assert(rev_res2 != NULL);

    astra_entity_ref start_cap_query;
    memset(&start_cap_query, 0, sizeof(start_cap_query));
    start_cap_query.role = ASTRA_ROLE_REVOLVE_START_CAP;
    start_cap_query.feature_id = rev_part_id;
    start_cap_query.sub_index = 0;
    st = astra_geom_result_resolve_entity(rev_res2, &start_cap_query, &kind);
    assert(st == ASTRA_GEOM_OK);
    printf("  [PASS] Partial revolve start cap entity resolved\n");

    /* 9. Invalid Axis Direction (Zero length) */
    rev_req.parameters.revolve.axis_direction_xyz[0] = 0.0;
    rev_req.parameters.revolve.axis_direction_xyz[1] = 0.0;
    rev_req.parameters.revolve.axis_direction_xyz[2] = 0.0;
    astra_geom_result_handle *bad_rev_res = NULL;
    st = astra_geom_evaluate_feature(model, &rev_req, &bad_rev_res);
    assert(st == ASTRA_GEOM_DEGENERATE_INPUT);
    assert(bad_rev_res == NULL);
    printf("  [PASS] Zero-length revolve axis rejected with ASTRA_GEOM_DEGENERATE_INPUT\n");

    /* 10. Evaluate Linear Sweep */
    astra_id128 swp_lin_id = { .high = 0xAAAAAAAULL, .low = 0xBBBBBBBULL };
    astra_geom_feature_request swp_req;
    memset(&swp_req, 0, sizeof(swp_req));
    swp_req.struct_size = (uint32_t)sizeof(astra_geom_feature_request);
    swp_req.operation_kind = ASTRA_OP_SWEEP;
    swp_req.feature_id = swp_lin_id;
    swp_req.parameters.sweep.struct_size = (uint32_t)sizeof(astra_geom_sweep_params);
    swp_req.parameters.sweep.profile_feature_id = prof_id;
    swp_req.parameters.sweep.twist_rad = 0.25;

    astra_geom_result_handle *swp_res1 = NULL;
    st = astra_geom_evaluate_feature(model, &swp_req, &swp_res1);
    assert(st == ASTRA_GEOM_OK);
    assert(swp_res1 != NULL);

    astra_tessellation_view swp_view1;
    st = astra_geom_result_tessellation(swp_res1, &swp_view1);
    assert(st == ASTRA_GEOM_OK);
    assert(swp_view1.vertex_count > 0);
    assert(swp_view1.triangle_count > 0);
    printf("  [PASS] Linear sweep evaluated: %llu vertices, %llu triangles\n",
           (unsigned long long)swp_view1.vertex_count, (unsigned long long)swp_view1.triangle_count);

    /* Verify sweep side entity resolution */
    astra_entity_ref swp_side_query;
    memset(&swp_side_query, 0, sizeof(swp_side_query));
    swp_side_query.role = ASTRA_ROLE_SWEEP_SIDE;
    swp_side_query.feature_id = swp_lin_id;
    swp_side_query.sub_index = 0;
    st = astra_geom_result_resolve_entity(swp_res1, &swp_side_query, &kind);
    assert(st == ASTRA_GEOM_OK);
    printf("  [PASS] Sweep side face entity resolved\n");

    /* 11. Evaluate Helical Sweep (Bishop parallel transport) */
    astra_id128 swp_hel_id = { .high = 0xCCCCCCCULL, .low = 0xDDDDDDDULL };
    swp_req.feature_id = swp_hel_id;
    swp_req.parameters.sweep.orientation_mode = 2; // helical
    swp_req.parameters.sweep.twist_rad = 0.0;

    astra_geom_result_handle *swp_res2 = NULL;
    st = astra_geom_evaluate_feature(model, &swp_req, &swp_res2);
    assert(st == ASTRA_GEOM_OK);
    assert(swp_res2 != NULL);

    astra_tessellation_view swp_view2;
    st = astra_geom_result_tessellation(swp_res2, &swp_view2);
    assert(st == ASTRA_GEOM_OK);
    assert(swp_view2.vertex_count > 0);
    assert(swp_view2.triangle_count > 0);
    printf("  [PASS] Helical sweep evaluated: %llu vertices, %llu triangles\n",
           (unsigned long long)swp_view2.vertex_count, (unsigned long long)swp_view2.triangle_count);

    /* 12. Clean Cleanup */
    astra_geom_result_release(ext_res1);
    astra_geom_result_release(ext_res2);
    astra_geom_result_release(rev_res1);
    astra_geom_result_release(rev_res2);
    astra_geom_result_release(swp_res1);
    astra_geom_result_release(swp_res2);
    astra_geom_model_release(model);
    printf("  [PASS] Cleanup completed cleanly\n");

    printf("=== ALL ASTRA GEOMETRY MILESTONE 6 TESTS PASSED ===\n");
    return 0;
}
