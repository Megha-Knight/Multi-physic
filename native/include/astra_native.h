#ifndef ASTRA_NATIVE_H
#define ASTRA_NATIVE_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

#ifdef _WIN32
  #ifdef ASTRA_BUILD_DLL
    #define ASTRA_API __declspec(dllexport)
  #else
    #define ASTRA_API __declspec(dllimport)
  #endif
#else
  #define ASTRA_API __attribute__((visibility("default")))
#endif

/* Status and Error Codes */
#define ASTRA_OK                   0
#define ASTRA_ERR_NULL_POINTER    -1
#define ASTRA_ERR_INVALID_RAY     -2
#define ASTRA_ERR_DEGENERATE_TRI  -3
#define ASTRA_ERR_BUFFER_TOO_SMALL -4
#define ASTRA_ERR_INVALID_STRIDE   -5

/* Version Information */
#define ASTRA_VERSION_MAJOR 0
#define ASTRA_VERSION_MINOR 1
#define ASTRA_VERSION_PATCH 0
#define ASTRA_VERSION_HEX   ((ASTRA_VERSION_MAJOR << 16) | (ASTRA_VERSION_MINOR << 8) | ASTRA_VERSION_PATCH)

/**
 * Returns packed hex version (e.g., 0x00000100 for 0.1.0).
 */
ASTRA_API uint32_t astra_get_version(void);

/**
 * Returns null-terminated human-readable version string.
 */
ASTRA_API const char* astra_get_version_string(void);

/**
 * Computes 3D Euclidean magnitude sqrt(x^2 + y^2 + z^2).
 */
ASTRA_API int32_t astra_vector_magnitude(double x, double y, double z, double* out_magnitude);

/**
 * Computes Möller–Trumbore ray-triangle intersection in 3D.
 * Inputs:
 *   ray_orig: 3 doubles (x, y, z)
 *   ray_dir:  3 doubles (dx, dy, dz) - does not need to be normalized
 *   v0, v1, v2: 3 vertices, each 3 doubles
 * Outputs:
 *   out_t: distance along ray to intersection (set to -1.0 if no hit)
 *   out_u, out_v: barycentric coordinates at hit point
 * Returns: ASTRA_OK on calculation, or error code on invalid parameters.
 */
ASTRA_API int32_t astra_ray_triangle_intersect(
    const double* ray_orig, const double* ray_dir,
    const double* v0, const double* v1, const double* v2,
    double* out_t, double* out_u, double* out_v);

/**
 * Transforms a 3D point (x, y, z, 1) by a 4x4 row-major matrix.
 * Output: out_pt (3 doubles).
 */
ASTRA_API int32_t astra_transform_point_4x4(
    const double* in_pt, const double* matrix_4x4, double* out_pt);

/**
 * Computes nearest ray-triangle intersection across an indexed triangle mesh (float coordinates).
 * Specifically optimized for JavaFX TriangleMesh buffers (stride = 6) and standard index buffers (stride = 3).
 */
ASTRA_API int32_t astra_ray_mesh_intersect_nearest_f32(
    const double* ray_orig, const double* ray_dir,
    const float* points, int32_t point_coord_count,
    const int32_t* faces, int32_t face_index_count, int32_t stride,
    int32_t* out_hit_index, double* out_hit_t,
    double* out_hit_u, double* out_hit_v);

/**
 * Retrieves the last error message for the calling thread.
 */
ASTRA_API int32_t astra_get_last_error(char* buffer, int32_t buffer_len);

#ifdef __cplusplus
}
#endif

#endif /* ASTRA_NATIVE_H */
