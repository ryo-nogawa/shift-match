package com.example.shiftmatch.controller;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 開始・終了の時間帯をバリデーションするアノテーション。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidTimeRangeValidator.class)
public @interface ValidTimeRange {

  /**
   * バリデーション失敗時のメッセージです。
   *
   * @return メッセージ
   */
  String message() default "";

  /**
   * グループです。
   *
   * @return グループの配列
   */
  Class<?>[] groups() default {};

  /**
   * ペイロードです。
   *
   * @return ペイロードの配列
   */
  Class<? extends Payload>[] payload() default {};
}
