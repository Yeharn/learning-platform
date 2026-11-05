async page => {
  await page.goto('http://127.0.0.1:5173/flow/instance')
  await page.waitForTimeout(2600)
  const rows = await page.locator('.el-table__row').count()
  await page.locator('.el-table__row').first().locator('button', { hasText: '详情' }).click()
  await page.waitForTimeout(2400)
  const sections = await page.locator('.el-drawer:visible .section-title').allInnerTexts()
  const descItems = await page.locator('.el-drawer:visible .el-descriptions__cell').allInnerTexts()
  const active = await page.locator('.flow-node.active').allInnerTexts()
  const nodes = await page.locator('.flow-node').allInnerTexts()
  const errs = await page.locator('.el-message--error').allInnerTexts()
  return JSON.stringify({ rows, sections, descItems: descItems.map(t => t.replace(/\n/g, ' ')).slice(0, 24), active: active.map(t => t.replace(/\n/g, ' ')), nodes: nodes.map(t => t.replace(/\n/g, ' ')), errs })
}
