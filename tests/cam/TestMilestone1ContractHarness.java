package tests.cam;

import ui.workspace.drafting.features.*;
import backend.native_bridge.astra_geometry_native_bridge_ui_main;
import java.util.*;

public class TestMilestone1ContractHarness {
    private static int passed = 0;
    private static int failed = 0;

    private static void assertTrue(boolean cond, String msg) {
        if (cond) {
            System.out.println("  [PASS] " + msg);
            passed++;
        } else {
            System.err.println("  [FAIL] " + msg);
            failed++;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Milestone 1: Geometry & Topology Contract Verification Harness ===");

        testDependencyOrdering();
        testCycleDetection();
        testMissingAndSuppressedDependencies();
        testDuplicateFeatureIdRejection();
        testEntityLineageContract();
        testNativeGeometryAbiFailClosed();

        System.out.println("\n--------------------------------------------------");
        System.out.printf("Milestone 1 Contract Tests: Passed = %d, Failed = %d%n", passed, failed);
        System.out.println("--------------------------------------------------");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testDependencyOrdering() {
        System.out.println("\n--- 1. Dependency Ordering (Topological Sort) ---");
        feature_dependency_graph_ui_main graph = new feature_dependency_graph_ui_main();

        feature_node_ui_main body = new feature_node_ui_main("body-1", "Body", feature_type_ui_main.SOLID_BODY, null);
        feature_node_ui_main sketch = new feature_node_ui_main("sketch-1", "Sketch", feature_type_ui_main.SKETCH, "body-1");
        feature_node_ui_main extrude = new feature_node_ui_main("extrude-1", "Extrude", feature_type_ui_main.EXTRUDE_FEATURE, "body-1");
        feature_node_ui_main hole = new feature_node_ui_main("hole-1", "Hole", feature_type_ui_main.HOLE_FEATURE, "body-1");

        graph.registerFeature(body);
        graph.registerFeature(sketch);
        graph.registerFeature(extrude);
        graph.registerFeature(hole);

        sketch.addDependency("body-1", dependency_type_ui_main.HOST_BODY);
        extrude.addDependency("sketch-1", dependency_type_ui_main.SKETCH_PROFILE);
        extrude.addDependency("body-1", dependency_type_ui_main.HOST_BODY);
        hole.addDependency("extrude-1", dependency_type_ui_main.HOST_BODY);

        List<String> order = graph.getRegenerationOrder();
        assertTrue(!order.isEmpty(), "Topological order is non-empty");
        int idxBody = order.indexOf("body-1");
        int idxSketch = order.indexOf("sketch-1");
        int idxExtrude = order.indexOf("extrude-1");
        int idxHole = order.indexOf("hole-1");

        assertTrue(idxBody < idxSketch, "Body precedes Sketch in regeneration order");
        assertTrue(idxSketch < idxExtrude, "Sketch precedes Extrude in regeneration order");
        assertTrue(idxExtrude < idxHole, "Extrude precedes Hole in regeneration order");
    }

    private static void testCycleDetection() {
        System.out.println("\n--- 2. Dependency Cycle Rejection ---");
        feature_dependency_graph_ui_main graph = new feature_dependency_graph_ui_main();
        feature_node_ui_main a = new feature_node_ui_main("feat-A", "A", feature_type_ui_main.EXTRUDE, null);
        feature_node_ui_main b = new feature_node_ui_main("feat-B", "B", feature_type_ui_main.EXTRUDE, null);
        graph.registerFeature(a);
        graph.registerFeature(b);

        a.addDependency("feat-B", dependency_type_ui_main.HOST_BODY);
        b.addDependency("feat-A", dependency_type_ui_main.HOST_BODY);

        assertTrue(graph.hasCycle(), "Circular dependency A <-> B is detected");
        List<String> order = graph.getRegenerationOrder();
        assertTrue(order.isEmpty(), "Regeneration order is empty when cycle is present");
    }

    private static void testMissingAndSuppressedDependencies() {
        System.out.println("\n--- 3. Missing & Suppressed Dependency Validation ---");
        feature_dependency_graph_ui_main graph = new feature_dependency_graph_ui_main();
        feature_node_ui_main valid = new feature_node_ui_main("valid-1", "Valid", feature_type_ui_main.SKETCH, null);
        feature_node_ui_main missing = new feature_node_ui_main("child-1", "Child", feature_type_ui_main.EXTRUDE, null);
        missing.addDependency("non-existent-id", dependency_type_ui_main.SKETCH_PROFILE);

        graph.registerFeature(valid);
        graph.registerFeature(missing);
        graph.validate(Collections.emptyList());

        assertTrue(missing.getState() == feature_state_ui_main.INVALID, "Node with missing dependency is marked INVALID");
        assertTrue(missing.getDiagnosticMessage().contains("Missing dependency"), "Diagnostic reports missing dependency");

        feature_node_ui_main supp = new feature_node_ui_main("supp-1", "Suppressed", feature_type_ui_main.SKETCH, null);
        supp.setState(feature_state_ui_main.SUPPRESSED);
        feature_node_ui_main childSupp = new feature_node_ui_main("child-supp", "ChildSupp", feature_type_ui_main.EXTRUDE, null);
        childSupp.addDependency("supp-1", dependency_type_ui_main.SKETCH_PROFILE);

        graph.registerFeature(supp);
        graph.registerFeature(childSupp);
        graph.validate(Collections.emptyList());

        assertTrue(childSupp.getState() == feature_state_ui_main.INVALID, "Node dependent on suppressed feature is marked INVALID");
    }

    private static void testDuplicateFeatureIdRejection() {
        System.out.println("\n--- 4. Duplicate Feature ID Rejection ---");
        feature_dependency_graph_ui_main graph = new feature_dependency_graph_ui_main();
        feature_node_ui_main n1 = new feature_node_ui_main("dup-id", "Node1", feature_type_ui_main.SOLID_BODY, null);
        feature_node_ui_main n2 = new feature_node_ui_main("dup-id", "Node2", feature_type_ui_main.SOLID_BODY, null);

        graph.registerFeature(n1);
        graph.registerFeature(n2);

        assertTrue(graph.getNodeCount() == 1, "Graph does not overwrite existing node on duplicate ID");
        assertTrue(n2.getState() == feature_state_ui_main.INVALID, "Second duplicate node is marked INVALID");
    }

    private static void testEntityLineageContract() {
        System.out.println("\n--- 5. Semantic Entity Lineage Contract ---");
        entity_lineage_contract_ui_main adapter = new entity_lineage_contract_ui_main();

        var ref1 = entity_lineage_contract_ui_main.EntityRef.of("feat-1", 1, 0);
        var resPreserved = adapter.resolveEntity(ref1);
        assertTrue(resPreserved.isUnique(), "Unrecorded entity defaults to PRESERVED unique reference");

        // Split scenario: face splits into 2 faces
        var refSrc = entity_lineage_contract_ui_main.EntityRef.of("feat-2", 3, 1);
        var refChild1 = entity_lineage_contract_ui_main.EntityRef.of("feat-2", 3, 10);
        var refChild2 = entity_lineage_contract_ui_main.EntityRef.of("feat-2", 3, 11);
        adapter.recordLineage(new entity_lineage_contract_ui_main.LineageRecord(
            entity_lineage_contract_ui_main.Outcome.SPLIT, 1L, 2L, refSrc, List.of(refChild1, refChild2)
        ));

        var resSplit = adapter.resolveEntity(refSrc);
        assertTrue(resSplit.isSplit(), "Split entity reports SPLIT outcome");
        assertTrue(resSplit.targets().size() == 2, "Split entity returns all 2 successors");
        assertTrue(!resSplit.isUnique(), "Split entity is not falsely reported as unique reference");

        // Consumed scenario
        var refConsumed = entity_lineage_contract_ui_main.EntityRef.of("feat-3", 3, 5);
        adapter.recordLineage(new entity_lineage_contract_ui_main.LineageRecord(
            entity_lineage_contract_ui_main.Outcome.CONSUMED, 1L, 2L, refConsumed, Collections.emptyList()
        ));
        var resConsumed = adapter.resolveEntity(refConsumed);
        assertTrue(resConsumed.isConsumed(), "Consumed entity reports CONSUMED outcome");
        assertTrue(resConsumed.targets().isEmpty(), "Consumed entity targets are empty");
    }

    private static void testNativeGeometryAbiFailClosed() {
        System.out.println("\n--- 6. Native Geometry ABI Integration & Milestone 3 Sketch -> Extrude ---");
        var bridge = astra_geometry_native_bridge_ui_main.getInstance();
        assertTrue(bridge.isAvailable(), "Geometry ABI successfully loaded and verified available in native DLL");
        assertTrue(bridge.getAbiVersion() == 1, "Native geometry ABI reports version 1");

        try (var model = bridge.createModel()) {
            assertTrue(model != null && model.isValid(), "Native model allocation succeeded");
            try (java.lang.foreign.Arena arena = java.lang.foreign.Arena.ofConfined()) {
                // 1. Evaluate Sketch Profile
                java.lang.foreign.MemorySegment profReq = arena.allocate(184);
                profReq.set(java.lang.foreign.ValueLayout.JAVA_INT, 0, 184); // struct_size
                profReq.set(java.lang.foreign.ValueLayout.JAVA_INT, 4, 1);   // op_kind = ASTRA_OP_SKETCH_PROFILE
                profReq.set(java.lang.foreign.ValueLayout.JAVA_LONG, 8, 0x11111111L); // feature_id.high
                profReq.set(java.lang.foreign.ValueLayout.JAVA_LONG, 16, 0x22222222L); // feature_id.low

                // sketch params offset = 48
                profReq.set(java.lang.foreign.ValueLayout.JAVA_INT, 48, 120); // struct_size
                profReq.set(java.lang.foreign.ValueLayout.JAVA_INT, 52, 1);   // loop_count = 1
                profReq.set(java.lang.foreign.ValueLayout.JAVA_INT, 56, 4);   // point_count = 4
                // plane u axis = (1, 0, 0)
                profReq.set(java.lang.foreign.ValueLayout.JAVA_DOUBLE, 88, 1.0);
                // plane v axis = (0, 1, 0)
                profReq.set(java.lang.foreign.ValueLayout.JAVA_DOUBLE, 120, 1.0);

                double[] squareCoords = { 0.0, 0.0,  10.0, 0.0,  10.0, 10.0,  0.0, 10.0 };
                java.lang.foreign.MemorySegment coords = arena.allocate(64);
                for (int i = 0; i < 8; i++) coords.set(java.lang.foreign.ValueLayout.JAVA_DOUBLE, i * 8L, squareCoords[i]);
                profReq.set(java.lang.foreign.ValueLayout.ADDRESS, 136, coords);

                var profRes = bridge.evaluateFeature(model, profReq);
                assertTrue(profRes != null && profRes.isValid(), "Sketch profile evaluation succeeded");
                profRes.close();

                // 2. Evaluate Extrude (25.0 mm along +Z)
                java.lang.foreign.MemorySegment extReq = arena.allocate(184);
                extReq.set(java.lang.foreign.ValueLayout.JAVA_INT, 0, 184); // struct_size
                extReq.set(java.lang.foreign.ValueLayout.JAVA_INT, 4, 3);   // op_kind = ASTRA_OP_EXTRUDE
                extReq.set(java.lang.foreign.ValueLayout.JAVA_LONG, 8, 0x33333333L); // feature_id.high
                extReq.set(java.lang.foreign.ValueLayout.JAVA_LONG, 16, 0x44444444L); // feature_id.low

                // extrude params offset = 48
                extReq.set(java.lang.foreign.ValueLayout.JAVA_INT, 48, 56); // struct_size
                extReq.set(java.lang.foreign.ValueLayout.JAVA_LONG, 56, 0x11111111L); // profile_feature_id.high
                extReq.set(java.lang.foreign.ValueLayout.JAVA_LONG, 64, 0x22222222L); // profile_feature_id.low
                // direction = (0, 0, 1)
                extReq.set(java.lang.foreign.ValueLayout.JAVA_DOUBLE, 88, 1.0);
                // distance = 25.0
                extReq.set(java.lang.foreign.ValueLayout.JAVA_DOUBLE, 96, 25.0);

                var extRes = bridge.evaluateFeature(model, extReq);
                assertTrue(extRes != null && extRes.isValid(), "Extrude evaluation succeeded");

                // 3. Copy Tessellation Snapshot
                var snapshot = bridge.copyTessellationSnapshot(extRes);
                assertTrue(snapshot != null, "Tessellation snapshot copied to Java successfully");
                assertTrue(snapshot.getVertexCount() == 8, "Snapshot has 8 vertices (4 bottom, 4 top)");
                assertTrue(snapshot.getTriangleCount() == 12, "Snapshot has 12 triangles (2 start cap + 2 end cap + 8 lateral)");

                // 4. Resolve Entity
                var latRef = entity_lineage_contract_ui_main.EntityRef.of("feat-ext", 3, 2);
                // Convert to ext_id
                var extRef = new entity_lineage_contract_ui_main.EntityRef(
                    new entity_lineage_contract_ui_main.Id128(0x33333333L, 0x44444444L), 3, 2, 0, new entity_lineage_contract_ui_main.Id128(0, 0));
                assertTrue(bridge.resolveEntity(extRes, extRef), "Lateral face entity resolved successfully via FFM");

                extRes.close();

                // 5. Fail-closed on unsupported operation (Boolean)
                java.lang.foreign.MemorySegment boolReq = arena.allocate(184);
                boolReq.set(java.lang.foreign.ValueLayout.JAVA_INT, 0, 184);
                boolReq.set(java.lang.foreign.ValueLayout.JAVA_INT, 4, 7); // op_kind = BOOLEAN
                try {
                    bridge.evaluateFeature(model, boolReq);
                    assertTrue(false, "Boolean operation should have failed closed");
                } catch (UnsupportedOperationException e) {
                    assertTrue(e.getMessage().contains("unsupported operation kind 7"), "Fail-closed diagnostic confirmed for Boolean: " + e.getMessage());
                }
            }
            model.close();
            assertTrue(!model.isValid(), "Model close is idempotent and invalidates handle");
        } catch (Throwable t) {
            assertTrue(false, "Unexpected exception in geometry model lifecycle: " + t.getMessage());
            t.printStackTrace();
        }
    }
}
