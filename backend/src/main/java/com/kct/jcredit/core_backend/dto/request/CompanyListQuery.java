package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.enums.CompanyStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Defines validated filters accepted by the administrator company list.
 */
@Data
public class CompanyListQuery {

    @Min(value = 1, message = "ページ番号は1以上で指定してください。")
    private int page = 1;

    @Min(value = 1, message = "表示件数は1以上で指定してください。")
    @Max(value = 100, message = "表示件数は100以下で指定してください。")
    private int limit = 20;

    private Set<CompanyStatus> status = new LinkedHashSet<>();

    @Size(max = 100, message = "検索キーワードは100文字以内で入力してください。")
    private String search;

    @Size(max = 10, message = "都道府県は10文字以内で入力してください。")
    private String prefecture;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdTo;

    @Pattern(
            regexp = "^(createdAt|updatedAt|nameJa|companyId)$",
            message = "ソート項目が正しくありません。"
    )
    private String sort = "createdAt";

    @Pattern(regexp = "^(?i)(asc|desc)$", message = "ソート順はascまたはdescで指定してください。")
    private String sortOrder = "desc";

    /**
     * Validates that the requested registration date range is chronological.
     */
    @AssertTrue(message = "開始日は終了日以前の日付を指定してください。")
    public boolean isCreatedDateRangeValid() {
        return createdFrom == null || createdTo == null || !createdFrom.isAfter(createdTo);
    }
}
