/**
 * シフト入力フォームの行を動的に追加・削除する機能を提供します。
 */
document.addEventListener("DOMContentLoaded", function () {
  const addRowBtn = document.getElementById("add-row-btn");
  const employeeRows = document.getElementById("employee-rows");

  function updateDataValue(select) {
    select.setAttribute("data-value", select.value);
  }

  function updateDeleteButtonState() {
    const rows = employeeRows.querySelectorAll("tr");
    const deleteButtons = employeeRows.querySelectorAll(".delete-row-btn");

    deleteButtons.forEach((btn) => {
      btn.disabled = rows.length === 1;
    });
  }

  function updateAddButtonState() {
    const maxRows = parseInt(addRowBtn.getAttribute("data-max-rows") || "12", 10);
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

  function createWishSelect(name) {
    const select = document.createElement("select");
    select.name = name;
    select.setAttribute("data-value", "");

    const options = [
      { value: "", text: "-- 未選択 --" },
      { value: "DESIRED", text: "◎ 希望" },
      { value: "AVAILABLE", text: "○ 可能" },
      { value: "UNAVAILABLE", text: "× 不可" },
    ];

    options.forEach((optionData) => {
      const option = document.createElement("option");
      option.value = optionData.value;
      option.textContent = optionData.text;
      select.appendChild(option);
    });

    return select;
  }

  employeeRows.querySelectorAll("select").forEach((select) => {
    updateDataValue(select);
  });

  // インデックスに欠番があると Spring MVC でリストをバインドできないため、削除後に振り直す
  function renumberInputIndices() {
    const rows = employeeRows.querySelectorAll("tr");

    rows.forEach((row, index) => {
      const inputs = row.querySelectorAll("input, select");

      inputs.forEach((input) => {
        input.name = input.name.replace(
          /employees\[\d+\]/,
          "employees[" + index + "]"
        );
      });
    });
  }

  addRowBtn.addEventListener("click", function () {
    const maxRows = parseInt(addRowBtn.getAttribute("data-max-rows") || "12", 10);
    const currentRowCount = employeeRows.querySelectorAll("tr").length;
    if (currentRowCount >= maxRows) {
      return;
    }

    const newRow = document.createElement("tr");

    const nameCell = document.createElement("td");
    nameCell.setAttribute("data-label", "氏名");
    const nameInput = document.createElement("input");
    nameInput.type = "text";
    nameInput.name = "employees[" + currentRowCount + "].name";
    nameInput.placeholder = "氏名を入力";
    nameCell.appendChild(nameInput);
    newRow.appendChild(nameCell);

    const table = document.querySelector("table.input-table");
    const slotLabelsAttr = table.getAttribute("data-slot-labels") || "";
    const workTimes = slotLabelsAttr ? slotLabelsAttr.split("|") : [];

    for (let slotIndex = 0; slotIndex < workTimes.length; slotIndex++) {
      const slotCell = document.createElement("td");
      slotCell.setAttribute("data-label", workTimes[slotIndex]);
      const slotSelect = createWishSelect(
        "employees[" + currentRowCount + "].wishes[" + slotIndex + "]"
      );
      slotSelect.setAttribute("data-label", workTimes[slotIndex]);
      slotCell.appendChild(slotSelect);
      newRow.appendChild(slotCell);
    }

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
    if (event.target.tagName === "SELECT") {
      updateDataValue(event.target);
    }
  });

  updateDeleteButtonState();
  updateAddButtonState();
  updateRowCount();
});
