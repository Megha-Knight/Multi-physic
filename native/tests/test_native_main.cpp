#include "astra_native.h"
#include <iostream>
#include <cmath>
#include <cassert>
#include <limits>

int main() {
    std::cout << "=== Astra Native Standalone Test Runner (Batched Mesh + Diagnostics) ===" << std::endl;

    // 1. Version Check
    uint32_t ver = astra_get_version();
    const char* ver_str = astra_get_version_string();
    std::cout << "[1/7] Version: 0x" << std::hex << ver << " (" << ver_str << ")" << std::endl;
    assert(ver == 0x00000100);

    // 2. Vector Magnitude
    double mag = 0.0;
    int32_t rc = astra_vector_magnitude(3.0, 4.0, 12.0, &mag);
    assert(rc == ASTRA_OK);
    assert(std::abs(mag - 13.0) < 1e-9);
    std::cout << "[2/7] Vector Magnitude: 13.0 verified." << std::endl;

    // 3. Single Ray-Triangle Intersection & Barycentrics
    double v0[3] = {0.0, 0.0, 0.0};
    double v1[3] = {10.0, 0.0, 0.0};
    double v2[3] = {0.0, 10.0, 0.0};
    double orig[3] = {2.0, 2.0, 5.0};
    double dir[3] = {0.0, 0.0, -1.0};
    double t = 0.0, u = 0.0, v = 0.0;

    rc = astra_ray_triangle_intersect(orig, dir, v0, v1, v2, &t, &u, &v);
    assert(rc == ASTRA_OK);
    assert(std::abs(t - 5.0) < 1e-9);
    assert(std::abs(u - 0.2) < 1e-9);
    assert(std::abs(v - 0.2) < 1e-9);

    double orig_miss[3] = {20.0, 20.0, 5.0};
    rc = astra_ray_triangle_intersect(orig_miss, dir, v0, v1, v2, &t, &u, &v);
    assert(rc == ASTRA_OK && t < 0.0);
    std::cout << "[3/7] Single Ray-Triangle Hit & Miss verified." << std::endl;

    // 4. Transform Point 4x4
    double mat[16] = {
        1.0, 0.0, 0.0, 10.0,
        0.0, 1.0, 0.0, 20.0,
        0.0, 0.0, 1.0, 30.0,
        0.0, 0.0, 0.0, 1.0
    };
    double in_pt[3] = {1.0, 2.0, 3.0};
    double out_pt[3] = {0.0, 0.0, 0.0};
    rc = astra_transform_point_4x4(in_pt, mat, out_pt);
    assert(rc == ASTRA_OK);
    assert(std::abs(out_pt[0] - 11.0) < 1e-9);
    assert(std::abs(out_pt[1] - 22.0) < 1e-9);
    assert(std::abs(out_pt[2] - 33.0) < 1e-9);
    std::cout << "[4/7] 4x4 Matrix Point Transformation verified." << std::endl;

    // 5. Batched Mesh: Stride 3 (Standard Index Buffer)
    // 4 points forming two triangles in XY plane at Z=0:
    // P0(0,0,0), P1(10,0,0), P2(10,10,0), P3(0,10,0)
    float pts_stride3[] = {
        0.0f, 0.0f, 0.0f,   // P0
        10.0f, 0.0f, 0.0f,  // P1
        10.0f, 10.0f, 0.0f, // P2
        0.0f, 10.0f, 0.0f   // P3
    };
    // Tri 0: (0, 1, 3), Tri 1: (1, 2, 3)
    int32_t faces_stride3[] = {
        0, 1, 3, // Tri 0
        1, 2, 3  // Tri 1
    };

    int32_t hit_idx = -1;
    double hit_t = -1.0, hit_u = 0.0, hit_v = 0.0;

    // Ray aiming at Tri 0: (2, 2, 5) -> dir(0, 0, -1)
    rc = astra_ray_mesh_intersect_nearest_f32(orig, dir, pts_stride3, 12, faces_stride3, 6, 3,
                                             &hit_idx, &hit_t, &hit_u, &hit_v);
    assert(rc == ASTRA_OK);
    assert(hit_idx == 0);
    assert(std::abs(hit_t - 5.0) < 1e-6);

    // Ray aiming at Tri 1: (8, 8, 5) -> dir(0, 0, -1)
    double ray_tri1_orig[] = {8.0, 8.0, 5.0};
    rc = astra_ray_mesh_intersect_nearest_f32(ray_tri1_orig, dir, pts_stride3, 12, faces_stride3, 6, 3,
                                             &hit_idx, &hit_t, &hit_u, &hit_v);
    assert(rc == ASTRA_OK);
    assert(hit_idx == 1);
    assert(std::abs(hit_t - 5.0) < 1e-6);
    std::cout << "[5/7] Batched Mesh Stride 3 (Multi-Triangle Dispatch) verified." << std::endl;

    // 6. Batched Mesh: Stride 6 (JavaFX Format: [p0, t0, p1, t1, p2, t2]) & Nearest Selection
    // Create two layers:
    // Tri 0 at Z=20.0 (far), Tri 1 at Z=10.0 (near)
    float pts_stride6[] = {
        // Far triangle at Z=20 (P0, P1, P2)
        0.0f, 0.0f, 20.0f,
        10.0f, 0.0f, 20.0f,
        0.0f, 10.0f, 20.0f,
        // Near triangle at Z=10 (P3, P4, P5)
        0.0f, 0.0f, 10.0f,
        10.0f, 0.0f, 10.0f,
        0.0f, 10.0f, 10.0f
    };
    int32_t faces_stride6[] = {
        0, 999, 1, 999, 2, 999, // Tri 0: indices 0, 1, 2 (Z=20)
        3, 888, 4, 888, 5, 888  // Tri 1: indices 3, 4, 5 (Z=10)
    };

    double ray_near_orig[] = {2.0, 2.0, 0.0};
    double ray_near_dir[]  = {0.0, 0.0, 1.0}; // Pointing towards +Z

    rc = astra_ray_mesh_intersect_nearest_f32(ray_near_orig, ray_near_dir,
                                             pts_stride6, 18, faces_stride6, 12, 6,
                                             &hit_idx, &hit_t, &hit_u, &hit_v);
    assert(rc == ASTRA_OK);
    // Nearest hit MUST be Tri 1 (t=10.0), NOT Tri 0 (t=20.0)
    assert(hit_idx == 1);
    assert(std::abs(hit_t - 10.0) < 1e-6);
    std::cout << "[6/7] Batched Mesh Stride 6 (JavaFX Format) & Nearest Depth Sorting verified." << std::endl;

    // 7. Robustness, Error Handling & Boundary Condition Validation
    // A. Invalid Strides: 4, 5, 2, 7 must fail
    rc = astra_ray_mesh_intersect_nearest_f32(orig, dir, pts_stride3, 12, faces_stride3, 6, 4,
                                             &hit_idx, &hit_t, &hit_u, &hit_v);
    assert(rc == ASTRA_ERR_INVALID_STRIDE);
    rc = astra_ray_mesh_intersect_nearest_f32(orig, dir, pts_stride3, 12, faces_stride3, 6, 5,
                                             &hit_idx, &hit_t, &hit_u, &hit_v);
    assert(rc == ASTRA_ERR_INVALID_STRIDE);

    // B. Non-finite Ray Coordinates: NaN / Inf must return ASTRA_ERR_INVALID_RAY
    double nan_ray_orig[] = {std::numeric_limits<double>::quiet_NaN(), 0.0, 0.0};
    rc = astra_ray_mesh_intersect_nearest_f32(nan_ray_orig, dir, pts_stride3, 12, faces_stride3, 6, 3,
                                             &hit_idx, &hit_t, &hit_u, &hit_v);
    assert(rc == ASTRA_ERR_INVALID_RAY);

    double zero_dir[] = {0.0, 0.0, 0.0};
    rc = astra_ray_mesh_intersect_nearest_f32(orig, zero_dir, pts_stride3, 12, faces_stride3, 6, 3,
                                             &hit_idx, &hit_t, &hit_u, &hit_v);
    assert(rc == ASTRA_ERR_INVALID_RAY);

    // C. Malformed & Out-of-bounds Indices: negative index and out-of-bounds vertex index
    int32_t faces_malformed[] = {
        -1, 0, 1,      // Negative index (must skip safely)
        0, 9999, 1,          // Out-of-bounds index (must skip safely)
        0, 1000000000, 1,    // Large index potential overflow (must skip safely)
        0, 1, 3              // Valid Tri 0 (must hit)
    };
    rc = astra_ray_mesh_intersect_nearest_f32(orig, dir, pts_stride3, 12, faces_malformed, 12, 3,
                                             &hit_idx, &hit_t, &hit_u, &hit_v);
    assert(rc == ASTRA_OK);
    assert(hit_idx == 3); // The 4th triangle in array
    assert(std::abs(hit_t - 5.0) < 1e-6);

    // D. Non-finite Vertex Coordinates in Mesh: skip safely
    float pts_corrupted[] = {
        std::numeric_limits<float>::infinity(), 0.0f, 0.0f,
        10.0f, 0.0f, 0.0f,
        0.0f, 10.0f, 0.0f,
        0.0f, 0.0f, 0.0f,
        10.0f, 0.0f, 0.0f,
        0.0f, 10.0f, 0.0f
    };
    int32_t faces_corrupted[] = {
        0, 1, 2, // Contains Infinity vertex (skip)
        3, 4, 5  // Valid vertex (hit)
    };
    rc = astra_ray_mesh_intersect_nearest_f32(orig, dir, pts_corrupted, 18, faces_corrupted, 6, 3,
                                             &hit_idx, &hit_t, &hit_u, &hit_v);
    assert(rc == ASTRA_OK);
    assert(hit_idx == 1);
    assert(std::abs(hit_t - 5.0) < 1e-6);

    // E. Null pointer handling
    rc = astra_ray_mesh_intersect_nearest_f32(nullptr, dir, pts_stride3, 12, faces_stride3, 6, 3,
                                             &hit_idx, &hit_t, &hit_u, &hit_v);
    assert(rc == ASTRA_ERR_NULL_POINTER);

    // F. Empty mesh handling (clean miss, no crash)
    rc = astra_ray_mesh_intersect_nearest_f32(orig, dir, pts_stride3, 0, faces_stride3, 0, 3,
                                             &hit_idx, &hit_t, &hit_u, &hit_v);
    assert(rc == ASTRA_OK && hit_idx == -1 && hit_t < 0.0);

    std::cout << "[7/7] Invalid Strides, Non-Finite Inputs, Index Safety, and Error Handling verified." << std::endl;
    std::cout << "=== All seven standalone native test sections passed. ===" << std::endl;
    return 0;
}
