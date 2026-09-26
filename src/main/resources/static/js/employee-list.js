/**
 * 従業員一覧と基本シフトパネルの管理機能を提供します。
 * 行の追加・削除・並べ替え、インデックスの管理、シフト要約の計算を実装しています。
 */

// 純粋な関数：行のインデックスを再番号付けする
function renumber(rows) {
  rows.forEach((row, index) => {
    const nameInput = row.querySelector('input[type="text"]');
    const typeSelect = row.querySelector("select");
    if (nameInput) nameInput.name = `employees[${index}].name`;
    if (typeSelect) typeSelect.name = `employees[${index}].employmentType`;

    // 基本シフトのインデックスを更新
    const daysInputs = row.querySelectorAll('input[type="checkbox"], select');
    daysInputs.forEach((input, dayIndex) => {
      const dayOfWeek = Math.floor(dayIndex / 3); // 3 入力 (off, start, end) ごとに 1 日
      const inputType = dayIndex % 3;
      if (inputType === 0) {
        input.name = `employees[${index}].days[${dayOfWeek}].off`;
      } else if (inputType === 1) {
        input.name = `employees[${index}].days[${dayOfWeek}].start`;
      } else {
        input.name = `employees[${index}].days[${dayOfWeek}].end`;
      }
    });
  });
}

// 純粋な関数：基本シフトの要約を生成
function summarize(daysArray) {
  if (!daysArray || daysArray.length === 0) return "";

  const workingDays = [];
  const offDays = [];
  const dayNames = ["月", "火", "水", "木", "金"];

  daysArray.forEach((day, index) => {
    if (day.off) {
      offDays.push(dayNames[index]);
    } else if (day.start && day.end) {
      workingDays.push({ start: day.start, end: day.end });
    }
  });

  let summary = "";

  if (workingDays.length > 0) {
    const starts = workingDays.map((d) => d.start);
    const ends = workingDays.map((d) => d.end);
    const minStart = starts.reduce((a, b) => (a < b ? a : b));
    const maxEnd = ends.reduce((a, b) => (a > b ? a : b));

    const allSame =
      starts.every((s) => s === starts[0]) && ends.every((e) => e === ends[0]);
    summary = allSame
      ? `${starts[0]}〜${ends[0]}`
      : `${minStart}〜${maxEnd}`;
  } else {
    summary = `休：${offDays.join("")}`;
  }

  if (offDays.length > 0 && workingDays.length > 0) {
    summary += `／休：${offDays.join("")}`;
  }

  return summary;
}

document.addEventListener("DOMContentLoaded", function () {
  const employeeRows = document.getElementById("employee-rows");
  const basePanels = document.getElementById("base-panels");
  const addBtn = document.getElementById("add-employee-btn");
  let nextRowId = 100;

  function getRowId(row) {
    return row.getAttribute("data-row-id");
  }

  function getOrCreateRowId(row) {
    let id = getRowId(row);
    if (!id) {
      id = String(nextRowId++);
      row.setAttribute("data-row-id", id);
    }
    return id;
  }

  function updateRows() {
    const rows = Array.from(employeeRows.querySelectorAll("tr"));
    renumber(rows);
    updateDeleteButtons();
    updateMoveButtons();
    updateSummaries();

    document.dispatchEvent(new CustomEvent("employees-changed"));
  }

  function updateDeleteButtons() {
    const rows = employeeRows.querySelectorAll("tr");
    rows.forEach((row) => {
      const btn = row.querySelector(".delete-btn");
      if (btn) btn.disabled = rows.length === 1;
    });
  }

  function updateMoveButtons() {
    const rows = Array.from(employeeRows.querySelectorAll("tr"));
    rows.forEach((row, index) => {
      const upBtn = row.querySelector(".move-up-btn");
      const downBtn = row.querySelector(".move-down-btn");
      if (upBtn) upBtn.disabled = index === 0;
      if (downBtn) downBtn.disabled = index === rows.length - 1;
    });
  }

  function updateSummaries() {
    employeeRows.querySelectorAll("tr").forEach((row) => {
      const rowId = getOrCreateRowId(row);
      const panel = basePanels.querySelector(`[data-row-id="${rowId}"]`);
      if (panel) {
        const daysData = [];
        const dayInputs = panel.querySelectorAll("tbody tr");
        dayInputs.forEach((dayRow) => {
          const off = dayRow.querySelector('input[type="checkbox"]').checked;
          const start = dayRow.querySelector("select:nth-of-type(1)").value;
          const end = dayRow.querySelector("select:nth-of-type(2)").value;
          daysData.push({ off, start, end });
        });
        const summary = summarize(daysData);
        const summaryCell = row.querySelector(".summary");
        if (summaryCell) summaryCell.textContent = summary;
      }
    });
  }

  addBtn.addEventListener("click", (e) => {
    e.preventDefault();
    if (employeeRows.querySelectorAll("tr").length >= 12) {
      addBtn.disabled = true;
      return;
    }

    const newRow = document.createElement("tr");
    newRow.className = "employee-row";
    const rowId = String(nextRowId++);
    newRow.setAttribute("data-row-id", rowId);
    newRow.innerHTML = `
      <td class="button-cell">
        <button type="button" class="move-up-btn">▲</button>
        <button type="button" class="move-down-btn">▼</button>
      </td>
      <td><input type="text" name="employees[${employeeRows.children.length}].name" /></td>
      <td><select name="employees[${employeeRows.children.length}].employmentType">
        <option value="FULL_TIME">常勤</option>
        <option value="PART_TIME">パート</option>
        <option value="MANAGER">管理職</option>
      </select></td>
      <td class="summary"></td>
      <td><button type="button" class="delete-btn">削除</button></td>
    `;

    const newPanel = document.createElement("div");
    newPanel.className = "base-panel";
    newPanel.setAttribute("data-row-id", rowId);
    newPanel.setAttribute("hidden", "");
    newPanel.innerHTML = `<table><thead><tr><th>曜日</th><th>休み</th><th>開始</th><th>終了</th><th></th></tr></thead><tbody></tbody></table>`;

    const dayNames = ["月", "火", "水", "木", "金"];
    const tbody = newPanel.querySelector("tbody");
    dayNames.forEach((name, d) => {
      const dayRow = document.createElement("tr");
      dayRow.innerHTML = `
        <td>${name}</td>
        <td><input type="checkbox" name="employees[${employeeRows.children.length}].days[${d}].off" /></td>
        <td><select name="employees[${employeeRows.children.length}].days[${d}].start">
          <option value="">-- 未選択 --</option>
          ${Array.from(document.querySelector("form").getAttribute("data-time-options").split("|"))
            .map((t) => `<option value="${t}">${t}</option>`)
            .join("")}
        </select></td>
        <td><select name="employees[${employeeRows.children.length}].days[${d}].end">
          <option value="">-- 未選択 --</option>
          ${Array.from(document.querySelector("form").getAttribute("data-time-options").split("|"))
            .map((t) => `<option value="${t}">${t}</option>`)
            .join("")}
        </select></td>
        <td><button type="button" class="copy-to-all-btn">全曜日へ</button></td>
      `;
      tbody.appendChild(dayRow);
    });

    employeeRows.appendChild(newRow);
    basePanels.appendChild(newPanel);

    // イベントリスナーを設定
    attachRowListeners(newRow);
    updateRows();
  });

  function attachRowListeners(row) {
    const upBtn = row.querySelector(".move-up-btn");
    const downBtn = row.querySelector(".move-down-btn");
    const deleteBtn = row.querySelector(".delete-btn");

    if (upBtn)
      upBtn.addEventListener("click", (e) => {
        e.preventDefault();
        const prev = row.previousElementSibling;
        if (prev) {
          employeeRows.insertBefore(row, prev);
          updateRows();
        }
      });

    if (downBtn)
      downBtn.addEventListener("click", (e) => {
        e.preventDefault();
        const next = row.nextElementSibling;
        if (next) {
          employeeRows.insertBefore(next, row);
          updateRows();
        }
      });

    if (deleteBtn)
      deleteBtn.addEventListener("click", (e) => {
        e.preventDefault();
        const rowId = getRowId(row);
        const panel = basePanels.querySelector(`[data-row-id="${rowId}"]`);
        if (panel) panel.remove();
        row.remove();
        updateRows();
      });

    // クリックで基本シフトパネルを表示
    row.addEventListener("click", () => {
      const rowId = getRowId(row);
      basePanels.querySelectorAll(".base-panel").forEach((panel) => {
        if (panel.getAttribute("data-row-id") === rowId) {
          panel.removeAttribute("hidden");
        } else {
          panel.setAttribute("hidden", "");
        }
      });
    });
  }

  // 既存行にリスナーを設定
  employeeRows.querySelectorAll("tr").forEach((row) => {
    getOrCreateRowId(row);
    attachRowListeners(row);
  });

  updateRows();
});
