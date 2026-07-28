package com.kct.jcredit.core_backend.entity.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaginationDetailResponse {
    private int page;
    private int limit;

    //for SQL
    private long totalItems;
    private int totalPages;

    //for DynamoDB
    private String nextPageToken; 
    private boolean hasMore; 
}

