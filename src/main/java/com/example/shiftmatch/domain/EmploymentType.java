package com.example.shiftmatch.domain;

import java.util.Optional;

/**
 * 従業員の雇用区分を表す列挙型。
 *
 * <p>常勤（{@code FULL_TIME}）・パート（{@code PART_TIME}）・管理職（{@code MANAGER}）
 * の3区分を定義します。割り当てロジックには影響しません。
 */
public enum EmploymentType {
  /** 常勤。 */
  FULL_TIME("常勤"),
  /** パート。 */
  PART_TIME("パート"),
  /** 管理職。 */
  MANAGER("管理職");

  private final String label;

  EmploymentType(String label) {
    this.label = label;
  }

  /**
   * この雇用区分の表示名を返します。
   *
   * @return 表示名（常勤・パート・管理職）
   */
  public String label() {
    return label;
  }

  /**
   * 文字列から雇用区分を解析します。
   *
   * <p>文字列が {@code "FULL_TIME"}・{@code "PART_TIME"}・{@code "MANAGER"}
   * のいずれかに一致した場合に対応する列挙値を返します。
   * {@code null}・空文字列・上記以外の値の場合は {@code Optional.empty()} を返します。
   *
   * @param value 解析対象の文字列
   * @return 解析結果。{@code Optional}で値を保持、一致しなかった場合は空
   */
  public static Optional<EmploymentType> parse(String value) {
    if (value == null || value.isEmpty()) {
      return Optional.empty();
    }
    try {
      return Optional.of(EmploymentType.valueOf(value));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
