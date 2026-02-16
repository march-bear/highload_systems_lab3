package org.itmo.secs.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.itmo.secs.domain.model.dto.ErrorDto;
import org.itmo.secs.domain.model.dto.FileDto;
import org.itmo.secs.application.services.FileService;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@RestController
@RequestMapping("file")
@RequiredArgsConstructor
@Tag(name = "Файлы (File API)")
public class FileController {
    private final FileService fileService;
    private final ConversionService conversionService;

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
    public Mono<FileDto> uploadFile(@Parameter(description = "Файл", required = true) @RequestPart("file") FilePart filePart) {
        return fileService.uploadFile(filePart)
                .map(fileMetadata -> Objects.requireNonNull(conversionService.convert(fileMetadata, FileDto.class)));
    }

    @Operation(summary = "Получить данные о файле", description = "Получить информацию о файле на сервере")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Информация выгружена",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = FileDto.class))
                    }
            ),
            @ApiResponse(responseCode = "404", description = "Файл с таким id не был найден в базе",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                    }
            )
    })
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Mono<FileDto> getFileMetadata(@Parameter(description = "Id файла", example = "1") @RequestParam(required=true) Long id) {
        return fileService.getFileMetadata(id)
                .map(
                        fileMetadata -> Objects.requireNonNull(conversionService.convert(fileMetadata, FileDto.class))
                );
    }

    @Operation(summary = "Скачать файл", description = "Скачать файл с сервера")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Файл скачан",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = FileDto.class))
                    }
            ),
            @ApiResponse(responseCode = "404", description = "Файл с таким id не был найден в базе",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))
                    }
            )
    })
    @GetMapping("/download")
    @ResponseStatus(HttpStatus.OK)
    public Flux<DataBuffer> downloadFile(@Parameter(description = "Id файла", example = "1") @RequestParam(required=true) Long id,
                                         ServerHttpResponse response) {

        return fileService.getFileMetadata(id)
                .flatMapMany(metadata -> {
                    response.getHeaders().set(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + metadata.getFileName() + "\"");
                    response.getHeaders().setContentType(MediaType.APPLICATION_OCTET_STREAM);

                    return fileService.downloadFile(metadata.getId());
                });
    }
}