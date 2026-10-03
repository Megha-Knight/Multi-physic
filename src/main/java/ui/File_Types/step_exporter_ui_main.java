package ui.File_Types;

import javafx.geometry.Point3D;
import ui.workspace.drafting.shape_item_ui_main;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * step_exporter_ui_main.java
 * ISO 10303-21 STEP AP203/AP214 CAD exporter for Astra models.
 * NOTE ON GEOMETRY HONESTY: Astra STEP export currently represents tessellated
 * geometry (faceted B-Rep using ADVANCED_FACE planar polygons with shared topology).
 * It is not an exact analytic/parametric feature-history STEP export.
 */
public final class step_exporter_ui_main {

    private step_exporter_ui_main() {}

    private record SharedVertex(int cartesianPointId, int vertexPointId) {}

    public static boolean exportToStep(File file, List<shape_item_ui_main> shapes) {
        if (file == null) return false;
        List<mesh_exporter_ui_main.Tri> tris = mesh_exporter_ui_main.generateAllTriangles(shapes);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String filename = file.getName();

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("ISO-10303-21;\nHEADER;\nFILE_DESCRIPTION(('Astra CAD Tessellated B-Rep Model','STEP AP203'),'2;1');");
            pw.printf(Locale.US, "FILE_NAME('%s','%s',('Astra User'),('Astra Multiphysics'),'Astra STEP Exporter 1.1','Astra CAD 2026','');%n", filename, timestamp);
            pw.println("FILE_SCHEMA(('CONFIG_CONTROL_DESIGN'));\nENDSEC;\nDATA;");

            pw.println("#1=APPLICATION_CONTEXT('configuration controlled 3d designs of mechanical parts');");
            pw.println("#2=APPLICATION_PROTOCOL_DEFINITION('international standard','config_control_design',1994,#1);");
            pw.println("#3=PRODUCT_CONTEXT('',#1,'mechanical');");
            pw.println("#4=PRODUCT('AstraModel','AstraModel','',(#3));");
            pw.println("#5=PRODUCT_DEFINITION_FORMATION('','',#4);");
            pw.println("#6=PRODUCT_DEFINITION_CONTEXT('part definition',#1,'design');");
            pw.println("#7=PRODUCT_DEFINITION('design','',#5,#6);\n#8=PRODUCT_DEFINITION_SHAPE('','',#7);");

            pw.println("#9=UNCERTAINTY_MEASURE_WITH_UNIT(LENGTH_MEASURE(1.E-05),#13,'DISTANCE_ACCURACY_VALUE','Maximum Tolerance');");
            pw.println("#10=(GEOMETRIC_REPRESENTATION_CONTEXT(3) GLOBAL_UNCERTAINTY_ASSIGNED_CONTEXT((#9)) GLOBAL_UNIT_ASSIGNED_CONTEXT((#13,#14,#15)) REPRESENTATION_CONTEXT('Context #1','3D Context with UNIT'));");
            pw.println("#11=DIMENSIONAL_EXPONENTS(1.,0.,0.,0.,0.,0.,0.);\n#12=SI_UNIT(*,.MILLI.,.METRE.);");
            pw.println("#13=(LENGTH_UNIT() NAMED_UNIT(*) SI_UNIT(*,.MILLI.,.METRE.));");
            pw.println("#14=(NAMED_UNIT(*) PLANE_ANGLE_UNIT() SI_UNIT($,.RADIAN.));");
            pw.println("#15=(NAMED_UNIT(*) SOLID_ANGLE_UNIT() SI_UNIT($,.STERADIAN.));");

            int[] id = new int[]{20};
            Map<String, SharedVertex> vertexMap = new LinkedHashMap<>();
            List<Integer> faceIds = new ArrayList<>();

            for (mesh_exporter_ui_main.Tri t : tris) {
                SharedVertex v1 = getOrCreateVertex(t.a(), vertexMap, pw, id);
                SharedVertex v2 = getOrCreateVertex(t.b(), vertexMap, pw, id);
                SharedVertex v3 = getOrCreateVertex(t.c(), vertexMap, pw, id);

                int loopId = id[0]++, boundId = id[0]++;
                pw.printf("#%d=POLY_LOOP('',(#%d,#%d,#%d));%n", loopId, v1.vertexPointId(), v2.vertexPointId(), v3.vertexPointId());
                pw.printf("#%d=FACE_OUTER_BOUND('',#%d,.T.);%n", boundId, loopId);

                int normDirId = id[0]++, axisPlacementId = id[0]++, planeId = id[0]++;
                Point3D n = t.n();
                pw.printf(Locale.US, "#%d=DIRECTION('',(%.6f,%.6f,%.6f));%n", normDirId, n.getX(), n.getY(), n.getZ());
                pw.printf("#%d=AXIS2_PLACEMENT_3D('',#%d,#%d,$);%n", axisPlacementId, v1.cartesianPointId(), normDirId);
                pw.printf("#%d=PLANE('',#%d);%n", planeId, axisPlacementId);

                int faceId = id[0]++;
                pw.printf("#%d=ADVANCED_FACE('',(#%d),#%d,.T.);%n", faceId, boundId, planeId);
                faceIds.add(faceId);
            }

            StringBuilder faceRefList = new StringBuilder();
            for (int i = 0; i < faceIds.size(); i++) {
                if (i > 0) faceRefList.append(",");
                faceRefList.append("#").append(faceIds.get(i));
            }

            int shellId = id[0]++, brepId = id[0]++, shapeRepId = id[0]++;
            pw.printf("#%d=CLOSED_SHELL('',(%s));%n", shellId, faceRefList.toString());
            pw.printf("#%d=MANIFOLD_SOLID_BREP('AstraSolid',#%d);%n", brepId, shellId);
            pw.printf("#%d=ADVANCED_BREP_SHAPE_REPRESENTATION('AstraRep',(#%d),#10);%n", shapeRepId, brepId);
            pw.printf("#19=SHAPE_DEFINITION_REPRESENTATION(#8,#%d);%n", shapeRepId);

            pw.println("ENDSEC;\nEND-ISO-10303-21;");
            return true;
        } catch (Exception e) {
            System.err.println("[Astra] Error exporting STEP: " + e.getMessage());
            return false;
        }
    }

    private static String key(Point3D p) {
        return String.format(Locale.US, "%.5f,%.5f,%.5f", p.getX(), p.getY(), p.getZ());
    }

    private static SharedVertex getOrCreateVertex(Point3D p, Map<String, SharedVertex> map, PrintWriter pw, int[] id) {
        String k = key(p);
        SharedVertex existing = map.get(k);
        if (existing != null) return existing;
        int pId = id[0]++, vId = id[0]++;
        pw.printf(Locale.US, "#%d=CARTESIAN_POINT('',(%.6f,%.6f,%.6f));%n", pId, p.getX(), p.getY(), p.getZ());
        pw.printf("#%d=VERTEX_POINT('',#%d);%n", vId, pId);
        SharedVertex created = new SharedVertex(pId, vId);
        map.put(k, created);
        return created;
    }
}
