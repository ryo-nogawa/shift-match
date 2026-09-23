package com.example.shiftmatch.domain;

import java.util.List;

/**
 * 重複する従業員名のエラー情報を表すレコード。
 *
 * <p>同じ氏名を持つ複数の従業員に関する情報を保持します。
 * {@code rowIndexes}には、元の従業員リストにおけるインデックスが格納されます。
 *
 * @param name 重複した従業員名
 * @param rowIndexes 元のリストにおける該当行のインデックス（昇順）
 */
public record DuplicateNameError(String name, List<Integer> rowIndexes) {

  /**
   * {@code rowIndexes} を不変なリストとして保持する。
   *
   * <p>生成後に呼び出し側がリストを変更すると検出時の行情報と食い違うため、
   * 生成経路によらず不変なリストにする。
   */
  public DuplicateNameError {
    rowIndexes = List.copyOf(rowIndexes);
  }
}
