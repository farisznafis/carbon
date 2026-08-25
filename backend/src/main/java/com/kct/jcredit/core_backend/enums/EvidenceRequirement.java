package com.kct.jcredit.core_backend.enums;


public enum EvidenceRequirement {
 
    /** 必須。未提出の場合はモニタリングデータを確定できない。 */
    REQUIRED,
 
    /**
     * 任意。未提出の場合は方法論のデフォルト値で算定する。
     * 提出された場合は実測値を優先する。
     */
    OPTIONAL,
 
    /**
     * 条件付き必須。設備情報等の条件を満たす場合のみ必須となる。
     * 現時点の EN-R-002 では使用していないが、他方法論への拡張を想定して定義する。
     */
    CONDITIONAL_REQUIRED,
 
    /** 対象外。当該設備では該当しないため、入力・提出ともに不要。 */
    NOT_APPLICABLE
}
