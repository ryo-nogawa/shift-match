/**
 * 日ごとの希望入力（営業日カレンダー・個別変更）の管理機能を提供します。
 * カレンダー描画、選択日の入力パネル、個別変更の保持と送信を実装しています。
 */

// 純粋な関数：基本シフトと調整内容が同じか判定
function isAdjustmentSame(baseShift, adjustment) {
  if (!adjustment) return true; // 調整がないなら基本シフトと同じ
  return (
    baseShift.off === adjustment.off &&
    baseShift.start === adjustment.start &&
    baseShift.end === adjustment.end
  );
}

// 純粋な関数：個別変更の一覧を組み立てる（対象月のみ）
function buildAdjustmentList(adjustmentMap, targetMonth) {
  const adjustments = [];
  let index = 0;
  for (const [key] of adjustmentMap) {
    const [date] = key.split("|");
    if (date.startsWith(targetMonth)) {
      const value = adjustmentMap.get(key);
      adjustments.push({
        k: index++,
        date,
        key,
        off: value.off,
        start: value.start,
        end: value.end,
      });
    }
  }
  return adjustments;
}

document.addEventListener("DOMContentLoaded", function () {
  const calendar = document.getElementById("calendar");
  const dayPanel = document.getElementById("day-panel");
  const adjustmentInputs = document.getElementById("adjustment-inputs");
  const form = document.querySelector("form");
  const targetMonthInput = document.querySelector("input[name='targetMonth']");
  const employeeRows = document.getElementById("employee-rows");

  let calendarData = null;
  let selectedDate = null;
  const adjustmentMap = new Map(); // key: "YYYY-MM-DD|employeeName"

  // ページ読み込み時に #adjustment-inputs から復元
  function restoreAdjustmentsFromForm() {
    // インデックスから date を取得
    const datesByIndex = new Map();
    adjustmentInputs
      .querySelectorAll("input[name*='date']")
      .forEach((input) => {
        const match = input.name.match(/adjustments\[(\d+)\]/);
        if (match) {
          const index = parseInt(match[1]);
          datesByIndex.set(index, input.value);
        }
      });

    // employeeName と調整内容を取得
    adjustmentInputs
      .querySelectorAll("input[name*='employeeName']")
      .forEach((input) => {
        const match = input.name.match(/adjustments\[(\d+)\]/);
        if (match) {
          const index = parseInt(match[1]);
          const date = datesByIndex.get(index);
          const employeeName = input.value;
          if (!date || !employeeName) return;

          const offInput = adjustmentInputs.querySelector(
            `input[name='adjustments[${index}].off']`
          );
          const startInput = adjustmentInputs.querySelector(
            `input[name='adjustments[${index}].start']`
          );
          const endInput = adjustmentInputs.querySelector(
            `input[name='adjustments[${index}].end']`
          );

          const key = `${date}|${employeeName}`;
          adjustmentMap.set(key, {
            off: !!offInput,
            start: startInput?.value || null,
            end: endInput?.value || null,
          });
        }
      });
  }

  // #adjustment-inputs を作り直す
  function updateAdjustmentInputs() {
    const targetMonth = targetMonthInput.value;
    adjustmentInputs.innerHTML = "";

    const adjustments = buildAdjustmentList(adjustmentMap, targetMonth);
    adjustments.forEach(({ k, date, off, start, end }) => {
      const employeeName = adjustmentMap
        .keys()
        .find(
          (key) => key.split("|")[0] === date && adjustmentMap.get(key) === adjustmentMap.get(k)
        )
        ?.split("|")[1];

      const dateInput = document.createElement("input");
      dateInput.type = "hidden";
      dateInput.name = `adjustments[${k}].date`;
      dateInput.value = date;
      adjustmentInputs.appendChild(dateInput);

      const nameInput = document.createElement("input");
      nameInput.type = "hidden";
      nameInput.name = `adjustments[${k}].employeeName`;
      nameInput.value = adjustmentMap.get(`${date}|${employeeName}`)?.employeeName || "";
      adjustmentInputs.appendChild(nameInput);

      if (off) {
        const offInput = document.createElement("input");
        offInput.type = "hidden";
        offInput.name = `adjustments[${k}].off`;
        offInput.value = "true";
        adjustmentInputs.appendChild(offInput);
      }

      if (start) {
        const startInput = document.createElement("input");
        startInput.type = "hidden";
        startInput.name = `adjustments[${k}].start`;
        startInput.value = start;
        adjustmentInputs.appendChild(startInput);
      }

      if (end) {
        const endInput = document.createElement("input");
        endInput.type = "hidden";
        endInput.name = `adjustments[${k}].end`;
        endInput.value = end;
        adjustmentInputs.appendChild(endInput);
      }
    });
  }

  // カレンダーを描画
  function renderCalendar() {
    if (!calendarData) return;

    calendar.innerHTML = "";
    const { businessDays, holidays } = calendarData;
    const holidayDates = new Set(holidays.map((h) => h.date));
    const adjustmentCounts = new Map();

    // 個別変更の数をカウント
    for (const [key] of adjustmentMap) {
      const [date] = key.split("|");
      if (date.startsWith(targetMonthInput.value)) {
        adjustmentCounts.set(date, (adjustmentCounts.get(date) || 0) + 1);
      }
    }

    // 週ごとにグループ化
    const weeks = [];
    let currentWeek = [];
    let firstDayOfMonth = true;

    businessDays.forEach((date) => {
      const dayOfWeek = new Date(date).getDay(); // 0=日, 1=月, ..., 6=土
      if (firstDayOfMonth) {
        firstDayOfMonth = false;
        // 月曜より前の空欄を追加
        for (let i = 1; i < dayOfWeek; i++) {
          currentWeek.push({ empty: true });
        }
      }

      if (currentWeek.length === 5) {
        weeks.push(currentWeek);
        currentWeek = [];
      }
      currentWeek.push({ date });
    });

    if (currentWeek.length > 0) {
      weeks.push(currentWeek);
    }

    // HTML を生成
    const calendarHTML = weeks
      .map(
        (week) =>
          `<div class="calendar-week">${week
            .map((day) => {
              if (day.empty) return `<div class="calendar-empty"></div>`;
              const { date } = day;
              const isHoliday = holidayDates.has(date);
              const holiday = holidays.find((h) => h.date === date);
              const hasChange = adjustmentCounts.has(date);
              const changeCount = adjustmentCounts.get(date) || 0;
              const selected = selectedDate === date ? "selected" : "";
              const disabled = isHoliday ? "disabled" : "";
              return `
                <button
                  type="button"
                  class="calendar-day ${hasChange ? "has-change" : ""} ${selected}"
                  data-date="${date}"
                  ${disabled}
                >
                  <span class="date">${date.split("-")[2]}</span>
                  ${isHoliday ? `<span class="holiday-name">${holiday.name}</span>` : ""}
                  ${hasChange ? `<span class="change-count">変更 ${changeCount} 名</span>` : ""}
                </button>
              `;
            })
            .join("")}</div>`
      )
      .join("");

    calendar.innerHTML = calendarHTML;

    // カレンダーボタンのイベントリスナー
    calendar.querySelectorAll(".calendar-day:not([disabled])").forEach((btn) => {
      btn.addEventListener("click", () => {
        const date = btn.getAttribute("data-date");
        selectedDate = date;
        renderCalendar();
        renderDayPanel();
      });
    });
  }

  // 選択日の入力パネルを描画
  function renderDayPanel() {
    if (!selectedDate) {
      dayPanel.innerHTML = "";
      return;
    }

    dayPanel.innerHTML = `<h3>${selectedDate}</h3>`;

    // 有効な従業員（氏名が空でない）を取得
    const dayOfWeek = new Date(selectedDate).getDay(); // 0=日, 1=月, ..., 6=土
    const localDayOfWeek = dayOfWeek === 0 ? 6 : dayOfWeek - 1; // 0=月, 1=火, ..., 4=金

    const employees = [];
    employeeRows.querySelectorAll("tr").forEach((row, rowIndex) => {
      const nameInput = row.querySelector('input[type="text"]');
      const name = nameInput?.value.trim();
      if (name) {
        employees.push({ name, rowIndex });
      }
    });

    const panelHTML = employees
      .map(({ name, rowIndex }) => {
        const basePanel = document.querySelector(
          `.base-panel[data-row-id="${rowIndex}"]`
        );
        const dayRow = basePanel?.querySelectorAll("tbody tr")[localDayOfWeek];
        const baseOff =
          dayRow?.querySelector('input[type="checkbox"]').checked || false;
        const baseStart =
          dayRow?.querySelector("select:nth-of-type(1)").value || "";
        const baseEnd =
          dayRow?.querySelector("select:nth-of-type(2)").value || "";

        const baseShift = { off: baseOff, start: baseStart, end: baseEnd };
        const adjustment = adjustmentMap.get(`${selectedDate}|${name}`);
        const currentOff = adjustment?.off ?? baseOff;
        const currentStart = adjustment?.start ?? baseStart;
        const currentEnd = adjustment?.end ?? baseEnd;

        const isSameAsBase = isAdjustmentSame(baseShift, adjustment);

        return `
          <div class="day-panel-row">
            <strong>${name}</strong>
            <label>
              <input type="checkbox" class="off-checkbox" ${currentOff ? "checked" : ""} data-employee-name="${name}" />
              休み
            </label>
            <select class="start-select" data-employee-name="${name}" ${currentOff ? "disabled" : ""}>
              <option value="">-- 未選択 --</option>
              ${form
                .getAttribute("data-time-options")
                .split("|")
                .map(
                  (t) =>
                    `<option value="${t}" ${t === currentStart ? "selected" : ""}>${t}</option>`
                )
                .join("")}
            </select>
            <select class="end-select" data-employee-name="${name}" ${currentOff ? "disabled" : ""}>
              <option value="">-- 未選択 --</option>
              ${form
                .getAttribute("data-time-options")
                .split("|")
                .map(
                  (t) =>
                    `<option value="${t}" ${t === currentEnd ? "selected" : ""}>${t}</option>`
                )
                .join("")}
            </select>
            <button type="button" class="reset-btn" data-employee-name="${name}" ${isSameAsBase ? "disabled" : ""}>
              この日を基本に戻す
            </button>
          </div>
        `;
      })
      .join("");

    const resetAllBtn = `
      <button type="button" class="reset-all-btn">この日をすべて基本に戻す</button>
    `;

    dayPanel.innerHTML += panelHTML + resetAllBtn;

    // イベントリスナー
    dayPanel.querySelectorAll(".off-checkbox").forEach((checkbox) => {
      checkbox.addEventListener("change", (e) => {
        const employeeName = e.target.getAttribute("data-employee-name");
        const startSelect = dayPanel.querySelector(
          `.start-select[data-employee-name="${employeeName}"]`
        );
        const endSelect = dayPanel.querySelector(
          `.end-select[data-employee-name="${employeeName}"]`
        );
        startSelect.disabled = e.target.checked;
        endSelect.disabled = e.target.checked;
        updateAdjustmentForEmployee(employeeName);
      });
    });

    dayPanel.querySelectorAll(".start-select, .end-select").forEach((select) => {
      select.addEventListener("change", () => {
        const employeeName = select.getAttribute("data-employee-name");
        updateAdjustmentForEmployee(employeeName);
      });
    });

    dayPanel.querySelectorAll(".reset-btn").forEach((btn) => {
      btn.addEventListener("click", (e) => {
        e.preventDefault();
        const employeeName = btn.getAttribute("data-employee-name");
        const key = `${selectedDate}|${employeeName}`;
        adjustmentMap.delete(key);
        renderDayPanel();
        updateAdjustmentInputs();
      });
    });

    dayPanel.querySelector(".reset-all-btn")?.addEventListener("click", (e) => {
      e.preventDefault();
      for (const [key] of adjustmentMap) {
        const [date] = key.split("|");
        if (date === selectedDate) {
          adjustmentMap.delete(key);
        }
      }
      renderCalendar();
      renderDayPanel();
      updateAdjustmentInputs();
    });
  }

  function updateAdjustmentForEmployee(employeeName) {
    if (!selectedDate) return;

    const offCheckbox = dayPanel.querySelector(
      `.off-checkbox[data-employee-name="${employeeName}"]`
    );
    const startSelect = dayPanel.querySelector(
      `.start-select[data-employee-name="${employeeName}"]`
    );
    const endSelect = dayPanel.querySelector(
      `.end-select[data-employee-name="${employeeName}"]`
    );

    const off = offCheckbox?.checked || false;
    const start = startSelect?.value || null;
    const end = endSelect?.value || null;

    // 従業員の行インデックスを取得
    let baseRowId = null;
    employeeRows.querySelectorAll("tr").forEach((row) => {
      const nameInput = row.querySelector('input[type="text"]');
      if (nameInput?.value === employeeName) {
        baseRowId = row.getAttribute("data-row-id");
      }
    });

    // 基本シフトを取得
    const dayOfWeek = new Date(selectedDate).getDay();
    const localDayOfWeek = dayOfWeek === 0 ? 6 : dayOfWeek - 1;
    const basePanelDiv = document.querySelector(
      `.base-panel[data-row-id="${baseRowId}"]`
    );
    const dayRow = basePanelDiv?.querySelectorAll("tbody tr")[localDayOfWeek];
    const baseOff =
      dayRow?.querySelector('input[type="checkbox"]').checked || false;
    const baseStart =
      dayRow?.querySelector("select:nth-of-type(1)").value || "";
    const baseEnd =
      dayRow?.querySelector("select:nth-of-type(2)").value || "";

    const key = `${selectedDate}|${employeeName}`;

    // 基本シフトと同じなら削除
    if (off === baseOff && (start || "") === baseStart && (end || "") === baseEnd) {
      adjustmentMap.delete(key);
    } else {
      adjustmentMap.set(key, {
        off,
        start: start || null,
        end: end || null,
      });
    }

    renderCalendar();
    updateAdjustmentInputs();
  }

  // calendar-loaded イベントを購読
  document.addEventListener("calendar-loaded", (e) => {
    calendarData = e.detail;
    if (!selectedDate && calendarData.businessDays.length > 0) {
      selectedDate = calendarData.businessDays[0];
    }
    renderCalendar();
    renderDayPanel();
  });

  // employees-changed イベントを購読
  document.addEventListener("employees-changed", () => {
    renderDayPanel();
  });

  // フォーム送信前に #adjustment-inputs を作り直す
  form.addEventListener("submit", () => {
    updateAdjustmentInputs();
  });

  // 初期化
  restoreAdjustmentsFromForm();
});
