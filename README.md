# ZStack REST API cookbook

This demo code for ZStack user to setup some classical private cloud scenarios by REST API.

We suppose you have read 《ZStack RESTful API User Manual》 before use demo code.

This project contains three parts:
## java
contains java demo code with java SDK to use REST API
## js-polling
contains java script demo code with polling method to use REST API
## js-webhook
contains java script demo code with webhook method to use REST API

Every part contains below scenario:

**Flat network with ceph storage**

 - Network: FlatNetwork with EIP
 - Primary Storage: ceph
 - Backup Storage: ceph
 
**Flat network with local storage**

 - Network: FlatNetwork with EIP
 - Primary Storage: local storage
 - Backup Storage: image store
 
**Flat network with local storage**

 - Network: FlatNetwork with EIP
 - Primary Storage: local storage
 - Backup Storage: image store

**Virtual Router network with local storage**

 - Network: Virtual Router supply private IP and EIP
 - Primary Storage: local storage
 - Backup Storage: image store




