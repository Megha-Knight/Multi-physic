#include "astra_geometry.h"
#include <cstdlib>
#include <cstring>
#include <string>

static thread_local std::string g_last_error;

static void set_error(const std::string &msg) {
    g_last_error = msg;
}

struct astra_geom_model_handle {
    uint32_t magic;
    uint64_t current_revision;
};

struct astra_geom_result_handle {
    uint32_t magic;
    uint64_t revision;
    astra_tessellation_view view;
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
    auto *m = (astra_geom_model_handle *)std::malloc(sizeof(astra_geom_model_handle));
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
        std::free(model);
    }
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
    if (out_result) {
        *out_result = nullptr;
    }
    set_error("astra_geom_evaluate_feature: B-rep evaluation not yet implemented for operation kind " +
              std::to_string(request->operation_kind));
    return ASTRA_GEOM_UNSUPPORTED_OPERATION;
}

void astra_geom_result_release(astra_geom_result_handle *result) {
    if (result && result->magic == RESULT_MAGIC) {
        result->magic = 0;
        std::free(result);
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
    set_error("astra_geom_result_resolve_entity: entity lookup not yet implemented in baseline result");
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
