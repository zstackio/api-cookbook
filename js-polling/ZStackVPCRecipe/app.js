const request = require('request');
const requestPromise = require('request-promise');



const url = 'http://172.20.12.229:8080/zstack/v1/accounts/login';
const ipAddress = 'http://172.20.12.229:8080/zstack';
const ipAddressT = "172.20.12.229";
const loginView = {
  "logInByAccount": {
    "password": "b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86",
    "accountName": "admin"
  }
}
const password = "b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86";
const accountName = "admin";

var sessionUuid = "mession";
var zoneInventory, clusterInventory;;
const server = '172.20.12.229:8080';
var allInventor = {
  zone: "",
  cluster: "",
  kvmHost: "",
  localPrimaryStorage: "",
  sftpBackupStorage: "",
  attachPrimaryStorageToCluste: "",
  attachBAckupStorageToZone: "",
  image: "",
  l2NoVlanNetwork: "",
  l3PublicNetwork: "",
  l3PublicNetworkIpRange: "",
  eip: "",
  vip: "",
  vMInstance: "",
  instanceOffering: "",
  attachNetworkServiceToL3Network: "",
  l3PrivateNetworkDns: "",
  l3PrivateNetworkIpRange: "",
  l3PrivateNetwork: "",
  attachL2NetworkToCluster: "",
  l2VlanNetwork: "",
  publicL3NetworkDns: "",
  attachL2NoVlanNetworkToCluster: "",
  networkServiceProvider: "",
  routerImage: "",
  virtualRouterOffering: ""
};

var polling = (responsed, sessionUuid, inventory, callback = "") => {
  if (responsed.statusCode == 202) {
    var url = responsed.body.location.replace(/[0-9]+(?:\.[0-9]+){3}:[0-9]+/, server);
    let interval = setInterval(() => {
      requestPromise({
          method: 'GET',
          url: url,
          headers: {
            'Authorization': 'OAuth ' + sessionUuid
          },
          resolveWithFullResponse: true,
          json: true
        })
        .then((response) => {
          if (response.statusCode == 200) {
            allInventor[inventory] = response.body.inventory;
            if (callback)
              callback(ipAddress, sessionUuid);
            clearInterval(interval);
          }

        })
    }, 1000);
  } else if (responsed.statusCode == 200) {
    allInventor[inventory] = responsed.body.inventories;
    if (callback)
      callback(ipAddress, sessionUuid);
  } else
    console.log("Error:" + responsed.err);
}

var login = (ipAddress, password, accountName) => {
  return requestPromise({
    method: 'PUT',
    url: ipAddress + '/v1/accounts/login',
    body: {
      "logInByAccount": {
        "password": password,
        "accountName": accountName
      }
    },
    json: true
  })
}



login(ipAddress, password, accountName)
  .then((parsedBody) => {
    sessionUuid = parsedBody.inventory.uuid;
    createZone(ipAddress, sessionUuid);
  })

var createZone = (ipAddress, sessionUuid) => {
  requestPromise({
      method: 'POST',
      url: ipAddress + '/v1/zones',
      body: {
        "params": {
          "name": "ZoneTest",
          "description": "test zone"
        },
        "systemTags": [],
        "userTages": []
      },
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "zone", createCluster);
    })
};

var createCluster = (ipAddress, sessionUuid) => {
  requestPromise({
      method: 'POST',
      url: ipAddress + '/v1/clusters',
      body: {
        "params": {
          "zoneUuid": allInventor.zone.uuid,
          "name": "ClusterTest",
          "description": "just test",
          "hypervisorType": "KVM"
        },
        "systemTags": [],
        "userTages": []
      },
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true,
    })
    .then((response) => {
      polling(response, sessionUuid, "cluster", addKvmHost);
    })
}

var addKvmHost = (ipAddress, sessionUuid) => {
  requestPromise({
      method: 'POST',
      url: ipAddress + '/v1/hosts/kvm',
      body: {
        "params": {
          "name": "HostTest",
          "clusterUuid": allInventor.cluster.uuid,
          "managementIp": ipAddressT,
          "username": "root",
          "password": "password",
          "sshPort": 22.0
        },
        "systemTags": [],
        "userTages": []
      },
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "kvmHost", addLocalPrimaryStorage);
    })
}

var addLocalPrimaryStorage = (ipAddress, sessionUuid) => {
  requestPromise({
      method: "POST",
      url: ipAddress + '/v1/primary-storage/local-storage',
      body: {
        "params": {
          "zoneUuid": allInventor.zone.uuid,
          "url": '/zstack_ps',
          "name": "PSTest",
        },
        "systemTags": [],
        "userTages": []
      },
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "localPrimaryStorage", attachPrimaryStorageToCluster);
    })
}

var attachPrimaryStorageToCluster = (ipAddress, sessionUuid) => {
  requestPromise({
      method: 'POST',
      url: ipAddress + '/v1/clusters/' + allInventor.cluster.uuid + '/primary-storage/' + allInventor.localPrimaryStorage.uuid,
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "attachPrimaryStorageToCluste", addSftpBackupStorage);
    })
}

var addSftpBackupStorage = (ipAddress, sessionUuid) => {
  requestPromise({
      method: "POST",
      url: ipAddress + "/v1/backup-storage/sftp",
      body: {
        "params": {
          "hostname": ipAddressT,
          "username": "root",
          "password": "password",
          "sshPort": 22.0,
          "url": "/home/sftpBAckupStorage",
          "name": "SftpTest",
          "importImage": false
        },
        "systemTags": [],
        "userTages": []
      },
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "sftpBackupStorage", attachBackupStorageToZone);
    })
}

var attachBackupStorageToZone = (ipAddress, sessionUuid) => {
  requestPromise({
      method: 'POST',
      url: ipAddress + '/v1/zones/' + allInventor.zone.uuid + '/backup-storage/' + allInventor.sftpBackupStorage.uuid,
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "attachBackupStorageToZone", addImage);
    })
}

var addImage = (ipAddress, sessionUuid) => {
  requestPromise({
      method: 'POST',
      url: ipAddress + '/v1/images',
      body: {
        "params": {
          "name": "TinyLinux",
          "url": "http://192.168.200.100/mirror/diskimages/CentOS6-test-image-4G.qcow2",
          "mediaType": "RootVolumeTemplate",
          "system": false,
          "format": "qcow2",
          "platform": "Linux",
          "backupStorageUuids": [allInventor.sftpBackupStorage.uuid]
        },
        "systemTags": [],
        "userTages": []
      },
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "image", addRouterImage);
    })
}

var addRouterImage = (ipAddress, sessionUuid) => {
  requestPromise({
      method: 'POST',
      url: ipAddress + '/v1/images',
      body: {
        "params": {
          "name": "Virtual-Router",
          "url": "file:///opt/zstack-dvd/zstack-vrouter-20161130.qcow2",
          "mediaType": "RootVolumeTemplate",
          "system": true,
          "format": "qcow2",
          "platform": "Linux",
          "backupStorageUuids": [allInventor.sftpBackupStorage.uuid]
        },
        "systemTags": [],
        "userTages": []
      },
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "routerImage", createL2NoVlanNetwork);
    })
}

var createL2NoVlanNetwork = (ipAddress, sessionUuid) => {
  requestPromise({
      method: "POST",
      url: ipAddress + '/v1/l2-networks/no-vlan',
      body: {
        "params": {
          "name": "Test-Net",
          "description": "Test",
          "zoneUuid": allInventor.zone.uuid,
          "physicalInterface": "eth0"
        },
        "systemTags": [],
        "userTags": []
      },
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "l2NoVlanNetwork", attachL2NoVlanNetworkToCluster);
    })
}

var attachL2NoVlanNetworkToCluster = (ipAddress, sessionUuid) => {
  requestPromise({
      method: "POST",
      url: ipAddress + "/v1/l2-networks/" + allInventor.l2NoVlanNetwork.uuid + "/clusters/" + allInventor.cluster.uuid,
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "attachL2NoVlanNetworkToCluster", createL3PublicNetwork);
    })
}

var createL3PublicNetwork = (ipAddress, sessionUuid) => {
  requestPromise({
      method: "POST",
      url: ipAddress + "/v1/l3-networks",
      body: {
        "params": {
          "name": "Test-L3Network",
          "type": "L3BasicNetwork",
          "l2NetworkUuid": allInventor.l2NoVlanNetwork.uuid,
          "system": true
        },
        "systemTags": [],
        "userTags": []
      },
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "l3PublicNetwork", addPublicIpRange);
    });
}

var addPublicIpRange = (ipAddress, sessionUuid) => {
  requestPromise({
      method: "POST",
      url: ipAddress + "/v1/l3-networks/" + allInventor.l3PublicNetwork.uuid + "/ip-ranges",
      body: {
        "params": {
          "name": "Test-IP-Range",
          "startIp": "10.97.0.121",
          "endIp": "10.97.0.130",
          "netmask": "255.0.0.0",
          "gateway": "10.0.0.1"
        },
        "systemTags": [],
        "userTags": []
      },
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "l3PublicNetworkIpRange", addDnsToPublicL3Network);
    })
}

var addDnsToPublicL3Network = (ipAddress, sessionUuid) => {
  requestPromise({
      method: "POST",
      url: ipAddress + "/v1/l3-networks/" + allInventor.l3PublicNetwork.uuid + "/dns",
      body: {
        "params": {
          "dns": "8.8.8.8"
        },
        "systemTags": [],
        "userTags": []
      },
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "publicL3NetworkDns", createl2VlanNetwork);
    })
}

var createl2VlanNetwork = (ipAddress, sessionUuid) => {
  requestPromise({
      method: "POST",
      url: ipAddress + "/v1/l2-networks/vlan",
      body: {
        "params": {
          "vlan": 10.0,
          "name": "Test-Net",
          "description": "Test",
          "zoneUuid": allInventor.zone.uuid,
          "physicalInterface": "eth0"
        },
        "systemTags": [],
        "userTags": []
      },
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "l2VlanNetwork", attachL2VlanNetworkToCluster);
    })
}

var attachL2VlanNetworkToCluster = (ipAddress, sessionUuid) => {
  requestPromise({
      method: "POST",
      url: ipAddress + "/v1/l2-networks/" + allInventor.l2VlanNetwork.uuid + "/clusters/" + allInventor.cluster.uuid,
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "attachL2NetworkToCluster", createL3PrivateNetwork);
    })
}

var createL3PrivateNetwork = (ipAddress, sessionUuid) => {
  requestPromise({
      method: "POST",
      url: ipAddress + "/v1/l3-networks",
      body: {
        "params": {
          "name": "Test-L3PrivateNetwork",
          "type": "L3BasicNetwork",
          "l2NetworkUuid": allInventor.l2VlanNetwork.uuid,
          "system": false
        },
        "systemTags": [],
        "userTags": []
      },
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "l3PrivateNetwork", addPrivateIpRange);
    });
}

var addPrivateIpRange = (ipAddress, sessionUuid) => {
  requestPromise({
      method: "POST",
      url: ipAddress + "/v1/l3-networks/" + allInventor.l3PrivateNetwork.uuid + "/ip-ranges/by-cidr",
      body: {
        "params": {
          "name": "Test-IP-Range",
          "networkCidr": "192.168.10.0/24"
        },
        "systemTags": [],
        "userTags": []
      },
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "l3PrivateNetworkIpRange", addDnsToL3PrivateNetwork);
    })
}

var addDnsToL3PrivateNetwork = (ipAddress, sessionUuid) => {
  requestPromise({
      method: "POST",
      url: ipAddress + "/v1/l3-networks/" + allInventor.l3PrivateNetwork.uuid + "/dns",
      body: {
        "params": {
          "dns": "8.8.8.8"
        },
        "systemTags": [],
        "userTags": []
      },
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "l3PrivateNetworkDns", queryNetworkServiveProvider);
    });
}

var queryNetworkServiveProvider = (ipAddress, sessionUuid) => {
  requestPromise({
      method: "GET",
      url: ipAddress + "/v1/network-services/providers",
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "networkServiceProvider", attachNetworkServiceToL3Network);
    })
}

var attachNetworkServiceToL3Network = (ipAddress, sessionUuid) => {
  let networkServiceProviderUuid = allInventor.networkServiceProvider.find((value, index, arr) => {
    return value.name =="vrouter";
  })
  requestPromise({
      method: "POST",
      url: ipAddress + "/v1/l3-networks/" + allInventor.l3PrivateNetwork.uuid + "/network-services",
      body: {
        "params": {
          "networkServices": {
            [networkServiceProviderUuid.uuid]: [
              "Eip", "DHCP", "DNS", "SNAT"
            ]
          }
        },
        "systemTags": [],
        "userTags": []
      },
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "attachNetworkServiceToL3Network", createInstanceOffering);
    })
}

var createInstanceOffering = (ipAddress, sessionUuid) => {
  requestPromise({
      method: "POST",
      url: ipAddress + "/v1/instance-offerings",
      body: {
        "params": {
          "name": "instanceOffering",
          "cpuNum": 2.0,
          "memorySize": 134217728.0,
          "sortKey": 0.0,
          "type": "UserVm"
        },
        "systemTags": [],
        "userTags": []
      },
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "instanceOffering", createVirtualRouterOffering);
    })
}

var createVirtualRouterOffering = (ipAddress, sessionUuid) => {
  requestPromise({
      method: "POST",
      url: ipAddress + "/v1/instance-offerings/virtual-routers",
      body: {
        "params": {
          "zoneUuid": allInventor.zone.uuid,
          "managementNetworkUuid": allInventor.l3PublicNetwork.uuid,
          "imageUuid": allInventor.routerImage.uuid,
          "publicNetworkUuid": allInventor.l3PublicNetwork.uuid,
          "isDefault": true,
          "name": "VirtualRouter-Offering",
          "cpuNum": 2.0,
          "memorySize": 134217728.0,
          "type": "VirtualRouter"
        },
        "systemTags": [],
        "userTags": []
      },
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "virtualRouterOffering", createVmInstance);
    })
}

var createVmInstance = (ipAddress, sessionUuid) => {
  requestPromise({
      method: "POST",
      url: ipAddress + "/v1/vm-instances",
      body: {
        "params": {
          "name": "vm1",
          "instanceOfferingUuid": allInventor.instanceOffering.uuid,
          "imageUuid": allInventor.image.uuid,
          "l3NetworkUuids": [
            allInventor.l3PrivateNetwork.uuid
          ],
          "clusterUuid": allInventor.cluster.uuid,
          "description": "this is a vm"
        },
        "systemTags": [],
        "userTags": []
      },
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "vMInstance", createVip);
    })
}

var createVip = (ipAddress, sessionUuid) => {
  requestPromise({
      method: "POST",
      url: ipAddress + "/v1/vips",
      body: {
        "params": {
          "name": "vip1",
          "l3NetworkUuid": allInventor.l3PublicNetwork.uuid,
          "requiredIp": "10.97.0.122"
        },
        "systemTags": [],
        "userTags": []
      },
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "vip", createEip);
    })
}

var createEip = (ipAddress, sessionUuid) => {
  requestPromise({
      method: "POST",
      url: ipAddress + "/v1/eips",
      body: {
        "params": {
          "name": "Test-eIP",
          "vipUuid": allInventor.vip.uuid,
          "vmNicUuid": allInventor.vMInstance.vmNics.uuid,
        },
        "systemTags": [],
        "userTags": []
      },
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      polling(response, sessionUuid, "eip");
    })
}



