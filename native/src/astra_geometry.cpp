#include "astra_geometry.h"
#include <cstdlib>
#include <cstring>
#include <string>
#include <vector>
#include <map>
#include <cmath>
#include <algorithm>

static thread_local std::string g_last_error;

static void set_error(const std::string &msg) {
    g_last_error = msg;
}

struct Point2D {
    double u, v;
};

struct StoredProfile {
    astra_id128 feature_id;
    double origin[3];
    double u_axis[3];
    double v_axis[3];
    std::vector<Point2D> points;
};

struct Id128Key {
    uint64_t high;
    uint64_t low;
    bool operator<(const Id128Key &o) const {
        if (high != o.high) return high < o.high;
        return low < o.low;
    }
};

static Id128Key to_key(const astra_id128 &id) {
    return Id128Key{id.high, id.low};
}

struct astra_geom_model_handle {
    uint32_t magic;
    uint64_t current_revision;
    std::map<Id128Key, StoredProfile> profiles;
};

struct astra_geom_result_handle {
    uint32_t magic;
    uint64_t revision;
    astra_tessellation_view view;
    std::vector<float> positions;
    std::vector<uint32_t> indices;
    std::vector<astra_entity_ref> entities;
};

static const uint32_t MODEL_MAGIC = 0x4153474Du;  /* "ASGM" */
static const uint32_t RESULT_MAGIC = 0x41534752u; /* "ASGR" */

uint32_t astra_geometry_abi_version(void) {
    return ASTRA_GEOMETRY_ABI_VERSION;
}

astra_geom_status astra_geometry_last_error(char *buffer, uint32_t capacity, uint32_t *out_required_bytes) {
    uint32_t len = (uint32_t)g_last_error.length() + 1u;
    if (out_required_bytes) {
        *out_required_bytes = len;
    }
    if (!buffer || capacity == 0) {
        return ASTRA_GEOM_OK;
    }
    if (capacity < len) {
        return ASTRA_GEOM_BUFFER_TOO_SMALL;
    }
    std::memcpy(buffer, g_last_error.c_str(), len);
    return ASTRA_GEOM_OK;
}

astra_geom_status astra_geom_model_create(astra_geom_model_handle **out_model) {
    if (!out_model) {
        set_error("astra_geom_model_create: out_model pointer is null");
        return ASTRA_GEOM_INVALID_ARGUMENT;
    }
    auto *m = new (std::nothrow) astra_geom_model_handle();
    if (!m) {
        set_error("astra_geom_model_create: out of memory");
        return ASTRA_GEOM_OUT_OF_MEMORY;
    }
    m->magic = MODEL_MAGIC;
    m->current_revision = 1u;
    *out_model = m;
    return ASTRA_GEOM_OK;
}

void astra_geom_model_release(astra_geom_model_handle *model) {
    if (model && model->magic == MODEL_MAGIC) {
        model->magic = 0;
        delete model;
    }
}

void astra_geom_result_release(astra_geom_result_handle *result) {
    if (result && result->magic == RESULT_MAGIC) {
        result->magic = 0;
        delete result;
    }
}

astra_geom_status astra_geom_result_revision(const astra_geom_result_handle *result, uint64_t *out_revision) {
    if (!result || result->magic != RESULT_MAGIC || !out_revision) {
        set_error("astra_geom_result_revision: invalid argument");
        return ASTRA_GEOM_INVALID_ARGUMENT;
    }
    *out_revision = result->revision;
    return ASTRA_GEOM_OK;
}

astra_geom_status astra_geom_result_tessellation(const astra_geom_result_handle *result, astra_tessellation_view *out_view) {
    if (!result || result->magic != RESULT_MAGIC || !out_view) {
        set_error("astra_geom_result_tessellation: invalid argument");
        return ASTRA_GEOM_INVALID_ARGUMENT;
    }
    *out_view = result->view;
    return ASTRA_GEOM_OK;
}

astra_geom_status astra_geom_result_resolve_entity(
    const astra_geom_result_handle *result, const astra_entity_ref *reference, uint32_t *out_entity_kind) {
    if (!result || result->magic != RESULT_MAGIC || !reference || !out_entity_kind) {
        set_error("astra_geom_result_resolve_entity: invalid argument");
        return ASTRA_GEOM_INVALID_ARGUMENT;
    }
    for (const auto &ref : result->entities) {
        if (ref.role == reference->role &&
            ref.feature_id.high == reference->feature_id.high &&
            ref.feature_id.low == reference->feature_id.low &&
            ref.sub_index == reference->sub_index) {
            *out_entity_kind = 1u; /* 1 = Face */
            return ASTRA_GEOM_OK;
        }
    }
    set_error("astra_geom_result_resolve_entity: entity reference not found in result");
    return ASTRA_GEOM_ENTITY_NOT_FOUND;
}

astra_geom_status astra_geom_result_lineage_count(
    const astra_geom_result_handle *result, uint64_t source_revision, uint64_t *out_count) {
    if (!result || result->magic != RESULT_MAGIC || !out_count) {
        set_error("astra_geom_result_lineage_count: invalid argument");
        return ASTRA_GEOM_INVALID_ARGUMENT;
    }
    (void)source_revision;
    *out_count = 0;
    return ASTRA_GEOM_OK;
}

astra_geom_status astra_geom_result_lineage_at(
    const astra_geom_result_handle *result, uint64_t source_revision, uint64_t index, astra_lineage_record *out_record) {
    if (!result || result->magic != RESULT_MAGIC || !out_record) {
        set_error("astra_geom_result_lineage_at: invalid argument");
        return ASTRA_GEOM_INVALID_ARGUMENT;
    }
    (void)source_revision;
    (void)index;
    set_error("astra_geom_result_lineage_at: lineage index out of bounds");
    return ASTRA_GEOM_INVALID_ARGUMENT;
}

astra_geom_status astra_geom_result_lineage_successor_at(
    const astra_geom_result_handle *result, uint64_t source_revision,
    uint64_t record_index, uint32_t successor_index, astra_entity_ref *out_successor) {
    if (!result || result->magic != RESULT_MAGIC || !out_successor) {
        set_error("astra_geom_result_lineage_successor_at: invalid argument");
        return ASTRA_GEOM_INVALID_ARGUMENT;
    }
    (void)source_revision;
    (void)record_index;
    (void)successor_index;
    set_error("astra_geom_result_lineage_successor_at: successor index out of bounds");
    return ASTRA_GEOM_INVALID_ARGUMENT;
}

/* --- Geometry Evaluation Helpers --- */

static double cross2d(const Point2D &a, const Point2D &b, const Point2D &c) {
    return (b.u - a.u) * (c.v - a.v) - (b.v - a.v) * (c.u - a.u);
}

static bool point_in_triangle(const Point2D &p, const Point2D &a, const Point2D &b, const Point2D &c) {
    double cp1 = cross2d(a, b, p);
    double cp2 = cross2d(b, c, p);
    double cp3 = cross2d(c, a, p);
    bool has_neg = (cp1 < -1e-9) || (cp2 < -1e-9) || (cp3 < -1e-9);
    bool has_pos = (cp1 > 1e-9) || (cp2 > 1e-9) || (cp3 > 1e-9);
    return !(has_neg && has_pos);
}

static bool segments_intersect(const Point2D &p1, const Point2D &p2, const Point2D &p3, const Point2D &p4) {
    double d1 = cross2d(p3, p4, p1);
    double d2 = cross2d(p3, p4, p2);
    double d3 = cross2d(p1, p2, p3);
    double d4 = cross2d(p1, p2, p4);

    if (((d1 > 1e-9 && d2 < -1e-9) || (d1 < -1e-9 && d2 > 1e-9)) &&
        ((d3 > 1e-9 && d4 < -1e-9) || (d3 < -1e-9 && d4 > 1e-9))) {
        return true;
    }
    return false;
}

static std::vector<uint32_t> triangulate_polygon(const std::vector<Point2D> &pts, bool is_ccw) {
    std::vector<uint32_t> triangles;
    int n = (int)pts.size();
    if (n < 3) return triangles;

    std::vector<int> V(n);
    for (int i = 0; i < n; i++) V[i] = i;

    int count = 2 * n;
    int v = n - 1;
    while (n > 2) {
        if ((count--) <= 0) break; // safeguard
        int u = v;
        if (n <= u) u = 0;
        v = u + 1;
        if (n <= v) v = 0;
        int w = v + 1;
        if (n <= w) w = 0;

        const Point2D &A = pts[V[u]];
        const Point2D &B = pts[V[v]];
        const Point2D &C = pts[V[w]];

        double cp = cross2d(A, B, C);
        bool convex = is_ccw ? (cp > 1e-9) : (cp < -1e-9);
        if (!convex) continue;

        bool has_internal_point = false;
        for (int p = 0; p < n; p++) {
            if ((p == u) || (p == v) || (p == w)) continue;
            if (point_in_triangle(pts[V[p]], A, B, C)) {
                has_internal_point = true;
                break;
            }
        }
        if (has_internal_point) continue;

        // Ear found
        triangles.push_back((uint32_t)V[u]);
        triangles.push_back((uint32_t)V[v]);
        triangles.push_back((uint32_t)V[w]);

        V.erase(V.begin() + v);
        n--;
        count = 2 * n;
    }
    return triangles;
}

static astra_geom_status evaluate_sketch_profile(
    astra_geom_model_handle *model,
    const astra_geom_feature_request *request,
    astra_geom_result_handle **out_result) {
    const auto &p = request->parameters.sketch_profile;
    if (p.loop_count != 1) {
        set_error("evaluate_sketch_profile: multiple loops or holes are not supported in this baseline");
        return ASTRA_GEOM_UNSUPPORTED_OPERATION;
    }
    if (p.point_count < 3 || !p.coordinates_uv_mm) {
        set_error("evaluate_sketch_profile: degenerate profile (fewer than 3 points or null coordinates)");
        return ASTRA_GEOM_DEGENERATE_INPUT;
    }

    std::vector<Point2D> pts(p.point_count);
    for (uint32_t i = 0; i < p.point_count; i++) {
        double u = p.coordinates_uv_mm[2 * i];
        double v = p.coordinates_uv_mm[2 * i + 1];
        if (!std::isfinite(u) || !std::isfinite(v)) {
            set_error("evaluate_sketch_profile: non-finite coordinate encountered");
            return ASTRA_GEOM_DEGENERATE_INPUT;
        }
        pts[i] = Point2D{u, v};
    }

    // Check signed area
    double area2 = 0;
    for (uint32_t i = 0; i < p.point_count; i++) {
        uint32_t j = (i + 1) % p.point_count;
        area2 += (pts[i].u * pts[j].v - pts[j].u * pts[i].v);
    }
    if (std::abs(area2) < 1e-12) {
        set_error("evaluate_sketch_profile: degenerate zero-area profile");
        return ASTRA_GEOM_DEGENERATE_INPUT;
    }

    // Self-intersection check
    for (uint32_t i = 0; i < p.point_count; i++) {
        uint32_t i_next = (i + 1) % p.point_count;
        for (uint32_t j = i + 1; j < p.point_count; j++) {
            uint32_t j_next = (j + 1) % p.point_count;
            if (i == j || i == j_next || i_next == j || i_next == j_next) continue;
            if (segments_intersect(pts[i], pts[i_next], pts[j], pts[j_next])) {
                set_error("evaluate_sketch_profile: self-intersecting polygon boundary");
                return ASTRA_GEOM_INVALID_TOPOLOGY;
            }
        }
    }

    StoredProfile sp;
    sp.feature_id = request->feature_id;
    std::memcpy(sp.origin, p.plane_origin_xyz_mm, sizeof(double) * 3);
    std::memcpy(sp.u_axis, p.plane_u_axis_xyz, sizeof(double) * 3);
    std::memcpy(sp.v_axis, p.plane_v_axis_xyz, sizeof(double) * 3);
    sp.points = pts;

    model->profiles[to_key(request->feature_id)] = sp;
    model->current_revision++;

    auto *res = new (std::nothrow) astra_geom_result_handle();
    if (!res) return ASTRA_GEOM_OUT_OF_MEMORY;
    res->magic = RESULT_MAGIC;
    res->revision = model->current_revision;
    std::memset(&res->view, 0, sizeof(res->view));
    res->view.struct_size = (uint32_t)sizeof(astra_tessellation_view);
    res->view.geometry_revision = res->revision;

    *out_result = res;
    return ASTRA_GEOM_OK;
}

static astra_geom_status evaluate_extrude(
    astra_geom_model_handle *model,
    const astra_geom_feature_request *request,
    astra_geom_result_handle **out_result) {
    const auto &p = request->parameters.extrude;
    auto it = model->profiles.find(to_key(p.profile_feature_id));
    if (it == model->profiles.end()) {
        set_error("evaluate_extrude: referenced profile feature ID not found in model");
        return ASTRA_GEOM_ENTITY_NOT_FOUND;
    }
    const StoredProfile &prof = it->second;
    if (p.distance_mm <= 1e-6 || !std::isfinite(p.distance_mm)) {
        set_error("evaluate_extrude: distance must be positive and finite");
        return ASTRA_GEOM_INVALID_ARGUMENT;
    }

    double dir_len = std::sqrt(p.direction_xyz[0]*p.direction_xyz[0] +
                               p.direction_xyz[1]*p.direction_xyz[1] +
                               p.direction_xyz[2]*p.direction_xyz[2]);
    if (dir_len < 1e-6) {
        set_error("evaluate_extrude: direction vector has near-zero length");
        return ASTRA_GEOM_DEGENERATE_INPUT;
    }

    double norm_dx = (p.direction_xyz[0] / dir_len) * p.distance_mm;
    double norm_dy = (p.direction_xyz[1] / dir_len) * p.distance_mm;
    double norm_dz = (p.direction_xyz[2] / dir_len) * p.distance_mm;

    uint32_t n = (uint32_t)prof.points.size();
    uint32_t total_verts = 2 * n;

    auto *res = new (std::nothrow) astra_geom_result_handle();
    if (!res) return ASTRA_GEOM_OUT_OF_MEMORY;
    res->magic = RESULT_MAGIC;
    model->current_revision++;
    res->revision = model->current_revision;

    res->positions.resize(total_verts * 3);

    // Compute ring 0 (start) and ring 1 (end) positions
    for (uint32_t i = 0; i < n; i++) {
        double u = prof.points[i].u;
        double v = prof.points[i].v;
        double px = prof.origin[0] + u * prof.u_axis[0] + v * prof.v_axis[0];
        double py = prof.origin[1] + u * prof.u_axis[1] + v * prof.v_axis[1];
        double pz = prof.origin[2] + u * prof.u_axis[2] + v * prof.v_axis[2];

        res->positions[3 * i + 0] = (float)px;
        res->positions[3 * i + 1] = (float)py;
        res->positions[3 * i + 2] = (float)pz;

        res->positions[3 * (i + n) + 0] = (float)(px + norm_dx);
        res->positions[3 * (i + n) + 1] = (float)(py + norm_dy);
        res->positions[3 * (i + n) + 2] = (float)(pz + norm_dz);
    }

    // Determine polygon orientation in UV plane
    double area2 = 0;
    for (uint32_t i = 0; i < n; i++) {
        uint32_t j = (i + 1) % n;
        area2 += (prof.points[i].u * prof.points[j].v - prof.points[j].u * prof.points[i].v);
    }
    bool is_ccw = (area2 > 0);

    // 1. Start Cap Triangles (normal pointing -extrusion)
    std::vector<uint32_t> cap_triangles = triangulate_polygon(prof.points, is_ccw);
    uint32_t cap_tri_count = (uint32_t)cap_triangles.size() / 3;

    astra_entity_ref start_cap_ref;
    std::memset(&start_cap_ref, 0, sizeof(start_cap_ref));
    start_cap_ref.struct_size = (uint32_t)sizeof(astra_entity_ref);
    start_cap_ref.role = ASTRA_ROLE_END_CAP_START;
    start_cap_ref.feature_id = request->feature_id;
    start_cap_ref.sub_index = 0;

    for (uint32_t t = 0; t < cap_tri_count; t++) {
        uint32_t i0 = cap_triangles[3 * t + 0];
        uint32_t i1 = cap_triangles[3 * t + 1];
        uint32_t i2 = cap_triangles[3 * t + 2];
        // Reverse winding for start cap so normal faces outward
        res->indices.push_back(i0);
        res->indices.push_back(i2);
        res->indices.push_back(i1);
        res->entities.push_back(start_cap_ref);
    }

    // 2. End Cap Triangles (normal pointing +extrusion)
    astra_entity_ref end_cap_ref;
    std::memset(&end_cap_ref, 0, sizeof(end_cap_ref));
    end_cap_ref.struct_size = (uint32_t)sizeof(astra_entity_ref);
    end_cap_ref.role = ASTRA_ROLE_END_CAP_END;
    end_cap_ref.feature_id = request->feature_id;
    end_cap_ref.sub_index = 0;

    for (uint32_t t = 0; t < cap_tri_count; t++) {
        uint32_t i0 = cap_triangles[3 * t + 0] + n;
        uint32_t i1 = cap_triangles[3 * t + 1] + n;
        uint32_t i2 = cap_triangles[3 * t + 2] + n;
        res->indices.push_back(i0);
        res->indices.push_back(i1);
        res->indices.push_back(i2);
        res->entities.push_back(end_cap_ref);
    }

    // 3. Lateral Faces (2 triangles per profile edge)
    for (uint32_t i = 0; i < n; i++) {
        uint32_t j = (i + 1) % n;
        uint32_t v0 = is_ccw ? i : j;
        uint32_t v1 = is_ccw ? j : i;
        uint32_t v0_top = v0 + n;
        uint32_t v1_top = v1 + n;

        astra_entity_ref lat_ref;
        std::memset(&lat_ref, 0, sizeof(lat_ref));
        lat_ref.struct_size = (uint32_t)sizeof(astra_entity_ref);
        lat_ref.role = ASTRA_ROLE_LATERAL_FACE;
        lat_ref.feature_id = request->feature_id;
        lat_ref.sub_index = (int32_t)i;
        lat_ref.related_feature_id = p.profile_feature_id;

        // Triangle 1: (v0, v1, v1_top)
        res->indices.push_back(v0);
        res->indices.push_back(v1);
        res->indices.push_back(v1_top);
        res->entities.push_back(lat_ref);

        // Triangle 2: (v0, v1_top, v0_top)
        res->indices.push_back(v0);
        res->indices.push_back(v1_top);
        res->indices.push_back(v0_top);
        res->entities.push_back(lat_ref);
    }

    res->view.struct_size = (uint32_t)sizeof(astra_tessellation_view);
    res->view.flags = 0;
    res->view.geometry_revision = res->revision;
    res->view.vertex_count = total_verts;
    res->view.triangle_count = (uint32_t)res->entities.size();
    res->view.positions_xyz = res->positions.data();
    res->view.triangle_indices = res->indices.data();
    res->view.triangle_entities = res->entities.data();

    *out_result = res;
    return ASTRA_GEOM_OK;
}

astra_geom_status astra_geom_evaluate_feature(
    const astra_geom_model_handle *model,
    const astra_geom_feature_request *request,
    astra_geom_result_handle **out_result) {
    if (!model || model->magic != MODEL_MAGIC) {
        set_error("astra_geom_evaluate_feature: invalid model handle");
        return ASTRA_GEOM_INVALID_ARGUMENT;
    }
    if (!request || request->struct_size != sizeof(astra_geom_feature_request)) {
        set_error("astra_geom_evaluate_feature: invalid request structure or size mismatch");
        return ASTRA_GEOM_INVALID_ARGUMENT;
    }
    if (!out_result) {
        set_error("astra_geom_evaluate_feature: out_result pointer is null");
        return ASTRA_GEOM_INVALID_ARGUMENT;
    }
    *out_result = nullptr;

    auto *non_const_model = const_cast<astra_geom_model_handle *>(model);

    switch (request->operation_kind) {
        case ASTRA_OP_SKETCH_PROFILE:
            return evaluate_sketch_profile(non_const_model, request, out_result);
        case ASTRA_OP_EXTRUDE:
            return evaluate_extrude(non_const_model, request, out_result);
        default:
            set_error("astra_geom_evaluate_feature: unsupported operation kind " +
                      std::to_string(request->operation_kind));
            return ASTRA_GEOM_UNSUPPORTED_OPERATION;
    }
}
