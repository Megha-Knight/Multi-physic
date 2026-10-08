package ui.workspace.drafting.loft;

import ui.workspace.drafting.features.feature_state_ui_main;
import ui.workspace.drafting.features.feature_type_ui_main;
import ui.workspace.drafting.profiles.profile_reference_ui_main;
import ui.workspace.drafting.profiles.profile_validator_ui_main;
import ui.workspace.drafting.shape_item_ui_main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * loft_feature_ui_main.java
 * Parametric Loft feature definition interpolating across ordered section profiles.
 */
public class loft_feature_ui_main {

    private final String id;
    private final String ownerShapeId;
    private String name;
    private final List<profile_reference_ui_main> sections = new ArrayList<>();
    private boolean solid = true;
    private feature_state_ui_main state = feature_state_ui_main.CLEAN;
    private boolean visible = true;
    private String diagnosticMessage = null;

    public loft_feature_ui_main(String id, String ownerShapeId, String name,
                               List<profile_reference_ui_main> sections, boolean solid) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.ownerShapeId = ownerShapeId;
        this.name = (name != null && !name.isBlank()) ? name : "Loft";
        this.solid = solid;
        if (sections != null) {
            for (profile_reference_ui_main p : sections) {
                if (p != null) this.sections.add(p);
            }
        }
        revalidate(null);
    }

    public String getId() { return id; }
    public String getOwnerShapeId() { return ownerShapeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isSolid() { return solid; }
    public void setSolid(boolean solid) { this.solid = solid; revalidate(null); }

    public List<profile_reference_ui_main> getSections() {
        return Collections.unmodifiableList(sections);
    }
    public int getSectionCount() { return sections.size(); }
    public void addSection(profile_reference_ui_main p) { if (p != null) { sections.add(p); revalidate(null); } }
    public void removeSection(int index) { if (index >= 0 && index < sections.size()) { sections.remove(index); revalidate(null); } }
    public void clearSections() { sections.clear(); revalidate(null); }
    public void setSections(List<profile_reference_ui_main> list) {
        sections.clear();
        if (list != null) for (profile_reference_ui_main p : list) if (p != null) sections.add(p);
        revalidate(null);
    }

    public feature_type_ui_main getFeatureType() { return feature_type_ui_main.LOFT; }
    public feature_state_ui_main getState() { return state; }
    public void setState(feature_state_ui_main state) { this.state = state != null ? state : feature_state_ui_main.CLEAN; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public String getDiagnosticMessage() { return diagnosticMessage; }
    public void setDiagnosticMessage(String msg) { this.diagnosticMessage = msg; }

    public boolean isValid() {
        if (state == feature_state_ui_main.INVALID || sections.size() < 2) return false;
        for (profile_reference_ui_main s : sections) {
            if (!profile_validator_ui_main.validate(s, solid).isValid()) return false;
        }
        return true;
    }

    public void revalidate(shape_item_ui_main host) {
        if (sections.size() < 2) {
            this.state = feature_state_ui_main.INVALID;
            this.diagnosticMessage = "Loft requires at least 2 section profiles (found: " + sections.size() + ").";
            return;
        }
        for (int i = 0; i < sections.size(); i++) {
            var res = profile_validator_ui_main.validate(sections.get(i), solid);
            if (!res.isValid()) {
                this.state = feature_state_ui_main.INVALID;
                this.diagnosticMessage = "Section " + (i + 1) + " invalid: " + res.getMessage();
                return;
            }
        }
        this.state = feature_state_ui_main.CLEAN;
        this.diagnosticMessage = null;
    }

    public loft_feature_ui_main copy() {
        loft_feature_ui_main cp = new loft_feature_ui_main(id, ownerShapeId, name, sections, solid);
        cp.setState(state);
        cp.setVisible(visible);
        cp.setDiagnosticMessage(diagnosticMessage);
        return cp;
    }
}
