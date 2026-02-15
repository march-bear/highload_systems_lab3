package org.itmo.secs.services;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itmo.secs.model.entities.FileMetadata;
import org.itmo.secs.repositories.FileRepository;
import org.itmo.secs.utils.exceptions.FileStorageException;
import org.itmo.secs.utils.exceptions.ItemNotFoundException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;
//    private final FileStorageConfig fileStorageConfig;

    private String uploadDir = "uploads";

    private Path getUploadPath() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public Mono<FileMetadata> uploadFile(FilePart filePart) {
        return Mono.fromCallable(() -> {
                    Path uploadPath = getUploadPath();
                    try {
                        Files.createDirectories(uploadPath);
                    } catch (IOException e) {
                        throw new FileStorageException("Failed to create upload directory: " + e.getMessage(), e);
                    }

                    String originalFileName = filePart.filename();
                    String fileExtension = getFileExtension(originalFileName);
                    String uniqueFileName = UUID.randomUUID() + fileExtension;
                    return uploadPath.resolve(uniqueFileName);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(filePath -> {
                    FileMetadata metadata = FileMetadata.builder()
                            .fileName(filePart.filename())
                            .filePath(filePath.toString())
                            .fileSize(0L)
                            .build();

                    return Mono.fromCallable(() -> fileRepository.save(metadata))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(savedMetadata -> {
                                final Long metadataId = savedMetadata.getId();
                                final Path finalFilePath = filePath;

                                return DataBufferUtils.write(filePart.content(), finalFilePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
                                        .then(Mono.fromCallable(() -> {
                                                    try {
                                                        long fileSize = Files.size(finalFilePath);
                                                        savedMetadata.setFileSize(fileSize);
                                                        fileRepository.save(savedMetadata);
                                                        log.info("File uploaded successfully: {} (size: {} bytes)", filePart.filename(), fileSize);
                                                        return savedMetadata;
                                                    } catch (IOException e) {
                                                        log.error("Error getting file size: {}", e.getMessage(), e);
                                                        fileRepository.deleteById(metadataId);
                                                        try {
                                                            Files.deleteIfExists(finalFilePath);
                                                        } catch (IOException ex) {
                                                            log.warn("Failed to delete file: {}", finalFilePath, ex);
                                                        }
                                                        throw new FileStorageException("Failed to get file size: " + e.getMessage(), e);
                                                    }
                                                })
                                                .subscribeOn(Schedulers.boundedElastic()))
                                        .publishOn(Schedulers.boundedElastic())
                                        .onErrorMap(IOException.class, e -> {
                                            log.error("Error saving file: {}", e.getMessage(), e);
                                            fileRepository.deleteById(metadataId);
                                            try {
                                                Files.deleteIfExists(finalFilePath);
                                            } catch (IOException ex) {
                                                log.warn("Failed to delete file: {}", finalFilePath, ex);
                                            }
                                            return new FileStorageException("Failed to save file: " + e.getMessage(), e);
                                        });
                            });
                })
                .onErrorMap(IOException.class, e -> new FileStorageException("Failed to upload file: " + e.getMessage(), e));
    }

    public Mono<FileMetadata> getFileMetadata(Long id) {
        return Mono.fromCallable(() -> {
                    FileMetadata metadata = fileRepository.findById(id)
                            .orElseThrow(() -> new ItemNotFoundException("File not found with id: " + id));
                    return metadata;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<DataBuffer> downloadFile(Long id) {
        return Mono.fromCallable(() -> {
                    FileMetadata metadata = fileRepository.findById(id)
                            .orElseThrow(() -> new ItemNotFoundException("File not found with id: " + id));
                    Path filePath = Path.of(metadata.getFilePath());
                    if (!Files.exists(filePath)) {
                        throw new ItemNotFoundException("File not found on disk: " + filePath);
                    }
                    return filePath;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(filePath -> DataBufferUtils.read(filePath, new DefaultDataBufferFactory(), 4096));
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex) : "";
    }
}