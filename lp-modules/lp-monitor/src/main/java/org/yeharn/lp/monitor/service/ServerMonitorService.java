/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 监控服务 - 服务器监控业务服务
 */

package org.yeharn.lp.monitor.service;

import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 服务器信息（CPU / 内存 / JVM / 磁盘 / 网络）
 */
@Service
public class ServerMonitorService {

    private final SystemInfo systemInfo = new SystemInfo();
    private long[] previousTicks = new long[0];

    public Map<String, Object> info() {
        Map<String, Object> result = new LinkedHashMap<>();
        HardwareAbstractionLayer hal = systemInfo.getHardware();
        OperatingSystem os = systemInfo.getOperatingSystem();

        result.put("sys", sysInfo(os));
        result.put("cpu", cpuInfo(hal.getProcessor()));
        result.put("mem", memoryInfo(hal.getMemory()));
        result.put("jvm", jvmInfo());
        result.put("disks", diskInfo(os));
        result.put("net", networkInfo(hal));
        result.put("time", LocalDateTime.now());
        return result;
    }

    private Map<String, Object> sysInfo(OperatingSystem os) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("computerName", safeHostName());
        map.put("osName", os.toString());
        map.put("osArch", System.getProperty("os.arch"));
        map.put("osVersion", System.getProperty("os.version"));
        map.put("userDir", System.getProperty("user.dir"));
        long bootTime = os.getSystemBootTime();
        map.put("bootTime", LocalDateTime.ofInstant(Instant.ofEpochSecond(bootTime), ZoneId.systemDefault()));
        map.put("runTime", formatDuration(ManagementFactory.getRuntimeMXBean().getUptime() / 1000));
        map.put("ip", localIp());
        map.put("processCount", os.getProcessCount());
        map.put("threadCount", os.getThreadCount());
        return map;
    }

    private Map<String, Object> cpuInfo(CentralProcessor processor) {
        Map<String, Object> map = new LinkedHashMap<>();
        long[] ticks = processor.getSystemCpuLoadTicks();
        double usage = previousTicks.length == 0 ? 0 : processor.getSystemCpuLoadBetweenTicks(previousTicks) * 100;
        previousTicks = ticks;
        map.put("name", processor.getProcessorIdentifier().getName());
        map.put("physicalCores", processor.getPhysicalProcessorCount());
        map.put("logicalCores", processor.getLogicalProcessorCount());
        map.put("usage", Math.round(usage * 100) / 100.0);
        map.put("frequency", processor.getProcessorIdentifier().getVendorFreq() / 1_000_000);
        return map;
    }

    private Map<String, Object> memoryInfo(GlobalMemory memory) {
        Map<String, Object> map = new LinkedHashMap<>();
        long total = memory.getTotal();
        long available = memory.getAvailable();
        long used = total - available;
        map.put("total", total);
        map.put("used", used);
        map.put("free", available);
        map.put("usage", total == 0 ? 0 : Math.round(used * 10000.0 / total) / 100.0);
        return map;
    }

    private Map<String, Object> jvmInfo() {
        Map<String, Object> map = new LinkedHashMap<>();
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long max = runtime.maxMemory();
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        map.put("name", System.getProperty("java.vm.name"));
        map.put("version", System.getProperty("java.version"));
        map.put("vendor", System.getProperty("java.vendor"));
        map.put("home", System.getProperty("java.home"));
        map.put("startTime", LocalDateTime.ofInstant(Instant.ofEpochMilli(bean.getStartTime()), ZoneId.systemDefault()));
        map.put("runTime", formatDuration(bean.getUptime() / 1000));
        map.put("total", total);
        map.put("used", total - free);
        map.put("free", free);
        map.put("max", max);
        map.put("usage", max == 0 ? 0 : Math.round((total - free) * 10000.0 / max) / 100.0);
        map.put("pid", ProcessHandle.current().pid());
        Properties props = System.getProperties();
        map.put("timezone", props.getProperty("user.timezone"));
        map.put("encoding", props.getProperty("file.encoding"));
        return map;
    }

    private List<Map<String, Object>> diskInfo(OperatingSystem os) {
        List<Map<String, Object>> list = new ArrayList<>();
        FileSystem fileSystem = os.getFileSystem();
        for (OSFileStore store : fileSystem.getFileStores()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", store.getName());
            map.put("mount", store.getMount());
            map.put("type", store.getType());
            map.put("total", store.getTotalSpace());
            map.put("usable", store.getUsableSpace());
            map.put("usage", store.getTotalSpace() == 0 ? 0
                    : Math.round((store.getTotalSpace() - store.getUsableSpace()) * 10000.0 / store.getTotalSpace()) / 100.0);
            list.add(map);
        }
        return list;
    }

    private Map<String, Object> networkInfo(HardwareAbstractionLayer hal) {
        List<Map<String, Object>> interfaces = new ArrayList<>();
        long rx = 0;
        long tx = 0;
        for (NetworkIF net : hal.getNetworkIFs()) {
            net.updateAttributes();
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", net.getName());
            map.put("displayName", net.getDisplayName());
            map.put("mac", net.getMacaddr());
            map.put("ipv4", String.join(",", net.getIPv4addr()));
            map.put("ipv6", String.join(",", net.getIPv6addr()));
            map.put("speed", net.getSpeed());
            map.put("recv", net.getBytesRecv());
            map.put("sent", net.getBytesSent());
            rx += net.getBytesRecv();
            tx += net.getBytesSent();
            interfaces.add(map);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRecv", rx);
        result.put("totalSent", tx);
        result.put("interfaces", interfaces);
        return result;
    }

    private String safeHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String localIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private String formatDuration(long seconds) {
        long day = seconds / 86400;
        long hour = (seconds % 86400) / 3600;
        long minute = (seconds % 3600) / 60;
        return day + "天" + hour + "小时" + minute + "分";
    }
}
