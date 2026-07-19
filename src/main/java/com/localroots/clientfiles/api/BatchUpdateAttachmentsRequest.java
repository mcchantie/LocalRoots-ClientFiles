package com.localroots.clientfiles.api;

import com.localroots.clientfiles.attachment.AttachmentCategory;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record BatchUpdateAttachmentsRequest(
        @NotEmpty(message = "Select at least one attachment")
        @Size(max = 100, message = "A maximum of 100 attachments can be updated at once")
        List<UUID> attachmentIds,
        boolean updateContact,
        UUID contactId,
        AttachmentCategory category
) {
}
