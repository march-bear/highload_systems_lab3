package org.itmo.secs.api.converters;

import lombok.AllArgsConstructor;
import org.itmo.secs.domain.model.dto.FileDto;
import org.itmo.secs.domain.model.entities.FileMetadata;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FileMetadataToFileDtoConverter implements Converter<FileMetadata, FileDto> {
    @Override
    public FileDto convert(FileMetadata f) {
        return new FileDto(
            f.getId(),
            f.getFileName(),
            f.getFileSize(),
            ("/file/download?id=" + f.getId())
        );
    }
}
