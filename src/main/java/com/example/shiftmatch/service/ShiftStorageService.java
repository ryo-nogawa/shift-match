package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.MonthlyShiftInput;
import com.example.shiftmatch.domain.MonthlyShiftResult;
import com.example.shiftmatch.persistence.SavedMonthlyShift;
import java.time.YearMonth;
import java.util.Optional;

/**
 * 入力と決定したシフトの保存・復元を行うサービスのインタフェース。
 *
 * <p>保存するのは対象年（暦年）の分だけです。
 */
public interface ShiftStorageService {

  /**
   * 入力と決定したシフトを保存します。
   *
   * @param input 月間シフトの入力
   * @param result 月間シフトの結果
   * @throws com.example.shiftmatch.domain.ShiftStorageException 保存に失敗した場合
   */
  void save(MonthlyShiftInput input, MonthlyShiftResult result);

  /**
   * 指定した月の保存済みシフトを取得します。
   *
   * @param month 対象月
   * @return 保存済みのシフト（保存がなければ空）
   */
  Optional<SavedMonthlyShift> load(YearMonth month);

  /**
   * 保存済みの入力（従業員・個別変更・最後の対象月）を取得します。
   *
   * @return 保存済みの入力
   */
  SavedInput loadInput();
}
