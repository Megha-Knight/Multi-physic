#ifndef ASTRA_GEOMETRY_H
#define ASTRA_GEOMETRY_H

#include <stdint.h>
#include <stddef.h>
#include "astra_native.h"

#ifdef __cplusplus
extern "C" {
#endif

#define ASTRA_GEOMETRY_ABI_VERSION 1u
#define ASTRA_GEOMETRY_ROLE_NAME_CAPACITY 48u

typedef int32_t astra_geom_status;
typedef enum astra_geom_status_code {
    ASTRA_GEOM_OK = 0,
    ASTRA_GEOM_INVALID_ARGUMENT = -100,
    ASTRA_GEOM_UNSUPPORTED_OPERATION = -101,
    ASTRA_GEOM_DEGENERATE_INPUT = -102,
    ASTRA_GEOM_INVALID_TOPOLOGY = -103,
    ASTRA_GEOM_ENTITY_NOT_FOUND = -104,
    ASTRA_GEOM_ENTITY_CONSUMED = -105,
    ASTRA_GEOM_ENTITY_AMBIGUOUS = -106,
    ASTRA_GEOM_OUT_OF_MEMORY = -107,
    ASTRA_GEOM_INTERNAL_ERROR = -108,
    ASTRA_GEOM_BUFFER_TOO_SMALL = -109,
    ASTRA_GEOM_ABI_MISMATCH = -110
} astra_geom_status_code;

typedef struct astra_geom_model_handle astra_geom_model_handle;
typedef struct astra_geom_body_handle astra_geom_body_handle;
typedef struct astra_geom_result_handle astra_geom_result_handle;

typedef struct astra_id128 {
    uint64_t high;
    uint64_t low;
} astra_id128;

typedef enum astra_entity_role {
    ASTRA_ROLE_UNSPECIFIED = 0,
    ASTRA_ROLE_END_CAP_START = 1,
    ASTRA_ROLE_END_CAP_END = 2,
    ASTRA_ROLE_LATERAL_FACE = 3,
    ASTRA_ROLE_INTERSECTION_EDGE = 4,
    ASTRA_ROLE_REVOLVE_START_CAP = 5,
    ASTRA_ROLE_REVOLVE_END_CAP = 6,
    ASTRA_ROLE_SWEEP_SIDE = 7,
    ASTRA_ROLE_SHELL_INNER_FACE = 8,
    ASTRA_ROLE_HOLE_CYLINDRICAL_WALL = 9,
    ASTRA_ROLE_HOLE_ENTRY_RIM = 10,
    ASTRA_ROLE_REVOLVE_CYLINDRICAL_FACE = 11,
    ASTRA_ROLE_REVOLVE_LATERAL_FACE = 12,
    ASTRA_ROLE_UNCLASSIFIED_DERIVED_ENTITY = 255
} astra_entity_role;

typedef struct astra_entity_ref {
    uint32_t struct_size;
    uint32_t role;
    astra_id128 feature_id;
    int32_t sub_index;
    uint32_t flags;
    astra_id128 related_feature_id;
} astra_entity_ref;

typedef enum astra_lineage_outcome {
    ASTRA_LINEAGE_PRESERVED = 1,
    ASTRA_LINEAGE_SPLIT = 2,
    ASTRA_LINEAGE_MERGED = 3,
    ASTRA_LINEAGE_CONSUMED = 4,
    ASTRA_LINEAGE_REPLACED = 5,
    ASTRA_LINEAGE_UNRESOLVED = 6
} astra_lineage_outcome;

typedef struct astra_lineage_record {
    uint32_t struct_size;
    uint32_t outcome;
    uint64_t source_revision;
    uint64_t destination_revision;
    astra_entity_ref source;
    uint32_t successor_count;
    uint32_t reserved;
} astra_lineage_record;

typedef enum astra_geom_operation_kind {
    ASTRA_OP_SKETCH_PROFILE = 1,
    ASTRA_OP_BASE_PRIMITIVE = 2,
    ASTRA_OP_EXTRUDE = 3,
    ASTRA_OP_REVOLVE = 4,
    ASTRA_OP_SWEEP = 5,
    ASTRA_OP_LOFT = 6,
    ASTRA_OP_BOOLEAN = 7,
    ASTRA_OP_HOLE = 8,
    ASTRA_OP_HOLE_PATTERN = 9,
    ASTRA_OP_SHELL = 10,
    ASTRA_OP_FILLET = 11,
    ASTRA_OP_CHAMFER = 12,
    ASTRA_OP_DRAFT = 13
} astra_geom_operation_kind;

typedef enum astra_geom_boolean_mode {
    ASTRA_BOOLEAN_UNION = 1,
    ASTRA_BOOLEAN_SUBTRACT = 2,
    ASTRA_BOOLEAN_INTERSECT = 3
} astra_geom_boolean_mode;

typedef struct astra_geom_sketch_profile_params {
    uint32_t struct_size;
    uint32_t loop_count;
    uint32_t point_count;
    uint32_t flags;
    double plane_origin_xyz_mm[3];
    double plane_u_axis_xyz[3];
    double plane_v_axis_xyz[3];
    const double *coordinates_uv_mm;
    const uint32_t *loop_offsets;
} astra_geom_sketch_profile_params;

typedef struct astra_geom_base_primitive_params {
    uint32_t struct_size;
    uint32_t primitive_kind;
    double origin_xyz_mm[3];
    double dimensions_mm[3];
    double radius_mm;
    double height_mm;
} astra_geom_base_primitive_params;

typedef struct astra_geom_extrude_params {
    uint32_t struct_size;
    astra_id128 profile_feature_id;
    double direction_xyz[3];
    double distance_mm;
    uint32_t operation_mode;
    uint32_t reserved;
} astra_geom_extrude_params;

typedef struct astra_geom_revolve_params {
    uint32_t struct_size;
    astra_id128 profile_feature_id;
    double axis_origin_xyz_mm[3];
    double axis_direction_xyz[3];
    double angle_rad;
    uint32_t operation_mode;
    uint32_t reserved;
} astra_geom_revolve_params;

typedef struct astra_geom_sweep_params {
    uint32_t struct_size;
    astra_id128 profile_feature_id;
    astra_id128 path_feature_id;
    double twist_rad;
    uint32_t orientation_mode;
    uint32_t operation_mode;
} astra_geom_sweep_params;

typedef struct astra_geom_loft_params {
    uint32_t struct_size;
    uint32_t section_count;
    uint32_t guide_count;
    uint32_t operation_mode;
} astra_geom_loft_params;

typedef struct astra_geom_boolean_params {
    uint32_t struct_size;
    uint32_t boolean_mode;
    uint32_t reserved[2];
} astra_geom_boolean_params;

typedef struct astra_geom_hole_params {
    uint32_t struct_size;
    astra_entity_ref placement_face;
    double axis_direction_xyz[3];
    double diameter_mm;
    double depth_mm;
    double counterbore_diameter_mm;
    double counterbore_depth_mm;
    double countersink_diameter_mm;
    double countersink_angle_rad;
    uint32_t hole_kind;
    uint32_t termination_mode;
} astra_geom_hole_params;

typedef struct astra_geom_hole_pattern_params {
    uint32_t struct_size;
    uint32_t pattern_kind;
    uint32_t instance_count;
    uint32_t reserved;
    double spacing_mm;
    double total_angle_rad;
    double axis_origin_xyz_mm[3];
    double axis_direction_xyz[3];
} astra_geom_hole_pattern_params;

typedef struct astra_geom_shell_params {
    uint32_t struct_size;
    double thickness_mm;
    uint32_t removed_face_count;
    uint32_t direction_mode;
} astra_geom_shell_params;

typedef struct astra_geom_machining_params {
    uint32_t struct_size;
    double primary_size_mm;
    double angle_rad;
    uint32_t selected_entity_count;
    uint32_t machining_kind;
} astra_geom_machining_params;

typedef struct astra_geom_input_ref {
    uint32_t struct_size;
    uint32_t input_role;
    astra_id128 feature_id;
    uint64_t geometry_revision;
    astra_entity_ref entity;
} astra_geom_input_ref;

typedef struct astra_geom_feature_request {
    uint32_t struct_size;
    uint32_t operation_kind;
    astra_id128 feature_id;
    uint64_t expected_source_revision;
    const astra_geom_input_ref *inputs;
    uint32_t input_count;
    uint32_t parameters_version;
    union {
        astra_geom_sketch_profile_params sketch_profile;
        astra_geom_base_primitive_params base_primitive;
        astra_geom_extrude_params extrude;
        astra_geom_revolve_params revolve;
        astra_geom_sweep_params sweep;
        astra_geom_loft_params loft;
        astra_geom_boolean_params boolean_op;
        astra_geom_hole_params hole;
        astra_geom_hole_pattern_params hole_pattern;
        astra_geom_shell_params shell;
        astra_geom_machining_params machining;
    } parameters;
} astra_geom_feature_request;

typedef struct astra_tessellation_view {
    uint32_t struct_size;
    uint32_t flags;
    uint64_t geometry_revision;
    uint64_t vertex_count;
    uint64_t triangle_count;
    const float *positions_xyz;
    const uint32_t *triangle_indices;
    const astra_entity_ref *triangle_entities;
} astra_tessellation_view;

ASTRA_API uint32_t astra_geometry_abi_version(void);
ASTRA_API astra_geom_status astra_geometry_last_error(char *buffer, uint32_t capacity, uint32_t *out_required_bytes);

ASTRA_API astra_geom_status astra_geom_model_create(astra_geom_model_handle **out_model);
ASTRA_API void astra_geom_model_release(astra_geom_model_handle *model);
ASTRA_API astra_geom_status astra_geom_evaluate_feature(
    const astra_geom_model_handle *model,
    const astra_geom_feature_request *request,
    astra_geom_result_handle **out_result);

ASTRA_API void astra_geom_result_release(astra_geom_result_handle *result);
ASTRA_API astra_geom_status astra_geom_result_revision(
    const astra_geom_result_handle *result, uint64_t *out_revision);
ASTRA_API astra_geom_status astra_geom_result_tessellation(
    const astra_geom_result_handle *result, astra_tessellation_view *out_view);

ASTRA_API astra_geom_status astra_geom_result_resolve_entity(
    const astra_geom_result_handle *result, const astra_entity_ref *reference,
    uint32_t *out_entity_kind);
ASTRA_API astra_geom_status astra_geom_result_lineage_count(
    const astra_geom_result_handle *result, uint64_t source_revision,
    uint64_t *out_count);
ASTRA_API astra_geom_status astra_geom_result_lineage_at(
    const astra_geom_result_handle *result, uint64_t source_revision,
    uint64_t index, astra_lineage_record *out_record);
ASTRA_API astra_geom_status astra_geom_result_lineage_successor_at(
    const astra_geom_result_handle *result, uint64_t source_revision,
    uint64_t record_index, uint32_t successor_index, astra_entity_ref *out_successor);

#ifdef __cplusplus
}
#endif

#endif /* ASTRA_GEOMETRY_H */
