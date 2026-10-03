package ui.workspace.drafting.topology;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * topology_signature_ui_main.java
 * Generates deterministic mathematical signatures of CAD body topology and derived regions.
 */
public final class topology_signature_ui_main {

    private topology_signature_ui_main() {}

    public static String generateSignature(topology_body_ui_main body) {
        if (body == null) return "EMPTY_BODY";
        StringBuilder sb = new StringBuilder();
        sb.append("BODY:").append(body.getId()).append(":").append(body.getType()).append("\n");

        List<String> faceDescs = new ArrayList<>();
        for (topology_face_ui_main f : body.getFaces()) {
            faceDescs.add(String.format(Locale.US, "F:%s:%s:%.2fx%.2f:E%d:V%d",
                f.getId(), f.getFaceKind().name(), f.getWidth(), f.getHeight(),
                f.getEdgeIds().size(), f.getVertexIds().size()));
        }
        Collections.sort(faceDescs);
        sb.append("BASE_FACES:\n");
        for (String fd : faceDescs) sb.append("  ").append(fd).append("\n");

        List<String> derivedDescs = new ArrayList<>();
        for (topology_derived_face_ui_main df : body.getDerivedFaces()) {
            derivedDescs.add(String.format(Locale.US, "DF:%s:%s:%s:%s:r=%.2f:d=%.2f:L%d:valid=%b",
                df.getId(), df.getRegionKind().name(),
                df.getSourceFaceKind() != null ? df.getSourceFaceKind().name() : "NONE",
                df.getCreatingFeatureId() != null ? df.getCreatingFeatureId() : "NONE",
                df.getRadius(), df.getDepth(), df.getLoops().size(), df.isValid()));
        }
        Collections.sort(derivedDescs);
        sb.append("DERIVED_FACES:\n");
        for (String dd : derivedDescs) sb.append("  ").append(dd).append("\n");

        List<String> edgeDescs = new ArrayList<>();
        for (topology_edge_ui_main e : body.getEdges()) {
            edgeDescs.add(String.format("E:%s:%s->%s:F%d", e.getId(), e.getV1Id(), e.getV2Id(), e.getAdjacentFaceIds().size()));
        }
        Collections.sort(edgeDescs);
        sb.append("EDGES:\n");
        for (String ed : edgeDescs) sb.append("  ").append(ed).append("\n");

        return computeSha256(sb.toString());
    }

    public static String generateTextReport(topology_body_ui_main body) {
        if (body == null) return "EMPTY_BODY";
        StringBuilder sb = new StringBuilder();
        sb.append("Body: ").append(body.getName()).append("\n");
        sb.append("Base Faces: ").append(body.getFaceCount()).append("\n");
        sb.append("Derived Faces: ").append(body.getDerivedFaceCount()).append("\n");
        for (topology_derived_face_ui_main df : body.getDerivedFaces()) {
            sb.append("  - ").append(df.getId()).append(" [").append(df.getRegionKind().getLabel()).append("]\n");
        }
        return sb.toString();
    }

    private static String computeSha256(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return Integer.toHexString(data.hashCode());
        }
    }
}
