package secs.service;

import org.itmo.secs.domain.model.entities.FileMetadata;
import org.itmo.secs.application.services.FileService;
import org.itmo.secs.application.repositories.FileRepository;
import org.itmo.secs.exception.FileStorageException;
import org.itmo.secs.exception.ItemNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private FileService fileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        fileService = new FileService(fileRepository);
        // подменяем uploadDir на temp
        ReflectionTestUtils.setField(fileService, "uploadDir", tempDir.toString());
    }

    @Test
    void uploadFile_success() {

        FilePart filePart = mock(FilePart.class);

        when(filePart.filename()).thenReturn("test.txt");

        Flux<DataBuffer> content =
                Flux.just(new DefaultDataBufferFactory().wrap("hello".getBytes()));

        when(filePart.content()).thenReturn(content);

        FileMetadata saved = FileMetadata.builder()
                .id(1L)
                .fileName("test.txt")
                .filePath(tempDir.resolve("test.txt").toString())
                .build();

        when(fileRepository.save(any()))
                .thenReturn(saved);

        Mono<FileMetadata> result = fileService.uploadFile(filePart);

        StepVerifier.create(result)
                .assertNext(metadata -> {
                    assertEquals("test.txt", metadata.getFileName());
                    assertNotNull(metadata.getFileSize());
                })
                .verifyComplete();
    }

    @Test
    void getFileMetadata_success() {

        FileMetadata metadata = FileMetadata.builder()
                .id(1L)
                .fileName("file.txt")
                .build();

        when(fileRepository.findById(1L))
                .thenReturn(Optional.of(metadata));

        StepVerifier.create(fileService.getFileMetadata(1L))
                .expectNext(metadata)
                .verifyComplete();
    }

    @Test
    void getFileMetadata_notFound() {

        when(fileRepository.findById(1L))
                .thenReturn(Optional.empty());

        StepVerifier.create(fileService.getFileMetadata(1L))
                .expectError(ItemNotFoundException.class)
                .verify();
    }

    @Test
    void downloadFile_success() throws IOException {

        Path file = Files.createFile(tempDir.resolve("file.txt"));
        Files.writeString(file, "hello");

        FileMetadata metadata = FileMetadata.builder()
                .id(1L)
                .filePath(file.toString())
                .build();

        when(fileRepository.findById(1L))
                .thenReturn(Optional.of(metadata));

        StepVerifier.create(fileService.downloadFile(1L))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void downloadFile_notFoundOnDisk() {

        FileMetadata metadata = FileMetadata.builder()
                .id(1L)
                .filePath("missing.txt")
                .build();

        when(fileRepository.findById(1L))
                .thenReturn(Optional.of(metadata));

        StepVerifier.create(fileService.downloadFile(1L))
                .expectError(ItemNotFoundException.class)
                .verify();
    }

    @Test
    void uploadFile_WhenCreateDirectoryFails_ShouldThrowFileStorageException() {
        // Arrange
        FilePart filePart = mock(FilePart.class);
//        when(filePart.filename()).thenReturn("test.txt");

        // Подменяем uploadDir на недоступную директорию
        Path invalidPath = Paths.get("Z:\\invalid\\path\\that\\doesnt\\exist");
        ReflectionTestUtils.setField(fileService, "uploadDir", invalidPath.toString());

        // Act
        Mono<FileMetadata> result = fileService.uploadFile(filePart);

        // Assert
        StepVerifier.create(result)
                .expectError(FileStorageException.class)
                .verify();
    }
}