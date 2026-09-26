package ui;

/**
 * framework_ui_main.java
 * Central UI Design System & Structural Framework Controller for Astra.
 *
 * Modifying values in this file directly changes the dimensions, proportions,
 * and design aesthetics of all bars, tools, navigators, and 3D viewports across the app.
 */
public final class framework_ui_main {

    private framework_ui_main() {} // Non-instantiable utility & config class

    // =========================================================================
    // 1. TOP TAB / TOOLBAR (tools_ui_main)
    // =========================================================================
    public static double TAB_TOOLBAR_HEIGHT       = 28.0; // Height of top tabs strip
    public static double TAB_FONT_SIZE            = 11.0; // Font size of tab titles
    public static String TAB_TOOLBAR_BG           = "#005A85"; // Matches footer accent blue
    public static String TAB_BORDER_COLOR         = "#00486B"; // Bottom divider border

    // =========================================================================
    // 2. RIBBON BAR & ACTION BUTTONS (ribbon_ui_main, splitbutton_ui_main)
    // =========================================================================
    public static double RIBBON_BAR_HEIGHT        = 60.0; // Height of ribbon tool strip
    public static double RIBBON_BUTTON_WIDTH      = 46.0; // Width of tool split-buttons
    public static double RIBBON_BUTTON_HEIGHT     = 46.0; // Height of tool split-buttons
    public static double RIBBON_ICON_SIZE         = 15.0; // Size of SVG action icons
    public static double RIBBON_LABEL_FONT_SIZE   = 9.5;  // Font size of button labels
    public static double RIBBON_GROUP_FONT_SIZE   = 8.5;  // Font size of section titles ("Files")
    public static String RIBBON_BAR_BG            = "#EBF3F8"; // Derivative tint of #005A85
    public static String RIBBON_BAR_BORDER        = "#C8DCE8";

    // =========================================================================
    // 3. BREADCRUMB ADDRESS BAR (breadcrumb_ui_main)
    // =========================================================================
    public static double BREADCRUMB_BAR_HEIGHT    = 28.0; // Height of full-width breadcrumb
    public static double BREADCRUMB_BUTTON_SIZE   = 22.0; // Size of Back/Forward/Up buttons
    public static double BREADCRUMB_FONT_SIZE     = 11.0; // Font size of path segments
    public static String BREADCRUMB_BAR_BG        = "#F8FAFC";
    public static String BREADCRUMB_BORDER        = "#C9D1D9";

    // =========================================================================
    // 4. MODEL NAVIGATOR / LEFT SIDEBAR (navigation_ui_main)
    // =========================================================================
    public static double NAVIGATOR_WIDTH          = 260.0; // Default sidebar width
    public static double NAVIGATOR_MIN_WIDTH      = 180.0; // Minimum resize width
    public static double NAVIGATOR_MAX_WIDTH      = 450.0; // Maximum resize width
    public static double NAVIGATOR_SPLIT_RATIO    = 0.5;   // Split ratio between File/Feature
    public static double NAVIGATOR_HEADER_HEIGHT  = 26.0;  // Height of panel headers
    public static String NAVIGATOR_HEADER_BG      = "#F1F5F9";
    public static String NAVIGATOR_BORDER         = "#CBD5E1";

    // =========================================================================
    // 5. 3D WORKSPACE, CAMERA & VIEW CUBE (workspace_ui_main, viewcube_ui_main)
    // =========================================================================
    public static double VIEW_CUBE_SIZE           = 98.0;  // Reduced by ~15% for optimal viewport fit
    public static double VIEW_CUBE_MARGIN         = 10.0;  // Margin from top-right corner
    public static double AXIS_LENGTH              = 55.0;  // Length of X/Y/Z coordinate axes
    public static double AXIS_RADIUS              = 0.35;  // Slender radius of coordinate axes
    public static double GRID_EXTENT              = 1800.0;// Floor grid visible area
    public static double GRID_SPACING             = 40.0;  // Grid line spacing
    public static String WORKSPACE_BG             = "#F8FAFC";

    // Standard Mathematical Axonometric Camera Angles (Exact 120-degree axis separation)
    public static double ISO_PITCH                = -35.264; // -arcsin(1/sqrt(3))
    public static double ISO_YAW                  = -45.0;   // Top-Front-Right Isometric
    public static double DIMETRIC_PITCH           = -20.7;   // Standard dimetric elevation
    public static double DIMETRIC_YAW             = -45.0;
    public static double TRIMETRIC_PITCH          = -25.0;   // Standard trimetric elevation
    public static double TRIMETRIC_YAW            = -60.0;

    // =========================================================================
    // 6. BOTTOM STATUS FOOTER (footer_ui_main)
    // =========================================================================
    public static double FOOTER_BAR_HEIGHT        = 22.0; // Height of bottom status bar
    public static double FOOTER_FONT_SIZE         = 11.0; // Font size of file path
    public static String FOOTER_BAR_BG            = "#005A85"; // Accent blue background
    public static String FOOTER_BORDER            = "#004364"; // Top border color
    public static String FOOTER_TEXT_COLOR        = "#FFFFFF"; // Text color

    // =========================================================================
    // 7. BRAND & THEME COLOR PALETTE
    // =========================================================================
    public static String PRIMARY_BRAND_COLOR      = "#005A85";
    public static String ACCENT_HOVER_COLOR       = "#29ABE2";
    public static String TEXT_MAIN_COLOR          = "#1F2933";
    public static String TEXT_MUTED_COLOR         = "#627D98";
    public static String BORDER_DEFAULT_COLOR     = "#CBD5E1";

    // =========================================================================
    // 8. PROJECT FILE EXTENSIONS
    // =========================================================================
    public static String DEFAULT_PROJECT_EXTENSION = ".nd";
    public static String LEGACY_PROJECT_EXTENSION  = ".nc";

    // =========================================================================
    // 9. DOCUMENT FILE TABS BAR (filetab_ui_main)
    // =========================================================================
    public static double FILE_TAB_HEIGHT          = 28.0;
    public static String FILE_TAB_BG              = "#F1F5F9";
    public static String FILE_TAB_BORDER          = "#CBD5E1";

    // =========================================================================
    // 10. BASIC SHAPES & 3D DRAFTING
    // =========================================================================
    public static String DRAFT_PROFILE_COLOR      = "#005A85";
    public static String DRAFT_PREVIEW_COLOR      = "#0284C7";
    public static String DRAFT_ACCENT_COLOR       = "#29ABE2";
    public static String DRAFT_SELECTED_COLOR     = "#0284C7";
    public static String DRAFT_HANDLE_COLOR       = "#29ABE2";
    public static String ROTATION_HANDLE_COLOR    = "#F59E0B";
    public static double DRAFT_LINE_RADIUS        = 0.70;
    public static double DRAFT_HANDLE_RADIUS      = 2.4;

    // CAD Object Selection & Edge Styling (Color grading between gray and white)
    public static String OBJECT_UNSELECTED_COLOR       = "#8C9DAE";
    public static String OBJECT_UNSELECTED_EDGE_COLOR  = "#475569";
    public static String OBJECT_SELECTED_COLOR         = "#0284C7";
    public static String OBJECT_SELECTED_EDGE_COLOR    = "#005A85";
    public static double OBJECT_SELECTION_STROKE_WIDTH = 1.2;

    // Feature Manager Selection Styling
    public static String FEATURE_ROW_SELECTED_BG       = "#E0F2FE";
    public static String FEATURE_ROW_HOVER_BG          = "#F1F5F9";
    public static String FEATURE_ROW_BORDER            = "#005A85";

    // Dimension Editor Dialog Dimensions
    public static double DIMENSION_DIALOG_WIDTH        = 340.0;
    public static double DIMENSION_DIALOG_HEIGHT       = 440.0;

    // XYZ Translation gizmo colours (world-axis handles on selected 3D objects)
    public static String GIZMO_X_COLOR           = "#DC2626"; // Red  — X axis
    public static String GIZMO_Y_COLOR           = "#16A34A"; // Green — Y axis
    public static String GIZMO_Z_COLOR           = "#2563EB"; // Blue  — Z axis
    public static String GIZMO_X_HOVER           = "#FCA5A5";
    public static String GIZMO_Y_HOVER           = "#86EFAC";
    public static String GIZMO_Z_HOVER           = "#93C5FD";
    public static double GIZMO_SHAFT_RADIUS      = 1.5;       // Shaft cylinder radius
    public static double GIZMO_TIP_RADIUS        = 4.0;       // Arrowhead sphere radius
    public static double GIZMO_LENGTH            = 42.0;      // Total axis arm length

    // =========================================================================
    // 11. DOCUMENTS & ASTRA WORKSPACE DIRECTORY
    // =========================================================================
    public static java.io.File documentsDir() {
        return astraDirectory();
    }

    public static java.io.File astraDirectory() {
        java.io.File userHome = new java.io.File(System.getProperty("user.home"));
        java.io.File docs = new java.io.File(userHome, "Documents");
        java.io.File base = (docs.exists() || docs.mkdir()) ? docs : userHome;
        java.io.File appDir = new java.io.File(base, "Multiphysics");
        if (!appDir.exists()) {
            boolean ok = appDir.mkdirs();
            if (!ok && !appDir.exists()) {
                System.err.println("[Multiphysics] Warning: Failed to create " + appDir.getAbsolutePath());
                return base;
            }
        }
        return appDir;
    }

    // =========================================================================
    // 12. DYNAMIC SHAPES PANEL (basicshapespanel_ui_main)
    // =========================================================================
    public static double SHAPES_PANEL_HEIGHT        = 48.0;
    public static String SHAPES_PANEL_BG            = "#FFFFFF";
    public static String SHAPES_PANEL_BORDER        = "#CBD5E1";
    public static String SHAPES_CATEGORY_ACTIVE_BG  = "#005A85";
    public static String SHAPES_CATEGORY_ACTIVE_TXT = "#FFFFFF";
    public static String SHAPES_CATEGORY_INACTIVE_TXT = "#64748B";
    public static String SHAPES_ITEM_HOVER_BG       = "rgba(41, 171, 226, 0.15)";
}
