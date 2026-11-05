async page => {
  await page.keyboard.press('Escape')
  await page.waitForTimeout(600)
  const rowTexts = await page.locator('.el-table__row').allInnerTexts()
  await page.locator('.el-table__row').first().locator('button', { hasText: '发起' }).click()
  await page.waitForTimeout(2200)
  const dialog = page.locator('.el-dialog:visible')
  const labels = await dialog.locator('.el-form-item__label').allInnerTexts()
  const hasForm = await dialog.locator('.flow-form').count()
  const processName = await dialog.locator('.el-form-item', { hasText: '流程名称' }).locator('input').first().inputValue()
  return JSON.stringify({ rowTexts: rowTexts.map(t => t.replace(/\n/g, '|').slice(0, 40)), labels, hasForm, processName })
}
