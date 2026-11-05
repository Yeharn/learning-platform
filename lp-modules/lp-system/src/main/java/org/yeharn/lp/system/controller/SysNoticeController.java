/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 公告接口控制器
 */

package org.yeharn.lp.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yeharn.lp.common.core.PageResult;
import org.yeharn.lp.common.core.R;
import org.yeharn.lp.common.log.BusinessType;
import org.yeharn.lp.common.log.OperLog;
import org.yeharn.lp.common.security.SecurityUtils;
import org.yeharn.lp.system.entity.SysNotice;
import org.yeharn.lp.system.mapper.SysNoticeMapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知公告
 */
@RestController
@RequestMapping("/system/notice")
@RequiredArgsConstructor
public class SysNoticeController {

    private final SysNoticeMapper noticeMapper;

    @SaCheckPermission("system:notice:list")
    @GetMapping("/list")
    public R<PageResult<SysNotice>> list(@RequestParam(required = false) String noticeTitle,
                                         @RequestParam(required = false) String noticeType,
                                         @RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize) {
        QueryWrapper qw = QueryWrapper.create()
                .like("notice_title", noticeTitle, StrUtil.isNotBlank(noticeTitle))
                .eq("notice_type", noticeType, StrUtil.isNotBlank(noticeType))
                .orderBy("notice_id", false);
        Page<SysNotice> page = noticeMapper.paginate(Page.of(pageNum, pageSize), qw);
        return R.ok(new PageResult<>(page.getTotalRow(), page.getRecords(), pageNum, pageSize));
    }

    @GetMapping("/latest")
    public R<List<SysNotice>> latest() {
        return R.ok(noticeMapper.selectListByQuery(QueryWrapper.create()
                .eq("status", "0").orderBy("notice_id", false).limit(5)));
    }

    @SaCheckPermission("system:notice:query")
    @GetMapping("/{noticeId}")
    public R<SysNotice> getInfo(@PathVariable Long noticeId) {
        return R.ok(noticeMapper.selectOneById(noticeId));
    }

    @SaCheckPermission("system:notice:add")
    @OperLog(title = "通知公告", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody SysNotice notice) {
        notice.setCreateBy(SecurityUtils.getUsername());
        notice.setCreateTime(LocalDateTime.now());
        noticeMapper.insertSelective(notice);
        return R.ok();
    }

    @SaCheckPermission("system:notice:edit")
    @OperLog(title = "通知公告", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody SysNotice notice) {
        notice.setUpdateBy(SecurityUtils.getUsername());
        notice.setUpdateTime(LocalDateTime.now());
        noticeMapper.update(notice);
        return R.ok();
    }

    @SaCheckPermission("system:notice:remove")
    @OperLog(title = "通知公告", businessType = BusinessType.DELETE)
    @DeleteMapping("/{noticeIds}")
    public R<Void> remove(@PathVariable List<Long> noticeIds) {
        noticeIds.forEach(noticeMapper::deleteById);
        return R.ok();
    }
}
