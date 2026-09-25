/**
 * シフト入力フォームの行を動的に追加・削除する機能を提供します。
 */
document.addEventListener("DOMContentLoaded", function () {
  const addRowBtn = document.getElementById("add-row-btn");
  const employeeRows = document.getElementById("employee-rows");
  const inputTable = document.querySelector("table.input-table");

  function updateDeleteButtonState() {
    const rows = employeeRows.querySelectorAll("tr");
    const deleteButtons = employeeRows.querySelectorAll(".delete-row-btn");

    deleteButtons.forEach((btn) => {
      btn.disabled = rows.length === 1;
    });
  }

  function updateAddButtonState() {
    const maxRows = parseInt(addRowBtn.getAttribute("data-max-rows"), 10);
    const currentRowCount = employeeRows.querySelectorAll("tr").length;
    addRowBtn.disabled = currentRowCount >= maxRows;
  }

  function updateRowCount() {
    const rowCount = employeeRows.querySelectorAll("tr").length;
    const rowCountElement = document.getElementById("row-count");
    if (rowCountElement) {
      rowCountElement.textContent = rowCount;
    }
  }

  // 開始・終了の選択肢は、サーバーが入力表の data-time-options に「|」区切りで設定する
  function readTimeOptions() {
    const attr = inputTable.getAttribute("data-time-options") || "";
    return attr ? attr.split("|") : [];
  }

  function createTimeSelect(name) {
    const select = document.createElement("select");
    select.name = name;

    const emptyOption = document.createElement("option");
    emptyOption.value = "";
    emptyOption.textContent = "-- 未選択 --";
    select.appendChild(emptyOption);

    readTimeOptions().forEach((time) => {
      const option = document.createElement("option");
      option.value = time;
      option.textContent = time;
      select.appendChild(option);
    });

    return select;
  }

  function createCell(label, element) {
    const cell = document.createElement("td");
    cell.setAttribute("data-label", label);
    cell.appendChild(element);
    return cell;
  }

  // 休みにチェックした行では、開始・終了を選択できなくする
  function updateTimeSelectsState(row) {
    const offCheckbox = row.querySelector(".off-checkbox");
    const off = offCheckbox !== null && offCheckbox.checked;
    row.querySelectorAll("select").forEach((select) => {
      select.disabled = off;
    });
  }

  // インデックスに欠番があると Spring MVC でリストをバインドできないため、削除後に振り直す。
  // 休みのチェックボックスに対応する隠しフィールド（_employees[N].off）も対象にする
  function renumberInputIndices() {
    const rows = employeeRows.querySelectorAll("tr");

    rows.forEach((row, index) => {
      const inputs = row.querySelectorAll("input, select");

      inputs.forEach((input) => {
        input.name = input.name.replace(/(_?employees)\[\d+\]/, "$1[" + index + "]");
      });
    });
  }

  addRowBtn.addEventListener("click", function () {
    const maxRows = parseInt(addRowBtn.getAttribute("data-max-rows"), 10);
    const currentRowCount = employeeRows.querySelectorAll("tr").length;
    if (currentRowCount >= maxRows) {
      return;
    }

    const prefix = "employees[" + currentRowCount + "]";
    const newRow = document.createElement("tr");

    const nameInput = document.createElement("input");
    nameInput.type = "text";
    nameInput.name = prefix + ".name";
    nameInput.placeholder = "氏名を入力";
    newRow.appendChild(createCell("氏名", nameInput));

    const offCell = document.createElement("td");
    offCell.setAttribute("data-label", "休み");
    const offCheckbox = document.createElement("input");
    offCheckbox.type = "checkbox";
    offCheckbox.className = "off-checkbox";
    offCheckbox.name = prefix + ".off";
    offCheckbox.value = "true";
    offCell.appendChild(offCheckbox);
    // Thymeleaf の th:field と同じく、未チェック時に false をバインドさせるための隠しフィールド
    const offHidden = document.createElement("input");
    offHidden.type = "hidden";
    offHidden.name = "_" + prefix + ".off";
    offHidden.value = "on";
    offCell.appendChild(offHidden);
    newRow.appendChild(offCell);

    newRow.appendChild(createCell("開始", createTimeSelect(prefix + ".start")));
    newRow.appendChild(createCell("終了", createTimeSelect(prefix + ".end")));

    const deleteCell = document.createElement("td");
    const deleteBtn = document.createElement("button");
    deleteBtn.type = "button";
    deleteBtn.className = "delete-row-btn";
    deleteBtn.textContent = "削除";
    deleteCell.appendChild(deleteBtn);
    newRow.appendChild(deleteCell);

    employeeRows.appendChild(newRow);

    updateDeleteButtonState();
    updateAddButtonState();
    updateRowCount();
  });

  employeeRows.addEventListener("click", function (event) {
    if (event.target.classList.contains("delete-row-btn")) {
      const row = event.target.closest("tr");
      if (row) {
        row.remove();
        renumberInputIndices();
        updateDeleteButtonState();
        updateAddButtonState();
        updateRowCount();
      }
    }
  });

  employeeRows.addEventListener("change", function (event) {
    if (event.target.classList.contains("off-checkbox")) {
      updateTimeSelectsState(event.target.closest("tr"));
    }
  });

  employeeRows.querySelectorAll("tr").forEach((row) => {
    updateTimeSelectsState(row);
  });
  updateDeleteButtonState();
  updateAddButtonState();
  updateRowCount();
});
