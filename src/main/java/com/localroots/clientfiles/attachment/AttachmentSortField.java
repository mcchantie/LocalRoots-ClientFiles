package com.localroots.clientfiles.attachment;

public enum AttachmentSortField {
    NAME("displayName"),
    CREATED_AT("createdAt"),
    UPDATED_AT("updatedAt"),
    SIZE("declaredSizeBytes");

    private final String property;

    AttachmentSortField(String property) {
        this.property = property;
    }

    public String property() {
        return property;
    }
}
