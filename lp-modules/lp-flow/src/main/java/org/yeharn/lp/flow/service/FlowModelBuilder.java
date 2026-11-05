/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: Warm-Flow 流程 JSON 模型构建器（兼容旧版表单化建模）
 */

package org.yeharn.lp.flow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.dromara.warm.flow.core.dto.DefJson;
import org.dromara.warm.flow.core.dto.NodeJson;
import org.dromara.warm.flow.core.dto.SkipJson;
import org.springframework.stereotype.Service;
import org.yeharn.lp.common.core.ServiceException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将「节点列表 + 抄送人」的简化配置转换为 warm-flow 的 DefJson 模型。
 * <p>
 * 节点类型：0 开始、1 审批、2 结束、3 互斥网关、4 并行网关、5 包容网关
 */
@Service
@RequiredArgsConstructor
public class FlowModelBuilder {

    private final ObjectMapper objectMapper;

    public String build(String key, String name, List<Map<String, String>> nodes, List<Map<String, String>> ccNodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new ServiceException("至少需要一个审批节点");
        }
        DefJson defJson = new DefJson()
                .setFlowCode(key)
                .setFlowName(name)
                .setModelValue("MIMIC");

        List<NodeJson> nodeList = new ArrayList<>();
        NodeJson start = new NodeJson().setNodeType(0).setNodeCode("start").setNodeName("发起人");
        nodeList.add(start);
        NodeJson previous = start;
        int index = 1;
        for (Map<String, String> node : nodes) {
            NodeJson approval = new NodeJson()
                    .setNodeType(1)
                    .setNodeCode("n" + index)
                    .setNodeName(value(node.get("name"), "审批节点" + index))
                    .setPermissionFlag(value(node.get("assigneeId"), "1"));
            nodeList.add(approval);
            previous.setSkipList(List.of(new SkipJson()
                    .setNowNodeCode(previous.getNodeCode())
                    .setNextNodeCode(approval.getNodeCode())
                    .setSkipType("PASS")));
            previous = approval;
            index++;
        }

        if (ccNodes != null && !ccNodes.isEmpty()) {
            NodeJson cc = new NodeJson()
                    .setNodeType(1)
                    .setNodeCode("cc")
                    .setNodeName("抄送人")
                    .setExt(ccExt(ccNodes));
            nodeList.add(cc);
            previous.setSkipList(List.of(new SkipJson()
                    .setNowNodeCode(previous.getNodeCode())
                    .setNextNodeCode(cc.getNodeCode())
                    .setSkipType("PASS")));
            previous = cc;
        }

        NodeJson end = new NodeJson().setNodeType(2).setNodeCode("end").setNodeName("结束");
        nodeList.add(end);
        previous.setSkipList(List.of(new SkipJson()
                .setNowNodeCode(previous.getNodeCode())
                .setNextNodeCode(end.getNodeCode())
                .setSkipType("PASS")));

        defJson.setNodeList(nodeList);
        try {
            return objectMapper.writeValueAsString(defJson);
        } catch (Exception e) {
            throw new ServiceException("流程模型生成失败：" + e.getMessage());
        }
    }

    private String ccExt(List<Map<String, String>> ccNodes) {
        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("cc", true);
        ext.put("autoApprove", true);
        ext.put("autoComment", "抄送知会");
        List<String> users = new ArrayList<>();
        for (Map<String, String> node : ccNodes) {
            String id = value(node.get("assigneeId"), null);
            if (id != null) {
                users.add(id);
            }
        }
        ext.put("ccUsers", users);
        try {
            return objectMapper.writeValueAsString(ext);
        } catch (Exception e) {
            return null;
        }
    }

    private String value(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
