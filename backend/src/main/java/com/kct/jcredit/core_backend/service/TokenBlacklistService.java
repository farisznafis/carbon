package com.kct.jcredit.core_backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    // Key: username, Value: password-changed timestamp (millis)
    private final ConcurrentHashMap<String, Long> passwordChangedAtMillis = new ConcurrentHashMap<>();

    public void recordPasswordChange(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        passwordChangedAtMillis.put(username, System.currentTimeMillis());
    }

    public Long getPasswordChangedAt(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return passwordChangedAtMillis.get(username);
    }
    
    // 無効化されたトークンを保存
    // Key: token, Value: 有効期限
    private final Map<String, LocalDateTime> blacklistedTokens = new ConcurrentHashMap<>();
    
    /**
     * トークンをブラックリストに追加
     * @param token 無効化するトークン
     * @param expiresAt トークンの有効期限（この時刻以降は自動削除される）
     */
    public void blacklistToken(String token, LocalDateTime expiresAt) {
        blacklistedTokens.put(token, expiresAt);
        System.out.println("Token blacklisted. Total: " + blacklistedTokens.size());
    }
    
    /**
     * トークンがブラックリストに入っているかチェック
     * @param token チェックするトークン
     * @return true = ブラックリストに入っている（無効）
     */
    public boolean isBlacklisted(String token) {
        return blacklistedTokens.containsKey(token);
    }
    
    /**
     * 期限切れのトークンを定期的にクリーンアップ
     * 1時間ごとに自動実行
     */
    @Scheduled(fixedRate = 3600000) // 3600000ミリ秒 = 1時間
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int beforeSize = blacklistedTokens.size();
        
        // 有効期限が過ぎたトークンを削除
        blacklistedTokens.entrySet().removeIf(entry -> 
            entry.getValue().isBefore(now)
        );
        
        int afterSize = blacklistedTokens.size();
        int removed = beforeSize - afterSize;
        
        if (removed > 0) {
            System.out.println("Cleaned up " + removed + " expired tokens. Remaining: " + afterSize);
        }
    }

    public void blacklistAllTokensForUser(String username) {
        // 実際の実装では、ユーザーに関連するすべてのトークンを取得し、それらをブラックリストに追加する必要があります。
        // ここでは例として、ユーザー名に基づいてトークンを生成し、ブラックリストに追加します。
        
        // 例: ユーザー名に基づくダミートークン
        //get the token for the user from the database
        

        String dummyToken = "token_for_" + username;
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1); // 1時間後に有効期限
        
        blacklistToken(dummyToken, expiresAt);
    }
    
    /**
     * ブラックリストの状態を確認（デバッグ用）
     */
    public int getBlacklistSize() {
        return blacklistedTokens.size();
    }
}