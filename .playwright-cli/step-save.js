async page => {
  await page.locator('.designer-toolbar button', { hasText: '保存并部署' }).click()
  await page.waitForTimeout(3000)
  const msg = await page.locator('.el-message').allInnerTexts()
  const drawer = await page.locator('.el-drawer:visible').count()
  const rows = await page.locator('.el-table__row').count()
  return JSON.stringify({ msg, drawer, rows })
}
