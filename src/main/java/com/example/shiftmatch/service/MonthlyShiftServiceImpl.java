package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.InputError;
import com.example.shiftmatch.domain.InvalidMonthlyInputException;
import com.example.shiftmatch.domain.MonthlyShiftInput;
import com.example.shiftmatch.domain.MonthlyShiftResult;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 月間シフト作成サービスの実装。
 *
 * <p>営業日ごとに最適なシフト割り当て案を算出します。
 */
@Service
public class MonthlyShiftServiceImpl implements MonthlyShiftService {

  private final HolidayService holidayService;
  private final WishResolver wishResolver;
  private final MonthlyInputValidator inputValidator;
  private final SelectionRationaleLogger rationaleLogger;
  private final WeekGrouper weekGrouper;
  private final WeeklyShiftPlanner weeklyPlanner;

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
    this(
        holidayService,
        assignmentService,
        inputValidator,
        rationaleLogger,
        new WeekGrouper(),
        new WeeklyShiftPlanner(assignmentService));
  }

  /**
   * 月間シフト作成サービスを初期化します。
   *
   * @param holidayService 祝日サービス
   * @param assignmentService 割り当てサービス
   * @param inputValidator 入力検証サービス
   * @param rationaleLogger 選定根拠ログサービス
   * @param weekGrouper 営業日を週にまとめるクラス
   * @param weeklyPlanner 週全体の最適化を行うクラス
   */
  @Autowired
  public MonthlyShiftServiceImpl(
      HolidayService holidayService,
      ShiftAssignmentService assignmentService,
      MonthlyInputValidator inputValidator,
      SelectionRationaleLogger rationaleLogger,
      WeekGrouper weekGrouper,
      WeeklyShiftPlanner weeklyPlanner) {
    this.holidayService = holidayService;
    this.wishResolver = new WishResolver();
    this.inputValidator = inputValidator;
    this.rationaleLogger = rationaleLogger;
    this.weekGrouper = weekGrouper;
    this.weeklyPlanner = weeklyPlanner;
  }

  @Override
  public MonthlyShiftResult create(MonthlyShiftInput input) {
    // 入力検証
    List<InputError> errors = inputValidator.validate(input);
    if (!errors.isEmpty()) {
      throw new InvalidMonthlyInputException(errors);
    }

    // H-4 だけが日をまたぐため、営業日を週に分け、週ごとに最適化する
    List<LocalDate> businessDays = holidayService.businessDays(input.month());
    Map<LocalDate, List<Employee>> employeesByDate = new HashMap<>();
    for (LocalDate date : businessDays) {
      employeesByDate.put(date, employeesOf(date, input));
    }
    List<DailyShiftResult> results =
        weeklyPlanner.planAll(
            weekGrouper.group(businessDays),
            (date) -> employeesByDate.get(date),
            (date) -> availableCount(employeesByDate.get(date)));
    for (DailyShiftResult dayResult : results) {
      rationaleLogger.log(dayResult.date(), dayResult);
    }

    return new MonthlyShiftResult(input.month(), results);
  }

  private List<Employee> employeesOf(LocalDate date, MonthlyShiftInput input) {
    // 従業員のうち、名前が空でないものだけを対象
    List<Employee> employees = new ArrayList<>();
    for (EmployeeProfile profile : input.employees()) {
      if (profile.name() != null && !profile.name().isBlank()) {
        DailyWish wish = wishResolver.resolve(profile, date, input.adjustments());
        employees.add(convertToEmployee(profile, wish));
      }
    }
    return employees;
  }

  // 勤務できる人の数（有効な従業員のうち休みでない人）
  private int availableCount(List<Employee> employees) {
    return (int) employees.stream().filter(e -> !e.off()).count();
  }

  private Employee convertToEmployee(EmployeeProfile profile, DailyWish wish) {
    if (wish.off()) {
      return Employee.onLeave(profile.name(), profile.employmentType());
    }
    return Employee.working(profile.name(), profile.employmentType(), wish.start(), wish.end());
  }
}
