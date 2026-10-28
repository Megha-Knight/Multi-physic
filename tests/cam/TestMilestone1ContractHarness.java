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
        System.out.println("\n--- 6. Native Geometry ABI Fail-Closed Behavior ---");
        var bridge = astra_geometry_native_bridge_ui_main.getInstance();
        assertTrue(!bridge.isAvailable(), "Geometry ABI correctly reports unavailable in absence of B-rep exports");
        try {
            bridge.evaluateFeature(null, 1);
            assertTrue(false, "Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("Geometry kernel unavailable"), "Fail-closed exception thrown with diagnostic");
        }
    }
}
