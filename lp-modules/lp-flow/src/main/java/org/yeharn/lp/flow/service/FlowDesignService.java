/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 流程设计数据规范化与校验
 */

package org.yeharn.lp.flow.service;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.dto.DefJson;
import org.dromara.warm.flow.core.dto.NodeJson;
import org.dromara.warm.flow.core.dto.SkipJson;
import org.springframework.stereotype.Service;
import org.yeharn.lp.common.core.ServiceException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 设计器数据兜底处理：
 * <ul>
 *     <li>保证存在开始节点（type=0）与结束节点（type=2）</li>
 *     <li>节点编码为空时自动生成，连线 nowNodeCode 缺失时按前驱补齐</li>
 *     <li>互斥网关的分支必须配置条件表达式</li>
 *     <li>产出可视化预览数据（层级 + 连线）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowDesignService {

    private final ObjectMapper objectMapper;

    public DefJson normalize(DefJson defJson) {
        if (defJson == null || defJson.getNodeList() == null || defJson.getNodeList().isEmpty()) {
            throw new ServiceException("流程节点不能为空");
        }
        List<NodeJson> nodes = defJson.getNodeList();
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < nodes.size(); i++) {
            NodeJson node = nodes.get(i);
            if (StrUtil.isBlank(node.getNodeCode())) {
                node.setNodeCode("n" + (i + 1));
            }
            if (!codes.add(node.getNodeCode())) {
                throw new ServiceException("节点编码重复：" + node.getNodeCode());
            }
            if (StrUtil.isBlank(node.getNodeName())) {
                node.setNodeName("节点" + (i + 1));
            }
            if (node.getNodeType() == null) {
                node.setNodeType(1);
            }
            if (StrUtil.isBlank(node.getFormCustom())) {
                node.setFormCustom("N");
            }
            if (StrUtil.isBlank(node.getNodeRatio())) {
                node.setNodeRatio("0");
            }
            fillSkip(node);
            checkGateway(node);
        }
        ensureStartAndEnd(nodes);
        return defJson;
    }

    /**
     * 校验互斥/包容网关的分支条件
     */
    private void checkGateway(NodeJson node) {
        Integer type = node.getNodeType();
        if (type == null || (type != 3 && type != 5)) {
            return;
        }
        List<SkipJson> skips = node.getSkipList();
        if (skips == null || skips.isEmpty()) {
            throw new ServiceException("网关节点「" + node.getNodeName() + "」缺少分支连线");
        }
        for (SkipJson skip : skips) {
            if (StrUtil.isBlank(skip.getSkipCondition())) {
                throw new ServiceException("网关节点「" + node.getNodeName() + "」的分支必须配置条件表达式");
            }
        }
    }

    private void fillSkip(NodeJson node) {
        if (node.getSkipList() == null) {
            node.setSkipList(new ArrayList<>());
            return;
        }
        for (SkipJson skip : node.getSkipList()) {
            if (StrUtil.isBlank(skip.getNowNodeCode())) {
                skip.setNowNodeCode(node.getNodeCode());
            }
            if (StrUtil.isBlank(skip.getSkipType())) {
                skip.setSkipType("PASS");
            }
        }
    }

    private void ensureStartAndEnd(List<NodeJson> nodes) {
        boolean hasStart = nodes.stream().anyMatch(n -> n.getNodeType() != null && n.getNodeType() == 0);
        boolean hasEnd = nodes.stream().anyMatch(n -> n.getNodeType() != null && n.getNodeType() == 2);
        if (!hasStart) {
            NodeJson start = new NodeJson().setNodeType(0).setNodeCode("start").setNodeName("发起人");
            start.setSkipList(List.of(new SkipJson().setNowNodeCode("start")
                    .setNextNodeCode(nodes.get(0).getNodeCode()).setSkipType("PASS")));
            nodes.add(0, start);
        }
        if (!hasEnd) {
            NodeJson last = nodes.get(nodes.size() - 1);
            NodeJson end = new NodeJson().setNodeType(2).setNodeCode("end").setNodeName("结束");
            List<SkipJson> skips = last.getSkipList() == null ? new ArrayList<>() : new ArrayList<>(last.getSkipList());
            if (skips.isEmpty()) {
                skips.add(new SkipJson().setNowNodeCode(last.getNodeCode())
                        .setNextNodeCode(end.getNodeCode()).setSkipType("PASS"));
                last.setSkipList(skips);
            }
            nodes.add(end);
        }
    }

    /**
     * 表单字段校验：必填项必须有 key 与 label
     */
    public void validateForm(List<Map<String, Object>> fields) {
        if (fields == null) {
            return;
        }
        Set<String> keys = new HashSet<>();
        for (Map<String, Object> field : fields) {
            String key = String.valueOf(field.getOrDefault("key", ""));
            if (StrUtil.isBlank(key)) {
                throw new ServiceException("表单字段标识不能为空");
            }
            if (!keys.add(key)) {
                throw new ServiceException("表单字段标识重复：" + key);
            }
        }
    }

    /**
     * 生成弹窗预览所需的结构（节点 + 连线 + 层级）
     */
    public Map<String, Object> preview(DefJson defJson) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (defJson == null || defJson.getNodeList() == null) {
            result.put("nodes", List.of());
            result.put("skips", List.of());
            return result;
        }
        List<Map<String, Object>> nodes = new ArrayList<>();
        Map<String, List<String>> nextMap = new HashMap<>();
        List<Map<String, Object>> skips = new ArrayList<>();
        for (NodeJson node : defJson.getNodeList()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("nodeCode", node.getNodeCode());
            item.put("nodeName", node.getNodeName());
            item.put("nodeType", node.getNodeType());
            item.put("permissionFlag", node.getPermissionFlag());
            nodes.add(item);
            if (node.getSkipList() != null) {
                for (SkipJson skip : node.getSkipList()) {
                    Map<String, Object> edge = new LinkedHashMap<>();
                    edge.put("from", node.getNodeCode());
                    edge.put("to", skip.getNextNodeCode());
                    edge.put("condition", skip.getSkipCondition());
                    edge.put("name", skip.getSkipName());
                    skips.add(edge);
                    nextMap.computeIfAbsent(node.getNodeCode(), k -> new ArrayList<>()).add(skip.getNextNodeCode());
                }
            }
        }
        List<Map<String, Object>> ordered = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        List<String> queue = new ArrayList<>();
        String startCode = defJson.getNodeList().stream()
                .filter(n -> n.getNodeType() != null && n.getNodeType() == 0)
                .map(NodeJson::getNodeCode).findFirst().orElse(null);
        if (startCode != null) {
            queue.add(startCode);
        }
        for (int i = 0; i < queue.size() && i < 500; i++) {
            String code = queue.get(i);
            if (!visited.add(code)) {
                continue;
            }
            defJson.getNodeList().stream().filter(n -> code.equals(n.getNodeCode())).findFirst().ifPresent(node -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("nodeCode", node.getNodeCode());
                item.put("nodeName", node.getNodeName());
                item.put("nodeType", node.getNodeType());
                ordered.add(item);
            });
            queue.addAll(nextMap.getOrDefault(code, List.of()));
        }
        result.put("nodes", ordered.isEmpty() ? nodes : ordered);
        result.put("skips", skips);
        return result;
    }

    /**
     * 设计器实时预览 / 保存前校验
     */
    public Map<String, Object> previewJson(String json) {
        if (StrUtil.isBlank(json)) {
            throw new ServiceException("流程模型不能为空");
        }
        try {
            DefJson defJson = objectMapper.readValue(json, DefJson.class);
            normalize(defJson);
            Map<String, Object> result = preview(defJson);
            result.put("model", defJson);
            return result;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("流程模型解析失败：" + e.getMessage());
        }
    }

    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new ServiceException("流程模型序列化失败");
        }
    }
}
