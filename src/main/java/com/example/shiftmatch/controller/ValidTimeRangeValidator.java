package com.example.shiftmatch.controller;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * EmployeeForm の開始・終了時間帯をバリデーションする実装です。
 */
public class ValidTimeRangeValidator implements ConstraintValidator<ValidTimeRange, EmployeeForm> {

  @Override
  public void initialize(ValidTimeRange annotation) {}

  @Override
  public boolean isValid(EmployeeForm form, ConstraintValidatorContext context) {
    context.disableDefaultConstraintViolation();

    // 氏名が空か空白のみ、または休みの場合は検証しない（V-1）
    if (form.getName() == null || form.getName().isBlank() || form.isOff()) {
      return true;
    }

    boolean valid = true;

    // 開始の検証
    String startError = validateTimeOption(form.getStart(), "開始");
    if (startError != null) {
      context
          .buildConstraintViolationWithTemplate(startError)
          .addPropertyNode("start")
          .addConstraintViolation();
      valid = false;
    }

    // 終了の検証
    String endError = validateTimeOption(form.getEnd(), "終了");
    if (endError != null) {
      context
          .buildConstraintViolationWithTemplate(endError)
          .addPropertyNode("end")
          .addConstraintViolation();
      valid = false;
    }

    // 開始と終了の大小関係をチェック（開始が null または空文字でない場合のみ）
    if (startError == null && endError == null && form.getStart().compareTo(form.getEnd()) >= 0) {
      context
          .buildConstraintViolationWithTemplate("開始は終了より前にしてください")
          .addPropertyNode("end")
          .addConstraintViolation();
      valid = false;
    }

    return valid;
  }

  /**
   * 時刻が選択肢のいずれかであるかをチェックします。
   *
   * @param time 時刻（HH:mm）
   * @param label 項目名（「開始」または「終了」）
   * @return エラーメッセージ。問題がなければ null
   */
  private String validateTimeOption(String time, String label) {
    if (time == null || time.isEmpty()) {
      return label + "が未選択です";
    }
    if (!TimeOptions.VALUES.contains(time)) {
      return label + "は選択肢にありません";
    }
    return null;
  }
}
