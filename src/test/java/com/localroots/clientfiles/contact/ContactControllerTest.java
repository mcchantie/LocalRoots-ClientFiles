package com.localroots.clientfiles.contact;

import com.localroots.clientfiles.api.AttachmentResponse;
import com.localroots.clientfiles.api.PageResponse;
import com.localroots.clientfiles.attachment.AttachmentCategory;
import com.localroots.clientfiles.attachment.AttachmentService;
import com.localroots.clientfiles.attachment.AttachmentStatus;
import com.localroots.clientfiles.security.RequestTenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContactControllerTest {

    @Test
    void listsAttachmentsForAContactWithinTheAuthenticatedTenant() {
        ContactService contactService = mock(ContactService.class);
        AttachmentService attachmentService = mock(AttachmentService.class);
        RequestTenantResolver tenantResolver = mock(RequestTenantResolver.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        ContactController controller = new ContactController(
                contactService,
                attachmentService,
                tenantResolver
        );

        UUID tenantId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        PageResponse<AttachmentResponse> expected = new PageResponse<>(
                List.of(),
                1,
                10,
                0,
                0,
                true,
                true
        );

        when(tenantResolver.requireTenantId(request)).thenReturn(tenantId);
        when(contactService.get(tenantId, contactId)).thenReturn(mock(ContactResponse.class));
        when(attachmentService.list(
                tenantId,
                contactId,
                AttachmentCategory.PROPERTY_PHOTOS,
                AttachmentStatus.READY,
                false,
                true,
                false,
                1,
                10
        )).thenReturn(expected);

        PageResponse<AttachmentResponse> actual = controller.listAttachments(
                request,
                contactId,
                AttachmentCategory.PROPERTY_PHOTOS,
                AttachmentStatus.READY,
                true,
                false,
                1,
                10
        );

        assertSame(expected, actual);
        verify(contactService).get(tenantId, contactId);
        verify(attachmentService).list(
                tenantId,
                contactId,
                AttachmentCategory.PROPERTY_PHOTOS,
                AttachmentStatus.READY,
                false,
                true,
                false,
                1,
                10
        );
    }
}
