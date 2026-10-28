# Milestone 1 — Geometry & Topology Contract Specification

**Project**: Astra / MultiPhysics CAD  
**Status**: Phase 1 — design and source audit; proposed contract, not yet implemented  
**Audit basis**: `Astra_MultiPhysics_Agent_Handoff_20261028(1).zip` (SHA-256 `B09B2063739DDDC5FB2ECDA0B70A7A8BF73ED1D58295D34359B14C6CA67FC4`)  
**Scope**: Geometry conventions, topological naming/lineage, native C ABI, feature dependency graph, regeneration semantics, and acceptance criteria.  

*This document distinguishes observed source behavior from normative target behavior. The Phase 1 work adds this specification only. It does not implement the C ABI, alter Java production files, change `pom.xml`, or modify `src/main/CPP/`.*

---

## 1. Executive decision

Astra should adopt one persistent, parametric feature-history model with a C-compatible native geometry boundary. The native model must become the authoritative source for solid topology and geometric validity; JavaFX meshes must be treated as disposable render/picking products derived from that model. The dependency graph must represent every feature operation that affects geometry, and regeneration must evaluate those operations in a deterministic topological order rather than choose one geometry-producing branch from a Java `if/else if` ladder.

The current native API is not yet a CAD kernel: the inspected `native/include/astra_native.h` and `native/src/astra_native.cpp` expose version/error reporting, vector magnitude, ray-triangle and ray-mesh intersection, and point transformation. The audited sources do not show native B-rep construction, Boolean, shell, extrusion, revolve, sweep, loft, or persistent topological lineage APIs. These are future capabilities, not features to claim as already implemented.

---

## 2. Source audit: verified current state

### 2.1 Feature graph construction
Inspected `src/main/java/ui/workspace/drafting/features/feature_graph_builder_ui_main.java` (133 lines):
- `buildFromShapes` registers a shape/body or sketch-profile node, then registers sketches, extrusions, holes, hole patterns, fillets, chamfers, and drafts.
- It does not register the per-shape Boolean, Shell, Loft, Sweep, or Revolve collections, although `shape_item_ui_main.java` exposes holders and lists for those families and `feature_type_ui_main.java` already has enum values for Boolean, Shell, Loft, Sweep, and Revolve.
- The graph builder generally adds host-body dependencies; it adds a sketch dependency for extrusion and a seed-hole dependency for a pattern. It does not yet encode a complete ordered operation history or all face/profile/axis/path/target dependencies.
- `syncValidationToShapes` writes validation state back for the body, sketch, extrusion, hole, pattern, fillet, chamfer, and draft. It does not synchronise the omitted feature families.

### 2.2 Regeneration behavior
Inspected `src/main/java/ui/workspace/drafting/features/feature_regeneration_engine_ui_main.java` (145 lines) and `feature_dependency_graph_ui_main.java`:
- The engine builds and validates a graph and asks it for a topological order.
- It then walks that order, but `rebuiltShapes` permits at most one `host.revalidateFeatures(); host.rebuild();` per `shape_item_ui_main` in a regeneration pass. The graph nodes are therefore not individually evaluated as feature operations in their topological order. The first dirty node associated with a host can trigger one aggregate rebuild; later nodes for that host are marked clean without a separate feature evaluation.
- `syncEntityState` covers body/profile, sketch, hole, pattern, and extrusion state. It has no equivalent state mapping for fillet, chamfer, draft, Boolean, Shell, Loft, Sweep, or Revolve.
- Cycle detection exists, but the current `getRegenerationOrder()` returns an empty order on a detected cycle. Callers must ensure the report exposes cycle and failure state and must not report a successful regeneration merely because no ordered nodes were returned.
- The current regeneration code restores JavaFX children on a caught failure, but this is render-tree rollback, not a transaction over persistent feature parameters, native topology, or a sequence of partially evaluated geometric operations.

### 2.3 Shape rebuilding and geometry ownership
Inspected `src/main/java/ui/workspace/drafting/shape_item_ui_main.java` (198 physical lines in the snapshot; within the project-wide $\le 200$-line invariant):
- `rebuild()` clears the shape group and selects geometry using a priority chain: custom mesh, Shell, Loft, Sweep, Revolve, supported machining solid, or primitive/profile geometry.
- Extrusions are then appended in a separate loop, and sketches are appended as visual groups. This means the primary geometry selection is not a general ordered feature-history evaluator; combinations may appear as overlays or separately drawn meshes rather than as one sequentially evaluated solid.
- `revalidateFeatures()` visits several holders, but validation/revalidation alone is not the same as executing each feature against the previous feature's result.
- The snapshot contains 315 production Java files and all are at or below 200 physical lines. `shape_item_ui_main.java` is close to the limit at 198 lines, so any implementation must avoid growing it past 200 and should extract responsibilities rather than append logic. In particular, `applyWorldDelta()` and `setWorldTranslation()` must continue to update transforms without calling `rebuild()` during dragging.

### 2.4 Native boundary and ownership
Inspected `native/include/astra_native.h`, `native/src/astra_native.cpp`, and `src/main/java/backend/native_bridge/astra_native_bridge_ui_main.java`:
- The existing header has a C-linkage boundary and fixed-width status types for ray/math functions, but no opaque geometry/model/body handles, no feature-evaluation API, no persistent entity references, no topology-lineage result, and no tessellation ownership API.
- The bridge uses Java 25 FFM downcall handles. Temporary input/output storage for calls is generally scoped to a confined Arena; mesh arrays are exposed through `MemorySegment.ofArray`, whose lifetime is tied to the backing Java arrays and call scope.
- The bridge's current lifetime model is adequate for synchronous math/raycast calls; it is not a defined ownership model for long-lived native B-rep objects or returned tessellation arrays.
- `astra_get_version_string()` returns a native-owned static string; the Java bridge reinterprets the returned address and reads it. New ABI calls must not return unbounded or ambiguously owned strings/buffers.
- The current API has no model-level units, coordinate-frame, tolerance, document version, or geometry-kernel version contract.

### 2.5 Snapshot integrity observations
- The handoff ZIP integrity has been verified.
- The native build files (`CMakeLists.txt`, `build_native.ps1`, `run_native_integration_test.ps1`) exist in `native/` and are verified.
- `PROJECT_REPORT_AND_AGENT_RULES.txt` limits edits to `src/main/java/ui/` and `src/main/resources/`; the user's explicit Milestone 1 Phase 2 authorization supersedes that boundary for the specified native header and bridge paths when Phase 2 begins. It does not authorize touching `pom.xml`, `src/main/CPP/`, or Git state destructively.

---

## 3. Normative system and coordinate conventions

### 3.1 Units
1. The canonical model length unit is the millimeter ($\text{mm}$). All persisted feature parameters, native geometric coordinates, bounding boxes, lengths, radii, offsets, and tessellation positions use millimeters unless an API explicitly declares otherwise.
2. No implicit unit conversion is permitted at the Java/C boundary. Conversion from UI/document import units must occur in a named adapter at the boundary and must be tested.
3. Angles are represented in radians internally. UI controls and user-facing labels may use degrees; convert at UI input/output boundaries only.
4. API and serialized-document fields must include unit context or inherit an explicitly versioned document-level unit declaration. A raw double must never be interpreted as degrees in one call and radians in another.

### 3.2 Coordinate frame
The canonical geometry-kernel frame is a right-handed Cartesian frame:
- $+X$: right.
- $+Y$: up.
- $+Z$: out toward the viewer; the canonical tool-spindle direction is $+Z$ and its normal is defined in this direction unless an operation explicitly specifies a different local frame.
- Right-handedness: $+X \times +Y = +Z$.
- Positive rotation follows the right-hand rule around the selected axis.

> [!WARNING]
> The existing project rules describe JavaFX elevation as world Y and depth as world Z, and CAM has a machine-coordinate abstraction. Do not silently reinterpret or rewrite those existing coordinate systems. The native-kernel frame above is the geometry contract; implementation must introduce and test explicit transforms between kernel, JavaFX view/world, sketch-local, and CAM machine frames. Each transform must declare source frame, destination frame, matrix storage convention, and whether it transforms points or directions. A point transform includes translation; a direction/normal transform does not. Normals under non-uniform transforms require inverse-transpose handling and normalization.

### 3.3 Tolerances
The required absolute tolerances are:
- Linear coincidence/degeneracy tolerance: $\varepsilon = 10^{-6}\text{ mm}$.
- Angular tolerance: $\theta_{\varepsilon} = 10^{-5}\text{ rad}$.

For points $p, q$, coincidence means $\|p - q\| \le \varepsilon$; a squared-distance implementation may compare $\|p-q\|^2 \le \varepsilon^2$. Angular comparisons must use a well-defined angular distance or normalized dot/cross formulation and must not compare raw dot products directly to $\theta_{\varepsilon}$.

These are absolute model tolerances, not display-pixel thresholds. UI snapping/picking tolerance is a separate interaction policy. Kernel operations may add documented scale-aware conditioning checks to detect ill-conditioned inputs, but must not silently replace the specified absolute tolerances. A feature result must distinguish an input rejected as degenerate from a valid result that is merely within display or selection tolerance.

### 3.4 Numeric and validity rules
- Reject NaN and infinities in coordinates, parameters, matrices, and tolerance overrides.
- Reject zero-length direction vectors where a normalized direction is required; otherwise document whether a direction is normalized by the callee.
- Distinguish an ordinary geometric miss from an invalid input or kernel failure. A ray miss is not an ABI error.
- Check integer counts, multiplication overflow, array lengths, face/index ranges, and output capacity before reading or writing caller memory.
- Kernel outputs are valid only if the operation reports success and the returned model/entity state passes the documented validity checks. A display mesh is not evidence that a closed valid solid exists.

---

## 4. Topological naming and entity identity

### 4.1 Core rule
Raw triangle indices, JavaFX `TriangleMesh` face indices, array offsets, traversal order, and monotonic native entity counters are not stable references across regeneration. They may be used as transient render/picking indices only within a single geometry revision.

The semantic reference model is:
$$\text{EntityRef} = (\text{FeatureID}, \text{EntityRole}, \text{SubIndex})$$

For operations whose role depends on another feature, the descriptor also carries the related feature identity (or a normalized pair of feature identities). For example, `INTERSECTION_EDGE(f1, f2)` is represented by an intersection-edge role plus the canonicalized related feature pair, not by a mesh edge number.

### 4.2 Identifier fields
- `FeatureID` is a persistent, document-scoped feature identity. It survives parameter edits and regeneration of that feature. It is not the feature's display name or current list position.
- `EntityRole` is a versioned semantic role, preferably a stable enum/code rather than a localized string. Example roles include `END_CAP_START`, `END_CAP_END`, `LATERAL_FACE`, `REVOLVE_START_CAP`, `REVOLVE_END_CAP`, `SWEEP_SIDE`, `SHELL_INNER_FACE`, `HOLE_CYLINDRICAL_WALL`, `HOLE_ENTRY_RIM`, `INTERSECTION_EDGE`, and `UNCLASSIFIED_DERIVED_ENTITY`.
- `SubIndex` disambiguates repeated entities within one semantic role. It is stable only under an explicit role-specific ordering rule; it must not be assigned from incidental mesh iteration order. If no defensible stable ordering can be produced, the reference is unresolved rather than falsely preserved.
- A feature-specific role schema must state whether subindices are ordered by profile-loop index, path segment, source boundary, parameter interval, or another deterministic semantic criterion.
`LATERAL_FACE_i` is acceptable only when $i$ is defined by a stable semantic ordering (for example, source profile edge identity). `INTERSECTION_EDGE(f1, f2)` must canonicalize $(f_1, f_2)$ so the same pair resolves independent of operand order when the operation is symmetric; directed operations must retain operand roles.

### 4.3 Topology lineage across edits
Every successful geometry regeneration produces a new geometry revision and a lineage map from prior semantic entity references to the new revision. Each prior entity has one of these outcomes:
- `PRESERVED`: one unambiguous successor.
- `SPLIT`: one source maps to two or more successors.
- `MERGED`: two or more source entities map to one successor.
- `CONSUMED`: no successor because a Boolean/shell/other operation removed the entity.
- `REPLACED`: a successor exists but its geometric/topological role changed materially.
- `UNRESOLVED`: correspondence cannot be established reliably.

Lineage records must include the source revision, destination revision, operation/feature that caused the change, outcome, and successor list. Do not silently map an ambiguous split or merge to the first entity. References to consumed or unresolved entities must fail explicitly and surface a feature diagnostic; callers may offer a deliberate reattachment workflow.

### 4.4 Matching and confidence
Lineage matching should prefer semantic provenance: source feature/entity references, operation role, profile-edge/path identity, and operation-specific ancestry. Geometric signatures (surface type, curve type, location, direction, parameter range, adjacency, area/length, and tolerance-bounded geometry) are supporting evidence, not the sole identity source. A geometry signature may be used to reattach when provenance is absent only if the match is unique under documented thresholds. Ambiguous matches become `UNRESOLVED`.

The contract must not promise persistent face identity where topology changes make correspondence mathematically ambiguous. The system promises explicit lineage and safe failure, not that every old face survives every edit.

### 4.5 Picking and display mapping
JavaFX tessellation data must include a per-triangle mapping to the owning native face/entity reference for the exact geometry revision that produced it. Picking returns `(geometryRevision, triangleIndex, barycentricCoordinates)` as a transient hit, then resolves through that revision's triangle-to-entity map. A stale hit whose revision differs from the current body revision must be discarded or resolved through lineage; it must never be treated as a current persistent face ID.

---

## 5. C-compatible geometry ABI contract

### 5.1 ABI principles
- The public boundary is C99-compatible and callable from C++17 via `extern "C"`; do not expose C++ classes, templates, STL containers, exceptions, references, or compiler-specific object layouts.
- Use fixed-width integer types, opaque handles, explicit struct sizes/version fields where extensible structs are used, and stable status codes.
- All exported functions must catch internal C++ exceptions and translate them to status codes. No exception may cross the C ABI.
- The geometry ABI is a separate logical layer from the existing ray/math ABI. It may live in `native/include/astra_geometry.h`, including `astra_native.h` for shared export/status definitions, or extend `astra_native.h` only if the boundary remains coherent. The implementation phase should prefer a dedicated header to avoid conflating triangle-ray utilities with persistent geometry ownership.
- ABI compatibility must be versioned independently from document format and kernel implementation version. Additive fields/functions are preferred; incompatible layout changes require an ABI version change.

### 5.2 Proposed C99 declarations (design only)

```c
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
    ASTRA_ROLE_UNCLASSIFIED_DERIVED_ENTITY = 255
} astra_entity_role;

typedef struct astra_entity_ref {
    uint32_t struct_size;
    uint32_t role;
    astra_id128 feature_id;
    int32_t sub_index;
    uint32_t flags;
    astra_id128 related_feature_id; /* used by roles such as INTERSECTION_EDGE */
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
    const double *coordinates_uv_mm; /* 2 doubles per point; borrowed for call only */
    const uint32_t *loop_offsets;    /* loop_count + 1 offsets into points; borrowed */
} astra_geom_sketch_profile_params;

typedef struct astra_geom_base_primitive_params {
    uint32_t struct_size;
    uint32_t primitive_kind; /* box, cylinder, sphere, cone, etc. */
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
    const float *positions_xyz;                /* borrowed until result release */
    const uint32_t *triangle_indices;          /* 3 indices per triangle; borrowed */
    const astra_entity_ref *triangle_entities; /* one ref per triangle; borrowed */
} astra_tessellation_view;

/* ABI/error diagnostics */
ASTRA_API uint32_t astra_geometry_abi_version(void);
ASTRA_API astra_geom_status astra_geometry_last_error(char *buffer, uint32_t capacity,
                                                      uint32_t *out_required_bytes);

/* Model lifetime */
ASTRA_API astra_geom_status astra_geom_model_create(astra_geom_model_handle **out_model);
ASTRA_API void astra_geom_model_release(astra_geom_model_handle *model);
ASTRA_API astra_geom_status astra_geom_evaluate_feature(
    const astra_geom_model_handle *model,
    const astra_geom_feature_request *request,
    astra_geom_result_handle **out_result);

/* Geometry result handles */
ASTRA_API void astra_geom_result_release(astra_geom_result_handle *result);
ASTRA_API astra_geom_status astra_geom_result_revision(
    const astra_geom_result_handle *result, uint64_t *out_revision);
ASTRA_API astra_geom_status astra_geom_result_tessellation(
    const astra_geom_result_handle *result, astra_tessellation_view *out_view);

/* Entity and lineage queries */
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
#endif
```

### 5.3 Status and error semantics
The ABI defines stable status codes:
- `ASTRA_GEOM_OK`: operation succeeded.
- `ASTRA_GEOM_INVALID_ARGUMENT`: null/invalid parameter or malformed struct.
- `ASTRA_GEOM_UNSUPPORTED_OPERATION`: valid request not supported by this kernel build.
- `ASTRA_GEOM_DEGENERATE_INPUT`: profile/axis/path/solid is degenerate under the contract tolerances.
- `ASTRA_GEOM_INVALID_TOPOLOGY`: operation would produce an invalid body or non-manifold result.
- `ASTRA_GEOM_ENTITY_NOT_FOUND`: reference cannot be resolved in the requested revision.
- `ASTRA_GEOM_ENTITY_CONSUMED`: entity was consumed by an operation.
- `ASTRA_GEOM_ENTITY_AMBIGUOUS`: entity mapping has multiple candidates and requires reattachment.
- `ASTRA_GEOM_OUT_OF_MEMORY`: allocation failure.
- `ASTRA_GEOM_INTERNAL_ERROR`: unexpected kernel/internal failure.

### 5.4 Ownership and memory lifecycle
1. Native handles are opaque and released only by the matching `astra_geom_*_release` function from the same loaded library instance.
2. Input arrays passed from Java are borrowed for the duration of the synchronous call only.
3. Output buffers allocated by Java are caller-owned.
4. `astra_tessellation_view` pointers are borrowed from an owning result handle and remain valid only while that result handle remains alive and unmodified.
5. Every native allocation has a matching exported native deallocator; never use `free()` from Java.
6. Prefer immutable result snapshots after publication. Rebuild creates a new revision; it must not mutate buffers concurrently observed by JavaFX.
7. Java FFM code retains the owning handle for the full lifetime of borrowed segments.
8. Native handles follow serialized mutation per model; immutable results may be read concurrently.
9. Handle release is idempotent in Java wrappers via `AutoCloseable` guard.

### 5.5 B-rep versus tessellation boundary
- **Authoritative native state**: parametric operation inputs, B-rep/solid topology, geometric validity, persistent entity references, adjacency, feature lineage, and operation diagnostics.
- **Derived render state**: JavaFX `TriangleMesh` positions, normals, triangle indices, material groups, edge overlay lines, and per-triangle entity mapping.
- Mesh triangles are not B-rep faces.
- JavaFX remains responsible for viewport presentation, camera, selection UX, overlays, and interaction.

---

## 6. Unified feature dependency graph

### 6.1 Node model
Every geometry-affecting operation must be represented by a graph node with:
- Persistent `FeatureID`, feature type, display name, owner/body ID, enabled/suppressed state, dirty/clean/invalid state, parameter revision, result geometry revision, and diagnostic code/message.
- Typed dependencies to input nodes/entities.
- Operation parameters stored on the persistent feature object.
- Output entity refs and lineage records produced by the last successful regeneration.

### 6.2 Required feature families and dependencies

| Feature family | Required dependencies | Geometry role in history |
| :--- | :--- | :--- |
| **Sketch / sketch profile** | Supporting plane or face, constraint/profile references | Produces a validated planar profile; not itself a solid unless explicitly designated |
| **Base primitive / base solid** | Primitive parameters | Creates the initial body/result |
| **Extrude** | Profile/sketch, direction/extent, optional target body/operation mode | Creates/adds/cuts solid from profile |
| **Revolve** | Profile, axis reference, angle/sweep parameters | Creates/adds/cuts solid around axis |
| **Sweep** | Profile, path, orientation/twist options | Creates/adds/cuts solid along path |
| **Loft** | Ordered section profiles and optional guide rails | Creates/adds/cuts transition solid |
| **Boolean union/subtract/intersect** | Target body and tool body/result refs | Consumes or replaces body topology; emits lineage |
| **Hole Wizard / hole** | Target body, placement face/entity, axis/direction, hole parameters | Removes material and produces semantic wall/rim entities |
| **Hole pattern** | Seed hole and pattern reference/parameters | Produces instances tied to seed and pattern definition |
| **Shell** | Target body, removed face refs, thickness/direction | Replaces solid topology; tracks inner/outer faces and consumed refs |
| **Fillet / chamfer / draft** | Target body and selected edge/face refs, parameters | Modifies target topology; selected refs require lineage-aware resolution |
| **Other machining feature** | Target body plus edge/face/profile refs and operation parameters | Modifies the prior result in declared history order |

### 6.3 Graph invariants
- Feature IDs are unique within a document.
- Every dependency must resolve to an existing feature.
- Cycles are rejected before any geometry is mutated; cycle path is reported.
- Dependencies are ordered deterministically using persisted history order or stable `FeatureID`.
- Suppressed features do not execute; downstream nodes follow an explicit suppression policy.
- Invalid upstream geometry blocks dependent operations.
- Graph construction/validation is separate from execution.

### 6.4 Sequential regeneration semantics
1. Capture document/model revision and dirty feature set.
2. Build complete graph from persistent feature records.
3. Validate IDs, dependencies, parameter domains, and acyclicity.
4. Compute deterministic topological order.
5. Evaluate each dirty, unsuppressed node against prerequisite result snapshots.
6. Validate each operation output.
7. Stage result in a transaction; on failure, abort and retain previous last-valid model revision.
8. Commit new immutable model revision; extract tessellation buffers for JavaFX.
9. Synchronize persistent node state covering every feature family.
10. Update selection references through lineage.

### 6.5 JavaFX integration and interaction invariants
- `shape_item_ui_main.java` acts as a UI adapter to the latest committed geometry result, not the feature evaluator.
- `rebuild()` rebuilds render nodes from an already committed result.
- Preserve direct body dragging, raycasting, mouse transparency, camera behavior, and existing transform updates (`applyWorldDelta()` and `setWorldTranslation()` must not call `rebuild()` during drag).
- Render updates replacing JavaFX nodes are scheduled on the JavaFX Application Thread.
- CAM remains 100% headless and decoupled from JavaFX.

---

## 7. Feature input/output contract
Each feature operation declares:
1. Inputs: feature IDs, entity refs, operation mode, parameters, unit/frame, and source geometry revisions.
2. Preconditions: profile closure/planarity, valid axis/path/section ordering, nonzero extent/angle, supported body type, valid selected entities.
3. Result: immutable body/result handle, new geometry revision, status, diagnostic, semantic output entity refs, lineage records, optional tessellation data.
4. Failure semantics: transactional rollback; previous committed revision remains valid.
5. Determinism: same kernel version, inputs, source revisions, tolerances yield equivalent topology/geometry.

---

## 8. Milestone 1 acceptance tests
1. **Coordinate and numeric contract**: Millimeter inputs/outputs, radian internals, right-handed basis $+X \times +Y = +Z$, coincidence at $\varepsilon$, angular alignment at $\theta_{\varepsilon}$, non-finite coordinate rejection.
2. **Dependency ordering and history**: Multi-step dependency chains evaluated in exact topological order; cycle detection; incremental dirty propagation.
3. **Topological naming and lineage**: Semantic start/end cap resolution; `SPLIT`, `MERGED`, `CONSUMED`, `UNRESOLVED` tracking; curved face tessellation independence.
4. **ABI and memory safety**: C99 compilation, Java FFM descriptor layout agreement, error codes, lifetime safety, concurrent read access, double-close guards.
5. **Transaction and regression safety**: Mid-chain rollback, clean state assignment, direct manipulation preservation, $\le 200$-line Java file limit, frozen `pom.xml` hash preservation, headless CAM isolation.

---

## 9. Implementation sequence and migration gates
1. Freeze contract types and ABI version.
2. Introduce feature operation descriptors and graph completeness.
3. Separate evaluation from rendering.
4. Implement native result/lineage/tessellation ownership.
5. Integrate one vertical slice (Sketch $\to$ Extrude end-to-end first).
6. Expand feature coverage (Revolve, Sweep, Loft, Boolean, Hole Wizard, Shell, Fillet, Chamfer, Draft).
7. Migration gate verification.

---

## 10. Explicit non-goals and unresolved decisions
- No third-party B-rep kernel is selected or embedded yet.
- LLVM/Clang is the compiler toolchain, not the B-rep kernel.
- The 128-bit persistent `FeatureID` mapping to Java string IDs is established in the adapter.
- `shape_item_ui_main.java` is 198 lines; implementation will preserve the $\le 200$-line limit by extracting helper classes rather than expanding `shape_item_ui_main`.
