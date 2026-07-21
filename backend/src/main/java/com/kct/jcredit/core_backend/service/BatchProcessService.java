package com.kct.jcredit.core_backend.service;



import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * バッチ計算処理サービス
 * 
 * Flow:
 * 1. 現在時刻取得
 * 2. 処理履歴テーブルの読み込み
 * 3. NoSQLデータ読み込み
 * 4. 処理対象ログの作成 (CSV)
 * 5. 計算処理
 * 6. 処理後ログの作成 (CSV)
 * 7. ログ比較
 * 8. 各プロジェクトの削減実績テーブルの更新
 * 9. 処理履歴テーブルの更新
 * 10. 次のプロジェクトへ (loop)
 */
@Slf4j
@Service
public class BatchProcessService {

    
}
