package com.localroots.clientfiles.api;

import com.localroots.clientfiles.attachment.AttachmentService;
import com.localroots.clientfiles.security.RequestTenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PatchMapping;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttachmentControllerTest {

    @Test
    void exposesPatchEndpointForAssignmentUpdates() throws Exception {
        Method method = AttachmentController.class.getDeclaredMethod(
                "updateAttachmentAssignment",
                HttpServletRequest.class,
                UUID.class,
                AssignAttachmentRequest.class
        );

        PatchMapping mapping = method.getAnnotation(PatchMapping.class);

        assertTrue(mapping != null);
        assertArrayEquals(new String[]{"/{attachmentId}"}, mapping.value());
    }

    @Test
    void patchAssignsAttachmentToSelectedContact() {
        AttachmentService attachmentService = mock(AttachmentService.class);
        RequestTenantResolver tenantResolver = mock(RequestTenantResolver.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        AttachmentResponse expected = mock(AttachmentResponse.class);

        AttachmentController controller = new AttachmentController(attachmentService, tenantResolver);
        UUID tenantId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();

        when(tenantResolver.requireTenantId(request)).thenReturn(tenantId);
        when(attachmentService.assignToContact(tenantId, attachmentId, contactId)).thenReturn(expected);

        AttachmentResponse actual = controller.updateAttachmentAssignment(
                request,
                attachmentId,
                new AssignAttachmentRequest(contactId)
        );

        assertSame(expected, actual);
        verify(attachmentService).assignToContact(tenantId, attachmentId, contactId);
    }

    @Test
    void patchWithNullContactIdMovesAttachmentToUnassigned() {
        AttachmentService attachmentService = mock(AttachmentService.class);
        RequestTenantResolver tenantResolver = mock(RequestTenantResolver.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        AttachmentResponse expected = mock(AttachmentResponse.class);

        AttachmentController controller = new AttachmentController(attachmentService, tenantResolver);
        UUID tenantId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();

        when(tenantResolver.requireTenantId(request)).thenReturn(tenantId);
        when(attachmentService.assignToContact(tenantId, attachmentId, null)).thenReturn(expected);

        AttachmentResponse actual = controller.updateAttachmentAssignment(
                request,
                attachmentId,
                new AssignAttachmentRequest(null)
        );

        assertSame(expected, actual);
        verify(attachmentService).assignToContact(tenantId, attachmentId, null);
    }
    @Test
    void deletedOnlyListRequestIsForwardedToService() {
        AttachmentService attachmentService = mock(AttachmentService.class);
        RequestTenantResolver tenantResolver = mock(RequestTenantResolver.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        PageResponse<AttachmentResponse> expected = new PageResponse<>(
                java.util.List.of(),
                0,
                25,
                0,
                0,
                true,
                true
        );

        AttachmentController controller = new AttachmentController(attachmentService, tenantResolver);
        UUID tenantId = UUID.randomUUID();
        when(tenantResolver.requireTenantId(request)).thenReturn(tenantId);
        when(attachmentService.list(
                tenantId,
                null,
                null,
                null,
                false,
                false,
                true,
                0,
                25
        )).thenReturn(expected);

        PageResponse<AttachmentResponse> actual = controller.listAttachments(
                request,
                null,
                null,
                null,
                false,
                false,
                true,
                0,
                25
        );

        assertSame(expected, actual);
        verify(attachmentService).list(
                tenantId,
                null,
                null,
                null,
                false,
                false,
                true,
                0,
                25
        );
    }

}
