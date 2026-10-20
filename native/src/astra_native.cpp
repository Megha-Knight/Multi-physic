#include "astra_native.h"

#include <cmath>
#include <cstring>
#include <string>

namespace {
    thread_local std::string g_last_error = "";
    constexpr double ASTRA_EPSILON = 1e-8;

    void set_error(const std::string& msg) {
        g_last_error = msg;
    }
}

extern "C" {

ASTRA_API uint32_t astra_get_version(void) {
    return ASTRA_VERSION_HEX;
}

ASTRA_API const char* astra_get_version_string(void) {
    return "Astra Native Backend 0.1.0 (LLVM/Clang 21.1.6 x86_64)";
}

ASTRA_API int32_t astra_vector_magnitude(double x, double y, double z, double* out_magnitude) {
    if (!out_magnitude) {
        set_error("astra_vector_magnitude: out_magnitude pointer is null");
        return ASTRA_ERR_NULL_POINTER;
    }
    *out_magnitude = std::sqrt(x * x + y * y + z * z);
    return ASTRA_OK;
}

ASTRA_API int32_t astra_ray_triangle_intersect(
    const double* ray_orig, const double* ray_dir,
    const double* v0, const double* v1, const double* v2,
    double* out_t, double* out_u, double* out_v) {

    if (!ray_orig || !ray_dir || !v0 || !v1 || !v2 || !out_t) {
        set_error("astra_ray_triangle_intersect: required pointer is null");
        return ASTRA_ERR_NULL_POINTER;
    }

    *out_t = -1.0;
    if (out_u) *out_u = 0.0;
    if (out_v) *out_v = 0.0;

    if (!std::isfinite(ray_orig[0]) || !std::isfinite(ray_orig[1]) || !std::isfinite(ray_orig[2]) ||
        !std::isfinite(ray_dir[0]) || !std::isfinite(ray_dir[1]) || !std::isfinite(ray_dir[2])) {
        set_error("astra_ray_triangle_intersect: ray parameters must be finite");
        return ASTRA_ERR_INVALID_RAY;
    }

    if (!std::isfinite(v0[0]) || !std::isfinite(v0[1]) || !std::isfinite(v0[2]) ||
        !std::isfinite(v1[0]) || !std::isfinite(v1[1]) || !std::isfinite(v1[2]) ||
        !std::isfinite(v2[0]) || !std::isfinite(v2[1]) || !std::isfinite(v2[2])) {
        set_error("astra_ray_triangle_intersect: vertex parameters must be finite");
        return ASTRA_ERR_INVALID_RAY;
    }

    double dir_len_sq = ray_dir[0] * ray_dir[0] + ray_dir[1] * ray_dir[1] + ray_dir[2] * ray_dir[2];
    if (dir_len_sq < ASTRA_EPSILON * ASTRA_EPSILON) {
        set_error("astra_ray_triangle_intersect: ray direction magnitude is zero");
        return ASTRA_ERR_INVALID_RAY;
    }

    // Edge vectors
    double e1_x = v1[0] - v0[0];
    double e1_y = v1[1] - v0[1];
    double e1_z = v1[2] - v0[2];

    double e2_x = v2[0] - v0[0];
    double e2_y = v2[1] - v0[1];
    double e2_z = v2[2] - v0[2];

    // pvec = ray_dir x edge2
    double pvec_x = ray_dir[1] * e2_z - ray_dir[2] * e2_y;
    double pvec_y = ray_dir[2] * e2_x - ray_dir[0] * e2_z;
    double pvec_z = ray_dir[0] * e2_y - ray_dir[1] * e2_x;

    // det = edge1 . pvec
    double det = e1_x * pvec_x + e1_y * pvec_y + e1_z * pvec_z;

    if (std::abs(det) < ASTRA_EPSILON) {
        // Ray is parallel to triangle plane or triangle is degenerate
        return ASTRA_OK;
    }

    double inv_det = 1.0 / det;

    // tvec = ray_orig - v0
    double tvec_x = ray_orig[0] - v0[0];
    double tvec_y = ray_orig[1] - v0[1];
    double tvec_z = ray_orig[2] - v0[2];

    // u = (tvec . pvec) * inv_det
    double u = (tvec_x * pvec_x + tvec_y * pvec_y + tvec_z * pvec_z) * inv_det;
    if (u < 0.0 || u > 1.0) {
        return ASTRA_OK; // Lies outside triangle
    }

    // qvec = tvec x edge1
    double qvec_x = tvec_y * e1_z - tvec_z * e1_y;
    double qvec_y = tvec_z * e1_x - tvec_x * e1_z;
    double qvec_z = tvec_x * e1_y - tvec_y * e1_x;

    // v = (ray_dir . qvec) * inv_det
    double v = (ray_dir[0] * qvec_x + ray_dir[1] * qvec_y + ray_dir[2] * qvec_z) * inv_det;
    if (v < 0.0 || u + v > 1.0) {
        return ASTRA_OK; // Lies outside triangle
    }

    // t = (edge2 . qvec) * inv_det
    double t = (e2_x * qvec_x + e2_y * qvec_y + e2_z * qvec_z) * inv_det;
    if (t < ASTRA_EPSILON) {
        return ASTRA_OK; // Hit is behind or exactly on ray origin
    }

    *out_t = t;
    if (out_u) *out_u = u;
    if (out_v) *out_v = v;

    return ASTRA_OK;
}

ASTRA_API int32_t astra_transform_point_4x4(
    const double* in_pt, const double* matrix_4x4, double* out_pt) {

    if (!in_pt || !matrix_4x4 || !out_pt) {
        set_error("astra_transform_point_4x4: required pointer is null");
        return ASTRA_ERR_NULL_POINTER;
    }

    double x = in_pt[0], y = in_pt[1], z = in_pt[2];
    double rx = matrix_4x4[0] * x + matrix_4x4[1] * y + matrix_4x4[2] * z + matrix_4x4[3];
    double ry = matrix_4x4[4] * x + matrix_4x4[5] * y + matrix_4x4[6] * z + matrix_4x4[7];
    double rz = matrix_4x4[8] * x + matrix_4x4[9] * y + matrix_4x4[10] * z + matrix_4x4[11];
    double rw = matrix_4x4[12] * x + matrix_4x4[13] * y + matrix_4x4[14] * z + matrix_4x4[15];

    if (std::abs(rw) > 1e-12 && std::abs(rw - 1.0) > 1e-12) {
        double inv_w = 1.0 / rw;
        rx *= inv_w;
        ry *= inv_w;
        rz *= inv_w;
    }

    out_pt[0] = rx;
    out_pt[1] = ry;
    out_pt[2] = rz;
    return ASTRA_OK;
}

ASTRA_API int32_t astra_ray_mesh_intersect_nearest_f32(
    const double* ray_orig, const double* ray_dir,
    const float* points, int32_t point_coord_count,
    const int32_t* faces, int32_t face_index_count, int32_t stride,
    int32_t* out_hit_index, double* out_hit_t,
    double* out_hit_u, double* out_hit_v) {

    if (!ray_orig || !ray_dir || !points || !faces || !out_hit_index || !out_hit_t) {
        set_error("astra_ray_mesh_intersect_nearest_f32: required pointer is null");
        return ASTRA_ERR_NULL_POINTER;
    }

    *out_hit_index = -1;
    *out_hit_t = -1.0;
    if (out_hit_u) *out_hit_u = 0.0;
    if (out_hit_v) *out_hit_v = 0.0;

    if (!std::isfinite(ray_orig[0]) || !std::isfinite(ray_orig[1]) || !std::isfinite(ray_orig[2]) ||
        !std::isfinite(ray_dir[0]) || !std::isfinite(ray_dir[1]) || !std::isfinite(ray_dir[2])) {
        set_error("astra_ray_mesh_intersect_nearest_f32: ray coordinates must be finite");
        return ASTRA_ERR_INVALID_RAY;
    }

    double dir_len_sq = ray_dir[0] * ray_dir[0] + ray_dir[1] * ray_dir[1] + ray_dir[2] * ray_dir[2];
    if (dir_len_sq < ASTRA_EPSILON * ASTRA_EPSILON) {
        set_error("astra_ray_mesh_intersect_nearest_f32: ray direction magnitude is zero");
        return ASTRA_ERR_INVALID_RAY;
    }

    if (stride != 3 && stride != 6) {
        set_error("astra_ray_mesh_intersect_nearest_f32: stride must be 3 or 6");
        return ASTRA_ERR_INVALID_STRIDE;
    }

    if (point_coord_count < 0 || face_index_count < 0) {
        set_error("astra_ray_mesh_intersect_nearest_f32: counts cannot be negative");
        return ASTRA_ERR_BUFFER_TOO_SMALL;
    }

    if (point_coord_count % 3 != 0) {
        set_error("astra_ray_mesh_intersect_nearest_f32: point_coord_count must be a multiple of 3 (XYZ)");
        return ASTRA_ERR_BUFFER_TOO_SMALL;
    }

    if (point_coord_count == 0 || face_index_count < stride) {
        return ASTRA_OK; // Clean empty mesh handling: valid miss
    }

    double min_t = 1e30;
    int32_t best_index = -1;
    double best_u = 0.0, best_v = 0.0;

    int32_t tri_count = face_index_count / stride;
    for (int32_t k = 0; k < tri_count; ++k) {
        int32_t base = k * stride;
        int32_t idx0 = faces[base];
        int32_t idx1 = faces[base + (stride == 6 ? 2 : 1)];
        int32_t idx2 = faces[base + (stride == 6 ? 4 : 2)];

        if (idx0 < 0 || idx1 < 0 || idx2 < 0) {
            continue; // Skip invalid face index safely
        }

        int64_t c0 = (int64_t)idx0 * 3, c1 = (int64_t)idx1 * 3, c2 = (int64_t)idx2 * 3;
        if (c0 + 2 >= point_coord_count || c1 + 2 >= point_coord_count || c2 + 2 >= point_coord_count) {
            continue; // Skip out-of-bounds vertex index safely
        }

        if (!std::isfinite(points[c0]) || !std::isfinite(points[c0 + 1]) || !std::isfinite(points[c0 + 2]) ||
            !std::isfinite(points[c1]) || !std::isfinite(points[c1 + 1]) || !std::isfinite(points[c1 + 2]) ||
            !std::isfinite(points[c2]) || !std::isfinite(points[c2 + 1]) || !std::isfinite(points[c2 + 2])) {
            continue; // Skip non-finite vertices safely
        }

        double v0[3] = { (double)points[c0], (double)points[c0 + 1], (double)points[c0 + 2] };
        double v1[3] = { (double)points[c1], (double)points[c1 + 1], (double)points[c1 + 2] };
        double v2[3] = { (double)points[c2], (double)points[c2 + 1], (double)points[c2 + 2] };

        double t = -1.0, u = 0.0, v = 0.0;
        int32_t rc = astra_ray_triangle_intersect(ray_orig, ray_dir, v0, v1, v2, &t, &u, &v);
        if (rc == ASTRA_OK && t > ASTRA_EPSILON && t < min_t) {
            min_t = t;
            best_index = k;
            best_u = u;
            best_v = v;
        }
    }

    if (best_index >= 0) {
        *out_hit_index = best_index;
        *out_hit_t = min_t;
        if (out_hit_u) *out_hit_u = best_u;
        if (out_hit_v) *out_hit_v = best_v;
    }

    return ASTRA_OK;
}

ASTRA_API int32_t astra_get_last_error(char* buffer, int32_t buffer_len) {
    if (!buffer || buffer_len <= 0) return ASTRA_ERR_NULL_POINTER;
    size_t copy_len = std::min(static_cast<size_t>(buffer_len - 1), g_last_error.length());
    std::memcpy(buffer, g_last_error.c_str(), copy_len);
    buffer[copy_len] = '\0';
    return ASTRA_OK;
}

} // extern "C"
