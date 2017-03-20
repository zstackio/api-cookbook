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
public class flatNetworkLocalStorageEipScene {

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
    public static void main(String[] args){

        //声明sessionId；每个action均需要sessionId
        String sessionId = null;

        //设置登录zstack的地址；通过172.20.12.4连接到部署zstack管理节点环境的主机
        ZSConfig.Builder zBuilder = new ZSConfig.Builder();
        zBuilder.setHostname("172.20.12.4");
        ZSClient.configure(zBuilder.build());

        //登录zstack；获取session
        LogInByAccountAction logInByAccountAction = new LogInByAccountAction();
        logInByAccountAction.accountName = "admin";
        logInByAccountAction.password = SHA("password","SHA-512");
        LogInByAccountAction.Result logInByAccountActionRes = logInByAccountAction.call();

        if(logInByAccountActionRes.error ==null) {
            System.out.println("logInByAccount successfully");
            sessionId = logInByAccountActionRes.value.getInventory().getUuid();
        }
        else logInByAccountActionRes.throwExceptionIfError();

        //创建区域
        CreateZoneAction createZoneAction = new CreateZoneAction();
        createZoneAction.name = "zone1";
        createZoneAction.description = "this is a zone";
        createZoneAction.sessionId = sessionId;
        ZoneInventory zone = createZoneAction.call().value.inventory;
        System.out.println(String.format("createZone:%s successfully",zone.name));

        //创建集群
        CreateClusterAction createClusterAction = new CreateClusterAction();
        createClusterAction.zoneUuid = zone.uuid;
        createClusterAction.name = "cluster1";
        createClusterAction.description = "this is a cluster";
        createClusterAction.hypervisorType = "KVM";
        createClusterAction.sessionId = sessionId;
        ClusterInventory cluster = createClusterAction.call().value.inventory;
        System.out.println(String.format("createCluster:%s successfully",cluster.name));

        //添加物理机；物理机IP应处于与公网IP不同的网络段，作为管理网络，实现公有网络与管理网络分离
        AddKVMHostAction addKVMHostAction = new AddKVMHostAction();
        addKVMHostAction.name="host1";
        addKVMHostAction.username = "root";
        addKVMHostAction.password = "password";
        addKVMHostAction.clusterUuid = cluster.uuid;
        addKVMHostAction.managementIp= "192.168.99.93";
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

        //添加本地镜像存储；笔者测试环境中部署ZStack、主存储、镜像存储三者实质是同一台物理机
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
        addImageAction.url = "http://cdn.zstack.io/product_downloads/iso/ZStack-Enterprise-x86_64-DVD-1.9.0.iso";
        addImageAction.format = "qcow2";
        addImageAction.backupStorageUuids = Collections.singletonList(imageStoreBackupStoage.uuid);
        addImageAction.sessionId = sessionId;
        ImageInventory image = addImageAction.call().value.inventory;
        System.out.println(String.format("addImage:%s successfully",image.name));

        //创建无服务L2NoVlan公有网络
        CreateL2NoVlanNetworkAction createL2NoVlanNetworkAction = new CreateL2NoVlanNetworkAction();
        createL2NoVlanNetworkAction.name = "public-l2";
        createL2NoVlanNetworkAction.description = "this is a no-serivce network";
        createL2NoVlanNetworkAction.zoneUuid = zone.uuid;
        createL2NoVlanNetworkAction.physicalInterface = "eth1";
        createL2NoVlanNetworkAction.sessionId = sessionId;
        L2NetworkInventory l2NoVlanNetwork = createL2NoVlanNetworkAction.call().value.inventory;
        System.out.println(String.format("createL2NoVlanNetwork:%s successfully",l2NoVlanNetwork.name));

        //挂载无服务L2NoVlan公有网络到集群
        AttachL2NetworkToClusterAction attachL2NoVlanNetworkToClusterAction = new AttachL2NetworkToClusterAction();
        attachL2NoVlanNetworkToClusterAction.l2NetworkUuid = l2NoVlanNetwork.uuid;
        attachL2NoVlanNetworkToClusterAction.clusterUuid = cluster.uuid;
        attachL2NoVlanNetworkToClusterAction.sessionId = sessionId;
        attachL2NoVlanNetworkToClusterAction.call();

        //创建无服务L2Vlan私有网络
        CreateL2VlanNetworkAction createL2VlanNetworkAction = new CreateL2VlanNetworkAction();
        createL2VlanNetworkAction.vlan = 3000;
        createL2VlanNetworkAction.name = "private-l2";
        createL2VlanNetworkAction.description = "this is a l2-vlan network";
        createL2VlanNetworkAction.zoneUuid = zone.uuid;
        createL2VlanNetworkAction.physicalInterface = "eth1";
        createL2VlanNetworkAction.sessionId = sessionId;
        L2NetworkInventory l2VlanNetwork = createL2VlanNetworkAction.call().value.inventory;
        System.out.println(String.format("createL2VlanNetwork:%s successfully",l2VlanNetwork.name));

        //挂载无服务L2Vlan私有网络到集群
        AttachL2NetworkToClusterAction attachL2VlanNetworkToClusterAction = new AttachL2NetworkToClusterAction();
        attachL2VlanNetworkToClusterAction.l2NetworkUuid = l2VlanNetwork.uuid;
        attachL2VlanNetworkToClusterAction.clusterUuid = cluster.uuid;
        attachL2VlanNetworkToClusterAction.sessionId = sessionId;
        attachL2VlanNetworkToClusterAction.call();

        //基于L2NoVlan公有网络创建L3公有网络
        CreateL3NetworkAction createL3PublicNetworkAction = new CreateL3NetworkAction();
        createL3PublicNetworkAction.name = "public-l3";
        createL3PublicNetworkAction.type = "L3BasicNetwork";
        createL3PublicNetworkAction.l2NetworkUuid = l2NoVlanNetwork.uuid;
        createL3PublicNetworkAction.system = false;
        createL3PublicNetworkAction.sessionId = sessionId;
        L3NetworkInventory l3PublicNetwork = createL3PublicNetworkAction.call().value.inventory;
        System.out.println(String.format("createL2VlanNetwork:%s successfully",l3PublicNetwork.name));

        //挂载IP地址段到l3公有网络；
        AddIpRangeAction addIpRangeAction = new AddIpRangeAction();
        addIpRangeAction.l3NetworkUuid = l3PublicNetwork.uuid;
        addIpRangeAction.name = "iprange1";
        addIpRangeAction.startIp = "10.101.50.2";
        addIpRangeAction.endIp = "10.101.50.254";
        addIpRangeAction.netmask = "255.0.0.0";
        addIpRangeAction.gateway = "10.0.0.1";
        addIpRangeAction.sessionId = sessionId;
        addIpRangeAction.call();

        //基于L2Vlan私有网络 创建L3私有网络
        CreateL3NetworkAction createL3PrivateNetworkAction = new CreateL3NetworkAction();
        createL3PrivateNetworkAction.name = "private-l3";
        createL3PrivateNetworkAction.type = "L3BasicNetwork";
        createL3PrivateNetworkAction.l2NetworkUuid = l2VlanNetwork.uuid;
        createL3PrivateNetworkAction.system = false;
        createL3PrivateNetworkAction.sessionId = sessionId;
        L3NetworkInventory l3PrivateNetwork = createL3PrivateNetworkAction.call().value.inventory;
        System.out.println(String.format("createL2VlanNetwork:%s successfully",l3PrivateNetwork.name));

        //挂载Ip地址段到l3私有网络；这里使用CIDR方式，与上文中的直接挂载IP地址段效果相同
        AddIpRangeByNetworkCidrAction addIpRangeByNetworkCidrAction = new AddIpRangeByNetworkCidrAction();
        addIpRangeByNetworkCidrAction.name = "iprange2";
        addIpRangeByNetworkCidrAction.l3NetworkUuid = l3PrivateNetwork.uuid;
        addIpRangeByNetworkCidrAction.networkCidr = "192.168.100.0/24";
        addIpRangeByNetworkCidrAction.sessionId = sessionId;
        addIpRangeByNetworkCidrAction.call();

        //为私有网络private-l3添加DNS服务
        AddDnsToL3NetworkAction addDnsToL3NetworkAction = new AddDnsToL3NetworkAction();
        addDnsToL3NetworkAction.l3NetworkUuid = l3PrivateNetwork.uuid;
        addDnsToL3NetworkAction.dns = "8.8.8.8";
        addDnsToL3NetworkAction.sessionId = sessionId;
        addDnsToL3NetworkAction.call();

        //获取扁平网络服务的Uuid
        QueryNetworkServiceProviderAction queryNetworkServiceProviderAction = new QueryNetworkServiceProviderAction();
        queryNetworkServiceProviderAction.conditions = Collections.singletonList("type=Flat");
        queryNetworkServiceProviderAction.sessionId = sessionId;
        NetworkServiceProviderInventory networkServiceProvider = queryNetworkServiceProviderAction.call().value.inventories.get(0);
        System.out.println(String.format("queryNetworkServiceprovider:%s successfully",networkServiceProvider.getName()));
        Map<String,List<String>>  networkServices = new HashMap<String, List<String>>();
        networkServices.put(networkServiceProvider.uuid, asList("DHCP", "Eip", "Userdata"));

        //为私有网络添加扁平网络服务
        AttachNetworkServiceToL3NetworkAction attachNetworkServiceToL3NetworkAction = new AttachNetworkServiceToL3NetworkAction();
        attachNetworkServiceToL3NetworkAction.l3NetworkUuid = l3PrivateNetwork.uuid;
        attachNetworkServiceToL3NetworkAction.networkServices = networkServices;
        attachNetworkServiceToL3NetworkAction.sessionId = sessionId;
        attachNetworkServiceToL3NetworkAction.call();
        System.out.println("attachNetworkServiceToL3Network successfully");

        //创建计算规格
        CreateInstanceOfferingAction createInstanceOfferingAction = new CreateInstanceOfferingAction();
        createInstanceOfferingAction.name = "instanceoffering1";
        createInstanceOfferingAction.cpuNum = 1;
        createInstanceOfferingAction.memorySize = 2148000000l;
        createInstanceOfferingAction.sessionId = sessionId;
        InstanceOfferingInventory instanceOffering = createInstanceOfferingAction.call().value.inventory;
        System.out.println(String.format("createInstanceOffering:%s successfully",instanceOffering.name));

        //创建云盘规格
        CreateDiskOfferingAction createDiskOfferingAction = new CreateDiskOfferingAction();
        createDiskOfferingAction.name = "diskOffering1";
        createDiskOfferingAction.diskSize = 2148000000l;
        createDiskOfferingAction.sessionId = sessionId;
        DiskOfferingInventory diskOffering = createDiskOfferingAction.call().value.inventory;
        System.out.println(String.format("createDiskOffering:%s successfully",diskOffering.name));

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
        System.out.println(String.format("createVm:%s successfully",vm.name));

        //基于公有网络public-l3 创建Vip,为Eip作准备
        CreateVipAction createVipAction = new CreateVipAction();
        createVipAction.name = "vip1";
        createVipAction.l3NetworkUuid = l3PublicNetwork.uuid;
        createVipAction.sessionId = sessionId;
        VipInventory vip = createVipAction.call().value.inventory;
        System.out.println(String.format("createVip:%s successfully",vip.name));

        //创建Eip
        CreateEipAction createEipAction = new CreateEipAction();
        createEipAction.name = "eip1";
        createEipAction.vipUuid = vip.uuid;
        createEipAction.vmNicUuid = vm.vmNics.get(0).uuid;
        createEipAction.sessionId = sessionId;
        EipInventory eip = createEipAction.call().value.inventory;
        System.out.println(String.format("createEip:%s successfully",eip.name));

        //挂载Eip到虚拟机
        AttachEipAction attachEipAction = new AttachEipAction();
        attachEipAction.eipUuid = eip.uuid;
        attachEipAction.vmNicUuid = vm.vmNics.get(0).uuid;
        attachEipAction.sessionId = sessionId;
        attachEipAction.call();

    }
}
