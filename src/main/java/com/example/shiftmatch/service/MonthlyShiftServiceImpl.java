package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.FailureReason;
import com.example.shiftmatch.domain.InputError;
import com.example.shiftmatch.domain.InvalidMonthlyInputException;
import com.example.shiftmatch.domain.MonthlyShiftInput;
import com.example.shiftmatch.domain.MonthlyShiftResult;
import com.example.shiftmatch.domain.ShiftAssignment;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 月間シフト作成サービスの実装。
 *
 * <p>営業日ごとに最適なシフト割り当て案を算出します。
 */
@Service
public class MonthlyShiftServiceImpl implements MonthlyShiftService {

  private final HolidayService holidayService;
  private final ShiftAssignmentService assignmentService;
  private final WishResolver wishResolver;
  private final MonthlyInputValidator inputValidator;
  private final SelectionRationaleLogger rationaleLogger;

  /**
   * 月間シフト作成サービスを初期化します。
   *
   * @param holidayService 祝日サービス
   * @param assignmentService 割り当てサービス
   * @param inputValidator 入力検証サービス
   * @param rationaleLogger 選定根拠ログサービス
   */
  public MonthlyShiftServiceImpl(
      HolidayService holidayService,
      ShiftAssignmentService assignmentService,
      MonthlyInputValidator inputValidator,
      SelectionRationaleLogger rationaleLogger) {
    this.holidayService = holidayService;
    this.assignmentService = assignmentService;
    this.wishResolver = new WishResolver();
    this.inputValidator = inputValidator;
    this.rationaleLogger = rationaleLogger;
  }

  @Override
  public MonthlyShiftResult create(MonthlyShiftInput input) {
    // 入力検証
    List<InputError> errors = inputValidator.validate(input);
    if (!errors.isEmpty()) {
      throw new InvalidMonthlyInputException(errors);
    }

    List<DailyShiftResult> results = new ArrayList<>();

    // H-4 の週は月〜金の営業日のうち対象月に含まれる日だけなので、営業日を日付順に処理し、
    // 週（月曜日）が変わるたびにパートの週の実労働時間の合計をリセットする（5.5 節）
    Map<String, Integer> partWeeklyActualWorkMinutes = new HashMap<>();
    // これまでの出勤日数（5.6 節）は対象月単位で 0 から数え、週をまたいでもリセットしない
    Map<String, Integer> priorWorkDaysByName = new HashMap<>();
    LocalDate currentWeekMonday = null;
    for (LocalDate businessDay : holidayService.businessDays(input.month())) {
      LocalDate weekMonday = businessDay.with(DayOfWeek.MONDAY);
      if (!weekMonday.equals(currentWeekMonday)) {
        partWeeklyActualWorkMinutes.clear();
        currentWeekMonday = weekMonday;
      }
      DailyShiftResult dayResult =
          createForDay(businessDay, input, partWeeklyActualWorkMinutes, priorWorkDaysByName);
      results.add(dayResult);
      rationaleLogger.log(businessDay, dayResult);
    }

    return new MonthlyShiftResult(input.month(), results);
  }

  private DailyShiftResult createForDay(
      LocalDate date,
      MonthlyShiftInput input,
      Map<String, Integer> partWeeklyActualWorkMinutes,
      Map<String, Integer> priorWorkDaysByName) {
    // 従業員のうち、名前が空でないものだけを対象
    List<Employee> employees = new ArrayList<>();
    for (EmployeeProfile profile : input.employees()) {
      if (profile.name() != null && !profile.name().isBlank()) {
        DailyWish wish = wishResolver.resolve(profile, date, input.adjustments());
        employees.add(convertToEmployee(profile, wish));
      }
    }

    // 勤務できる人の数（有効な従業員のうち休みでない人）
    int availableCount = (int) employees.stream().filter(e -> !e.off()).count();

    // これまでの出勤日数（5.6 節）を全員に設定してから、パートには週の残り時間（H-4）を設定する（5.5 節 2）
    List<Employee> employeesWithPriorWorkDays = applyPriorWorkDays(employees, priorWorkDaysByName);
    List<Employee> employeesWithWeeklyLimit =
        applyWeeklyRemainingMinutes(employeesWithPriorWorkDays, partWeeklyActualWorkMinutes);
    Optional<AssignmentResult> assignment = assignmentService.assign(employeesWithWeeklyLimit);

    if (assignment.isPresent()) {
      accumulatePartTimeActualWorkMinutes(assignment.get(), partWeeklyActualWorkMinutes);
      accumulatePriorWorkDays(assignment.get(), priorWorkDaysByName);
      return new DailyShiftResult(date, availableCount, assignment);
    }

    // 不成立の理由（6 章）は、H-4 を除いた従業員（週の残り時間なしの元の候補）でもう一度 assign を呼び、
    // 案があればパートの週上限、なければ人員不足と判定する（枠ごとの可能人数を独立に数えると誤判定するため）
    FailureReason failureReason =
        assignmentService.assign(employeesWithPriorWorkDays).isPresent()
            ? FailureReason.WEEKLY_LIMIT
            : FailureReason.STAFF_SHORTAGE;
    return new DailyShiftResult(date, availableCount, Optional.empty(), failureReason);
  }

  private List<Employee> applyPriorWorkDays(
      List<Employee> employees, Map<String, Integer> priorWorkDaysByName) {
    List<Employee> result = new ArrayList<>();
    for (Employee employee : employees) {
      result.add(employee.withPriorWorkDays(priorWorkDaysByName.getOrDefault(employee.name(), 0)));
    }
    return result;
  }

  private void accumulatePriorWorkDays(
      AssignmentResult assignment, Map<String, Integer> priorWorkDaysByName) {
    for (ShiftAssignment shiftAssignment : assignment.assignments()) {
      String name = shiftAssignment.employee().name();
      priorWorkDaysByName.put(name, priorWorkDaysByName.getOrDefault(name, 0) + 1);
    }
  }

  private List<Employee> applyWeeklyRemainingMinutes(
      List<Employee> employees, Map<String, Integer> partWeeklyActualWorkMinutes) {
    List<Employee> result = new ArrayList<>();
    for (Employee employee : employees) {
      if (employee.employmentType() != EmploymentType.PART_TIME) {
        result.add(employee);
        continue;
      }
      int used = partWeeklyActualWorkMinutes.getOrDefault(employee.name(), 0);
      result.add(
          employee.withWeeklyRemainingMinutes(
              EmploymentType.PART_TIME_WEEKLY_LIMIT_MINUTES - used));
    }
    return result;
  }

  private void accumulatePartTimeActualWorkMinutes(
      AssignmentResult assignment, Map<String, Integer> partWeeklyActualWorkMinutes) {
    for (ShiftAssignment shiftAssignment : assignment.assignments()) {
      Employee employee = shiftAssignment.employee();
      if (employee.employmentType() == EmploymentType.PART_TIME) {
        partWeeklyActualWorkMinutes.merge(
            employee.name(), shiftAssignment.slot().actualWorkMinutes(), (a, b) -> a + b);
      }
    }
  }

  private Employee convertToEmployee(EmployeeProfile profile, DailyWish wish) {
    if (wish.off()) {
      return Employee.onLeave(profile.name(), profile.employmentType());
    }
    return Employee.working(profile.name(), profile.employmentType(), wish.start(), wish.end());
  }
}
