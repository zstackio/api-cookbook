const request = require('request');
const requestPromise = require('request-promise');


const ipAddress = 'http://172.20.12.129:8080/zstack';
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

var allInventor = {
  zone: "",
  cluster: "",
  kvmHost: "",
  nfsPrimaryStorage: "",
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
  attachL2NetworkToCluster: "",
  publicL3NetworkDns: "",
  attachL2NoVlanNetworkToCluster: "",
  networkServiceProvider: ""
};

var polling = (responsed, sessionUuid, inventory, callback = "") => {
  if (responsed.statusCode == 202) {
    var url = responsed.body.location.replace(/[0-9]+(?:\.[0-9]+){3}:[0-9]+/, '172.20.12.129:8080');
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
            console.log('inventory:' + inventory);
            allInventor[inventory] = response.body.inventory;
            if (callback)
              callback(ipAddress, sessionUuid);
            clearInterval(interval);
          }

        })
    }, 1000);
  } else if (responsed.statusCode == 200) {
    console.log('inventory:' + inventory);
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
          "managementIp": "10.0.198.19",
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
      polling(response, sessionUuid, "kvmHost", addNfsPrimaryStorage);
    })
}

var addNfsPrimaryStorage = (ipAddress, sessionUuid) => {
  requestPromise({
      method: "POST",
      url: ipAddress + '/v1/primary-storage/nfs',
      body: {
        "params": {
          "url": "172.20.12.129" + ":/nfs_root",
          "zoneUuid": allInventor.zone.uuid,
          "name": "PS1",
          "type": "NFS"
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
      polling(response, sessionUuid, "nfsPrimaryStorage", attachPrimaryStorageToCluster);
    })
}


var attachPrimaryStorageToCluster = (ipAddress, sessionUuid) => {
  requestPromise({
      method: 'POST',
      url: ipAddress + '/v1/clusters/' + allInventor.cluster.uuid + '/primary-storage/' + allInventor.nfsPrimaryStorage.uuid,
      headers: {
        'Authorization': 'OAuth ' + sessionUuid
      },
      resolveWithFullResponse: true,
      json: true
    })
    .then((response) => {
      console.log("here");
      polling(response, sessionUuid, "attachPrimaryStorageToCluste", addSftpBackupStorage);
    })
}


var addSftpBackupStorage = (ipAddress, sessionUuid) => {
  requestPromise({
      method: "POST",
      url: ipAddress + "/v1/backup-storage/sftp",
      body: {
        "params": {
          "hostname": "10.0.198.19",
          "username": "root",
          "password": "password",
          "sshPort": 22.0,
          "url": "/home/sftpBackupStorage",
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
      polling(response, sessionUuid, "image", createL2NoVlanNetwork);
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
          "startIp": "10.97.0.131",
          "endIp": "10.97.0.140",
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
      polling(response, sessionUuid, "publicL3NetworkDns", createInstanceOffering);
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
      polling(response, sessionUuid, "instanceOffering", createVmInstance);
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
            allInventor.l3PublicNetwork.uuid
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
          "requiredIp": "10.97.0.132"
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
          "name": "Test-EIP",
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

