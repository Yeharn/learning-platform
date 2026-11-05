async page => {
  const dialog = page.locator('.el-dialog:visible')
  const amountInput = dialog.locator('.el-form-item', { hasText: '报销金额' }).locator('input').first()
  await amountInput.fill('2000')
  await amountInput.blur()
  await page.waitForTimeout(300)
  await dialog.locator('.el-form-item', { hasText: '申请说明' }).locator('textarea').fill('差旅报销 2000 元')
  await dialog.locator('.el-form-item', { hasText: '附加参数' }).locator('textarea').fill('{"deptId":100}')
  await dialog.locator('.el-dialog__footer button').last().click()
  await page.waitForTimeout(2800)
  const msg = await page.locator('.el-message').allInnerTexts()
  const dialogOpen = await page.locator('.el-dialog:visible').count()
  return JSON.stringify({ msg, dialogOpen })
}
