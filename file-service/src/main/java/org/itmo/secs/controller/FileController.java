package org.itmo.secs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.itmo.secs.model.dto.ErrorDto;
import org.itmo.secs.model.dto.FileDto;
import org.itmo.secs.service.FileService;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("file")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;
    private final ConversionService conversionService;

//    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    @Operation(summary = "Загрузить файл", description = "Загружает прикрепленный файл на сервер")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Файл был успешно загружен",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = FileDto.class))
                    }
            ),
            @ApiResponse(responseCode = "400", description = "Файл с таким именем уже есть в базе",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                    }
            )
    })
    @PostMapping( consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<FileDto> uploadFile(@RequestPart("file") FilePart filePart) {
        return fileService.uploadFile(filePart)
                .map(fileMetadata -> Objects.requireNonNull(conversionService.convert(fileMetadata, FileDto.class)));
    }

    @GetMapping
//    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    public Mono<ResponseEntity<FileDto>> getFileMetadata(@Parameter Long id) {
        return fileService.getFileMetadata(id)
                .map(fileMetadata -> Objects.requireNonNull(conversionService.convert(fileMetadata, FileDto.class)))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/download")
//    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFile(@Parameter Long id) {
        return fileService.getFileMetadata(id)
                .flatMap(metadata -> fileService.downloadFile(id)
                        .map(dataBufferFlux -> {
                            HttpHeaders headers = new HttpHeaders();
                            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                            headers.setContentDispositionFormData("attachment", metadata.getFileName());
                            return ResponseEntity.ok()
                                    .headers(headers)
                                    .body(dataBufferFlux);
                        }));
    }
}