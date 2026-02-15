package org.itmo.secs.model.dto;

public record FileDto (
    Long id,
    String fileName,
    Long fileSize,
    String downloadUrl
) { }
