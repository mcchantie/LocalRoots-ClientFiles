package com.localroots.clientfiles.api;

import java.util.List;

public record BatchUpdateAttachmentsResponse(
        int updatedCount,
        List<AttachmentResponse> attachments
) {
}
