package com.example.shiftmatch.domain;

import java.util.List;

/**
 * 月間シフト入力の検証エラーを表す実行時例外。
 *
 * <p>複数のエラーを保持します。
 */
public class InvalidMonthlyInputException extends RuntimeException {

  private final List<InputError> errors;

  /**
   * エラーリストを指定して例外を作成します。
   *
   * @param errors 入力チェックのエラーリスト
   */
  public InvalidMonthlyInputException(List<InputError> errors) {
    this.errors = List.copyOf(errors);
  }

  /**
   * エラーリストを返します。
   *
   * @return エラーリスト
   */
  public List<InputError> errors() {
    return errors;
  }

  @Override
  public String getMessage() {
    if (errors.isEmpty()) {
      return "入力エラーがありません";
    }
    return errors.size() + "個の入力エラーがあります";
  }
}
