package cam.post.verifier;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Independent lexical parser for ISO 6983 / Fanuc G-code blocks.
 * Extracts word addresses, modal commands, and coordinate parameters without relying
 * on internal postprocessor state.
 */
public class NcBlockParser {

    private static final Pattern WORD_PATTERN = Pattern.compile("([A-Za-z])([+-]?(?:[0-9]+\\.?[0-9]*|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?)");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("\\((.*?)\\)");

    private final String rawText;
    private final String strippedText;
    private final String comment;
    private final Integer lineNumber;
    private final Set<String> gCodes = new LinkedHashSet<>();
    private final Set<String> mCodes = new LinkedHashSet<>();
    private final Map<Character, Double> words = new HashMap<>();
    private boolean malformed = false;

    public NcBlockParser(String line) {
        this.rawText = (line != null) ? line : "";
        String text = rawText.trim();

        // Extract comment
        Matcher commentMatcher = COMMENT_PATTERN.matcher(text);
        if (commentMatcher.find()) {
            this.comment = commentMatcher.group(1);
            text = commentMatcher.replaceAll(" ").trim();
        } else {
            this.comment = null;
        }

        this.strippedText = text;
        parseWords(text);

        // Line number
        if (words.containsKey('N')) {
            this.lineNumber = words.get('N').intValue();
        } else {
            this.lineNumber = null;
        }
    }

    private void parseWords(String text) {
        if (text.isEmpty() || text.equals("%")) return;

        // Split by whitespace
        String[] tokens = text.split("\\s+");
        for (String token : tokens) {
            if (token.isEmpty()) continue;

            // Handle combined G/M codes like G90, G21, M06, M30
            Matcher m = WORD_PATTERN.matcher(token);
            if (m.matches()) {
                char letter = Character.toUpperCase(m.group(1).charAt(0));
                String numStr = m.group(2);
                try {
                    double val = Double.parseDouble(numStr);
                    words.put(letter, val);

                    if (letter == 'G') {
                        gCodes.add(normalizeCode('G', (int) Math.round(val)));
                    } else if (letter == 'M') {
                        mCodes.add(normalizeCode('M', (int) Math.round(val)));
                    }
                } catch (NumberFormatException e) {
                    malformed = true;
                }
            } else if (!token.equals("%") && !token.startsWith("O")) {
                malformed = true;
            }
        }
    }

    private String normalizeCode(char prefix, int num) {
        if (prefix == 'G' && num < 10) return String.format(Locale.US, "G%02d", num);
        if (prefix == 'M' && num < 10) return String.format(Locale.US, "M%02d", num);
        return prefix + String.valueOf(num);
    }

    public String getRawText()                       { return rawText; }
    public String getStrippedText()                  { return strippedText; }
    public String getComment()                       { return comment; }
    public Integer getLineNumber()                   { return lineNumber; }
    public Set<String> getGCodes()                   { return Collections.unmodifiableSet(gCodes); }
    public Set<String> getMCodes()                   { return Collections.unmodifiableSet(mCodes); }
    public boolean hasWord(char c)                   { return words.containsKey(Character.toUpperCase(c)); }
    public Double getWord(char c)                    { return words.get(Character.toUpperCase(c)); }
    public boolean isCommentOnly()                   { return strippedText.isEmpty() && comment != null; }
    public boolean isTapeHeaderOrRewind()            { return strippedText.equals("%"); }
    public boolean isProgramNumber()                 { return strippedText.startsWith("O") || words.containsKey('O'); }
    public boolean isMalformed()                     { return malformed; }

    public boolean hasG(String code) {
        if (code == null) return false;
        String norm = code.toUpperCase(Locale.US);
        if (norm.equals("G0") || norm.equals("G00")) return gCodes.contains("G00") || gCodes.contains("G0");
        if (norm.equals("G1") || norm.equals("G01")) return gCodes.contains("G01") || gCodes.contains("G1");
        return gCodes.contains(norm);
    }

    public boolean hasM(String code) {
        if (code == null) return false;
        String norm = code.toUpperCase(Locale.US);
        if (norm.equals("M3") || norm.equals("M03")) return mCodes.contains("M03") || mCodes.contains("M3");
        if (norm.equals("M5") || norm.equals("M05")) return mCodes.contains("M05") || mCodes.contains("M5");
        if (norm.equals("M6") || norm.equals("M06")) return mCodes.contains("M06") || mCodes.contains("M6");
        return mCodes.contains(norm);
    }
}
