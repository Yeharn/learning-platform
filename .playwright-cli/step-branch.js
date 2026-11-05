async page => {
  await page.locator('.el-tabs__item', { hasText: '流程设计' }).click()
  await page.waitForTimeout(600)
  const pane = page.locator('.el-tab-pane:visible')
  await pane.locator('.palette-item', { hasText: '条件审批' }).click()
  await page.waitForTimeout(900)
  const props = page.locator('.designer-props')
  const editors = props.locator('.branch-editor')
  const nameInputs = props.locator('.branch-editor input[placeholder="如：金额大于 1000"]')
  await nameInputs.nth(0).fill('金额大于1000')
  await nameInputs.nth(1).fill('其他情况')

  await editors.nth(0).locator('.el-select').nth(0).click()
  await page.waitForTimeout(700)
  const optTexts = await page.locator('.el-select-dropdown__item:visible').allInnerTexts()
  await page.locator('.el-select-dropdown__item:visible').filter({ hasText: 'amount' }).first().click()
  await page.waitForTimeout(500)
  await editors.nth(0).locator('input[placeholder="值"]').fill('1000')

  await editors.nth(0).locator('.el-select').nth(2).click()
  await page.waitForTimeout(700)
  await page.locator('.el-select-dropdown__item:visible').filter({ hasText: '超级管理员' }).first().click()
  await page.keyboard.press('Escape')
  await page.waitForTimeout(500)

  await editors.nth(1).locator('.el-select').nth(2).click()
  await page.waitForTimeout(700)
  await page.locator('.el-select-dropdown__item:visible').filter({ hasText: '测试用户' }).first().click()
  await page.keyboard.press('Escape')
  await page.waitForTimeout(600)

  const preview = await page.locator('.canvas-branch').allInnerTexts()
  return JSON.stringify({ optTexts, preview })
}
