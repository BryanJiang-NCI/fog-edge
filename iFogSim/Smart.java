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
import org.cloudbus.cloudsim.Log;
import org.fog.application.AppEdge;
import org.fog.entities.Tuple;
import org.fog.smart.MyFogBroker;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SmartMeter {

    static List<FogDevice> fogDevices = new ArrayList<>();
    static final String APPLICATION_ID = "SmartMeterApp";

    public static void main(String[] args) {
        Log.printLine("Starting Smart Meter Simulation...");

        try {
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            // 1. create broker
            MyFogBroker broker = new MyFogBroker("broker");

            // 2. create edge gateway
            FogDevice gateway = createFogDevice("Gateway", 2000, 100000, 10000, 10000, 1, 0.0, 100.0, 100.0);
            fogDevices.add(gateway);

            // 3. create two type of sensor: water and electricity meters
            Sensor waterMeter = new Sensor("WaterMeter", "WATER_SENSOR", broker.getId(), APPLICATION_ID, new DeterministicDistribution(300.0));
            Sensor electricityMeter = new Sensor("ElectricityMeter", "ELEC_SENSOR", broker.getId(), APPLICATION_ID, new DeterministicDistribution(300.0));
            
            // 4. set the device id
            waterMeter.setGatewayDeviceId(gateway.getId());
            electricityMeter.setGatewayDeviceId(gateway.getId());
            
            // 5. set the latency for sensors
            waterMeter.setLatency(1.0);
            electricityMeter.setLatency(1.0);

            // 6. add application and modules
            Application app = createApp(broker.getId());
            waterMeter.setApp(app);
            electricityMeter.setApp(app);

            // 7. add mapping relationship
            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
            moduleMapping.addModuleToDevice("WaterAppModule", "Gateway");
            moduleMapping.addModuleToDevice("ElecAppModule", "Gateway");

            // 8. controller
            Controller controller = new Controller("controller", fogDevices, new ArrayList<>(), new ArrayList<>());
            controller.submitApplication(app, 0, new ModulePlacementMapping(fogDevices, app, moduleMapping));
            
            // 9. simulate data the gateway upload data to the edge
            CloudSim.send(broker.getId(), broker.getId(), 2000, MyFogBroker.UPLOAD_DATA_NOW, null);

            System.out.println("------for edge ca project-------");
            CloudSim.startSimulation();

            System.out.println("Smart Meter finished!");

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    // create fog device method
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

    // applciation define include sensor and edge gateway
    private static Application createApp(int userId) {
        Application application = Application.createApplication(APPLICATION_ID, userId);
        application.addAppModule("WaterAppModule", 256);
        application.addAppModule("ElecAppModule", 256);
        application.addAppEdge("WATER_SENSOR", "WaterAppModule", 100, 100, "WATER_SENSOR", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("ELEC_SENSOR", "ElecAppModule", 100, 100, "ELEC_SENSOR", Tuple.UP, AppEdge.SENSOR);
        return application;
    }
}
