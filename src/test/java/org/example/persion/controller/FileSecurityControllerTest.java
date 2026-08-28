package org.example.persion.controller;

import org.example.persion.common.Result;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.service.ChatGroupAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileSecurityControllerTest {

    private static final Long USER_ID = 7L;
    private static final Long GROUP_ID = 11L;

    @TempDir
    Path tempDir;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsBrowserExecutableAttachmentTypes() {
        authenticate();
        ChatGroupAccessService accessService = mock(ChatGroupAccessService.class);
        FileUploadController controller = new FileUploadController(accessService);
        ReflectionTestUtils.setField(controller, "uploadPath", tempDir.resolve("uploads").toString());

        MockMultipartFile html = new MockMultipartFile(
                "file", "payload.html", "text/html", "<script>alert(1)</script>".getBytes()
        );

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> controller.uploadGeneralFile(html, null)
        );
        assertEquals(400, error.getCode());
    }

    @Test
    void rejectsUploadToUnauthorizedChatGroup() {
        authenticate();
        ChatGroupAccessService accessService = mock(ChatGroupAccessService.class);
        when(accessService.canAccess(USER_ID, GROUP_ID)).thenReturn(false);
        FileUploadController controller = new FileUploadController(accessService);
        ReflectionTestUtils.setField(controller, "uploadPath", tempDir.resolve("uploads").toString());

        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.png", "image/png", new byte[]{1, 2, 3}
        );

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> controller.uploadImage(image, GROUP_ID)
        );
        assertEquals(403, error.getCode());
    }

    @Test
    void storesAllowedImageUnderGeneratedNameInsideUploadRoot() throws Exception {
        authenticate();
        ChatGroupAccessService accessService = mock(ChatGroupAccessService.class);
        when(accessService.canAccess(USER_ID, GROUP_ID)).thenReturn(true);
        FileUploadController controller = new FileUploadController(accessService);
        Path uploadRoot = tempDir.resolve("uploads");
        ReflectionTestUtils.setField(controller, "uploadPath", uploadRoot.toString());

        MockMultipartFile image = new MockMultipartFile(
                "image", "../../photo.png", "image/png", new byte[]{1, 2, 3}
        );

        Result<FileUploadController.FileUploadResult> result = controller.uploadImage(image, GROUP_ID);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().getFilename().endsWith(".png"));
        assertEquals("photo.png", result.getData().getOriginalFilename());
        assertTrue(Files.walk(uploadRoot).anyMatch(Files::isRegularFile));
    }

    @Test
    void blocksDownloadPathTraversalOutsideUploadRoot() throws Exception {
        Path uploadRoot = tempDir.resolve("uploads");
        Files.createDirectories(uploadRoot);
        Files.writeString(tempDir.resolve("secret.txt"), "secret");

        FileDownloadController controller = new FileDownloadController();
        ReflectionTestUtils.setField(controller, "uploadPath", uploadRoot.toString());

        ResponseEntity<Resource> response = controller.downloadFile("/uploads/../secret.txt");

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void servesNormalDownloadInsideUploadRoot() throws Exception {
        Path uploadRoot = tempDir.resolve("uploads");
        Path file = uploadRoot.resolve("file/2026-08-28/note.txt");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "hello");

        FileDownloadController controller = new FileDownloadController();
        ReflectionTestUtils.setField(controller, "uploadPath", uploadRoot.toString());

        ResponseEntity<Resource> response = controller.downloadFile("/uploads/file/2026-08-28/note.txt");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of())
        );
    }
}
