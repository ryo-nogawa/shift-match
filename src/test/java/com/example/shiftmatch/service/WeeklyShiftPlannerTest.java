package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.FailureReason;
import com.example.shiftmatch.domain.ShiftAssignment;
import com.example.shiftmatch.domain.ShiftSlot;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("WeeklyShiftPlanner")
class WeeklyShiftPlannerTest {

  private static final long FAIL_WEIGHT = 1_000_000L;
  private static final int LIMIT = EmploymentType.PART_TIME_WEEKLY_LIMIT_MINUTES;

  private final ShiftAssignmentService assignmentService = new ShiftAssignmentServiceImpl();
  private final WeeklyShiftPlanner planner = new WeeklyShiftPlanner(assignmentService);

  private static LocalTime time(int hour, int minute) {
    return LocalTime.of(hour, minute);
  }

  private static Employee part(String name, LocalTime start, LocalTime end) {
    return Employee.working(name, EmploymentType.PART_TIME, start, end);
  }

  private static Employee full(String name, LocalTime start, LocalTime end) {
    return Employee.working(name, EmploymentType.FULL_TIME, start, end);
  }

  private static List<LocalDate> days(int count) {
    List<LocalDate> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(LocalDate.of(2026, 10, 5).plusDays(i));
    }
    return list;
  }

  private List<DailyShiftResult> plan(List<List<Employee>> employeesByDay) {
    List<LocalDate> weekDays = days(employeesByDay.size());
    return planner.plan(
        weekDays,
        date -> employeesByDay.get(weekDays.indexOf(date)),
        date ->
            (int)
                employeesByDay.get(weekDays.indexOf(date)).stream().filter(e -> !e.off()).count());
  }

  private static int partMinutes(List<DailyShiftResult> results, String name) {
    int total = 0;
    for (DailyShiftResult result : results) {
      if (result.assignment().isEmpty()) {
        continue;
      }
      for (ShiftAssignment a : result.assignment().get().assignments()) {
        if (a.employee().name().equals(name)) {
          total += a.slot().netWorkMinutes();
        }
      }
    }
    return total;
  }

  private static List<String> namesOf(DailyShiftResult result) {
    return result.assignment().stream()
        .flatMap(r -> r.assignments().stream())
        .map(a -> a.employee().name())
        .toList();
  }

  private static List<Employee> identicalParts(int count) {
    List<Employee> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(part("P" + i, time(7, 30), time(18, 30)));
    }
    return list;
  }

  @Nested
  class パート以外だけの週 {

    @Test
    @DisplayName("[H-4] Given: パートがいない週, When: plan すると, Then: 各日の従来の assign の結果と同じになる")
    void sameAsPlainAssignWithoutParts() {
      List<Employee> day1 = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        day1.add(full("F" + i, time(7 + (i % 3), (i % 2) * 30), time(16 + (i % 3), 30)));
      }
      List<Employee> day2 = new ArrayList<>(identicalFull(10));
      day2.set(9, Employee.onLeave("F9"));
      List<Employee> day3 = new ArrayList<>(identicalFull(10));
      for (int i = 7; i < 10; i++) {
        day3.set(i, Employee.onLeave("F" + i));
      }

      List<DailyShiftResult> results = plan(List.of(day1, day2, day3));

      assertEquals(3, results.size());
      assertEquals(assignmentService.assign(day1), results.get(0).assignment());
      assertEquals(assignmentService.assign(day2), results.get(1).assignment());
      assertEquals(Optional.empty(), results.get(2).assignment());
      assertEquals(Optional.of(FailureReason.SHORTAGE), results.get(2).failureReason());
    }

    private List<Employee> identicalFull(int count) {
      List<Employee> list = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        list.add(full("F" + i, time(7, 30), time(18, 30)));
      }
      return list;
    }
  }

  @Nested
  class 上限に余裕がある週 {

    @Test
    @DisplayName("[H-4] Given: 上限に余裕がある週, When: plan すると, Then: 各パートの週合計が 1200 分を超えない")
    void weeklyTotalDoesNotExceedLimit() {
      List<Employee> day = identicalParts(10);

      List<DailyShiftResult> results = plan(List.of(day, day, day, day, day));

      for (int i = 0; i < 10; i++) {
        assertTrue(partMinutes(results, "P" + i) <= LIMIT);
      }
    }

    @Test
    @DisplayName("[5.3] Given: 全員同じ希望のパートで 1 日だけの週, When: plan すると, Then: 入力順が早い人が枠 1 に入る")
    void earlierInputWinsTie() {
      List<DailyShiftResult> results = plan(List.of(identicalParts(10)));

      assertEquals(
          List.of("P0", "P1", "P2", "P3", "P4", "P5", "P6", "P7"), namesOf(results.get(0)));
    }

    @Test
    @DisplayName("[H-4] Given: 営業日が 2 日しかない週, When: plan すると, Then: 上限を守った結果になる")
    void shortWeekRespectsLimit() {
      List<Employee> day = identicalParts(9);

      List<DailyShiftResult> results = plan(List.of(day, day));

      assertTrue(results.stream().allMatch(r -> r.assignment().isPresent()));
      for (int i = 0; i < 9; i++) {
        assertTrue(partMinutes(results, "P" + i) <= LIMIT);
      }
      assertEquals(assignmentService.assign(day), results.get(0).assignment());
    }
  }

  @Nested
  class 上限に届く週 {

    @Test
    @DisplayName("[H-4][5.2][6章] Given: 8 名のパートで 3 日, When: plan すると, Then: 不成立は 1 日で最後の日が週上限になる")
    void lastDayFailsByWeeklyLimit() {
      List<Employee> day = identicalParts(8);

      List<DailyShiftResult> results = plan(List.of(day, day, day));

      assertEquals(assignmentService.assign(day), results.get(0).assignment());
      assertEquals(assignmentService.assign(day), results.get(1).assignment());
      assertTrue(results.get(2).assignment().isEmpty());
      assertEquals(Optional.of(FailureReason.WEEKLY_LIMIT), results.get(2).failureReason());
      for (int i = 0; i < 8; i++) {
        assertTrue(partMinutes(results, "P" + i) <= LIMIT);
      }
    }

    @Test
    @DisplayName("[6章] Given: 人員不足の日と上限の日, When: plan すると, Then: 理由がそれぞれ人員不足・パートの週上限になる")
    void reasonsAreDistinguished() {
      List<Employee> day = identicalParts(8);
      List<Employee> shortDay = new ArrayList<>(day.subList(0, 7));
      shortDay.add(Employee.onLeave("P7", EmploymentType.PART_TIME));

      List<DailyShiftResult> results = plan(List.of(day, shortDay, day, day));

      assertEquals(Optional.of(FailureReason.SHORTAGE), results.get(1).failureReason());
      long weekly =
          results.stream()
              .filter(r -> r.failureReason().equals(Optional.of(FailureReason.WEEKLY_LIMIT)))
              .count();
      assertEquals(1, weekly);
      assertEquals(2, results.stream().filter(r -> r.assignment().isEmpty()).count());
    }
  }

  @Nested
  class 総当たりとの一致 {

    private List<Employee> base(LocalTime partEnd) {
      List<Employee> list = new ArrayList<>();
      list.add(full("F0", time(7, 30), time(14, 30)));
      list.add(full("F1", time(7, 30), time(14, 30)));
      list.add(full("F2", time(8, 0), time(15, 30)));
      list.add(full("F3", time(8, 30), time(16, 30)));
      list.add(full("G0", time(9, 0), time(18, 30)));
      for (int i = 0; i < 4; i++) {
        list.add(part("P" + i, time(9, 0), partEnd));
      }
      return list;
    }

    @Test
    @DisplayName("[H-4][5.2][5.3] Given: 9 名（パート 4 名）で 3 日, When: plan すると, Then: 総当たりの結果と一致する")
    void matchesBruteForceCase1() {
      List<List<Employee>> week =
          List.of(base(time(18, 30)), base(time(18, 30)), base(time(18, 30)));

      assertMatchesBruteForce(week);
    }

    @Test
    @DisplayName(
        "[H-4][5.2][5.3] Given: 日ごとに希望が違う 10 名（パート 4 名）で 3 日, When: plan すると, Then: 総当たりの結果と一致する")
    void matchesBruteForceCase2() {
      List<Employee> day1 = base(time(18, 30));
      day1.add(full("G1", time(9, 0), time(18, 0)));
      List<Employee> day2 = base(time(18, 0));
      day2.add(full("G1", time(9, 0), time(18, 30)));
      List<Employee> day3 = base(time(18, 30));
      day3.set(5, Employee.onLeave("P0", EmploymentType.PART_TIME));
      day3.add(full("G1", time(9, 0), time(16, 30)));

      assertMatchesBruteForce(List.of(day1, day2, day3));
    }

    @Test
    @DisplayName("[H-4][5.2][5.3] Given: パート 5 名で 3 日, When: plan すると, Then: 総当たりの結果と一致する")
    void matchesBruteForceCase3() {
      List<Employee> day = base(time(18, 30));
      day.set(4, part("G0", time(9, 0), time(18, 30)));
      day.add(part("P4", time(9, 0), time(18, 30)));

      assertMatchesBruteForce(List.of(day, day, day));
    }

    @Test
    @DisplayName("[H-4][5.2][5.3] Given: 上限に触れない構成で 3 日, When: plan すると, Then: 総当たりの結果と一致する")
    void matchesBruteForceCase4() {
      List<Employee> day = base(time(16, 30));
      day.add(part("P4", time(9, 0), time(18, 30)));
      day.add(part("P5", time(9, 0), time(18, 30)));

      assertMatchesBruteForce(List.of(day, day, day));
    }

    private void assertMatchesBruteForce(List<List<Employee>> week) {
      List<DailyShiftResult> actual = plan(week);
      List<List<String>> expected = bruteForce(week);

      assertEquals(expected.size(), actual.size());
      for (int d = 0; d < expected.size(); d++) {
        List<String> names = actual.get(d).assignment().isPresent() ? namesOf(actual.get(d)) : null;
        assertEquals(expected.get(d), names, "day " + d);
      }
    }

    // 各日の案を 5.3 節の順に列挙し、5.2 節の比較で最初に最小へ到達する組を返す
    private List<List<String>> bruteForce(List<List<Employee>> week) {
      List<List<int[]>> options = new ArrayList<>();
      for (List<Employee> day : week) {
        List<int[]> list = new ArrayList<>();
        enumerate(day, 0, new int[8], 0, new boolean[day.size()], list);
        assertTrue(list.size() <= 3000, "案が多すぎます: " + list.size());
        options.add(list);
      }
      Best best = new Best();
      search(week, options, 0, new int[week.get(0).size()], new int[week.size()][], 0, 0, best);
      List<List<String>> result = new ArrayList<>();
      for (int d = 0; d < week.size(); d++) {
        int[] tuple = best.choice[d];
        if (tuple == null) {
          result.add(null);
        } else {
          List<String> names = new ArrayList<>();
          for (int idx : tuple) {
            names.add(week.get(d).get(idx).name());
          }
          result.add(names);
        }
      }
      return result;
    }

    private static class Best {
      long value = Long.MAX_VALUE;
      int[][] choice;
    }

    private void enumerate(
        List<Employee> day, int pos, int[] tuple, int minIndex, boolean[] used, List<int[]> out) {
      if (pos == 8) {
        out.add(tuple.clone());
        return;
      }
      ShiftSlot slot = slotAt(pos);
      boolean sameSlotAsPrevious = pos > 0 && slotAt(pos - 1) == slot;
      int from = sameSlotAsPrevious ? tuple[pos - 1] + 1 : 0;
      for (int i = from; i < day.size(); i++) {
        if (!used[i] && day.get(i).canWork(slot)) {
          used[i] = true;
          tuple[pos] = i;
          enumerate(day, pos + 1, tuple, minIndex, used, out);
          used[i] = false;
        }
      }
    }

    private static ShiftSlot slotAt(int pos) {
      int count = 0;
      for (ShiftSlot slot : ShiftSlot.values()) {
        count += slot.numberOfEmployees();
        if (pos < count) {
          return slot;
        }
      }
      throw new IllegalArgumentException();
    }

    private void search(
        List<List<Employee>> week,
        List<List<int[]>> options,
        int d,
        int[] used,
        int[][] choice,
        long fails,
        long score,
        Best best) {
      if (d == week.size()) {
        long value = fails * FAIL_WEIGHT + score;
        if (value < best.value) {
          best.value = value;
          best.choice = choice.clone();
        }
        return;
      }
      List<Employee> day = week.get(d);
      for (int[] tuple : options.get(d)) {
        int[] next = used.clone();
        boolean ok = true;
        long dayScore = 0;
        for (int pos = 0; pos < 8 && ok; pos++) {
          Employee e = day.get(tuple[pos]);
          ShiftSlot slot = slotAt(pos);
          dayScore += e.gapMinutes(slot);
          if (e.employmentType().hasWeeklyLimit()) {
            next[tuple[pos]] += slot.netWorkMinutes();
            ok = next[tuple[pos]] <= LIMIT;
          }
        }
        if (ok) {
          choice[d] = tuple;
          search(week, options, d + 1, next, choice, fails, score + dayScore, best);
        }
      }
      choice[d] = null;
      search(week, options, d + 1, used, choice, fails + 1, score, best);
    }
  }
}
