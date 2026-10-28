package ui.workspace.drafting.features;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public final class entity_lineage_contract_ui_main {
    public enum Outcome { PRESERVED, SPLIT, MERGED, CONSUMED, REPLACED, UNRESOLVED }

    public record Id128(long high, long low) {
        public static Id128 fromString(String name) {
            if (name == null || name.isBlank()) return new Id128(0L, 0L);
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] b = md.digest(name.getBytes(StandardCharsets.UTF_8));
                ByteBuffer buf = ByteBuffer.wrap(b);
                return new Id128(buf.getLong(), buf.getLong());
            } catch (NoSuchAlgorithmException e) {
                long h = name.hashCode();
                return new Id128(h, h ^ 0x5555555555555555L);
            }
        }
    }

    public record EntityRef(Id128 featureId, int role, int subIndex, int flags, Id128 relatedFeatureId) {
        public static EntityRef of(String fId, int role, int subIndex) {
            return new EntityRef(Id128.fromString(fId), role, subIndex, 0, new Id128(0L, 0L));
        }
        public static EntityRef of(String fId, int role, int subIndex, String relId) {
            return new EntityRef(Id128.fromString(fId), role, subIndex, 0, Id128.fromString(relId));
        }
    }

    public record LineageRecord(Outcome outcome, long srcRev, long dstRev, EntityRef source, List<EntityRef> successors) {}

    private final Map<EntityRef, LineageRecord> lineageMap = new HashMap<>();

    public void recordLineage(LineageRecord rec) {
        if (rec != null && rec.source() != null) {
            lineageMap.put(rec.source(), rec);
        }
    }

    public ResolutionResult resolveEntity(EntityRef source) {
        if (source == null) return new ResolutionResult(Outcome.UNRESOLVED, Collections.emptyList());
        LineageRecord rec = lineageMap.get(source);
        if (rec == null) return new ResolutionResult(Outcome.PRESERVED, List.of(source));
        if (rec.outcome() == Outcome.SPLIT) {
            return new ResolutionResult(Outcome.SPLIT, Collections.unmodifiableList(rec.successors()));
        }
        if (rec.outcome() == Outcome.CONSUMED) {
            return new ResolutionResult(Outcome.CONSUMED, Collections.emptyList());
        }
        if (rec.outcome() == Outcome.UNRESOLVED) {
            return new ResolutionResult(Outcome.UNRESOLVED, Collections.emptyList());
        }
        return new ResolutionResult(rec.outcome(), Collections.unmodifiableList(rec.successors()));
    }

    public record ResolutionResult(Outcome outcome, List<EntityRef> targets) {
        public boolean isUnique() { return outcome == Outcome.PRESERVED && targets.size() == 1; }
        public boolean isConsumed() { return outcome == Outcome.CONSUMED; }
        public boolean isSplit() { return outcome == Outcome.SPLIT; }
    }
}
