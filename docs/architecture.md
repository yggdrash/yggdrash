# Architecture

![Architecture 180404](images/architecture.png)

## Gateway
- network area
    - grpc method
    - json rpc method
    - restful method

## Cache
- synced blockchain network address
- block data
- unconfirmed transaction pool
- pending block
- auth cache

## Authentication
- node access
- issue client token
- blockchain access control
- blockchain data access control
- node information access

## Blockchain Area

### Chain Manager
### Chain Event

## Storage Manager

## Node Management Services
* Node Log
* Node Status api
* execute method
    * reload
    * shutdown (kill)
    * stop
    