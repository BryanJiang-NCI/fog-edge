package org.fog.smart;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.Application;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.utils.FogUtils;
import org.fog.placement.Controller;
import org.fog.placement.ModulePlacementMapping;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.placement.ModuleMapping;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.fog.scheduler.StreamOperatorScheduler;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Storage;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.entities.Tuple;
import org.fog.application.AppEdge;
import org.cloudbus.cloudsim.Log;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import java.util.*;

public class SmartMeter {

    static List<FogDevice> fogDevices = new ArrayList<>();
    static Map<String, Integer> appModulesMap = new HashMap<>();
    static final String APPLICATION_ID = "SmartMeterApp";

    public static void main(String[] args) {
        Log.printLine("Starting Smart Meter Demo...");
        try {
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            // 1. 创建Broker
            FogBroker broker = new FogBroker("broker");

            // 2. 创建各节点
            FogDevice edgeNode = createFogDevice("Edge-Node", 5000, 4000, 10000, 10000, 0, 0.01, 100.0, 100.0);
            fogDevices.add(edgeNode);

            FogDevice gateway = createFogDevice("Gateway", 2000, 1000, 1000, 1000, 1, 0.0, 100.0, 100.0);
            fogDevices.add(gateway);

            // 指定层级关系
            gateway.setParentId(edgeNode.getId());

            // 3. 设备（传感器）
            Sensor waterMeter = new Sensor("WaterMeter", "WATER_SENSOR", broker.getId(), APPLICATION_ID, new DeterministicDistribution(30.0));
            Sensor electricityMeter = new Sensor("ElectricityMeter", "ELEC_SENSOR", broker.getId(), APPLICATION_ID, new DeterministicDistribution(30.0));
            waterMeter.setGatewayDeviceId(gateway.getId());
            electricityMeter.setGatewayDeviceId(gateway.getId());
            waterMeter.setLatency(2.0);
            electricityMeter.setLatency(2.0);

            // 4. 应用和模块
            Application app = createApp(broker.getId());

            appModulesMap.put("WaterAppModule", gateway.getId());
            appModulesMap.put("ElecAppModule", gateway.getId());
            appModulesMap.put("EdgeAnalysis", edgeNode.getId());

            waterMeter.setApp(app);
            electricityMeter.setApp(app);

            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
            moduleMapping.addModuleToDevice("WaterAppModule", "Gateway");
            moduleMapping.addModuleToDevice("ElecAppModule", "Gateway");
            moduleMapping.addModuleToDevice("EdgeAnalysis", "Edge-Node");

            // 5. 控制器
            Controller controller = new Controller("controller", fogDevices, new ArrayList<>(), new ArrayList<>());
            controller.submitApplication(app, 0, new ModulePlacementMapping(fogDevices, app, moduleMapping));

            System.out.println("Simulation started...");
            CloudSim.startSimulation();
            CloudSim.stopSimulation();
            System.out.println("Simulation finished.");

            String examplePayload = "{"
            	    + "\"timestamp\":\"" + System.currentTimeMillis() + "\","
            	    + "\"device_type\":\"water_meter\","
            	    + "\"device_id\":\"WM1001\","
            	    + "\"reading\":15.7,"
            	    + "\"unit\":\"L\","
            	    + "\"battery\":92,"
            	    + "\"location\":\"Apt-305\","
            	    + "\"status\":\"normal\","
            	    + "\"anomaly\":false"
            	    + "}";

            	// 实际应用时可循环、批量、从仿真输出构造 payload
            httpUploadToAWS(examplePayload);
            System.out.println("Smart Meter Demo finished!");

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    // FogDevice 工厂
    private static FogDevice createFogDevice(String nodeName, long mips, int ram, long upBw, long downBw, int level,
                                             double ratePerMips, double busyPower, double idlePower) {
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerSimple(mips)));

        int hostId = FogUtils.generateEntityId();
        int storage = 1000000;
        int bw = 10000;

        PowerHost host = new PowerHost(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw),
                storage,
                peList,
                new StreamOperatorScheduler(peList),
                new PowerModelLinear(busyPower, idlePower)
        );

        List<Host> hostList = new ArrayList<>();
        hostList.add(host);

        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;
        LinkedList<Storage> storageList = new LinkedList<>();

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, host, time_zone, cost, costPerMem,
                costPerStorage, costPerBw
        );

        FogDevice device = null;

        try {
            device = new FogDevice(
                    nodeName,
                    characteristics,
                    new AppModuleAllocationPolicy(hostList),
                    storageList,
                    1.0,
                    (double) upBw,
                    (double) downBw,
                    0.01,
                    ratePerMips
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        device.setLevel(level);
        return device;
    }

    // 应用定义
    private static Application createApp(int userId) {
        Application application = Application.createApplication(APPLICATION_ID, userId);
        application.addAppModule("WaterAppModule", 1000);
        application.addAppModule("ElecAppModule", 1000);
        application.addAppModule("EdgeAnalysis", 5000);

        // 数据流配置：传感器->网关->边缘节点
        application.addAppEdge("WATER_SENSOR", "WaterAppModule", 100000, 20000, "WATER_SENSOR", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("ELEC_SENSOR", "ElecAppModule", 100000, 20000, "ELEC_SENSOR", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("WaterAppModule", "EdgeAnalysis", 1000, 200, "WATER_AGG", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("ElecAppModule", "EdgeAnalysis", 1000, 200, "ELEC_AGG", Tuple.UP, AppEdge.MODULE);

        // 可以在这里继续定义数据返回/控制流
        return application;
    }
    
    // 在 SmartMeter 类内部加
    public static void httpUploadToAWS(String jsonPayload) {
        try {
            String endpoint = "https://your-aws-endpoint.com/iot/upload"; // 改成你的API地址
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);

            try(OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            int code = conn.getResponseCode();
            System.out.println("网关模拟数据已上传，HTTP返回状态：" + code);
            conn.disconnect();
        } catch (Exception ex) {
            System.err.println("上传异常：" + ex.getMessage());
        }
    }

}
