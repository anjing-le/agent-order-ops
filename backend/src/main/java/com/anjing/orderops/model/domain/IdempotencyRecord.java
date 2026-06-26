package com.anjing.orderops.model.domain;

import com.anjing.orderops.model.vo.ExecutionResultView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {

    private String idempotencyKey;

    private String fingerprint;

    private ExecutionResultView result;

    private String createdAt;

    private String updatedAt;
}
