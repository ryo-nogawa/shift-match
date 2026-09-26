package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.MonthlyShiftInput;
import com.example.shiftmatch.domain.MonthlyShiftResult;

/**
 * 月間シフト作成サービスのインタフェース。
 *
 * <p>営業日ごとに最適なシフト割り当て案を算出します。
 */
public interface MonthlyShiftService {

  /**
   * 月間シフト作成の入力から、営業日ごとのシフト割り当て結果を算出します。
   *
   * @param input 入力（対象月、従業員、個別変更）
   * @return 営業日ごとの割り当て結果
   */
  MonthlyShiftResult create(MonthlyShiftInput input);
}
