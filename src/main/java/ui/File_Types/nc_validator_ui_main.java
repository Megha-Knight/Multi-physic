package ui.File_Types;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * nc_validator_ui_main.java
 * Lightweight, robust G-code validator for NC manufacturing toolpath verification.
 */
public final class nc_validator_ui_main {

    private nc_validator_ui_main() {}

    public record NcReport(boolean valid, int totalLines, int motions, int toolChanges, int drillCycles, List<String> errors) {
        public int drills() { return drillCycles; }
    }

    private static final Set<String> KNOWN_G = Set.of("G0", "G00", "G1", "G01", "G2", "G02", "G3", "G03", "G17", "G18", "G19", "G20", "G21", "G40", "G80", "G81", "G82", "G83", "G90", "G91", "G94");
    private static final Set<String> KNOWN_M = Set.of("M3", "M03", "M5", "M05", "M6", "M06", "M30");

    public static NcReport validate(File file) {
        if (file == null || !file.exists()) return new NcReport(false, 0, 0, 0, 0, List.of("File does not exist"));
        List<String> errors = new ArrayList<>();
        int lines = 0, motions = 0, toolChanges = 0, drills = 0;
        boolean sawM30 = false, hasHeader = false;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String raw;
            while ((raw = br.readLine()) != null) {
                lines++;
                String line = raw.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("(") && line.endsWith(")")) {
                    if (line.contains("ASTRA CAD/CAM") || line.contains("NUMERICAL CONTROL")) hasHeader = true;
                    continue;
                }

                // Strip inline parenthesized comments
                String clean = line.replaceAll("\\([^)]*\\)", "").trim();
                if (clean.isEmpty()) continue;

                String[] tokens = clean.split("\\s+");
                for (String t : tokens) {
                    if (t.isEmpty()) continue;
                    char prefix = Character.toUpperCase(t.charAt(0));
                    String valStr = t.substring(1);

                    if (prefix == 'G') {
                        if (!KNOWN_G.contains(t.toUpperCase(Locale.US))) errors.add("Unknown G-code at line " + lines + ": " + t);
                        if (t.equalsIgnoreCase("G81") || t.equalsIgnoreCase("G82") || t.equalsIgnoreCase("G83")) drills++;
                        if (t.equalsIgnoreCase("G00") || t.equalsIgnoreCase("G0") || t.equalsIgnoreCase("G01") || t.equalsIgnoreCase("G1") || t.equalsIgnoreCase("G02") || t.equalsIgnoreCase("G03")) motions++;
                    } else if (prefix == 'M') {
                        if (!KNOWN_M.contains(t.toUpperCase(Locale.US))) errors.add("Unknown M-code at line " + lines + ": " + t);
                        if (t.equalsIgnoreCase("M30")) sawM30 = true;
                        if (t.equalsIgnoreCase("M06") || t.equalsIgnoreCase("M6")) toolChanges++;
                    } else if ("XYZIJKRFS".indexOf(prefix) >= 0) {
                        try {
                            double num = Double.parseDouble(valStr);
                            if (Double.isNaN(num) || Double.isInfinite(num)) errors.add("Non-finite coordinate at line " + lines + ": " + t);
                        } catch (NumberFormatException e) {
                            errors.add("Malformed numeric parameter at line " + lines + ": " + t);
                        }
                    } else if (prefix == 'T') {
                        try {
                            int tNum = Integer.parseInt(valStr);
                            if (tNum < 1 || tNum > 99) errors.add("Tool index out of bounds at line " + lines + ": " + t);
                        } catch (NumberFormatException e) {
                            errors.add("Malformed tool number at line " + lines + ": " + t);
                        }
                    } else {
                        errors.add("Unrecognized token prefix at line " + lines + ": " + t);
                    }
                }
            }
        } catch (Exception e) {
            return new NcReport(false, lines, motions, toolChanges, drills, List.of("IO error: " + e.getMessage()));
        }

        if (!hasHeader) errors.add("Missing Astra NC program header");
        if (!sawM30) errors.add("Missing program end M30");
        if (motions == 0) errors.add("Program contains no motion commands");

        return new NcReport(errors.isEmpty(), lines, motions, toolChanges, drills, errors);
    }
}
