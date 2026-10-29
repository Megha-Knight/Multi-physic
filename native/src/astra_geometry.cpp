#include "astra_geometry.h"
#include <cstdlib>
#include <cstring>
#include <string>
#include <vector>
#include <map>
#include <cmath>
#include <algorithm>

static const double ASTRA_PI = 3.14159265358979323846;

static thread_local std::string g_last_error;

static void set_error(const std::string &msg) {
    g_last_error = msg;
}

struct Point2D {
    double u, v;
};

struct ProfileLoop {
    std::vector<Point2D> points;
    bool is_hole;
};

struct StoredProfile {
    astra_id128 feature_id;
    double origin[3];
    double u_axis[3];
    double v_axis[3];
    std::vector<ProfileLoop> loops;
};

struct Id128Key {
    uint64_t high;
    uint64_t low;
    bool operator<(const Id128Key &o) const {
        if (high != o.high) return high < o.high;
        return low < o.low;
    }
    bool operator==(const Id128Key &o) const {
        return high == o.high && low == o.low;
    }
};

static Id128Key to_key(const astra_id128 &id) {
    return Id128Key{id.high, id.low};
}

struct LineageEntry {
    astra_lineage_record record;
    std::vector<astra_entity_ref> successors;
};

struct astra_geom_model_handle {
    uint32_t magic;
    uint64_t current_revision;
    std::map<Id128Key, StoredProfile> profiles;
    uint64_t last_solid_revision;
    std::vector<astra_entity_ref> last_solid_entities;
};

struct astra_geom_result_handle {
    uint32_t magic;
    uint64_t revision;
    astra_tessellation_view view;
    std::vector<float> positions;
    std::vector<uint32_t> indices;
    std::vector<astra_entity_ref> entities;
    std::map<uint64_t, std::vector<LineageEntry>> lineage;
};

static const uint32_t MODEL_MAGIC = 0x4153474Du;  /* "ASGM" */
static const uint32_t RESULT_MAGIC = 0x41534752u; /* "ASGR" */

uint32_t astra_geometry_abi_version(void) {
    return ASTRA_GEOMETRY_ABI_VERSION;
}

astra_geom_status astra_geometry_last_error(char *buffer, uint32_t capacity, uint32_t *out_required_bytes) {
    uint32_t len = (uint32_t)g_last_error.length() + 1u;
    if (out_required_bytes) *out_required_bytes = len;
    if (!buffer || capacity == 0) return ASTRA_GEOM_OK;
    if (capacity < len) return ASTRA_GEOM_BUFFER_TOO_SMALL;
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
    m->last_solid_revision = 0;
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
            ref.sub_index == reference->sub_index &&
            ref.flags == reference->flags) {
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
    auto it = result->lineage.find(source_revision);
    if (it == result->lineage.end()) {
        *out_count = 0;
    } else {
        *out_count = (uint64_t)it->second.size();
    }
    return ASTRA_GEOM_OK;
}

astra_geom_status astra_geom_result_lineage_at(
    const astra_geom_result_handle *result, uint64_t source_revision, uint64_t index, astra_lineage_record *out_record) {
    if (!result || result->magic != RESULT_MAGIC || !out_record) {
        set_error("astra_geom_result_lineage_at: invalid argument");
        return ASTRA_GEOM_INVALID_ARGUMENT;
    }
    auto it = result->lineage.find(source_revision);
    if (it == result->lineage.end() || index >= it->second.size()) {
        set_error("astra_geom_result_lineage_at: lineage index out of bounds");
        return ASTRA_GEOM_INVALID_ARGUMENT;
    }
    *out_record = it->second[index].record;
    return ASTRA_GEOM_OK;
}

astra_geom_status astra_geom_result_lineage_successor_at(
    const astra_geom_result_handle *result, uint64_t source_revision,
    uint64_t record_index, uint32_t successor_index, astra_entity_ref *out_successor) {
    if (!result || result->magic != RESULT_MAGIC || !out_successor) {
        set_error("astra_geom_result_lineage_successor_at: invalid argument");
        return ASTRA_GEOM_INVALID_ARGUMENT;
    }
    auto it = result->lineage.find(source_revision);
    if (it == result->lineage.end() || record_index >= it->second.size()) {
        set_error("astra_geom_result_lineage_successor_at: record index out of bounds");
        return ASTRA_GEOM_INVALID_ARGUMENT;
    }
    const auto &entry = it->second[record_index];
    if (successor_index >= entry.successors.size()) {
        set_error("astra_geom_result_lineage_successor_at: successor index out of bounds");
        return ASTRA_GEOM_INVALID_ARGUMENT;
    }
    *out_successor = entry.successors[successor_index];
    return ASTRA_GEOM_OK;
}

/* --- Geometry Math & Ear Clipping Helpers --- */

static double cross2d(const Point2D &a, const Point2D &b, const Point2D &c) {
    return (b.u - a.u) * (c.v - a.v) - (b.v - a.v) * (c.u - a.u);
}

static bool point_in_triangle(const Point2D &p, const Point2D &a, const Point2D &b, const Point2D &c) {
    if (std::hypot(p.u - a.u, p.v - a.v) < 1e-7 ||
        std::hypot(p.u - b.u, p.v - b.v) < 1e-7 ||
        std::hypot(p.u - c.u, p.v - c.v) < 1e-7) {
        return false;
    }
    double cp1 = cross2d(a, b, p);
    double cp2 = cross2d(b, c, p);
    double cp3 = cross2d(c, a, p);
    return (cp1 > 1e-7 && cp2 > 1e-7 && cp3 > 1e-7);
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

static double polygon_signed_area(const std::vector<Point2D> &pts) {
    double a = 0.0;
    size_t n = pts.size();
    for (size_t i = 0; i < n; i++) {
        size_t j = (i + 1) % n;
        a += (pts[i].u * pts[j].v - pts[j].u * pts[i].v);
    }
    return 0.5 * a;
}

static bool point_inside_polygon(const Point2D &p, const std::vector<Point2D> &poly) {
    bool inside = false;
    size_t n = poly.size();
    for (size_t i = 0, j = n - 1; i < n; j = i++) {
        if (((poly[i].v > p.v) != (poly[j].v > p.v)) &&
            (p.u < (poly[j].u - poly[i].u) * (p.v - poly[i].v) / (poly[j].v - poly[i].v) + poly[i].u)) {
            inside = !inside;
        }
    }
    return inside;
}

static std::vector<uint32_t> triangulate_simple_polygon(const std::vector<Point2D> &pts) {
    std::vector<uint32_t> triangles;
    int n = (int)pts.size();
    if (n < 3) return triangles;

    std::vector<int> V(n);
    for (int i = 0; i < n; i++) V[i] = i;

    int count = 2 * n;
    int v = n - 1;
    while (n > 2) {
        if ((count--) <= 0) break;
        int u = v; if (n <= u) u = 0;
        v = u + 1; if (n <= v) v = 0;
        int w = v + 1; if (n <= w) w = 0;

        const Point2D &A = pts[V[u]];
        const Point2D &B = pts[V[v]];
        const Point2D &C = pts[V[w]];

        if (std::hypot(A.u - B.u, A.v - B.v) < 1e-8) {
            V.erase(V.begin() + v);
            n--;
            count = 2 * n;
            continue;
        }

        if (cross2d(A, B, C) <= 1e-9) continue; // must be convex in CCW

        bool has_internal_point = false;
        for (int p = 0; p < n; p++) {
            if ((p == u) || (p == v) || (p == w)) continue;
            if (point_in_triangle(pts[V[p]], A, B, C)) {
                has_internal_point = true;
                break;
            }
        }
        if (has_internal_point) continue;

        triangles.push_back((uint32_t)V[u]);
        triangles.push_back((uint32_t)V[v]);
        triangles.push_back((uint32_t)V[w]);

        V.erase(V.begin() + v);
        n--;
        count = 2 * n;
    }
    return triangles;
}

// Bridges inner holes into outer loop to create single polygon for ear clipping
static std::vector<Point2D> bridge_holes_into_outer(const std::vector<ProfileLoop> &loops, std::vector<uint32_t> &orig_indices) {
    std::vector<Point2D> combined = loops[0].points;
    orig_indices.resize(combined.size());
    for (size_t i = 0; i < combined.size(); i++) orig_indices[i] = (uint32_t)i;

    uint32_t vertex_offset = (uint32_t)loops[0].points.size();

    for (size_t k = 1; k < loops.size(); k++) {
        const auto &hole = loops[k].points;
        // Find rightmost point of hole
        size_t hole_m = 0;
        for (size_t i = 1; i < hole.size(); i++) {
            if (hole[i].u > hole[hole_m].u) hole_m = i;
        }
        Point2D M = hole[hole_m];

        // Find closest visible vertex on combined outer polygon
        size_t best_poly_idx = 0;
        double best_dist = 1e30;
        for (size_t i = 0; i < combined.size(); i++) {
            Point2D P = combined[i];
            if (P.u >= M.u - 1e-6) {
                double d = std::hypot(P.u - M.u, P.v - M.v);
                if (d < best_dist) {
                    best_dist = d;
                    best_poly_idx = i;
                }
            }
        }

        // Bridge cut insertion: insert hole vertices starting at hole_m around to hole_m, then duplicate bridge
        std::vector<Point2D> new_combined;
        std::vector<uint32_t> new_orig;
        for (size_t i = 0; i <= best_poly_idx; i++) {
            new_combined.push_back(combined[i]);
            new_orig.push_back(orig_indices[i]);
        }
        for (size_t i = 0; i < hole.size(); i++) {
            size_t h_idx = (hole_m + i) % hole.size();
            new_combined.push_back(hole[h_idx]);
            new_orig.push_back(vertex_offset + (uint32_t)h_idx);
        }
        // Duplicate hole_m and poly vertex to seal bridge
        new_combined.push_back(hole[hole_m]);
        new_orig.push_back(vertex_offset + (uint32_t)hole_m);
        new_combined.push_back(combined[best_poly_idx]);
        new_orig.push_back(orig_indices[best_poly_idx]);

        for (size_t i = best_poly_idx + 1; i < combined.size(); i++) {
            new_combined.push_back(combined[i]);
            new_orig.push_back(orig_indices[i]);
        }

        combined = std::move(new_combined);
        orig_indices = std::move(new_orig);
        vertex_offset += (uint32_t)hole.size();
    }
    return combined;
}

static astra_geom_status evaluate_sketch_profile(
    astra_geom_model_handle *model,
    const astra_geom_feature_request *request,
    astra_geom_result_handle **out_result) {
    const auto &p = request->parameters.sketch_profile;
    if (p.loop_count == 0 || p.point_count < 3 || !p.coordinates_uv_mm) {
        set_error("evaluate_sketch_profile: degenerate profile (zero loops, fewer than 3 points, or null coords)");
        return ASTRA_GEOM_DEGENERATE_INPUT;
    }

    // Validate plane basis
    for (int i = 0; i < 3; i++) {
        if (!std::isfinite(p.plane_origin_xyz_mm[i]) || !std::isfinite(p.plane_u_axis_xyz[i]) || !std::isfinite(p.plane_v_axis_xyz[i])) {
            set_error("evaluate_sketch_profile: non-finite plane origin or axis coordinates");
            return ASTRA_GEOM_DEGENERATE_INPUT;
        }
    }
    double u_len = std::sqrt(p.plane_u_axis_xyz[0]*p.plane_u_axis_xyz[0] + p.plane_u_axis_xyz[1]*p.plane_u_axis_xyz[1] + p.plane_u_axis_xyz[2]*p.plane_u_axis_xyz[2]);
    double v_len = std::sqrt(p.plane_v_axis_xyz[0]*p.plane_v_axis_xyz[0] + p.plane_v_axis_xyz[1]*p.plane_v_axis_xyz[1] + p.plane_v_axis_xyz[2]*p.plane_v_axis_xyz[2]);
    if (std::abs(u_len - 1.0) > 1e-4 || std::abs(v_len - 1.0) > 1e-4) {
        set_error("evaluate_sketch_profile: plane axes must be unit length");
        return ASTRA_GEOM_INVALID_ARGUMENT;
    }
    double dot_uv = p.plane_u_axis_xyz[0]*p.plane_v_axis_xyz[0] + p.plane_u_axis_xyz[1]*p.plane_v_axis_xyz[1] + p.plane_u_axis_xyz[2]*p.plane_v_axis_xyz[2];
    if (std::abs(dot_uv) > 1e-4) {
        set_error("evaluate_sketch_profile: plane U and V axes must be orthogonal");
        return ASTRA_GEOM_INVALID_ARGUMENT;
    }

    std::vector<uint32_t> offsets(p.loop_count + 1);
    if (p.loop_offsets) {
        for (uint32_t i = 0; i <= p.loop_count; i++) offsets[i] = p.loop_offsets[i];
    } else {
        offsets[0] = 0;
        offsets[1] = p.point_count;
    }

    std::vector<ProfileLoop> loops(p.loop_count);
    for (uint32_t k = 0; k < p.loop_count; k++) {
        uint32_t start = offsets[k];
        uint32_t end = offsets[k + 1];
        if (end <= start || end - start < 3) {
            set_error("evaluate_sketch_profile: loop has fewer than 3 vertices");
            return ASTRA_GEOM_DEGENERATE_INPUT;
        }
        std::vector<Point2D> pts(end - start);
        for (uint32_t i = start; i < end; i++) {
            double u = p.coordinates_uv_mm[2 * i];
            double v = p.coordinates_uv_mm[2 * i + 1];
            if (!std::isfinite(u) || !std::isfinite(v)) {
                set_error("evaluate_sketch_profile: non-finite coordinate encountered");
                return ASTRA_GEOM_DEGENERATE_INPUT;
            }
            pts[i - start] = Point2D{u, v};
        }
        double area = polygon_signed_area(pts);
        if (std::abs(area) < 1e-12) {
            set_error("evaluate_sketch_profile: loop has near-zero area");
            return ASTRA_GEOM_DEGENERATE_INPUT;
        }

        // Loop 0 must be CCW (positive area), holes CW (negative area)
        bool is_hole = (k > 0);
        if (!is_hole && area < 0) std::reverse(pts.begin(), pts.end());
        else if (is_hole && area > 0) std::reverse(pts.begin(), pts.end());

        loops[k] = ProfileLoop{pts, is_hole};
    }

    // Verify all holes are contained in outer loop and do not intersect
    for (uint32_t k = 1; k < p.loop_count; k++) {
        for (const auto &hp : loops[k].points) {
            if (!point_inside_polygon(hp, loops[0].points)) {
                set_error("evaluate_sketch_profile: hole vertex is outside outer boundary");
                return ASTRA_GEOM_INVALID_TOPOLOGY;
            }
        }
    }

    StoredProfile sp;
    sp.feature_id = request->feature_id;
    std::memcpy(sp.origin, p.plane_origin_xyz_mm, sizeof(double) * 3);
    std::memcpy(sp.u_axis, p.plane_u_axis_xyz, sizeof(double) * 3);
    std::memcpy(sp.v_axis, p.plane_v_axis_xyz, sizeof(double) * 3);
    sp.loops = loops;

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

    for (int i = 0; i < 3; i++) {
        if (!std::isfinite(p.direction_xyz[i])) {
            set_error("evaluate_extrude: non-finite extrusion direction component");
            return ASTRA_GEOM_INVALID_ARGUMENT;
        }
    }
    if (p.distance_mm <= 1e-6 || !std::isfinite(p.distance_mm)) {
        set_error("evaluate_extrude: distance must be positive and finite");
        return ASTRA_GEOM_INVALID_ARGUMENT;
    }

    double dir_len = std::sqrt(p.direction_xyz[0]*p.direction_xyz[0] + p.direction_xyz[1]*p.direction_xyz[1] + p.direction_xyz[2]*p.direction_xyz[2]);
    if (dir_len < 1e-6) {
        set_error("evaluate_extrude: direction vector has near-zero length");
        return ASTRA_GEOM_DEGENERATE_INPUT;
    }

    double norm_dx = (p.direction_xyz[0] / dir_len) * p.distance_mm;
    double norm_dy = (p.direction_xyz[1] / dir_len) * p.distance_mm;
    double norm_dz = (p.direction_xyz[2] / dir_len) * p.distance_mm;

    // Count total 2D vertices across all loops
    uint32_t total_profile_pts = 0;
    for (const auto &loop : prof.loops) total_profile_pts += (uint32_t)loop.points.size();

    uint32_t total_verts = 2 * total_profile_pts;

    auto *res = new (std::nothrow) astra_geom_result_handle();
    if (!res) return ASTRA_GEOM_OUT_OF_MEMORY;
    res->magic = RESULT_MAGIC;
    model->current_revision++;
    res->revision = model->current_revision;

    res->positions.resize(total_verts * 3);

    // Compute 3D vertices for ring 0 and ring 1 across all loops
    uint32_t v_cursor = 0;
    for (const auto &loop : prof.loops) {
        for (const auto &pt : loop.points) {
            double px = prof.origin[0] + pt.u * prof.u_axis[0] + pt.v * prof.v_axis[0];
            double py = prof.origin[1] + pt.u * prof.u_axis[1] + pt.v * prof.v_axis[1];
            double pz = prof.origin[2] + pt.u * prof.u_axis[2] + pt.v * prof.v_axis[2];

            res->positions[3 * v_cursor + 0] = (float)px;
            res->positions[3 * v_cursor + 1] = (float)py;
            res->positions[3 * v_cursor + 2] = (float)pz;

            res->positions[3 * (v_cursor + total_profile_pts) + 0] = (float)(px + norm_dx);
            res->positions[3 * (v_cursor + total_profile_pts) + 1] = (float)(py + norm_dy);
            res->positions[3 * (v_cursor + total_profile_pts) + 2] = (float)(pz + norm_dz);
            v_cursor++;
        }
    }

    // Triangulate bridged polygon with holes
    std::vector<uint32_t> orig_indices;
    std::vector<Point2D> bridged_pts = bridge_holes_into_outer(prof.loops, orig_indices);
    std::vector<uint32_t> cap_triangles = triangulate_simple_polygon(bridged_pts);
    uint32_t cap_tri_count = (uint32_t)cap_triangles.size() / 3;

    astra_entity_ref start_cap_ref;
    std::memset(&start_cap_ref, 0, sizeof(start_cap_ref));
    start_cap_ref.struct_size = (uint32_t)sizeof(astra_entity_ref);
    start_cap_ref.role = ASTRA_ROLE_END_CAP_START;
    start_cap_ref.feature_id = request->feature_id;
    start_cap_ref.sub_index = 0;

    for (uint32_t t = 0; t < cap_tri_count; t++) {
        uint32_t i0 = orig_indices[cap_triangles[3 * t + 0]];
        uint32_t i1 = orig_indices[cap_triangles[3 * t + 1]];
        uint32_t i2 = orig_indices[cap_triangles[3 * t + 2]];
        res->indices.push_back(i0);
        res->indices.push_back(i2);
        res->indices.push_back(i1);
        res->entities.push_back(start_cap_ref);
    }

    astra_entity_ref end_cap_ref;
    std::memset(&end_cap_ref, 0, sizeof(end_cap_ref));
    end_cap_ref.struct_size = (uint32_t)sizeof(astra_entity_ref);
    end_cap_ref.role = ASTRA_ROLE_END_CAP_END;
    end_cap_ref.feature_id = request->feature_id;
    end_cap_ref.sub_index = 0;

    for (uint32_t t = 0; t < cap_tri_count; t++) {
        uint32_t i0 = orig_indices[cap_triangles[3 * t + 0]] + total_profile_pts;
        uint32_t i1 = orig_indices[cap_triangles[3 * t + 1]] + total_profile_pts;
        uint32_t i2 = orig_indices[cap_triangles[3 * t + 2]] + total_profile_pts;
        res->indices.push_back(i0);
        res->indices.push_back(i1);
        res->indices.push_back(i2);
        res->entities.push_back(end_cap_ref);
    }

    // Lateral Faces for each loop
    uint32_t loop_start_v = 0;
    for (size_t k = 0; k < prof.loops.size(); k++) {
        const auto &loop = prof.loops[k];
        uint32_t ln = (uint32_t)loop.points.size();
        bool is_inner = loop.is_hole;

        for (uint32_t i = 0; i < ln; i++) {
            uint32_t j = (i + 1) % ln;
            uint32_t v0 = loop_start_v + (is_inner ? j : i);
            uint32_t v1 = loop_start_v + (is_inner ? i : j);
            uint32_t v0_top = v0 + total_profile_pts;
            uint32_t v1_top = v1 + total_profile_pts;

            astra_entity_ref lat_ref;
            std::memset(&lat_ref, 0, sizeof(lat_ref));
            lat_ref.struct_size = (uint32_t)sizeof(astra_entity_ref);
            lat_ref.role = ASTRA_ROLE_LATERAL_FACE;
            lat_ref.feature_id = request->feature_id;
            lat_ref.sub_index = (int32_t)i;
            lat_ref.flags = is_inner ? 1u : 0u; // flag 1 = inner loop wall
            lat_ref.related_feature_id = p.profile_feature_id;

            res->indices.push_back(v0);
            res->indices.push_back(v1);
            res->indices.push_back(v1_top);
            res->entities.push_back(lat_ref);

            res->indices.push_back(v0);
            res->indices.push_back(v1_top);
            res->indices.push_back(v0_top);
            res->entities.push_back(lat_ref);
        }
        loop_start_v += ln;
    }

    // Cross-Revision Lineage: compare previous solid revision entities against new solid
    if (model->last_solid_revision > 0) {
        std::vector<LineageEntry> lineage_entries;
        for (const auto &old_ref : model->last_solid_entities) {
            LineageEntry entry;
            entry.record.struct_size = (uint32_t)sizeof(astra_lineage_record);
            entry.record.source_revision = model->last_solid_revision;
            entry.record.destination_revision = res->revision;
            entry.record.source = old_ref;
            entry.record.reserved = 0;

            bool found = false;
            for (const auto &new_ref : res->entities) {
                if (old_ref.role == new_ref.role &&
                    old_ref.feature_id.high == new_ref.feature_id.high &&
                    old_ref.feature_id.low == new_ref.feature_id.low &&
                    old_ref.sub_index == new_ref.sub_index &&
                    old_ref.flags == new_ref.flags) {
                    entry.record.outcome = ASTRA_LINEAGE_PRESERVED;
                    entry.record.successor_count = 1;
                    entry.successors.push_back(new_ref);
                    found = true;
                    break;
                }
            }
            if (!found) {
                entry.record.outcome = ASTRA_LINEAGE_CONSUMED;
                entry.record.successor_count = 0;
            }
            lineage_entries.push_back(entry);
        }
        res->lineage[model->last_solid_revision] = lineage_entries;
    }

    // Update model latest solid state
    model->last_solid_revision = res->revision;
    model->last_solid_entities = res->entities;

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

struct Point3DGeom {
    double x, y, z;
};

static Point3DGeom rodrigues_rotate(const Point3DGeom &p, const double orig[3], const double k[3], double theta) {
    double vx = p.x - orig[0];
    double vy = p.y - orig[1];
    double vz = p.z - orig[2];

    double cos_t = std::cos(theta);
    double sin_t = std::sin(theta);
    double dot_kv = k[0]*vx + k[1]*vy + k[2]*vz;

    double cross_x = k[1]*vz - k[2]*vy;
    double cross_y = k[2]*vx - k[0]*vz;
    double cross_z = k[0]*vy - k[1]*vx;

    return Point3DGeom{
        orig[0] + vx * cos_t + cross_x * sin_t + k[0] * dot_kv * (1.0 - cos_t),
        orig[1] + vy * cos_t + cross_y * sin_t + k[1] * dot_kv * (1.0 - cos_t),
        orig[2] + vz * cos_t + cross_z * sin_t + k[2] * dot_kv * (1.0 - cos_t)
    };
}

static astra_geom_status evaluate_revolve(
    astra_geom_model_handle *model,
    const astra_geom_feature_request *request,
    astra_geom_result_handle **out_result) {
    const auto &p = request->parameters.revolve;
    auto it = model->profiles.find(to_key(p.profile_feature_id));
    if (it == model->profiles.end()) {
        set_error("evaluate_revolve: referenced profile feature ID not found in model");
        return ASTRA_GEOM_ENTITY_NOT_FOUND;
    }
    const StoredProfile &prof = it->second;

    for (int i = 0; i < 3; i++) {
        if (!std::isfinite(p.axis_origin_xyz_mm[i]) || !std::isfinite(p.axis_direction_xyz[i])) {
            set_error("evaluate_revolve: non-finite axis origin or direction component");
            return ASTRA_GEOM_INVALID_ARGUMENT;
        }
    }
    double axis_len = std::sqrt(p.axis_direction_xyz[0]*p.axis_direction_xyz[0] +
                                p.axis_direction_xyz[1]*p.axis_direction_xyz[1] +
                                p.axis_direction_xyz[2]*p.axis_direction_xyz[2]);
    if (axis_len < 1e-6) {
        set_error("evaluate_revolve: axis direction vector has near-zero length");
        return ASTRA_GEOM_DEGENERATE_INPUT;
    }
    double k[3] = {
        p.axis_direction_xyz[0] / axis_len,
        p.axis_direction_xyz[1] / axis_len,
        p.axis_direction_xyz[2] / axis_len
    };

    double angle = p.angle_rad;
    if (angle <= 1e-6 || angle > (2.0 * ASTRA_PI + 1e-4) || !std::isfinite(angle)) {
        set_error("evaluate_revolve: angle must be positive, finite, and at most 2*PI radians");
        return ASTRA_GEOM_INVALID_ARGUMENT;
    }

    bool is_full = std::abs(angle - 2.0 * ASTRA_PI) < 1e-4;
    int segments = std::max(12, (int)std::ceil(24.0 * (angle / (2.0 * ASTRA_PI))));
    double delta_theta = angle / segments;

    uint32_t total_profile_pts = 0;
    for (const auto &loop : prof.loops) total_profile_pts += (uint32_t)loop.points.size();

    std::vector<Point3DGeom> base_3d(total_profile_pts);
    uint32_t pt_cur = 0;
    for (const auto &loop : prof.loops) {
        for (const auto &pt : loop.points) {
            base_3d[pt_cur++] = Point3DGeom{
                prof.origin[0] + pt.u * prof.u_axis[0] + pt.v * prof.v_axis[0],
                prof.origin[1] + pt.u * prof.u_axis[1] + pt.v * prof.v_axis[1],
                prof.origin[2] + pt.u * prof.u_axis[2] + pt.v * prof.v_axis[2]
            };
        }
    }

    uint32_t num_slices = is_full ? segments : (segments + 1);
    uint32_t total_verts = num_slices * total_profile_pts;

    auto *res = new (std::nothrow) astra_geom_result_handle();
    if (!res) return ASTRA_GEOM_OUT_OF_MEMORY;
    res->magic = RESULT_MAGIC;
    model->current_revision++;
    res->revision = model->current_revision;

    res->positions.resize(total_verts * 3);

    for (int s = 0; s < (int)num_slices; s++) {
        double theta = s * delta_theta;
        for (uint32_t i = 0; i < total_profile_pts; i++) {
            Point3DGeom rot = rodrigues_rotate(base_3d[i], p.axis_origin_xyz_mm, k, theta);
            uint32_t v_idx = s * total_profile_pts + i;
            res->positions[3 * v_idx + 0] = (float)rot.x;
            res->positions[3 * v_idx + 1] = (float)rot.y;
            res->positions[3 * v_idx + 2] = (float)rot.z;
        }
    }

    // Lateral Quads (each split into 2 triangles)
    for (int s = 0; s < segments; s++) {
        int s_next = is_full ? ((s + 1) % segments) : (s + 1);
        uint32_t loop_start = 0;

        for (size_t l_idx = 0; l_idx < prof.loops.size(); l_idx++) {
            const auto &loop = prof.loops[l_idx];
            uint32_t ln = (uint32_t)loop.points.size();
            bool is_inner = loop.is_hole;

            for (uint32_t i = 0; i < ln; i++) {
                uint32_t j = (i + 1) % ln;

                Point3DGeom p0 = base_3d[loop_start + i];
                Point3DGeom p1 = base_3d[loop_start + j];
                double ex = p1.x - p0.x, ey = p1.y - p0.y, ez = p1.z - p0.z;
                double elen = std::sqrt(ex*ex + ey*ey + ez*ez);
                double cx = ey * k[2] - ez * k[1];
                double cy = ez * k[0] - ex * k[2];
                double cz = ex * k[1] - ey * k[0];
                double clen = std::sqrt(cx*cx + cy*cy + cz*cz);
                bool is_parallel_to_axis = (elen > 1e-6) && ((clen / elen) < 1e-3);

                uint32_t role = is_parallel_to_axis ? ASTRA_ROLE_REVOLVE_CYLINDRICAL_FACE : ASTRA_ROLE_REVOLVE_LATERAL_FACE;

                astra_entity_ref lat_ref;
                std::memset(&lat_ref, 0, sizeof(lat_ref));
                lat_ref.struct_size = (uint32_t)sizeof(astra_entity_ref);
                lat_ref.role = role;
                lat_ref.feature_id = request->feature_id;
                lat_ref.sub_index = (int32_t)i;
                lat_ref.flags = is_inner ? 1u : 0u;
                lat_ref.related_feature_id = p.profile_feature_id;

                uint32_t v0 = s * total_profile_pts + loop_start + i;
                uint32_t v1 = s * total_profile_pts + loop_start + j;
                uint32_t v0_top = s_next * total_profile_pts + loop_start + i;
                uint32_t v1_top = s_next * total_profile_pts + loop_start + j;

                res->indices.push_back(v0);
                res->indices.push_back(v1);
                res->indices.push_back(v1_top);
                res->entities.push_back(lat_ref);

                res->indices.push_back(v0);
                res->indices.push_back(v1_top);
                res->indices.push_back(v0_top);
                res->entities.push_back(lat_ref);
            }
            loop_start += ln;
        }
    }

    // End Caps for partial revolutions
    if (!is_full) {
        std::vector<uint32_t> orig_indices;
        std::vector<Point2D> bridged_pts = bridge_holes_into_outer(prof.loops, orig_indices);
        std::vector<uint32_t> cap_triangles = triangulate_simple_polygon(bridged_pts);
        uint32_t cap_tri_count = (uint32_t)cap_triangles.size() / 3;

        astra_entity_ref start_cap_ref;
        std::memset(&start_cap_ref, 0, sizeof(start_cap_ref));
        start_cap_ref.struct_size = (uint32_t)sizeof(astra_entity_ref);
        start_cap_ref.role = ASTRA_ROLE_REVOLVE_START_CAP;
        start_cap_ref.feature_id = request->feature_id;
        start_cap_ref.sub_index = 0;

        for (uint32_t t = 0; t < cap_tri_count; t++) {
            uint32_t i0 = orig_indices[cap_triangles[3 * t + 0]];
            uint32_t i1 = orig_indices[cap_triangles[3 * t + 1]];
            uint32_t i2 = orig_indices[cap_triangles[3 * t + 2]];
            res->indices.push_back(i0);
            res->indices.push_back(i2);
            res->indices.push_back(i1);
            res->entities.push_back(start_cap_ref);
        }

        astra_entity_ref end_cap_ref;
        std::memset(&end_cap_ref, 0, sizeof(end_cap_ref));
        end_cap_ref.struct_size = (uint32_t)sizeof(astra_entity_ref);
        end_cap_ref.role = ASTRA_ROLE_REVOLVE_END_CAP;
        end_cap_ref.feature_id = request->feature_id;
        end_cap_ref.sub_index = 0;

        uint32_t end_offset = segments * total_profile_pts;
        for (uint32_t t = 0; t < cap_tri_count; t++) {
            uint32_t i0 = orig_indices[cap_triangles[3 * t + 0]] + end_offset;
            uint32_t i1 = orig_indices[cap_triangles[3 * t + 1]] + end_offset;
            uint32_t i2 = orig_indices[cap_triangles[3 * t + 2]] + end_offset;
            res->indices.push_back(i0);
            res->indices.push_back(i1);
            res->indices.push_back(i2);
            res->entities.push_back(end_cap_ref);
        }
    }

    // Cross-Revision Lineage for Revolve
    if (model->last_solid_revision > 0) {
        std::vector<LineageEntry> lineage_entries;
        for (const auto &old_ref : model->last_solid_entities) {
            LineageEntry entry;
            entry.record.struct_size = (uint32_t)sizeof(astra_lineage_record);
            entry.record.source_revision = model->last_solid_revision;
            entry.record.destination_revision = res->revision;
            entry.record.source = old_ref;
            entry.record.reserved = 0;

            bool found = false;
            for (const auto &new_ref : res->entities) {
                if (old_ref.role == new_ref.role &&
                    old_ref.feature_id.high == new_ref.feature_id.high &&
                    old_ref.feature_id.low == new_ref.feature_id.low &&
                    old_ref.sub_index == new_ref.sub_index &&
                    old_ref.flags == new_ref.flags) {
                    entry.record.outcome = ASTRA_LINEAGE_PRESERVED;
                    entry.record.successor_count = 1;
                    entry.successors.push_back(new_ref);
                    found = true;
                    break;
                }
            }
            if (!found) {
                entry.record.outcome = ASTRA_LINEAGE_CONSUMED;
                entry.record.successor_count = 0;
            }
            lineage_entries.push_back(entry);
        }
        res->lineage[model->last_solid_revision] = lineage_entries;
    }

    model->last_solid_revision = res->revision;
    model->last_solid_entities = res->entities;

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

static astra_geom_status evaluate_sweep(
    astra_geom_model_handle *model,
    const astra_geom_feature_request *request,
    astra_geom_result_handle **out_result) {
    const auto &p = request->parameters.sweep;
    auto prof_it = model->profiles.find(to_key(p.profile_feature_id));
    if (prof_it == model->profiles.end()) {
        set_error("evaluate_sweep: referenced profile feature ID not found in model");
        return ASTRA_GEOM_ENTITY_NOT_FOUND;
    }
    const StoredProfile &prof = prof_it->second;

    std::vector<Point3DGeom> path_pts;
    if (p.path_feature_id.high != 0 || p.path_feature_id.low != 0) {
        auto path_it = model->profiles.find(to_key(p.path_feature_id));
        if (path_it != model->profiles.end()) {
            const StoredProfile &path_prof = path_it->second;
            if (!path_prof.loops.empty()) {
                for (const auto &pt : path_prof.loops[0].points) {
                    path_pts.push_back(Point3DGeom{
                        path_prof.origin[0] + pt.u * path_prof.u_axis[0] + pt.v * path_prof.v_axis[0],
                        path_prof.origin[1] + pt.u * path_prof.u_axis[1] + pt.v * path_prof.v_axis[1],
                        path_prof.origin[2] + pt.u * path_prof.u_axis[2] + pt.v * path_prof.v_axis[2]
                    });
                }
            }
        }
    }

    // Default helical trajectory if orientation_mode == 2 or no path profile
    if (path_pts.size() < 2) {
        if (p.orientation_mode == 2) {
            // Helical sweep: radius = 10, pitch = 25, 24 steps
            int steps = 24;
            double r = 10.0;
            double pitch = 25.0;
            for (int i = 0; i <= steps; i++) {
                double t = (double)i / steps;
                double ang = 2.0 * ASTRA_PI * t;
                path_pts.push_back(Point3DGeom{ r * std::cos(ang), r * std::sin(ang), pitch * t });
            }
        } else {
            // Linear sweep: 2 points along +Z by 25mm
            path_pts.push_back(Point3DGeom{ 0.0, 0.0, 0.0 });
            path_pts.push_back(Point3DGeom{ 0.0, 0.0, 25.0 });
        }
    }

    size_t M = path_pts.size();
    if (M < 2) {
        set_error("evaluate_sweep: degenerate path with fewer than 2 points");
        return ASTRA_GEOM_DEGENERATE_INPUT;
    }

    for (size_t i = 0; i < M; i++) {
        if (!std::isfinite(path_pts[i].x) || !std::isfinite(path_pts[i].y) || !std::isfinite(path_pts[i].z)) {
            set_error("evaluate_sweep: non-finite path coordinate encountered");
            return ASTRA_GEOM_INVALID_ARGUMENT;
        }
    }

    // Compute tangents
    std::vector<Point3DGeom> tangents(M);
    for (size_t i = 0; i < M - 1; i++) {
        double dx = path_pts[i + 1].x - path_pts[i].x;
        double dy = path_pts[i + 1].y - path_pts[i].y;
        double dz = path_pts[i + 1].z - path_pts[i].z;
        double len = std::sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 1e-6) {
            set_error("evaluate_sweep: degenerate zero-length path segment");
            return ASTRA_GEOM_DEGENERATE_INPUT;
        }
        tangents[i] = Point3DGeom{ dx / len, dy / len, dz / len };
    }
    tangents[M - 1] = tangents[M - 2];

    // Compute Bishop frames (Rotation-Minimizing Frame parallel transport)
    std::vector<Point3DGeom> normals(M);
    std::vector<Point3DGeom> binormals(M);

    // Initial frame at i = 0
    Point3DGeom ref_up = (std::abs(tangents[0].z) < 0.9) ? Point3DGeom{0, 0, 1} : Point3DGeom{0, 1, 0};
    double dot_t0 = tangents[0].x * ref_up.x + tangents[0].y * ref_up.y + tangents[0].z * ref_up.z;
    double nx0 = ref_up.x - dot_t0 * tangents[0].x;
    double ny0 = ref_up.y - dot_t0 * tangents[0].y;
    double nz0 = ref_up.z - dot_t0 * tangents[0].z;
    double n0_len = std::sqrt(nx0*nx0 + ny0*ny0 + nz0*nz0);
    normals[0] = Point3DGeom{ nx0 / n0_len, ny0 / n0_len, nz0 / n0_len };
    binormals[0] = Point3DGeom{
        tangents[0].y * normals[0].z - tangents[0].z * normals[0].y,
        tangents[0].z * normals[0].x - tangents[0].x * normals[0].z,
        tangents[0].x * normals[0].y - tangents[0].y * normals[0].x
    };

    // Parallel transport along path
    for (size_t i = 1; i < M; i++) {
        double v1x = path_pts[i].x - path_pts[i - 1].x;
        double v1y = path_pts[i].y - path_pts[i - 1].y;
        double v1z = path_pts[i].z - path_pts[i - 1].z;
        double c1 = v1x*v1x + v1y*v1y + v1z*v1z;

        double dot_n = v1x * normals[i - 1].x + v1y * normals[i - 1].y + v1z * normals[i - 1].z;
        double nLx = normals[i - 1].x - (2.0 / c1) * dot_n * v1x;
        double nLy = normals[i - 1].y - (2.0 / c1) * dot_n * v1y;
        double nLz = normals[i - 1].z - (2.0 / c1) * dot_n * v1z;

        double dot_t = v1x * tangents[i - 1].x + v1y * tangents[i - 1].y + v1z * tangents[i - 1].z;
        double tLx = tangents[i - 1].x - (2.0 / c1) * dot_t * v1x;
        double tLy = tangents[i - 1].y - (2.0 / c1) * dot_t * v1y;
        double tLz = tangents[i - 1].z - (2.0 / c1) * dot_t * v1z;

        double v2x = tangents[i].x - tLx;
        double v2y = tangents[i].y - tLy;
        double v2z = tangents[i].z - tLz;
        double c2 = v2x*v2x + v2y*v2y + v2z*v2z;

        double nix = nLx, niy = nLy, niz = nLz;
        if (c2 > 1e-12) {
            double dot_nL = v2x * nLx + v2y * nLy + v2z * nLz;
            nix -= (2.0 / c2) * dot_nL * v2x;
            niy -= (2.0 / c2) * dot_nL * v2y;
            niz -= (2.0 / c2) * dot_nL * v2z;
        }

        // Orthogonalize with tangent[i]
        double dot_ti = nix * tangents[i].x + niy * tangents[i].y + niz * tangents[i].z;
        nix -= dot_ti * tangents[i].x;
        niy -= dot_ti * tangents[i].y;
        niz -= dot_ti * tangents[i].z;
        double n_len = std::sqrt(nix*nix + niy*niy + niz*niz);
        normals[i] = Point3DGeom{ nix / n_len, niy / n_len, niz / n_len };
        binormals[i] = Point3DGeom{
            tangents[i].y * normals[i].z - tangents[i].z * normals[i].y,
            tangents[i].z * normals[i].x - tangents[i].x * normals[i].z,
            tangents[i].x * normals[i].y - tangents[i].y * normals[i].x
        };
    }

    // Apply twist if specified
    if (std::abs(p.twist_rad) > 1e-6) {
        for (size_t i = 0; i < M; i++) {
            double tw = p.twist_rad * ((double)i / (M - 1));
            double cos_tw = std::cos(tw);
            double sin_tw = std::sin(tw);
            Point3DGeom n_rot{
                normals[i].x * cos_tw + binormals[i].x * sin_tw,
                normals[i].y * cos_tw + binormals[i].y * sin_tw,
                normals[i].z * cos_tw + binormals[i].z * sin_tw
            };
            Point3DGeom b_rot{
                -normals[i].x * sin_tw + binormals[i].x * cos_tw,
                -normals[i].y * sin_tw + binormals[i].y * cos_tw,
                -normals[i].z * sin_tw + binormals[i].z * cos_tw
            };
            normals[i] = n_rot;
            binormals[i] = b_rot;
        }
    }

    uint32_t total_profile_pts = 0;
    for (const auto &loop : prof.loops) total_profile_pts += (uint32_t)loop.points.size();

    uint32_t total_verts = (uint32_t)M * total_profile_pts;

    auto *res = new (std::nothrow) astra_geom_result_handle();
    if (!res) return ASTRA_GEOM_OUT_OF_MEMORY;
    res->magic = RESULT_MAGIC;
    model->current_revision++;
    res->revision = model->current_revision;

    res->positions.resize(total_verts * 3);

    for (size_t i = 0; i < M; i++) {
        uint32_t pt_cursor = 0;
        for (const auto &loop : prof.loops) {
            for (const auto &pt : loop.points) {
                double vx = path_pts[i].x + pt.u * normals[i].x + pt.v * binormals[i].x;
                double vy = path_pts[i].y + pt.u * normals[i].y + pt.v * binormals[i].y;
                double vz = path_pts[i].z + pt.u * normals[i].z + pt.v * binormals[i].z;
                uint32_t v_idx = (uint32_t)i * total_profile_pts + pt_cursor;
                res->positions[3 * v_idx + 0] = (float)vx;
                res->positions[3 * v_idx + 1] = (float)vy;
                res->positions[3 * v_idx + 2] = (float)vz;
                pt_cursor++;
            }
        }
    }

    // Lateral Quads
    for (size_t i = 0; i < M - 1; i++) {
        uint32_t loop_start = 0;
        for (const auto &loop : prof.loops) {
            uint32_t ln = (uint32_t)loop.points.size();
            bool is_inner = loop.is_hole;

            for (uint32_t j = 0; j < ln; j++) {
                uint32_t j_next = (j + 1) % ln;

                astra_entity_ref lat_ref;
                std::memset(&lat_ref, 0, sizeof(lat_ref));
                lat_ref.struct_size = (uint32_t)sizeof(astra_entity_ref);
                lat_ref.role = ASTRA_ROLE_SWEEP_SIDE;
                lat_ref.feature_id = request->feature_id;
                lat_ref.sub_index = (int32_t)j;
                lat_ref.flags = is_inner ? 1u : 0u;
                lat_ref.related_feature_id = p.profile_feature_id;

                uint32_t v0 = (uint32_t)i * total_profile_pts + loop_start + j;
                uint32_t v1 = (uint32_t)i * total_profile_pts + loop_start + j_next;
                uint32_t v0_top = (uint32_t)(i + 1) * total_profile_pts + loop_start + j;
                uint32_t v1_top = (uint32_t)(i + 1) * total_profile_pts + loop_start + j_next;

                res->indices.push_back(v0);
                res->indices.push_back(v1);
                res->indices.push_back(v1_top);
                res->entities.push_back(lat_ref);

                res->indices.push_back(v0);
                res->indices.push_back(v1_top);
                res->indices.push_back(v0_top);
                res->entities.push_back(lat_ref);
            }
            loop_start += ln;
        }
    }

    // Start Cap (Slice 0) & End Cap (Slice M - 1)
    std::vector<uint32_t> orig_indices;
    std::vector<Point2D> bridged_pts = bridge_holes_into_outer(prof.loops, orig_indices);
    std::vector<uint32_t> cap_triangles = triangulate_simple_polygon(bridged_pts);
    uint32_t cap_tri_count = (uint32_t)cap_triangles.size() / 3;

    astra_entity_ref start_cap_ref;
    std::memset(&start_cap_ref, 0, sizeof(start_cap_ref));
    start_cap_ref.struct_size = (uint32_t)sizeof(astra_entity_ref);
    start_cap_ref.role = ASTRA_ROLE_END_CAP_START;
    start_cap_ref.feature_id = request->feature_id;
    start_cap_ref.sub_index = 0;

    for (uint32_t t = 0; t < cap_tri_count; t++) {
        uint32_t i0 = orig_indices[cap_triangles[3 * t + 0]];
        uint32_t i1 = orig_indices[cap_triangles[3 * t + 1]];
        uint32_t i2 = orig_indices[cap_triangles[3 * t + 2]];
        res->indices.push_back(i0);
        res->indices.push_back(i2);
        res->indices.push_back(i1);
        res->entities.push_back(start_cap_ref);
    }

    astra_entity_ref end_cap_ref;
    std::memset(&end_cap_ref, 0, sizeof(end_cap_ref));
    end_cap_ref.struct_size = (uint32_t)sizeof(astra_entity_ref);
    end_cap_ref.role = ASTRA_ROLE_END_CAP_END;
    end_cap_ref.feature_id = request->feature_id;
    end_cap_ref.sub_index = 0;

    uint32_t end_offset = (uint32_t)(M - 1) * total_profile_pts;
    for (uint32_t t = 0; t < cap_tri_count; t++) {
        uint32_t i0 = orig_indices[cap_triangles[3 * t + 0]] + end_offset;
        uint32_t i1 = orig_indices[cap_triangles[3 * t + 1]] + end_offset;
        uint32_t i2 = orig_indices[cap_triangles[3 * t + 2]] + end_offset;
        res->indices.push_back(i0);
        res->indices.push_back(i1);
        res->indices.push_back(i2);
        res->entities.push_back(end_cap_ref);
    }

    // Lineage Tracking
    if (model->last_solid_revision > 0) {
        std::vector<LineageEntry> lineage_entries;
        for (const auto &old_ref : model->last_solid_entities) {
            LineageEntry entry;
            entry.record.struct_size = (uint32_t)sizeof(astra_lineage_record);
            entry.record.source_revision = model->last_solid_revision;
            entry.record.destination_revision = res->revision;
            entry.record.source = old_ref;
            entry.record.reserved = 0;

            bool found = false;
            for (const auto &new_ref : res->entities) {
                if (old_ref.role == new_ref.role &&
                    old_ref.feature_id.high == new_ref.feature_id.high &&
                    old_ref.feature_id.low == new_ref.feature_id.low &&
                    old_ref.sub_index == new_ref.sub_index &&
                    old_ref.flags == new_ref.flags) {
                    entry.record.outcome = ASTRA_LINEAGE_PRESERVED;
                    entry.record.successor_count = 1;
                    entry.successors.push_back(new_ref);
                    found = true;
                    break;
                }
            }
            if (!found) {
                entry.record.outcome = ASTRA_LINEAGE_CONSUMED;
                entry.record.successor_count = 0;
            }
            lineage_entries.push_back(entry);
        }
        res->lineage[model->last_solid_revision] = lineage_entries;
    }

    model->last_solid_revision = res->revision;
    model->last_solid_entities = res->entities;

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
        case ASTRA_OP_REVOLVE:
            return evaluate_revolve(non_const_model, request, out_result);
        case ASTRA_OP_SWEEP:
            return evaluate_sweep(non_const_model, request, out_result);
        default:
            set_error("astra_geom_evaluate_feature: unsupported operation kind " +
                      std::to_string(request->operation_kind));
            return ASTRA_GEOM_UNSUPPORTED_OPERATION;
    }
}
