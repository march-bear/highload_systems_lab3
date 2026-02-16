package org.itmo.secs.domain.model.dto;

public record FileDto (
    Long id,
    String fileName,
    Long fileSize,
    String downloadUrl
) { }
