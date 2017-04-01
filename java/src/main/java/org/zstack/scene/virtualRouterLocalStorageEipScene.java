package org.zstack.scene;

import org.zstack.sdk.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * Created by ZStack on 17-3-3.
 */
public class virtualRouterLocalStorageEipScene {
    public static String SHA(final String strText, final String strType){
        String strResult = null;
        if (strText != null && strText.length() > 0) {
            try {
                // SHA 加密开始
                // 创建加密对象 并传入加密类型
                MessageDigest messageDigest = MessageDigest.getInstance(strType);
                // 传入要加密的字符串
                messageDigest.update(strText.getBytes());
                // 得到 byte 类型结果
                byte byteBuffer[] = messageDigest.digest();
                // 将 byte 转换为 string
                StringBuffer strHexString = new StringBuffer();
                // 遍历 byte buffer
                for (int i = 0; i < byteBuffer.length; i++)
                {
                    String hex = Integer.toHexString(0xff & byteBuffer[i]);
                    if (hex.length() == 1)
                    {
                        strHexString.append('0');
                    }
                    strHexString.append(hex);
                }
                // 得到返回结果
                strResult = strHexString.toString();
            }
            catch (NoSuchAlgorithmException e)
            {
                e.printStackTrace();
            }
        }

        return strResult;
    }
    public static void main(String[] args) {

        //声明sessionId；每个action均需要sessionId
        String sessionId = null;

        //设置登录zstack的地址；通过172.20.12.4连接到部署zstack管理节点环境的主机
        ZSConfig.Builder zBuilder = new ZSConfig.Builder();
        zBuilder.setHostname("172.20.12.4");
        ZSClient.configure(zBuilder.build());

        //登录zstack；获取session
        LogInByAccountAction logInByAccountAction = new LogInByAccountAction();
        logInByAccountAction.accountName = "admin";
        logInByAccountAction.password = SHA("password", "SHA-512");
        LogInByAccountAction.Result logInByAccountActionRes = logInByAccountAction.call();

        if (logInByAccountActionRes.error == null) {
            System.out.println("logInByAccount successfully");
            sessionId = logInByAccountActionRes.value.getInventory().getUuid();
        } else logInByAccountActionRes.throwExceptionIfError();

        //创建区域
        CreateZoneAction createZoneAction = new CreateZoneAction();
        createZoneAction.name = "zone1";
        createZoneAction.description = "this is a zone";
        createZoneAction.sessionId = sessionId;
        ZoneInventory zone = createZoneAction.call().value.inventory;
        System.out.println(String.format("createZone:%s successfully", zone.name));

        //创建集群
        CreateClusterAction createClusterAction = new CreateClusterAction();
        createClusterAction.zoneUuid = zone.uuid;
        createClusterAction.name = "cluster1";
        createClusterAction.description = "this is a cluster";
        createClusterAction.hypervisorType = "KVM";
        createClusterAction.sessionId = sessionId;
        ClusterInventory cluster = createClusterAction.call().value.inventory;
        System.out.println(String.format("createCluster:%s successfully", cluster.name));

        //添加物理机；物理机IP应处于与公网IP不同的网络段，实现公有网络与管理网络分离
        AddKVMHostAction addKVMHostAction = new AddKVMHostAction();
        addKVMHostAction.name = "host1";
        addKVMHostAction.username = "root";
        addKVMHostAction.password = "password";
        addKVMHostAction.clusterUuid = cluster.uuid;
        addKVMHostAction.managementIp = "192.168.99.93";
        addKVMHostAction.sessionId = sessionId;
        HostInventory host = addKVMHostAction.call().value.inventory;
        System.out.println("addKVMHost successfully");

        //添加本地主存储
        AddLocalPrimaryStorageAction addLocalPrimaryStorageAction = new AddLocalPrimaryStorageAction();
        addLocalPrimaryStorageAction.url = "/zstack_ps";
        addLocalPrimaryStorageAction.name = "ps1";
        addLocalPrimaryStorageAction.zoneUuid = zone.uuid;
        addLocalPrimaryStorageAction.sessionId = sessionId;
        PrimaryStorageInventory primaryStorage = addLocalPrimaryStorageAction.call().value.inventory;
        System.out.println(String.format("addLocalPrimaryStorage:%s successfully",primaryStorage.name));

        //将本地主存储挂载到集群
        AttachPrimaryStorageToClusterAction attachPrimaryStorageToClusterAction = new AttachPrimaryStorageToClusterAction();
        attachPrimaryStorageToClusterAction.clusterUuid = cluster.uuid;
        attachPrimaryStorageToClusterAction.primaryStorageUuid = primaryStorage.uuid;
        attachPrimaryStorageToClusterAction.sessionId = sessionId;
        attachPrimaryStorageToClusterAction.call();
        System.out.println("attachPrimaryStorageToCluster successfully");

        //添加本地镜像存储
        AddImageStoreBackupStorageAction addImageStoreBackupStorageAction = new AddImageStoreBackupStorageAction();
        addImageStoreBackupStorageAction.hostname = "192.168.99.93";
        addImageStoreBackupStorageAction.username = "root";
        addImageStoreBackupStorageAction.password = "password";
        addImageStoreBackupStorageAction.url = "/zstack_bs";
        addImageStoreBackupStorageAction.name = "bs1";
        addImageStoreBackupStorageAction.sessionId = sessionId;
        ImageStoreBackupStorageInventory imageStoreBackupStoage = addImageStoreBackupStorageAction.call().value.inventory;
        System.out.println(String.format("addImageStoreBackupStorage:%s successfully",imageStoreBackupStoage.name));

        //将镜像存储挂载到区域；注意与主存储不同，这是由zstack的设计架构决定的
        AttachBackupStorageToZoneAction attachBackupStorageToZoneAction = new AttachBackupStorageToZoneAction();
        attachBackupStorageToZoneAction.zoneUuid = zone.uuid;
        attachBackupStorageToZoneAction.backupStorageUuid = imageStoreBackupStoage.uuid;
        attachBackupStorageToZoneAction.sessionId = sessionId;
        attachBackupStorageToZoneAction.call();
        System.out.println("attachBackupStorageToZone successfully");

        //添加镜像到本地镜像仓库
        AddImageAction addImageAction = new AddImageAction();
        addImageAction.name = "image1";
        addImageAction.url = "http://cdn.zstack.io/product_downloads/images/zstack-image.qcow2";
        addImageAction.format = "qcow2";
        addImageAction.backupStorageUuids = Collections.singletonList(imageStoreBackupStoage.uuid);
        addImageAction.sessionId = sessionId;
        ImageInventory image = addImageAction.call().value.inventory;
        System.out.println(String.format("addImage:%s successfully",image.name));

        //添加云路由镜像到本地镜像仓库
        AddImageAction addVRImageAction = new AddImageAction();
        addVRImageAction.name = "vrimage";
        addVRImageAction.url = "http://cdn.zstack.io/product_downloads/vrouter/vCenter-Vrouter-template-20170208.vmdk";
        addVRImageAction.format = "qcow2";
        addVRImageAction.system = true;
        addVRImageAction.backupStorageUuids = Collections.singletonList(imageStoreBackupStoage.uuid);
        addVRImageAction.sessionId = sessionId;
        ImageInventory VRImage = addVRImageAction.call().value.inventory;
        System.out.println(String.format("addImage:%s successfully", VRImage.name));


        //创建无服务L2NoVlan公有网络
        CreateL2NoVlanNetworkAction createL2NoVlanPublicNetworkAction = new CreateL2NoVlanNetworkAction();
        createL2NoVlanPublicNetworkAction.name = "public-l2";
        createL2NoVlanPublicNetworkAction.description = "this is a no-serivce network";
        createL2NoVlanPublicNetworkAction.zoneUuid = zone.uuid;
        createL2NoVlanPublicNetworkAction.physicalInterface = "eth1";
        createL2NoVlanPublicNetworkAction.sessionId = sessionId;
        L2NetworkInventory l2NoVlanPublicNetwork = createL2NoVlanPublicNetworkAction.call().value.inventory;
        System.out.println(String.format("createL2NoVlanPublicNetwork:%s successfully", l2NoVlanPublicNetwork.name));

        //挂载无服务L2NoVlan公有网络到集群
        AttachL2NetworkToClusterAction attachL2NoVlanPublicNetworkToClusterAction = new AttachL2NetworkToClusterAction();
        attachL2NoVlanPublicNetworkToClusterAction.l2NetworkUuid = l2NoVlanPublicNetwork.uuid;
        attachL2NoVlanPublicNetworkToClusterAction.clusterUuid = cluster.uuid;
        attachL2NoVlanPublicNetworkToClusterAction.sessionId = sessionId;
        attachL2NoVlanPublicNetworkToClusterAction.call();

        //创建无服务L2Vlan管理网络;云路由环境中用户需要手动创建管理网络，与扁平网络环境不同
        CreateL2NoVlanNetworkAction createL2NoVlanManagementNetworkAction = new CreateL2NoVlanNetworkAction();
        createL2NoVlanManagementNetworkAction.name = "management-l2";
        createL2NoVlanManagementNetworkAction.description = "this is a no-serivce network";
        createL2NoVlanManagementNetworkAction.zoneUuid = zone.uuid;
        createL2NoVlanManagementNetworkAction.physicalInterface = "eth0";
        createL2NoVlanManagementNetworkAction.sessionId = sessionId;
        L2NetworkInventory l2NoVlanManagmentNetwork = createL2NoVlanManagementNetworkAction.call().value.inventory;
        System.out.println(String.format("createL2NoVlanManagementNetwork:%s successfully", l2NoVlanManagmentNetwork.name));

        //挂载无服务L2NoVlan管理网络到集群
        AttachL2NetworkToClusterAction attachL2NoVlanManagementNetworkToClusterAction = new AttachL2NetworkToClusterAction();
        attachL2NoVlanManagementNetworkToClusterAction.l2NetworkUuid = l2NoVlanManagmentNetwork.uuid;
        attachL2NoVlanManagementNetworkToClusterAction.clusterUuid = cluster.uuid;
        attachL2NoVlanManagementNetworkToClusterAction.sessionId = sessionId;
        attachL2NoVlanManagementNetworkToClusterAction.call();

        //创建无服务L2Vlan私有网络
        CreateL2VlanNetworkAction createL2VlanPrivateNetworkAction = new CreateL2VlanNetworkAction();
        createL2VlanPrivateNetworkAction.vlan = 3000;
        createL2VlanPrivateNetworkAction.name = "private-l2";
        createL2VlanPrivateNetworkAction.description = "this is a l2-vlan network";
        createL2VlanPrivateNetworkAction.zoneUuid = zone.uuid;
        createL2VlanPrivateNetworkAction.physicalInterface = "eth1";
        createL2VlanPrivateNetworkAction.sessionId = sessionId;
        L2NetworkInventory l2VlanPrivateNetwork = createL2VlanPrivateNetworkAction.call().value.inventory;
        System.out.println(String.format("createL2VlanPrivateNetwork:%s successfully", l2VlanPrivateNetwork.name));

        //挂载无服务L2Vlan私有网络到集群
        AttachL2NetworkToClusterAction attachL2VlanPirvateNetworkToClusterAction = new AttachL2NetworkToClusterAction();
        attachL2VlanPirvateNetworkToClusterAction.l2NetworkUuid = l2VlanPrivateNetwork.uuid;
        attachL2VlanPirvateNetworkToClusterAction.clusterUuid = cluster.uuid;
        attachL2VlanPirvateNetworkToClusterAction.sessionId = sessionId;
        attachL2VlanPirvateNetworkToClusterAction.call();

        //基于L2NoVlan公有网络创建L3公有网络
        CreateL3NetworkAction createL3PublicNetworkAction = new CreateL3NetworkAction();
        createL3PublicNetworkAction.name = "public-l3";
        createL3PublicNetworkAction.type = "L3BasicNetwork";
        createL3PublicNetworkAction.l2NetworkUuid = l2NoVlanPublicNetwork.uuid;
        createL3PublicNetworkAction.system = false;
        createL3PublicNetworkAction.sessionId = sessionId;
        L3NetworkInventory l3PublicNetwork = createL3PublicNetworkAction.call().value.inventory;
        System.out.println(String.format("createL3PublicNetwork:%s successfully", l3PublicNetwork.name));

        //挂载IP地址段到公有网络public-l3；
        AddIpRangeAction addPublicIpRangeAction = new AddIpRangeAction();
        addPublicIpRangeAction.l3NetworkUuid = l3PublicNetwork.uuid;
        addPublicIpRangeAction.name = "iprange1";
        addPublicIpRangeAction.startIp = "10.101.20.2";
        addPublicIpRangeAction.endIp = "10.101.20.254";
        addPublicIpRangeAction.netmask = "255.0.0.0";
        addPublicIpRangeAction.gateway = "10.0.0.1";
        addPublicIpRangeAction.sessionId = sessionId;
        addPublicIpRangeAction.call();

        //基于L2Vlan管理网络创建L3管理网络
        CreateL3NetworkAction createL3MangementNetworkAction = new CreateL3NetworkAction();
        createL3MangementNetworkAction.name = "management-l3";
        createL3MangementNetworkAction.type = "L3BasicNetwork";
        createL3MangementNetworkAction.l2NetworkUuid = l2NoVlanManagmentNetwork.uuid;
        createL3MangementNetworkAction.system = false;
        createL3MangementNetworkAction.sessionId = sessionId;
        L3NetworkInventory l3MangementNetwork = createL3MangementNetworkAction.call().value.inventory;
        System.out.println(String.format("createL3ManagementNetwork:%s successfully", l3PublicNetwork.name));

        //挂载IP地址段到管理网络management-l3；注意此处挂载的网络段须与物理机实际配置的IP相匹配
        AddIpRangeAction addManagementIpRangeAction = new AddIpRangeAction();
        addManagementIpRangeAction.l3NetworkUuid = l3MangementNetwork.uuid;
        addManagementIpRangeAction.name = "iprange2";
        addManagementIpRangeAction.startIp = "192.168.99.200";
        addManagementIpRangeAction.endIp = "192.168.99.210";
        addManagementIpRangeAction.netmask = "255.255.255.0";
        addManagementIpRangeAction.gateway = "192.168.99.1";
        addManagementIpRangeAction.sessionId = sessionId;
        addManagementIpRangeAction.call();

        //基于L2Vlan网络 创建L3私有网络
        CreateL3NetworkAction createL3PrivateNetworkAction = new CreateL3NetworkAction();
        createL3PrivateNetworkAction.name = "private-l3";
        createL3PrivateNetworkAction.type = "L3BasicNetwork";
        createL3PrivateNetworkAction.l2NetworkUuid = l2VlanPrivateNetwork.uuid;
        createL3PrivateNetworkAction.system = false;
        createL3PrivateNetworkAction.sessionId = sessionId;
        L3NetworkInventory l3PrivateNetwork = createL3PrivateNetworkAction.call().value.inventory;
        System.out.println(String.format("createl3PrivaetNetwork:%s successfully", l3PrivateNetwork.name));

        //挂载Ip地址段到私有网络private-l3；这里使用CIDR方式，与上文中的直接挂载IP地址段效果相同
        AddIpRangeByNetworkCidrAction addPrivateIpRangeByNetworkCidrAction = new AddIpRangeByNetworkCidrAction();
        addPrivateIpRangeByNetworkCidrAction.name = "iprange3";
        addPrivateIpRangeByNetworkCidrAction.l3NetworkUuid = l3PrivateNetwork.uuid;
        addPrivateIpRangeByNetworkCidrAction.networkCidr = "192.168.100.0/24";
        addPrivateIpRangeByNetworkCidrAction.sessionId = sessionId;
        addPrivateIpRangeByNetworkCidrAction.call();

        //为私有网络private-l3添加DNS服务
        AddDnsToL3NetworkAction addDnsToL3NetworkAction = new AddDnsToL3NetworkAction();
        addDnsToL3NetworkAction.l3NetworkUuid = l3PrivateNetwork.uuid;
        addDnsToL3NetworkAction.dns = "8.8.8.8";
        addDnsToL3NetworkAction.sessionId = sessionId;
        addDnsToL3NetworkAction.call();

        //获取云路由网络服务的Uuid
        QueryNetworkServiceProviderAction queryNetworkServiceProviderAction = new QueryNetworkServiceProviderAction();
        queryNetworkServiceProviderAction.conditions = Collections.singletonList("type=vrouter");
        queryNetworkServiceProviderAction.sessionId = sessionId;
        NetworkServiceProviderInventory networkServiceProvider = queryNetworkServiceProviderAction.call().value.inventories.get(0);
        System.out.println(String.format("queryNetworkServiceprovider:%s successfully", networkServiceProvider.getName()));
        Map<String, List<String>> networkServices = new HashMap<String, List<String>>();
        networkServices.put(networkServiceProvider.uuid, asList("DHCP", "Eip", "DNS","SNAT"));

        //为私有网络private-l3添加网络服务
        AttachNetworkServiceToL3NetworkAction attachNetworkServiceToL3NetworkAction = new AttachNetworkServiceToL3NetworkAction();
        attachNetworkServiceToL3NetworkAction.l3NetworkUuid = l3PrivateNetwork.uuid;
        attachNetworkServiceToL3NetworkAction.networkServices = networkServices;
        attachNetworkServiceToL3NetworkAction.sessionId = sessionId;
        attachNetworkServiceToL3NetworkAction.call();
        System.out.println("attachNetworkServiceToL3Network successfully");

        //创建计算规格
        CreateInstanceOfferingAction createInstanceOfferingAction = new CreateInstanceOfferingAction();
        createInstanceOfferingAction.name = "instanceoffering1";
        createInstanceOfferingAction.cpuNum = 2;
        createInstanceOfferingAction.memorySize = 2148000000l;
        createInstanceOfferingAction.sessionId = sessionId;
        InstanceOfferingInventory instanceOffering = createInstanceOfferingAction.call().value.inventory;
        System.out.println(String.format("createInstanceOffering:%s successfully", instanceOffering.name));

        //创建云盘规格
        CreateDiskOfferingAction createDiskOfferingAction = new CreateDiskOfferingAction();
        createDiskOfferingAction.name = "diskOffering1";
        createDiskOfferingAction.diskSize = 2148000000l;
        createDiskOfferingAction.sessionId = sessionId;
        DiskOfferingInventory diskOffering = createDiskOfferingAction.call().value.inventory;
        System.out.println(String.format("createDiskOffering:%s successfully", diskOffering.name));

        //创建云路由规格；推荐管理网络、公有网络隔离
        CreateVirtualRouterOfferingAction createVirtualRouterOfferingAction = new CreateVirtualRouterOfferingAction();
        createVirtualRouterOfferingAction.zoneUuid = zone.uuid;
        createVirtualRouterOfferingAction.managementNetworkUuid = l3MangementNetwork.uuid;
        createVirtualRouterOfferingAction.publicNetworkUuid = l3PublicNetwork.uuid;
        createVirtualRouterOfferingAction.imageUuid = VRImage.uuid;
        createVirtualRouterOfferingAction.name = "vr1";
        createVirtualRouterOfferingAction.cpuNum = 4;
        createVirtualRouterOfferingAction.memorySize = 2148000000l;
        createVirtualRouterOfferingAction.sessionId = sessionId;
        createVirtualRouterOfferingAction.call();
        System.out.println("createVirtualRouterOffering successfully");

        //创建虚拟机
        CreateVmInstanceAction createVmInstanceAction = new CreateVmInstanceAction();
        createVmInstanceAction.name = "vm1";
        createVmInstanceAction.instanceOfferingUuid = instanceOffering.uuid;
        createVmInstanceAction.imageUuid = image.uuid;
        createVmInstanceAction.l3NetworkUuids = Collections.singletonList(l3PrivateNetwork.uuid);
        createVmInstanceAction.dataDiskOfferingUuids = Collections.singletonList(diskOffering.uuid);
        createVmInstanceAction.clusterUuid = cluster.uuid;
        createVmInstanceAction.description = "this is a vm";
        createVmInstanceAction.sessionId = sessionId;
        VmInstanceInventory vm = createVmInstanceAction.call().value.inventory;
        System.out.println(String.format("createVm:%s successfully", vm.name));

        //基于公有网络 创建Vip,为Eip作准备
        CreateVipAction createVipAction = new CreateVipAction();
        createVipAction.name = "vip1";
        createVipAction.l3NetworkUuid = l3PublicNetwork.uuid;
        createVipAction.sessionId = sessionId;
        VipInventory vip = createVipAction.call().value.inventory;
        System.out.println(String.format("createVip:%s successfully", vip.name));

        //创建Eip
        CreateEipAction createEipAction = new CreateEipAction();
        createEipAction.name = "eip1";
        createEipAction.vipUuid = vip.uuid;
        createEipAction.vmNicUuid = vm.vmNics.get(0).uuid;
        createEipAction.sessionId = sessionId;
        EipInventory eip = createEipAction.call().value.inventory;
        System.out.println(String.format("createEip:%s successfully", eip.name));

        //挂载Eip到虚拟机
        AttachEipAction attachEipAction = new AttachEipAction();
        attachEipAction.eipUuid = eip.uuid;
        attachEipAction.vmNicUuid = vm.vmNics.get(0).uuid;
        attachEipAction.sessionId = sessionId;
        attachEipAction.call();
    }
}
