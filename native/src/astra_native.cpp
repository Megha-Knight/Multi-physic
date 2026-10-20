#include "astra_native.h"

#include <cmath>
#include <cstring>
#include <string>

namespace {
    thread_local std::string g_last_error = "";

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

    const double EPSILON = 1e-8;

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

    if (std::abs(det) < EPSILON) {
        // Ray is parallel to triangle plane
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
    if (t < EPSILON) {
        return ASTRA_OK; // Hit is behind ray origin
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

ASTRA_API int32_t astra_get_last_error(char* buffer, int32_t buffer_len) {
    if (!buffer || buffer_len <= 0) return ASTRA_ERR_NULL_POINTER;
    size_t copy_len = std::min(static_cast<size_t>(buffer_len - 1), g_last_error.length());
    std::memcpy(buffer, g_last_error.c_str(), copy_len);
    buffer[copy_len] = '\0';
    return ASTRA_OK;
}

} // extern "C"
