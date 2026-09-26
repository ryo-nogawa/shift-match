package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.MonthlyShiftInput;
import com.example.shiftmatch.domain.MonthlyShiftResult;
import com.example.shiftmatch.domain.ShiftAdjustment;
import com.example.shiftmatch.domain.ShiftStorageException;
import com.example.shiftmatch.persistence.MonthlyShiftRepository;
import com.example.shiftmatch.persistence.SavedMonthlyShift;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

@DisplayName("ShiftStorageServiceImpl")
class ShiftStorageServiceImplTest {

  private static final YearMonth MONTH = YearMonth.of(2026, 10);

  private MonthlyShiftRepository repository;
  private ShiftStorageServiceImpl service;

  @BeforeEach
  void setUp() {
    repository = mock(MonthlyShiftRepository.class);
    service = new ShiftStorageServiceImpl(repository);
  }

  private static EmployeeProfile profile(String name) {
    return new EmployeeProfile(
        name,
        EmploymentType.FULL_TIME,
        Map.of(DayOfWeek.MONDAY, new DailyWish(false, LocalTime.of(9, 0), LocalTime.of(17, 0))));
  }

  private static MonthlyShiftInput inputOf(String... names) {
    List<EmployeeProfile> employees =
        java.util.Arrays.stream(names).map(name -> profile(name)).toList();
    return new MonthlyShiftInput(MONTH, employees, List.of());
  }

  @Nested
  @DisplayName("保存")
  class Save {

    @Nested
    class 正常系 {

      @Test
      @DisplayName("[F-7][8.4節] Given: 名前が空の従業員を含む入力, When: 保存すると, Then: 空名を除いた名前を入力順でリポジトリへ渡す")
      void delegatesWithNonBlankNamesInOrder() {
        MonthlyShiftInput input = inputOf("佐藤", "", "鈴木");
        MonthlyShiftResult result = new MonthlyShiftResult(MONTH, List.of());

        service.save(input, result);

        verify(repository).save(input, result, List.of("佐藤", "鈴木"));
      }

      @Test
      @DisplayName(
          "[V-1][F-7][8.4節] Given: 半角・全角の空白だけの名前を含む入力, When: 保存すると, Then: 空白だけの名前も除いてリポジトリへ渡す")
      void excludesWhitespaceOnlyNames() {
        MonthlyShiftInput input = inputOf("佐藤", " ", "　", "鈴木");
        MonthlyShiftResult result = new MonthlyShiftResult(MONTH, List.of());

        service.save(input, result);

        verify(repository).save(input, result, List.of("佐藤", "鈴木"));
      }
    }

    @Nested
    class Exception {

      @Test
      @DisplayName(
          "[F-7][8.4節] Given: リポジトリが DataAccessException を投げるとき, When: 保存すると, Then:"
              + " ShiftStorageException に包んで投げる")
      void wrapsDataAccessException() {
        MonthlyShiftInput input = inputOf("佐藤");
        MonthlyShiftResult result = new MonthlyShiftResult(MONTH, List.of());
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("失敗");
        doThrow(cause).when(repository).save(any(), any(), any());

        ShiftStorageException thrown =
            assertThrows(ShiftStorageException.class, () -> service.save(input, result));

        assertSame(cause, thrown.getCause());
      }
    }
  }

  @Nested
  @DisplayName("復元")
  class Load {

    @Test
    @DisplayName("[F-7][8.3節] Given: 保存済みのシフトがあるとき, When: 月を指定して取得すると, Then: リポジトリの値を返す")
    void returnsSavedShift() {
      SavedMonthlyShift saved =
          new SavedMonthlyShift(new MonthlyShiftResult(MONTH, List.of()), List.of("佐藤"));
      when(repository.findShift(MONTH)).thenReturn(Optional.of(saved));

      assertEquals(Optional.of(saved), service.load(MONTH));
    }

    @Test
    @DisplayName("[F-7][8.3節] Given: 保存がないとき, When: 月を指定して取得すると, Then: 空が返る")
    void returnsEmptyWhenNotSaved() {
      when(repository.findShift(MONTH)).thenReturn(Optional.empty());

      assertTrue(service.load(MONTH).isEmpty());
    }

    @Test
    @DisplayName("[F-7][8.1節] Given: 従業員・個別変更・最後の月が保存済みのとき, When: 入力を取得すると, Then: まとめて返る")
    void returnsSavedInput() {
      List<EmployeeProfile> employees = List.of(profile("佐藤"));
      List<ShiftAdjustment> adjustments =
          List.of(
              new ShiftAdjustment(
                  LocalDate.of(2026, 10, 1),
                  "佐藤",
                  new DailyWish(false, LocalTime.of(9, 0), LocalTime.of(17, 0))));
      when(repository.findEmployees()).thenReturn(employees);
      when(repository.findAdjustments()).thenReturn(adjustments);
      when(repository.findLastTargetMonth()).thenReturn(Optional.of(MONTH));

      SavedInput input = service.loadInput();

      assertEquals(employees, input.employees());
      assertEquals(adjustments, input.adjustments());
      assertEquals(Optional.of(MONTH), input.lastTargetMonth());
    }

    @Test
    @DisplayName("[F-7][8.1節] Given: 何も保存していないとき, When: 入力を取得すると, Then: 空のリストと空の月が返る")
    void returnsEmptyInputWhenNothingSaved() {
      when(repository.findEmployees()).thenReturn(List.of());
      when(repository.findAdjustments()).thenReturn(List.of());
      when(repository.findLastTargetMonth()).thenReturn(Optional.empty());

      SavedInput input = service.loadInput();

      assertTrue(input.employees().isEmpty());
      assertTrue(input.adjustments().isEmpty());
      assertTrue(input.lastTargetMonth().isEmpty());
    }
  }
}
