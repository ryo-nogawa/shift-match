package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.FailureReason;
import com.example.shiftmatch.domain.ShiftSlot;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * 週全体の割り当てを最適化するクラス。
 *
 * <p>パートの週の実働時間の上限（H-4）が日をまたぐため、週ごとに「不成立の日数 → 成立した日のずれの合計」の順（5.2
 * 節）で最良の組み合わせを動的計画法で求めます（5.4 節の週の探索）。同点は 5.3 節の列挙順で最初の案を採用します。
 *
 * <p>状態は「次に決める日」と「パートごとのこれまでの実働分」です。同じ希望で同じ実働分のパートは入れ替えても先の結果が変わらないため、
 * 状態を多重集合として数えて状態数を抑えます。
 */
@Component
public class WeeklyShiftPlanner {

  private static final long FAIL_WEIGHT = 1_000_000L;
  private static final long INF = Long.MAX_VALUE / 4;
  private static final int NP_INF = Integer.MAX_VALUE / 4;
  private static final int NP_UNCOMPUTED = -2;
  private static final int LIMIT = EmploymentType.PART_TIME_WEEKLY_LIMIT_MINUTES;
  private static final ShiftSlot[] SLOTS = ShiftSlot.values();
  private static final int SLOT_COUNT = SLOTS.length;
  private static final int TOTAL = ShiftSlot.totalEmployees();
  private static final int SIG_RADIX = 4096;

  private final ShiftAssignmentService assignmentService;

  /**
   * 週の最適化を初期化します。
   *
   * @param assignmentService 1 日分の割り当てサービス
   */
  public WeeklyShiftPlanner(ShiftAssignmentService assignmentService) {
    this.assignmentService = assignmentService;
  }

  /**
   * 複数の週について、案または不成立を決める。
   *
   * <p>従業員の希望と営業日の並びがまったく同じ週は、同じ結果になるため、最初の 1 回だけ探索して使い回します（9 章の性能のため）。
   *
   * @param weeks 週ごとの営業日（昇順）
   * @param employeesOf 日付から、その日の有効な従業員（入力順）を返す関数
   * @param availableCountOf 日付から、その日に勤務できる人数を返す関数
   * @return 日付順の結果
   */
  public List<DailyShiftResult> planAll(
      List<List<LocalDate>> weeks,
      Function<LocalDate, List<Employee>> employeesOf,
      Function<LocalDate, Integer> availableCountOf) {
    Map<String, List<DailyShiftResult>> planned = new HashMap<>();
    List<DailyShiftResult> all = new ArrayList<>();
    for (List<LocalDate> week : weeks) {
      StringBuilder signature = new StringBuilder();
      for (LocalDate date : week) {
        signature.append(employeesOf.apply(date)).append(';');
      }
      List<DailyShiftResult> cached = planned.get(signature.toString());
      if (cached == null) {
        cached = plan(week, employeesOf, availableCountOf);
        planned.put(signature.toString(), cached);
        all.addAll(cached);
        continue;
      }
      for (int i = 0; i < week.size(); i++) {
        DailyShiftResult source = cached.get(i);
        all.add(
            new DailyShiftResult(
                week.get(i),
                availableCountOf.apply(week.get(i)),
                source.assignment(),
                source.failureReason()));
      }
    }
    return all;
  }

  /**
   * 1 週分の営業日について、案または不成立を決める。
   *
   * <p>各日の従業員リストは、同じ従業員が同じ順序で並んでいることを前提とします。
   *
   * @param weekDays 週の営業日（昇順）
   * @param employeesOf 日付から、その日の有効な従業員（入力順）を返す関数
   * @param availableCountOf 日付から、その日に勤務できる人数を返す関数
   * @return 日付順の結果
   */
  public List<DailyShiftResult> plan(
      List<LocalDate> weekDays,
      Function<LocalDate, List<Employee>> employeesOf,
      Function<LocalDate, Integer> availableCountOf) {
    List<List<Employee>> emps = new ArrayList<>();
    for (LocalDate date : weekDays) {
      emps.add(employeesOf.apply(date));
    }
    Week week = new Week(emps);
    List<DailyShiftResult> results = new ArrayList<>();
    if (week.partCount == 0) {
      // 上限が効かない週は、各日を独立に解いた結果が週の最良案と一致する
      for (int d = 0; d < weekDays.size(); d++) {
        Optional<com.example.shiftmatch.domain.AssignmentResult> assignment =
            assignmentService.assign(emps.get(d));
        results.add(
            new DailyShiftResult(
                weekDays.get(d),
                availableCountOf.apply(weekDays.get(d)),
                assignment,
                assignment.isPresent() ? Optional.empty() : Optional.of(FailureReason.SHORTAGE)));
      }
      return results;
    }
    int[] used = new int[week.partCount];
    for (int d = 0; d < weekDays.size(); d++) {
      LocalDate date = weekDays.get(d);
      int available = availableCountOf.apply(date);
      long total = value(week, d, used);
      int[] tuple = null;
      if (bestSuccess(week, d, used, null) == total) {
        tuple = chooseTuple(week, d, used, total);
      }
      if (tuple == null) {
        boolean anyCase = assignmentService.assign(emps.get(d)).isPresent();
        FailureReason reason = anyCase ? FailureReason.WEEKLY_LIMIT : FailureReason.SHORTAGE;
        results.add(new DailyShiftResult(date, available, Optional.empty(), Optional.of(reason)));
        continue;
      }
      List<Employee> assignees = new ArrayList<>();
      int slotPos = 0;
      for (int pos = 0; pos < TOTAL; pos++) {
        while (pos >= slotEnd(slotPos)) {
          slotPos++;
        }
        assignees.add(emps.get(d).get(tuple[pos]));
        int q = week.partIndexOf[tuple[pos]];
        if (q >= 0) {
          used[q] += SLOTS[slotPos].netWorkMinutes();
        }
      }
      results.add(
          new DailyShiftResult(
              date,
              available,
              Optional.of(assignmentService.buildAssignment(emps.get(d), assignees)),
              Optional.empty()));
    }
    return results;
  }

  private static int slotEnd(int slotIndex) {
    int end = 0;
    for (int i = 0; i <= slotIndex; i++) {
      end += SLOTS[i].numberOfEmployees();
    }
    return end;
  }

  // ---- 探索（5.4 節の週の探索） ----

  private long value(Week w, int d, int[] rawUsed) {
    if (d == w.days) {
      return 0;
    }
    int[] used = normalizeAll(w, d, rawUsed);
    String key = stateKey(w, d, used);
    Long cached = w.memo.get(d).get(key);
    if (cached != null) {
      return cached;
    }
    long best = FAIL_WEIGHT + value(w, d + 1, used);
    long success = bestSuccess(w, d, used, null);
    if (success < best) {
      best = success;
    }
    w.memo.get(d).put(key, best);
    return best;
  }

  /** 成立する案のうち最小の値を返す。{@code collector} があれば、値が {@code target} に等しい案をすべて渡す。 */
  private long bestSuccess(Week w, int d, int[] rawUsed, LeafCollector collector) {
    int[] used = normalizeAll(w, d, rawUsed);
    ClassSet cs = classesOf(w, d, used);
    long[] best = {INF};
    int[][] take = new int[cs.count][SLOT_COUNT];
    int[] avail = new int[cs.count];
    for (int c = 0; c < cs.count; c++) {
      avail[c] = cs.members.get(c).length;
    }
    enumerateSlot(
        w,
        d,
        cs,
        0,
        take,
        avail,
        new int[SLOT_COUNT],
        0,
        (residual) -> {
          long v = leafValue(w, d, used, cs, take, residual);
          if (v < best[0]) {
            best[0] = v;
          }
          if (collector != null) {
            collector.collect(v, cs, take, residual);
          }
        });
    return best[0];
  }

  private interface LeafVisitor {
    void visit(int[] residual);
  }

  private interface LeafCollector {
    void collect(long value, ClassSet cs, int[][] take, int[] residual);
  }

  private void enumerateSlot(
      Week w,
      int d,
      ClassSet cs,
      int slot,
      int[][] take,
      int[] avail,
      int[] residual,
      int residualSum,
      LeafVisitor visitor) {
    if (slot == SLOT_COUNT) {
      visitor.visit(residual);
      return;
    }
    enumerateClass(
        w,
        d,
        cs,
        slot,
        0,
        SLOTS[slot].numberOfEmployees(),
        take,
        avail,
        residual,
        residualSum,
        visitor);
  }

  private void enumerateClass(
      Week w,
      int d,
      ClassSet cs,
      int slot,
      int ci,
      int left,
      int[][] take,
      int[] avail,
      int[] residual,
      int residualSum,
      LeafVisitor visitor) {
    if (ci == cs.count) {
      // 枠に入るパートの残りを非パートで埋めるため、非パートの人数を超える不足は成立しない
      if (residualSum + left > w.nonPartCount[d]) {
        return;
      }
      residual[slot] = left;
      enumerateSlot(w, d, cs, slot + 1, take, avail, residual, residualSum + left, visitor);
      return;
    }
    int max = cs.allowed[ci][slot] ? Math.min(avail[ci], left) : 0;
    for (int x = 0; x <= max; x++) {
      take[ci][slot] = x;
      avail[ci] -= x;
      enumerateClass(w, d, cs, slot, ci + 1, left - x, take, avail, residual, residualSum, visitor);
      avail[ci] += x;
    }
    take[ci][slot] = 0;
  }

  private long leafValue(Week w, int d, int[] used, ClassSet cs, int[][] take, int[] residual) {
    int nonPart = nonPartCost(w, d, residual);
    if (nonPart >= NP_INF) {
      return INF;
    }
    long score = nonPart;
    for (int c = 0; c < cs.count; c++) {
      for (int s = 0; s < SLOT_COUNT; s++) {
        score += (long) take[c][s] * cs.gap[c][s];
      }
    }
    int[] next = used.clone();
    for (int c = 0; c < cs.count; c++) {
      int pointer = 0;
      for (int s = 0; s < SLOT_COUNT; s++) {
        for (int x = 0; x < take[c][s]; x++) {
          next[cs.members.get(c)[pointer++]] += SLOTS[s].netWorkMinutes();
        }
      }
    }
    return score + value(w, d + 1, next);
  }

  // ---- 非パートの最小ずれ ----

  private int nonPartCost(Week w, int d, int[] residual) {
    int code = 0;
    for (int s = SLOT_COUNT - 1; s >= 0; s--) {
      code = code * 3 + residual[s];
    }
    int[][] table = w.nonPartTables.get(d).get(code);
    if (table == null) {
      table = new int[SLOT_COUNT + 1][1 << w.nonPartCount[d]];
      for (int[] row : table) {
        Arrays.fill(row, NP_UNCOMPUTED);
      }
      w.nonPartTables.get(d).put(code, table);
    }
    return nonPartSolve(w, d, residual, table, 0, 0);
  }

  private int nonPartSolve(Week w, int d, int[] residual, int[][] table, int slot, int mask) {
    if (slot == SLOT_COUNT) {
      return 0;
    }
    if (table[slot][mask] != NP_UNCOMPUTED) {
      return table[slot][mask];
    }
    int need = residual[slot];
    int best = NP_INF;
    if (need == 0) {
      best = nonPartSolve(w, d, residual, table, slot + 1, mask);
    } else {
      int[] idx = w.nonPartIndexes[d];
      for (int i = 0; i < idx.length; i++) {
        if ((mask & (1 << i)) != 0 || !w.can[d][idx[i]][slot]) {
          continue;
        }
        if (need == 1) {
          best =
              Math.min(
                  best,
                  add(
                      w.gap[d][idx[i]][slot],
                      nonPartSolve(w, d, residual, table, slot + 1, mask | (1 << i))));
          continue;
        }
        for (int j = i + 1; j < idx.length; j++) {
          if ((mask & (1 << j)) != 0 || !w.can[d][idx[j]][slot]) {
            continue;
          }
          int rest = nonPartSolve(w, d, residual, table, slot + 1, mask | (1 << i) | (1 << j));
          best = Math.min(best, add(w.gap[d][idx[i]][slot] + w.gap[d][idx[j]][slot], rest));
        }
      }
    }
    table[slot][mask] = best;
    return best;
  }

  private static int add(int gap, int rest) {
    return rest >= NP_INF ? NP_INF : gap + rest;
  }

  // ---- 案の復元（5.3 節の列挙順で最初の案） ----

  /** 最適な成立案のうち、5.3 節の列挙順で最初の案（8 名の従業員インデックス）を返す。 */
  private int[] chooseTuple(Week w, int d, int[] used, long target) {
    int[][] best = {null};
    bestSuccess(
        w,
        d,
        used,
        (value, cs, take, residual) -> {
          if (value != target) {
            return;
          }
          int[] tuple = buildTuple(w, d, cs, take, residual);
          if (best[0] == null || Arrays.compare(tuple, best[0]) < 0) {
            best[0] = tuple;
          }
        });
    return best[0];
  }

  private int[] buildTuple(Week w, int d, ClassSet cs, int[][] take, int[] residual) {
    // 同じ区分のパートは、入力順が早い人ほど早い枠に入れた案が列挙順で先になる
    List<List<Integer>> partsInSlot = new ArrayList<>();
    for (int s = 0; s < SLOT_COUNT; s++) {
      partsInSlot.add(new ArrayList<>());
    }
    for (int c = 0; c < cs.count; c++) {
      int pointer = 0;
      for (int s = 0; s < SLOT_COUNT; s++) {
        for (int x = 0; x < take[c][s]; x++) {
          partsInSlot.get(s).add(w.partEmployee[cs.members.get(c)[pointer++]]);
        }
      }
    }
    nonPartCost(w, d, residual);
    int code = 0;
    for (int s = SLOT_COUNT - 1; s >= 0; s--) {
      code = code * 3 + residual[s];
    }
    int[][] table = w.nonPartTables.get(d).get(code);
    int[] idx = w.nonPartIndexes[d];
    int[] tuple = new int[TOTAL];
    int pos = 0;
    int mask = 0;
    for (int s = 0; s < SLOT_COUNT; s++) {
      List<Integer> members = new ArrayList<>(partsInSlot.get(s));
      int target = nonPartSolve(w, d, residual, table, s, mask);
      int need = residual[s];
      int chosenMask = mask;
      if (need == 1) {
        for (int i = 0; i < idx.length; i++) {
          if ((mask & (1 << i)) != 0 || !w.can[d][idx[i]][s]) {
            continue;
          }
          int rest = nonPartSolve(w, d, residual, table, s + 1, mask | (1 << i));
          if (add(w.gap[d][idx[i]][s], rest) == target) {
            members.add(idx[i]);
            chosenMask = mask | (1 << i);
            break;
          }
        }
      } else if (need == 2) {
        boolean found = false;
        for (int i = 0; i < idx.length && !found; i++) {
          if ((mask & (1 << i)) != 0 || !w.can[d][idx[i]][s]) {
            continue;
          }
          for (int j = i + 1; j < idx.length && !found; j++) {
            if ((mask & (1 << j)) != 0 || !w.can[d][idx[j]][s]) {
              continue;
            }
            int rest = nonPartSolve(w, d, residual, table, s + 1, mask | (1 << i) | (1 << j));
            int gaps = w.gap[d][idx[i]][s] + w.gap[d][idx[j]][s];
            if (add(gaps, rest) == target) {
              members.add(idx[i]);
              members.add(idx[j]);
              chosenMask = mask | (1 << i) | (1 << j);
              found = true;
            }
          }
        }
      }
      mask = chosenMask;
      members.sort(null);
      for (int member : members) {
        tuple[pos++] = member;
      }
    }
    return tuple;
  }

  // ---- 状態のまとめ方 ----

  /**
   * 残り日数で実現できる実働の合計に丸めた、実働分を返す。
   *
   * <p>残りの日数で足せる枠の組み合わせの合計が同じなら、残りが違っても以後の結果は変わらない。
   */
  private static int[] normalizeAll(Week w, int d, int[] used) {
    int[] sums = w.futureSums[w.days - d];
    int[] result = new int[used.length];
    for (int q = 0; q < used.length; q++) {
      if (!w.alive[d][q]) {
        continue;
      }
      int remaining = LIMIT - used[q];
      int index = Arrays.binarySearch(sums, remaining);
      int rounded = index >= 0 ? sums[index] : sums[-index - 2];
      result[q] = LIMIT - rounded;
    }
    return result;
  }

  private static String stateKey(Week w, int d, int[] used) {
    int[] composite = new int[w.partCount];
    for (int q = 0; q < w.partCount; q++) {
      composite[q] = w.sig[d][q] * SIG_RADIX + used[q];
    }
    Arrays.sort(composite);
    StringBuilder sb = new StringBuilder(composite.length * 2);
    for (int v : composite) {
      sb.append((char) (v >>> 16)).append((char) v);
    }
    return sb.toString();
  }

  private static ClassSet classesOf(Week w, int d, int[] used) {
    Map<Integer, List<Integer>> groups = new LinkedHashMap<>();
    for (int q = 0; q < w.partCount; q++) {
      if (!w.candidate[d][w.partEmployee[q]]) {
        continue;
      }
      int key = w.sig[d][q] * SIG_RADIX + used[q];
      groups.computeIfAbsent(key, (k) -> new ArrayList<>()).add(q);
    }
    ClassSet cs = new ClassSet(groups.size());
    int c = 0;
    for (List<Integer> group : groups.values()) {
      int[] members = group.stream().mapToInt((v) -> v).toArray();
      cs.members.add(members);
      int employee = w.partEmployee[members[0]];
      for (int s = 0; s < SLOT_COUNT; s++) {
        cs.allowed[c][s] =
            w.can[d][employee][s] && SLOTS[s].netWorkMinutes() <= LIMIT - used[members[0]];
        cs.gap[c][s] = w.can[d][employee][s] ? w.gap[d][employee][s] : 0;
      }
      c++;
    }
    return cs;
  }

  /** 同じ日の希望・同じ実働分で、入れ替えても先の結果が変わらないパートの集まり。 */
  private static final class ClassSet {
    final int count;
    final List<int[]> members = new ArrayList<>();
    final boolean[][] allowed;
    final int[][] gap;

    ClassSet(int count) {
      this.count = count;
      this.allowed = new boolean[count][SLOT_COUNT];
      this.gap = new int[count][SLOT_COUNT];
    }
  }

  /** 1 週分の探索に使う入力と、メモ化テーブル。 */
  private static final class Week {
    final int days;
    final int partCount;
    final int[][] futureSums;
    final int[] partEmployee;
    final int[] partIndexOf;
    final boolean[][] candidate;
    final boolean[][][] can;
    final int[][][] gap;
    final int[][] nonPartIndexes;
    final int[] nonPartCount;
    final int[][] sig;
    final boolean[][] alive;
    final List<Map<String, Long>> memo = new ArrayList<>();
    final List<Map<Integer, int[][]>> nonPartTables = new ArrayList<>();

    Week(List<List<Employee>> emps) {
      days = emps.size();
      int max = 0;
      for (ShiftSlot slot : SLOTS) {
        max = Math.max(max, slot.netWorkMinutes());
      }
      futureSums = computeFutureSums(days);
      int n = days == 0 ? 0 : emps.get(0).size();
      candidate = new boolean[days][n];
      can = new boolean[days][n][SLOT_COUNT];
      gap = new int[days][n][SLOT_COUNT];
      boolean[] workedSomeDay = new boolean[n];
      for (int d = 0; d < days; d++) {
        for (int i = 0; i < n; i++) {
          Employee e = emps.get(d).get(i);
          candidate[d][i] = !e.off() && e.start() != null && e.end() != null;
          for (int s = 0; s < SLOT_COUNT; s++) {
            can[d][i][s] = candidate[d][i] && e.canWork(SLOTS[s]);
            if (can[d][i][s]) {
              gap[d][i][s] = e.gapMinutes(SLOTS[s]);
              workedSomeDay[i] = true;
            }
          }
        }
      }
      List<Integer> parts = new ArrayList<>();
      partIndexOf = new int[n];
      Arrays.fill(partIndexOf, -1);
      for (int i = 0; i < n; i++) {
        boolean isPart = days > 0 && emps.get(0).get(i).employmentType().hasWeeklyLimit();
        // 上限が 1 日の最大実働の合計を超える週では、上限は効かない
        if (isPart && workedSomeDay[i] && (long) days * max > LIMIT) {
          partIndexOf[i] = parts.size();
          parts.add(i);
        }
      }
      partCount = parts.size();
      partEmployee = parts.stream().mapToInt((v) -> v).toArray();
      nonPartIndexes = new int[days][];
      nonPartCount = new int[days];
      for (int d = 0; d < days; d++) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
          boolean workable = false;
          for (int s = 0; s < SLOT_COUNT; s++) {
            workable |= can[d][i][s];
          }
          if (partIndexOf[i] < 0 && workable) {
            list.add(i);
          }
        }
        nonPartIndexes[d] = list.stream().mapToInt((v) -> v).toArray();
        nonPartCount[d] = list.size();
        memo.add(new HashMap<>());
        nonPartTables.add(new HashMap<>());
      }
      sig = new int[days + 1][partCount];
      alive = new boolean[days + 1][partCount];
      Map<String, Integer> interned = new HashMap<>();
      String[] tail = new String[partCount];
      Arrays.fill(tail, "");
      for (int d = days - 1; d >= 0; d--) {
        for (int q = 0; q < partCount; q++) {
          Employee e = emps.get(d).get(partEmployee[q]);
          String wish = candidate[d][partEmployee[q]] ? e.start() + "-" + e.end() : "O";
          tail[q] = wish + ";" + tail[q];
          sig[d][q] = interned.computeIfAbsent(tail[q], (k) -> interned.size());
          boolean today = false;
          for (int s = 0; s < SLOT_COUNT; s++) {
            today |= can[d][partEmployee[q]][s];
          }
          alive[d][q] = today || alive[d + 1][q];
        }
      }
    }

    /** 残り k 日で足せる実働の合計（上限以内）を、k ごとに昇順で返す。 */
    private static int[][] computeFutureSums(int days) {
      int[][] futureSums = new int[days + 1][];
      boolean[] reachable = new boolean[LIMIT + 1];
      reachable[0] = true;
      for (int k = 0; k <= days; k++) {
        int count = 0;
        for (boolean r : reachable) {
          count += r ? 1 : 0;
        }
        futureSums[k] = new int[count];
        int at = 0;
        for (int v = 0; v <= LIMIT; v++) {
          if (reachable[v]) {
            futureSums[k][at++] = v;
          }
        }
        boolean[] next = reachable.clone();
        for (int v = 0; v <= LIMIT; v++) {
          if (reachable[v]) {
            for (ShiftSlot slot : SLOTS) {
              if (v + slot.netWorkMinutes() <= LIMIT) {
                next[v + slot.netWorkMinutes()] = true;
              }
            }
          }
        }
        reachable = next;
      }
      return futureSums;
    }
  }
}
