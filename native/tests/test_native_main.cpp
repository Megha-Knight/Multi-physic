#include "astra_native.h"
#include <iostream>
#include <cmath>
#include <cassert>

int main() {
    std::cout << "=== Astra Native Standalone Test Runner ===" << std::endl;

    // 1. Version Check
    uint32_t ver = astra_get_version();
    const char* ver_str = astra_get_version_string();
    std::cout << "Version: 0x" << std::hex << ver << " (" << ver_str << ")" << std::endl;
    assert(ver == 0x00000100);

    // 2. Vector Magnitude
    double mag = 0.0;
    int32_t rc = astra_vector_magnitude(3.0, 4.0, 12.0, &mag);
    std::cout << "Magnitude(3, 4, 12) = " << mag << " (rc=" << rc << ")" << std::endl;
    assert(rc == ASTRA_OK);
    assert(std::abs(mag - 13.0) < 1e-9);

    // 3. Ray-Triangle Intersection
    // Triangle in XY plane at Z=0: (0,0,0), (10,0,0), (0,10,0)
    double v0[3] = {0.0, 0.0, 0.0};
    double v1[3] = {10.0, 0.0, 0.0};
    double v2[3] = {0.0, 10.0, 0.0};

    // Ray starting at (2, 2, 5) pointing down (0, 0, -1) -> should hit at t=5.0
    double orig[3] = {2.0, 2.0, 5.0};
    double dir[3] = {0.0, 0.0, -1.0};
    double t = 0.0, u = 0.0, v = 0.0;

    rc = astra_ray_triangle_intersect(orig, dir, v0, v1, v2, &t, &u, &v);
    std::cout << "Ray-Tri Hit: t=" << t << ", u=" << u << ", v=" << v << " (rc=" << rc << ")" << std::endl;
    assert(rc == ASTRA_OK);
    assert(std::abs(t - 5.0) < 1e-9);
    assert(std::abs(u - 0.2) < 1e-9);
    assert(std::abs(v - 0.2) < 1e-9);

    // Ray that misses: (20, 20, 5)
    double orig_miss[3] = {20.0, 20.0, 5.0};
    rc = astra_ray_triangle_intersect(orig_miss, dir, v0, v1, v2, &t, &u, &v);
    std::cout << "Ray-Tri Miss: t=" << t << " (rc=" << rc << ")" << std::endl;
    assert(rc == ASTRA_OK);
    assert(t < 0.0);

    // 4. Transform Point 4x4
    // Identity with translation by (10, 20, 30)
    double mat[16] = {
        1.0, 0.0, 0.0, 10.0,
        0.0, 1.0, 0.0, 20.0,
        0.0, 0.0, 1.0, 30.0,
        0.0, 0.0, 0.0, 1.0
    };
    double in_pt[3] = {1.0, 2.0, 3.0};
    double out_pt[3] = {0.0, 0.0, 0.0};
    rc = astra_transform_point_4x4(in_pt, mat, out_pt);
    std::cout << "Transform pt (1,2,3) -> (" << out_pt[0] << ", " << out_pt[1] << ", " << out_pt[2] << ")" << std::endl;
    assert(rc == ASTRA_OK);
    assert(std::abs(out_pt[0] - 11.0) < 1e-9);
    assert(std::abs(out_pt[1] - 22.0) < 1e-9);
    assert(std::abs(out_pt[2] - 33.0) < 1e-9);

    // 5. Error handling
    rc = astra_vector_magnitude(1.0, 2.0, 3.0, nullptr);
    assert(rc == ASTRA_ERR_NULL_POINTER);
    char err_buf[128] = {0};
    astra_get_last_error(err_buf, sizeof(err_buf));
    std::cout << "Safe error handling: rc=" << rc << ", msg=\"" << err_buf << "\"" << std::endl;

    std::cout << "=== ALL STANDALONE NATIVE TESTS PASSED (100%) ===" << std::endl;
    return 0;
}
