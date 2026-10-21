package com.kct.jcredit.core_backend.exception;

import lombok.Getter;
import com.kct.jcredit.core_backend.enums.*;

@Getter
public class FileVersionLimitExceededException extends RuntimeException {
    private final ProjectFileType fileType;
    private final int limit;

    public FileVersionLimitExceededException(ProjectFileType fileType, int limit) {
        super(String.format(
                "%sは最大%d件まで保存できます。新しいファイルをアップロードするには、既存のファイルを削除してください。",
                fileType.name(), limit));
        this.fileType = fileType;
        this.limit = limit;
    }
}
