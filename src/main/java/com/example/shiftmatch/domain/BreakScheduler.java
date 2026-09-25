package com.example.shiftmatch.domain;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 従業員の休憩時刻を割り当てるクラス。
 *
 * <p>枠 1 → 6 の順・同枠内は入力順に展開した 8 名分の枠のリストを受け取り、
 * 各人の休憩の開始・終了時刻を同じ順序で返します。
 */
public class BreakScheduler {

  private static final LocalTime BREAK_START_TIME = LocalTime.of(12, 0);
  private static final int BREAK_INCREMENT_MINUTES = 15;
  private static final int MAX_CONCURRENT_BREAKS = 2;

  /**
   * 与えられた枠のリストに対して、休憩時刻を割り当てます。
   *
   * @param slots 8 人分の枠（枠 1 → 6 の順・同枠内は入力順）
   * @return 各人の休憩時刻（開始・終了）のリスト（8 件）
   */
  public List<BreakInterval> schedule(List<ShiftSlot> slots) {
    List<BreakInterval> breaks = new ArrayList<>();
    List<BreakInterval> assignedBreaks = new ArrayList<>();

    for (ShiftSlot slot : slots) {
      LocalTime breakStart = findEarliestBreakStart(slot, assignedBreaks);
      LocalTime breakEnd = breakStart.plusMinutes(slot.breakDurationMinutes());
      BreakInterval breakInterval = new BreakInterval(breakStart, breakEnd);

      breaks.add(breakInterval);
      assignedBreaks.add(breakInterval);
    }

    return breaks;
  }

  /**
   * 与えられた枠に対して、休憩を開始できる最も早い時刻を返します。
   *
   * @param slot 従業員の枠
   * @param assignedBreaks 既に割り当てられた休憩のリスト
   * @return 休憩開始時刻
   */
  private LocalTime findEarliestBreakStart(ShiftSlot slot, List<BreakInterval> assignedBreaks) {
    LocalTime candidate = BREAK_START_TIME;

    while (true) {
      if (candidate.isBefore(slot.startTime())) {
        candidate = incrementByQuarterHour(candidate);
        continue;
      }

      LocalTime breakEnd = candidate.plusMinutes(slot.breakDurationMinutes());
      if (breakEnd.isAfter(slot.endTime())) {
        return null;
      }

      int concurrentBreaks = 0;
      for (BreakInterval breakInterval : assignedBreaks) {
        if (isOverlapping(
            candidate, breakEnd, breakInterval.startTime(), breakInterval.endTime())) {
          concurrentBreaks++;
        }
      }

      if (concurrentBreaks < MAX_CONCURRENT_BREAKS) {
        return candidate;
      }

      candidate = incrementByQuarterHour(candidate);
    }
  }

  /**
   * 時刻を 15 分進めます。
   *
   * @param time 時刻
   * @return 15 分後の時刻
   */
  private LocalTime incrementByQuarterHour(LocalTime time) {
    return time.plusMinutes(BREAK_INCREMENT_MINUTES);
  }

  /**
   * 2 つの時間帯が重複しているかをチェックします。
   *
   * @param start1 第 1 の時間帯開始時刻
   * @param end1 第 1 の時間帯終了時刻
   * @param start2 第 2 の時間帯開始時刻
   * @param end2 第 2 の時間帯終了時刻
   * @return 重複している場合は true
   */
  private boolean isOverlapping(
      LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
    return start1.isBefore(end2) && start2.isBefore(end1);
  }
}
