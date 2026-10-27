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
    private static final Set<Character> SUPPORTED_LETTERS = Set.of('N', 'G', 'M', 'X', 'Y', 'Z', 'F', 'S', 'T', 'H', 'R', 'Q', 'P', 'O');
    private static final Set<String> SUPPORTED_G = Set.of("G00", "G01", "G02", "G03", "G04", "G17", "G18", "G19", "G20", "G21", "G28", "G30", "G43", "G44", "G49", "G53", "G54", "G55", "G56", "G57", "G58", "G59", "G73", "G80", "G81", "G82", "G83", "G90", "G91", "G98", "G99");
    private static final Set<String> SUPPORTED_M = Set.of("M00", "M01", "M02", "M03", "M04", "M05", "M06", "M08", "M09", "M30");

    private final String rawText;
    private final String strippedText;
    private final String comment;
    private final Integer lineNumber;
    private final Set<String> gCodes = new LinkedHashSet<>();
    private final Set<String> mCodes = new LinkedHashSet<>();
    private final Map<Character, Double> words = new HashMap<>();
    private final Set<Character> duplicateWords = new LinkedHashSet<>();
    private final Set<String> unsupportedCommands = new LinkedHashSet<>();
    private boolean conflictingModalCommands = false;
    private boolean malformed = false;

    public NcBlockParser(String line) {
        this.rawText = (line != null) ? line : "";
        String text = rawText.trim();

        Matcher commentMatcher = COMMENT_PATTERN.matcher(text);
        if (commentMatcher.find()) {
            this.comment = commentMatcher.group(1);
            text = commentMatcher.replaceAll(" ").trim();
        } else {
            this.comment = null;
        }

        this.strippedText = text;
        parseWords(text);
        this.lineNumber = words.containsKey('N') ? words.get('N').intValue() : null;
        checkConflictingModalCommands();
    }

    private void parseWords(String text) {
        if (text.isEmpty() || text.equals("%")) return;

        String[] tokens = text.split("\\s+");
        for (String token : tokens) {
            if (token.isEmpty()) continue;

            Matcher m = WORD_PATTERN.matcher(token);
            if (m.matches()) {
                char letter = Character.toUpperCase(m.group(1).charAt(0));
                String numStr = m.group(2);
                if (!SUPPORTED_LETTERS.contains(letter)) unsupportedCommands.add(token);
                if (words.containsKey(letter) && letter != 'G' && letter != 'M') duplicateWords.add(letter);

                try {
                    double val = Double.parseDouble(numStr);
                    words.put(letter, val);
                    if (letter == 'G') {
                        String g = normalizeCode('G', (int) Math.round(val));
                        if (!SUPPORTED_G.contains(g)) unsupportedCommands.add(g);
                        if (gCodes.contains(g)) duplicateWords.add('G');
                        gCodes.add(g);
                    } else if (letter == 'M') {
                        String mc = normalizeCode('M', (int) Math.round(val));
                        if (!SUPPORTED_M.contains(mc)) unsupportedCommands.add(mc);
                        if (mCodes.contains(mc)) duplicateWords.add('M');
                        mCodes.add(mc);
                    }
                } catch (NumberFormatException e) {
                    malformed = true;
                }
            } else if (!token.equals("%") && !token.startsWith("O")) {
                malformed = true;
            }
        }
    }

    private void checkConflictingModalCommands() {
        if ((hasG("G00") && hasG("G01")) || (hasG("G90") && hasG("G91"))
                || (hasG("G17") && (hasG("G18") || hasG("G19")))) {
            conflictingModalCommands = true;
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
    public Set<Character> getDuplicateWords()        { return Collections.unmodifiableSet(duplicateWords); }
    public Set<String> getUnsupportedCommands()      { return Collections.unmodifiableSet(unsupportedCommands); }
    public boolean hasConflictingModalCommands()     { return conflictingModalCommands; }
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
