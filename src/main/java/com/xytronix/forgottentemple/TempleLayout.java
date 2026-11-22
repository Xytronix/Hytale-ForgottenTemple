package com.xytronix.forgottentemple;

public enum TempleLayout {
    CHAPTER0("Chapter0", "Forgotten_Temple", false),
    CHAPTER1("Chapter1", "Forgotten_Temple_Goblins", true);

    public static final TempleLayout DEFAULT = CHAPTER1;

    private final String configName;
    private final String templateName;
    private final boolean requiresTriggerVolumes;

    TempleLayout(String configName, String templateName, boolean requiresTriggerVolumes) {
        this.configName = configName;
        this.templateName = templateName;
        this.requiresTriggerVolumes = requiresTriggerVolumes;
    }

    public String configName() {
        return configName;
    }

    public String templateName() {
        return templateName;
    }

    boolean requiresTriggerVolumes() {
        return requiresTriggerVolumes;
    }

    public static TempleLayout fromConfig(String value) {
        for (TempleLayout layout : values()) {
            if (layout.configName.equalsIgnoreCase(value == null ? "" : value.strip())) {
                return layout;
            }
        }
        return null;
    }

    static TempleLayout fromTemplate(String templateName) {
        for (TempleLayout layout : values()) {
            if (layout.templateName.equals(templateName)) {
                return layout;
            }
        }
        return null;
    }
}
