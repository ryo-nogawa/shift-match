/**
 * 画面 2 の日ごとの希望（営業日カレンダーと個別変更。F-11、8.2 節）。
 *
 * 個別変更は「日付|従業員名」をキーにした Map で持つ。従業員の追加・削除・並べ替え・氏名の変更や
 * 対象月の変更では Map を消さない（8.2 節）。送信は #adjustment-inputs の hidden 入力だけで行う。
 */
(function () {
  "use strict";

  const WEEKDAY_LABELS = ["月", "火", "水", "木", "金"];
  const DEFAULT_START = "07:30";
  const DEFAULT_END = "18:30";

  function adjustmentKey(date, employeeName) {
    return date + "|" + employeeName;
  }

  /** キーを日付と従業員名に分ける。氏名に "|" が含まれても壊れないよう、最初の "|" で分ける。 */
  function splitKey(key) {
    const at = key.indexOf("|");
    return { date: key.slice(0, at), employeeName: key.slice(at + 1) };
  }

  /** YYYY-MM-DD の曜日を 0＝月〜6＝日で返す（タイムゾーンの影響を受けないよう UTC で計算する）。 */
  function weekdayIndex(date) {
    const parts = date.split("-");
    const day = new Date(Date.UTC(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2])));
    return (day.getUTCDay() + 6) % 7;
  }

  /** 対象月の月〜金をすべて日付順に返す（祝日も含む）。 */
  function listWeekdays(yearMonth) {
    const parts = yearMonth.split("-");
    const year = Number(parts[0]);
    const month = Number(parts[1]);
    const length = new Date(Date.UTC(year, month, 0)).getUTCDate();
    const dates = [];
    for (let day = 1; day <= length; day++) {
      const date = yearMonth + "-" + String(day).padStart(2, "0");
      if (weekdayIndex(date) < 5) {
        dates.push(date);
      }
    }
    return dates;
  }

  /**
   * カレンダーの週（月〜金の 5 列）を組み立てる（8.2 節）。祝日も列に置くので、曜日の列はずれない。
   * 各セルは null（対象月の外）か {date, day, kind: "business"|"holiday"|"closed", holidayName}。
   */
  function buildCalendarWeeks(yearMonth, businessDays, holidays) {
    const businessSet = new Set(businessDays);
    const holidayNames = new Map();
    holidays.forEach(function (holiday) {
      holidayNames.set(holiday.date, holiday.name);
    });
    const weeks = [];
    let week = null;
    listWeekdays(yearMonth).forEach(function (date) {
      const column = weekdayIndex(date);
      if (week === null || column === 0) {
        week = [null, null, null, null, null];
        weeks.push(week);
      }
      let kind = "closed";
      if (businessSet.has(date)) {
        kind = "business";
      } else if (holidayNames.has(date)) {
        kind = "holiday";
      }
      week[column] = {
        date: date,
        day: Number(date.slice(8)),
        kind: kind,
        holidayName: holidayNames.get(date) || "",
      };
    });
    return weeks;
  }

  /** 休みどうしは時刻を比べない（休みの時刻は送信されず、サーバーも見ないため）。 */
  function isSameWish(a, b) {
    if (a.off || b.off) {
      return a.off === b.off;
    }
    return a.start === b.start && a.end === b.end;
  }

  /** 選択日の入力を Map に反映する。基本シフトと同じ内容なら個別変更を消す（8.2 節）。 */
  function applyEdit(map, date, employeeName, wish, base) {
    const key = adjustmentKey(date, employeeName);
    if (isSameWish(wish, base)) {
      map.delete(key);
    } else {
      map.set(key, { off: wish.off, start: wish.start, end: wish.end });
    }
  }

  /** 「この日を基本に戻す」：その日の個別変更をすべて消す。 */
  function resetDay(map, date) {
    Array.from(map.keys()).forEach(function (key) {
      if (splitKey(key).date === date) {
        map.delete(key);
      }
    });
  }

  /** その日の個別変更のうち、現在の有効な従業員に当たる人数。 */
  function countChanges(map, date, employeeNames) {
    return employeeNames.filter(function (name) {
      return map.has(adjustmentKey(date, name));
    }).length;
  }

  /**
   * 送信用の一覧を作る。V-9 で他の月の日付がエラーにならないよう、対象月の分だけを返す
   * （他の月の分は Map に残す）。並びは日付順、同じ日は Map への登録順。
   */
  function buildAdjustmentList(map, targetMonth) {
    const list = [];
    map.forEach(function (value, key) {
      const parts = splitKey(key);
      if (parts.employeeName !== "" && parts.date.startsWith(targetMonth + "-")) {
        list.push({
          date: parts.date,
          employeeName: parts.employeeName,
          off: value.off,
          start: value.start,
          end: value.end,
        });
      }
    });
    list.sort(function (a, b) {
      if (a.date === b.date) {
        return 0;
      }
      return a.date < b.date ? -1 : 1;
    });
    return list;
  }

  /** 一覧を hidden 入力の name と value の組にする。インデックスは 0 から連番（8.5 節）。 */
  function toHiddenFields(list) {
    const fields = [];
    list.forEach(function (item, k) {
      const prefix = "adjustments[" + k + "].";
      fields.push({ name: prefix + "date", value: item.date });
      fields.push({ name: prefix + "employeeName", value: item.employeeName });
      if (item.off) {
        fields.push({ name: prefix + "off", value: "true" });
      } else {
        fields.push({ name: prefix + "start", value: item.start });
        fields.push({ name: prefix + "end", value: item.end });
      }
    });
    return fields;
  }

  /** サーバーから戻された hidden 入力（name と value の組）を Map に取り込む。 */
  function parseHiddenFields(fields) {
    const byIndex = new Map();
    fields.forEach(function (field) {
      const match = /^adjustments\[(\d+)\]\.(date|employeeName|off|start|end)$/.exec(field.name);
      if (!match) {
        return;
      }
      const index = Number(match[1]);
      if (!byIndex.has(index)) {
        byIndex.set(index, { date: "", employeeName: "", off: false, start: "", end: "" });
      }
      const entry = byIndex.get(index);
      if (match[2] === "off") {
        entry.off = field.value === "true";
      } else {
        entry[match[2]] = field.value;
      }
    });
    const map = new Map();
    Array.from(byIndex.keys())
      .sort(function (a, b) {
        return a - b;
      })
      .forEach(function (index) {
        const entry = byIndex.get(index);
        if (entry.date !== "" && entry.employeeName !== "") {
          map.set(adjustmentKey(entry.date, entry.employeeName), {
            off: entry.off,
            start: entry.off ? "" : entry.start,
            end: entry.off ? "" : entry.end,
          });
        }
      });
    return map;
  }

  /** 選択日の移動先。今の選択日が新しい月の営業日になければ先頭の営業日（なければ null）。 */
  function chooseSelectedDate(current, businessDays) {
    if (current !== null && businessDays.indexOf(current) >= 0) {
      return current;
    }
    return businessDays.length > 0 ? businessDays[0] : null;
  }

  if (typeof module !== "undefined" && module.exports) {
    module.exports = {
      adjustmentKey,
      splitKey,
      weekdayIndex,
      listWeekdays,
      buildCalendarWeeks,
      isSameWish,
      applyEdit,
      resetDay,
      countChanges,
      buildAdjustmentList,
      toHiddenFields,
      parseHiddenFields,
      chooseSelectedDate,
    };
  }

  if (typeof document === "undefined") {
    return;
  }

  document.addEventListener("DOMContentLoaded", function () {
    const form = document.querySelector("form");
    const monthInput = form.querySelector("input[name='targetMonth']");
    const calendar = document.getElementById("calendar");
    const dayPanel = document.getElementById("day-panel");
    const hiddenContainer = document.getElementById("adjustment-inputs");
    const rowsContainer = document.getElementById("employee-rows");
    const panelsContainer = document.getElementById("base-panels");
    const timeOptions = (form.getAttribute("data-time-options") || "")
      .split("|")
      .filter(function (value) {
        return value !== "";
      });

    const adjustments = parseHiddenFields(
      Array.from(hiddenContainer.querySelectorAll("input[name]")).map(function (input) {
        return { name: input.name, value: input.value };
      })
    );
    let calendarData = null;
    let selectedDate = null;
    // 画面に出している従業員（描画した時点の並び）。入力行の data-employee-index はこの添字
    let shownEmployees = [];

    /** 氏名が空白だけでない行（有効な従業員。V-1）を、並び順に基本シフト付きで読む。 */
    function readEmployees() {
      const panels = new Map();
      panelsContainer.querySelectorAll(".base-panel").forEach(function (panel) {
        panels.set(panel.getAttribute("data-row-id"), panel);
      });
      const employees = [];
      rowsContainer.querySelectorAll(".employee-row").forEach(function (row) {
        const name = row.querySelector(".name-input").value;
        const panel = panels.get(row.getAttribute("data-row-id"));
        if (name.trim() === "" || !panel) {
          return;
        }
        const days = Array.from(panel.querySelectorAll(".day-row")).map(function (dayRow) {
          return {
            off: dayRow.querySelector(".off-input").checked,
            start: dayRow.querySelector(".start-select").value,
            end: dayRow.querySelector(".end-select").value,
          };
        });
        employees.push({ name: name, days: days });
      });
      return employees;
    }

    function baseWishOf(employee, date) {
      return employee.days[weekdayIndex(date)] || { off: false, start: "", end: "" };
    }

    function rebuildHiddenInputs() {
      const fields = toHiddenFields(buildAdjustmentList(adjustments, monthInput.value));
      hiddenContainer.replaceChildren();
      fields.forEach(function (field) {
        const input = document.createElement("input");
        input.type = "hidden";
        input.name = field.name;
        input.value = field.value;
        hiddenContainer.appendChild(input);
      });
    }

    function employeeNames() {
      return readEmployees().map(function (employee) {
        return employee.name;
      });
    }

    function message(text) {
      const paragraph = document.createElement("p");
      paragraph.className = "calendar-message";
      paragraph.textContent = text;
      return paragraph;
    }

    function renderCalendar() {
      calendar.replaceChildren();
      if (calendarData === null) {
        calendar.appendChild(message("対象月を判定できません。祝日データにない月です。"));
        return;
      }
      const names = employeeNames();
      const header = document.createElement("div");
      header.className = "calendar-week calendar-header";
      WEEKDAY_LABELS.forEach(function (label) {
        const cell = document.createElement("div");
        cell.textContent = label;
        header.appendChild(cell);
      });
      calendar.appendChild(header);

      buildCalendarWeeks(calendarData.month, calendarData.businessDays, calendarData.holidays).forEach(
        function (week) {
          const weekElement = document.createElement("div");
          weekElement.className = "calendar-week";
          week.forEach(function (cell) {
            if (cell === null) {
              const empty = document.createElement("div");
              empty.className = "calendar-empty";
              weekElement.appendChild(empty);
              return;
            }
            const button = document.createElement("button");
            button.type = "button";
            button.className = "calendar-day";
            button.setAttribute("data-date", cell.date);
            const dayNumber = document.createElement("span");
            dayNumber.className = "date";
            dayNumber.textContent = String(cell.day);
            button.appendChild(dayNumber);
            if (cell.kind !== "business") {
              button.disabled = true;
              button.classList.add("holiday");
              const name = document.createElement("span");
              name.className = "holiday-name";
              name.textContent = cell.holidayName || "休業日";
              button.appendChild(name);
            } else {
              const count = countChanges(adjustments, cell.date, names);
              if (count > 0) {
                button.classList.add("has-change");
                const badge = document.createElement("span");
                badge.className = "change-count";
                badge.textContent = "変更 " + count + " 名";
                button.appendChild(badge);
              }
              if (cell.date === selectedDate) {
                button.classList.add("selected");
                button.setAttribute("aria-pressed", "true");
              }
            }
            weekElement.appendChild(button);
          });
          calendar.appendChild(weekElement);
        }
      );
    }

    function createTimeSelect(className, value, disabled) {
      const select = document.createElement("select");
      select.className = className;
      const empty = document.createElement("option");
      empty.value = "";
      empty.textContent = "-- 未選択 --";
      select.appendChild(empty);
      timeOptions.forEach(function (time) {
        const option = document.createElement("option");
        option.value = time;
        option.textContent = time;
        select.appendChild(option);
      });
      select.value = value;
      select.disabled = disabled;
      return select;
    }

    function renderDayPanel() {
      dayPanel.replaceChildren();
      shownEmployees = readEmployees();
      if (selectedDate === null) {
        dayPanel.appendChild(message("選択できる営業日がありません。"));
        return;
      }
      const heading = document.createElement("h3");
      heading.textContent =
        selectedDate + "（" + WEEKDAY_LABELS[weekdayIndex(selectedDate)] + "）の希望";
      dayPanel.appendChild(heading);
      if (shownEmployees.length === 0) {
        dayPanel.appendChild(message("氏名を入力した従業員がいません。画面 1 で入力してください。"));
        return;
      }
      shownEmployees.forEach(function (employee, index) {
        const base = baseWishOf(employee, selectedDate);
        const change = adjustments.get(adjustmentKey(selectedDate, employee.name));
        const wish = change || base;
        const row = document.createElement("div");
        row.className = "day-panel-row";
        row.setAttribute("data-employee-index", String(index));
        row.classList.toggle("changed", Boolean(change));

        const name = document.createElement("strong");
        name.textContent = employee.name;
        row.appendChild(name);

        const label = document.createElement("label");
        const off = document.createElement("input");
        off.type = "checkbox";
        off.className = "day-off";
        off.checked = wish.off;
        label.appendChild(off);
        label.appendChild(document.createTextNode("休み"));
        row.appendChild(label);

        // 休みの日も時刻の欄には基本シフトの時刻（なければ既定の 07:30〜18:30）を残し、
        // 休みを外したときにそのまま有効な時間帯になるようにする
        const start = wish.off ? base.start || DEFAULT_START : wish.start;
        const end = wish.off ? base.end || DEFAULT_END : wish.end;
        row.appendChild(createTimeSelect("day-start", start, wish.off));
        row.appendChild(createTimeSelect("day-end", end, wish.off));
        dayPanel.appendChild(row);
      });
      const reset = document.createElement("button");
      reset.type = "button";
      reset.className = "reset-all-btn";
      reset.textContent = "この日を基本に戻す";
      dayPanel.appendChild(reset);
    }

    function renderAll() {
      renderCalendar();
      renderDayPanel();
      rebuildHiddenInputs();
    }

    dayPanel.addEventListener("change", function (event) {
      const row = event.target.closest(".day-panel-row");
      if (!row || selectedDate === null) {
        return;
      }
      const employee = shownEmployees[Number(row.getAttribute("data-employee-index"))];
      if (!employee) {
        return;
      }
      const off = row.querySelector(".day-off").checked;
      row.querySelector(".day-start").disabled = off;
      row.querySelector(".day-end").disabled = off;
      const wish = {
        off: off,
        start: off ? "" : row.querySelector(".day-start").value,
        end: off ? "" : row.querySelector(".day-end").value,
      };
      applyEdit(adjustments, selectedDate, employee.name, wish, baseWishOf(employee, selectedDate));
      row.classList.toggle("changed", adjustments.has(adjustmentKey(selectedDate, employee.name)));
      renderCalendar();
      rebuildHiddenInputs();
    });

    dayPanel.addEventListener("click", function (event) {
      if (event.target.closest(".reset-all-btn") && selectedDate !== null) {
        resetDay(adjustments, selectedDate);
        renderAll();
      }
    });

    calendar.addEventListener("click", function (event) {
      const button = event.target.closest(".calendar-day");
      if (!button || button.disabled) {
        return;
      }
      selectedDate = button.getAttribute("data-date");
      renderCalendar();
      renderDayPanel();
    });

    document.addEventListener("calendar-loaded", function (event) {
      calendarData = event.detail;
      selectedDate = chooseSelectedDate(selectedDate, calendarData.businessDays);
      renderAll();
    });

    document.addEventListener("calendar-unavailable", function () {
      calendarData = null;
      selectedDate = null;
      renderAll();
    });

    document.addEventListener("employees-changed", function () {
      if (calendarData !== null) {
        renderCalendar();
      }
      renderDayPanel();
    });

    form.addEventListener("submit", rebuildHiddenInputs);
  });
})();
