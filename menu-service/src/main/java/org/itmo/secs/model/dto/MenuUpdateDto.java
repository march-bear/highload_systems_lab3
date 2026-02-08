package org.itmo.secs.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import org.itmo.secs.utils.json.LocalDateSerializer;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(name = "Объект для обновления меню", description = "Содержит основные поля меню и его ID")
public record MenuUpdateDto(
    @Schema(description = "ID меню", type = "number", example = "1")
    Long id,
    @Schema(description = "Дата трапезы в формате yyyy-MM-dd", type = "date", example = "2007-01-01")
    @DateTimeFormat(pattern = "yyyy-MM-dd") 
    @JsonSerialize(using = LocalDateSerializer.class)
    LocalDate date,
    @Schema(description = "Прием пищи (DINNER, LUNCH или BREAKFAST)", type = "string", example = "BREAKFAST")
    String meal
) { }
