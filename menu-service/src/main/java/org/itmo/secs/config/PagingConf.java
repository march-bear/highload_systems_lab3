package org.itmo.secs.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class PagingConf {
    private final Integer maxPageSize;
    private final Integer defaultPageSize;

    public PagingConf(
        @Value("${app.max-page-size}") Integer maxPageSize,
        @Value("${app.default-page-size}") Integer defaultPageSize
    ) {
        this.maxPageSize = maxPageSize;
        this.defaultPageSize = defaultPageSize;
    }
}
