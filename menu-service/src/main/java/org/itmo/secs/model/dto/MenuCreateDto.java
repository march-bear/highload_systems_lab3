package org.itmo.secs.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(name = "Объект для создания меню", description = "Содержит поля для создания нового меню")
public record MenuCreateDto(
    @Schema(description = "Прием пищи (DINNER, LUNCH или BREAKFAST)", type = "string", example = "DINNER")
    String meal,
    @Schema(description = "Дата трапезы в формате yyyy-MM-dd", type = "date", example = "2007-01-01")
    @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
) { }
