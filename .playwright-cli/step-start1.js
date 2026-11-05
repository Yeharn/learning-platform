async page => {
  await page.locator('.el-table__row').last().locator('button', { hasText: '发起' }).click()
  await page.waitForTimeout(2200)
  const dialog = page.locator('.el-dialog:visible')
  const labels = await dialog.locator('.el-form-item__label').allInnerTexts()
  const hasForm = await dialog.locator('.flow-form').count()
  const nameInput = dialog.locator('.el-form-item', { hasText: '流程名称' }).locator('input').first()
  const processName = await nameInput.inputValue()
  return JSON.stringify({ labels, hasForm, processName })
}
