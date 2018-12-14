# JSON RPC
**Contents**
* [JSON RPC API](#json-rpc-api)
  * [JavaScript API](#javascript-api)
  * [JSON-RPC Endpoint](#json-rpc-endpoint)
  * [Curl Examples Explained](#curl-examples-explained)
  * [JSON RPC API Reference](#json-rpc-api-reference)
     * [createAccount](#createAccount)
     * [accounts](#accounts)
     * [balanceOf](#balanceOf)
     * [getBalance](#getBalance)
     * [blockNumber](#blockNumber)
     * [getBlockByHash](#getBlockByHash)
     * [getBlockByNumber](#getBlockByNumber)
     * [newBlockFilter](#newBlockFilter)
     * [getTransactionCountByBlockHash](#getTransactionCountByBlockHash)
     * [getTransactionCountByBlockNumber](#getTransactionCountByBlockNumber)
     * [getTransactionByHash](#getTransactionByHash)
     * [getTransactionByBlockHash](#getTransactionByBlockHash)
     * [getTransactionByBlockNumber](#getTransactionByBlockNumber)
     * [getTransactionReceipt](#getTransactionReceipt)
     * [sendTransaction](#sendTransaction)
     * [sendRawTransaction](#sendRawTransaction)
     * [newPendingTransactionFilter](#newPendingTransactionFilter)
     * [getTransactionReceipt](#getTransactionReceipt)
     * [getAllTransactionReceipt](#getAllTransactionReceipt)
     * [[contract] query](#contract-query)
     * [[peer] add](#peer-add)
     * [[peer] getAll](#peer-getall)
     * [[peer] getAllActivePeer](#peer-getAllActivePeer)
     * [[admin] nodeHello](#admin-nodeHello)
     * [[admin] requestCommand](#admin-requestcommand) 

# JSON RPC API 

[JSON](http://json.org/) is a lightweight data-interchange format. 
It can represent numbers, strings, ordered sequences of values, and collections of name/value pairs.

[JSON-RPC](https://www.jsonrpc.org/specification) is a stateless, light-weight remote procedure call (RPC) protocol. 
Primarily this specification defines several data structures and the rules around their processing. 
It is transport agnostic in that the concepts can be used within the same process, over sockets, over HTTP, or in many various message passing environments. 
It uses JSON ([RFC 4627](http://www.ietf.org/rfc/rfc4627.txt)) as data format.

## JavaScript API

## JSON-RPC Endpoint

Default JSON-RPC endpoints : 

| Client | URL                                            |
| :----: | :--------------------------------------------: |
| Java   | [http://localhost:8080](http://localhost:8080) |

## Curl Examples Explained

The curl options below might return a response where the node complains about the content type, 
this is because the --data option sets the content type to application/x-www-form-urlencoded . 
If your node does complain, manually set the header by placing -H "Content-Type: application/json" at the start of the call.

The examples also do not include the URL/IP & port combination which must be the last argument given to curl e.x. 127.0.0.1:8080

## JSON RPC API Reference
  
### createAccount 

Create a new account.

**Parameter**

none

**Returns**

`String`, 20 bytes - an address

**Example**
 
```
// Request
{
    "id":"1",
    "jsonrpc":"2.0",
    "method":"createAccount"
}

// Result
{
    "jsonrpc":"2.0",
    "id":"1973130775",
    "result":"eddc6ac0d1c6b4b84842f1b8f154aee07ead74b9"
}
```  
  
------

#### accounts

Returns a list of addresses owned by client.

**Parameter**

none

**Returns**

`Array of DATA`, 20 bytes - addresses owned by the client

**Example**

```
// Request
{
    "id":"2051005236",
    "jsonrpc":"2.0",
    "method":"accounts"
}

// Result
{
    "jsonrpc":"2.0",
    "id":"2051005236",
    "result":["0xA6cf59D72cB6c253b3CFe10d498aC8615453689B","0x2Aa4BCaC31F7F67B9a15681D5e4De2FBc778066A","0x1662E2457A0e079B03214dc3D5009bA2137006C7"]
}
```  
  
------

#### balanceOf

Returns a balance of address

**Parameter**

`String of DATA` - query

```
params: {
		"data": "{\"address\":\"a08ee962cd8b2bd0edbfee989c1a9f7884d26532\",\"method\":\"balanceOf\",\"params\":[{\"address\":\"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\"}]}"
}
```

**Returns**

`String of DATA` - balance of address

**Example**

```
// Request
{
	"id": "1241106686",
	"jsonrpc": "2.0",
	"method": "balanceOf",
	"params": {
		"data": "{\"address\":\"a08ee962cd8b2bd0edbfee989c1a9f7884d26532\",\"method\":\"balanceOf\",\"params\":[{\"address\":\"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\"}]}"
	}
}

// Response
{
	"jsonrpc": "2.0",
	"id": "1241106686",
	"result": "{\"result\":\"1000000000\"}"
}
```

 
------

#### getBalance

Returns the balance of the account of given address.

**Parameter**

1. `DATA`, 20 bytes - address to check for balance.
2. `QUANTITY | TAG` - integer block number, or the string `"latest"`, `"earliest"` or `"pending"`

**Returns**

`QUANTITY` - integer of the current balance (i.e. Yeed)

**Example**

```
// Request
{
    "id":"623943523",
    "jsonrpc":"2.0",
    "method":"getBalance",
    "params":{
                "address":"0x2Aa4BCaC31F7F67B9a15681D5e4De2FBc778066A",
                "tag":"latest"
              }
}

// Result
{
    "jsonrpc":"2.0",
    "id":"623943523",
    "result":100000
}
```  
  
------

#### blockNumber

Returns the number of most recent block.

**Parameter**

none

**Returns**

`QUANTITY` - integer of the current block number the client is on.

**Example**

```
// Request
{
	"id": "1606256378",
	"jsonrpc": "2.0",
	"method": "blockNumber",
	"params": {
		"branchId": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7"
	}
}

// Result
{
	"jsonrpc": "2.0",
	"id": "1606256378",
	"result": 28
}
```
  
-----

#### getBlockByHash

Returns information about a block by hash.

**Parameter**

1. `DATA`, 32 Bytes - Hash of a block.
2. `Boolean` - If `true` it returns the full transaction objects, if `false` only the hashes of the transactions.

```
params: {
 		"branchId": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7",
 		"blockId": "ad7dd0552336ebf3b2f4f648c4a87d7c35ed74382219e2954047ad9138a247c5",
 		"bool": true
}
```

**Returns**

`Object` - A block object, or `null` when no block was found:


**Example**

```
/// Request (to Stem chain)
 {
 	"id": "888983999",
 	"jsonrpc": "2.0",
 	"method": "getBlockByHash",
 	"params": {
 		"branchId": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7",
 		"blockId": "ad7dd0552336ebf3b2f4f648c4a87d7c35ed74382219e2954047ad9138a247c5",
 		"bool": true
 	}
 }
 
 // Result (from Stem chain)
 {
 	"jsonrpc": "2.0",
 	"id": "888983999",
 	"result": {
 		"chain": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7",
 		"version": "0000000000000000",
 		"type": "0000000000000000",
 		"prevBlockHash": "7525186314769ae0cabfe8addafe5e40e1b4d92478d9afd13943a8d1112b201f",
 		"index": 1,
 		"timestamp": 1537435370004,
 		"merkleRoot": "0000000000000000000000000000000000000000000000000000000000000000",
 		"bodyLength": 0,
 		"signature": "1c4fb772df3f7409c10b82af425b30a005cd38babd93cacd07726e09733abdd19d6e64c8ae03688a9e70ea3d751cd7e7dba3a182f6f567e2c7f1c542c1e95c8159",
 		"body": [],
 		"author": "aaa2aaab0fb041c5cb2a60a12291cbc3097352bb",
 		"hash": "ad7dd0552336ebf3b2f4f648c4a87d7c35ed74382219e2954047ad9138a247c5"
 	}
 }
```
  
-----

#### getBlockByNumber

Returns information about a block by block number.

**Parameter**

1. `QUANTITY|TAG` - integer of a block number, or the string `"earliest"`, `"latest"` or `"pending"`, as in the default block parameter.
2. `Boolean` - If `true` it returns the full transaction objects, if `false` only the hashes of the transactions.

```
params: [
   true
   '0xbbF5029Fd710d227630c8b7d338051B8E76d50B3'
]
```

**Returns**

See [getBlockByHash]()

**Example**

```
// Request (to Stem chain)
{
	"id": "988795691",
	"jsonrpc": "2.0",
	"method": "getBlockByNumber",
	"params": {
		"branchId": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7",
		"numOfBlock": 0,
		"bool": true
	}
}

// Result (from Stem chain)
{
	"jsonrpc": "2.0",
	"id": "988795691",
	"result": {
		"chain": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7",
		"version": "0000000000000000",
		"type": "0000000000000000",
		"prevBlockHash": "0000000000000000000000000000000000000000000000000000000000000000",
		"index": 0,
		"timestamp": 1537246807962,
		"merkleRoot": "1cf4b39c9530026fb5294c8e53041304252749e9bdccc7014a6bc78985699d60",
		"bodyLength": 1734,
		"signature": "1c8d4fdb448e55ada01ec98add95a360d18e750cc3a5230a142b0027684e1b7642207ffc435a93a9408040b0466c7d6bc1045b9ec0d6bf31c0783a4fc2859fc3f0",
		"body": [{
			"chain": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7",
			"version": "0000000000000000",
			"type": "0000000000000000",
			"timestamp": 1537246807962,
			"bodyHash": "09ecdfc92e99c14e67ad93dd0d7b13c69042ff652206b2aea89878f045dd598c",
			"bodyLength": 1585,
			"signature": "1cf604a2e271344dacdfa3119e066a87966514b018d24980ca89b11b58e672b16949fd05f63f9178916489c39594819b0efd26801b8572b3c962ac9fc0f38d52f5",
			"body": "[{\"method\":\"genesis\",\"branchId\":\"fe7b7c93dd23f78e12ad42650595bc0f874c88f7\",\"params\":[{\"branchId\":\"fe7b7c93dd23f78e12ad42650595bc0f874c88f7\",\"branch\":\"{\\\"name\\\":\\\"STEM\\\",\\\"symbol\\\":\\\"STEM\\\",\\\"property\\\":\\\"ecosystem\\\",\\\"type\\\":\\\"immunity\\\",\\\"description\\\":\\\"The Basis of the YGGDRASH Ecosystem. It is also an aggregate and a blockchain containing information of all Branch Chains.\\\",\\\"tag\\\":0.1,\\\"version\\\":\\\"0xcc9612ff91ff844938acdb6608e58506a2f21b8a5d77e88726c0897e8d1d02c0\\\",\\\"reference_address\\\":\\\"\\\",\\\"reserve_address\\\":\\\"0xcee3d4755e47055b530deeba062c5bd0c17eb00f\\\",\\\"owner\\\":\\\"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\\\",\\\"timestamp\\\":1536755670444,\\\"version_history\\\":[\\\"0xcc9612ff91ff844938acdb6608e58506a2f21b8a5d77e88726c0897e8d1d02c0\\\"]}\"}],\"delegator\":{\"a7a9ad6f834bcc080586c55e9b1981b7ec39f31d\":{\"ip\":\"172.16.10.150\",\"port\":\"32918\"},\"50c7b8af63f57ddef4b954e38e7a1253c896b742\":{\"ip\":\"172.16.10.150\",\"port\":\"32919\"},\"5db10750e8caff27f906b41c71b3471057dd2002\":{\"ip\":\"100.100.100.3\",\"port\":\"32918\"},\"5db10750e8caff27f906b41c71b3471057dd2003\":{\"ip\":\"100.100.100.4\",\"port\":\"32918\"},\"5db10750e8caff27f906b41c71b3471057dd2004\":{\"ip\":\"100.100.100.5\",\"port\":\"32918\"}},\"node\":{\"5db10750e8caff27f906b41c71b3471057dd2006\":{\"ip\":\"100.100.100.6\",\"port\":\"32918\"},\"5db10750e8caff27f906b41c71b3471057dd2007\":{\"ip\":\"100.100.100.7\",\"port\":\"32918\"},\"5db10750e8caff27f906b41c71b3471057dd2008\":{\"ip\":\"100.100.100.8\",\"port\":\"32918\"},\"5db10750e8caff27f906b41c71b3471057dd2009\":{\"ip\":\"100.100.100.9\",\"port\":\"32918\"},\"5db10750e8caff27f906b41c71b3471057dd2010\":{\"ip\":\"100.100.100.10\",\"port\":\"32918\"}}}]",
			"author": "e68dc7c57c52221b291cc7440becc025554082d9",
			"hash": "76f2292411ea0c8233cc5302ccb7035d16e13d9b6f6c53cb26a8afd126cc9864"
		}],
		"author": "e68dc7c57c52221b291cc7440becc025554082d9",
		"hash": "7525186314769ae0cabfe8addafe5e40e1b4d92478d9afd13943a8d1112b201f"
	}
}
```
  
-----

#### newBlockFilter

Creates a filter in the node, to notify when a new block arrives.   
(To check if the state has changed, call eth_getFilterChanges.)

**Parameter**

none 

**Returns**

`QUANTITY` - A filter id.

**Example**
  
```
// Request
{
    "id":"2075477934",
    "jsonrpc":"2.0",
    "method":"newBlockFilter"
}

// Result
{
    "jsonrpc":"2.0",
    "id":"2075477934",
    "result":0
}
```
 
-----

#### getTransactionCountByBlockHash

Returns the number of transactions in a block from a block matching the given block hash.

**Parameter**

`DATA`, 32 Bytes - hash of a block

**Returns**

`QUANTITY` - integer of the number of transactions in this block.

**Example**
  
```
// Request (to Stem chain)
{
	"id": "1327275333",
	"jsonrpc": "2.0",
	"method": "getTransactionCountByBlockHash",
	"params": {
		"branchId": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7",
		"blockId": "d52fffa14f5b88b141d05d8e28c90d8131db1aa63e076bfea9c28c3060049e12"
	}
}

// Result (from Stem chain)
{
	"jsonrpc": "2.0",
	"id": "1327275333",
	"result": 50
}
```
 
-----

#### getTransactionCountByBlockNumber

Returns the number of transactions in a block matching the given block number.

**Parameter**

`QUANTITY|TAG` - integer of a block number, or the string `"earliest"`, `"latest"` or `"pending"`, as in the default block parameter.
`QUANTITY` - the transaction index position.

```
params: {
		"branchId": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7",
		"blockNumber": 3
}

params: {
		"branchId": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7",
		"txIndexPosition": 2,
		"blockNumber": 31
}
```

**Returns**

`QUANTITY` - integer of the number of transactions in this block.

**Example**
  
```
// Request (to Stem chain)
{
	"id": "1322610796",
	"jsonrpc": "2.0",
	"method": "getTransactionCountByBlockNumber",
	"params": {
		"branchId": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7",
		"blockNumber": 3
	}
}

// Result (from Stem chain)
{
	"jsonrpc": "2.0",
	"id": "1322610796",
	"result": 50
}

// Request (to Stem chain)
{
	"id": "729387017",
	"jsonrpc": "2.0",
	"method": "getTransactionCountByBlockNumber",
	"params": {
		"branchId": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7",
		"txIndexPosition": 2,
		"blockNumber": 31
	}
}

// Result (from Stem chain)
{
	"jsonrpc": "2.0",
	"id": "729387017",
	"result": {
		"chain": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7",
		"version": "0000000000000000",
		"type": "0000000000000000",
		"timestamp": 1537426002627,
		"bodyHash": "441a7570b535215b9d25f1a16f05e6c12fb33151d5fa74203f49aa74733a713e",
		"bodyLength": 674,
		"signature": "1c426c1b0579b0fd724548720c3927283abd00c564f48e2f08e7b0084bd95161c26793d586b401c3c5f5f38b70911c52847f7ae5ef4522f269dcc744e7eb277a52",
		"body": "[{\"method\":\"create\",\"params\":[{\"branchId\":\"a08ee962cd8b2bd0edbfee989c1a9f7884d26532\",\"branch\":{\"name\":\"YEED\",\"owner\":\"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\",\"symbol\":\"YEED\",\"property\":\"currency\",\"type\":\"immunity\",\"timestamp\":1536756751728,\"description\":\"YEED is the currency used inside YGGDRASH. The vitality of the new branch chain is decided by the amount of YEED, which will be consumed gradually.\",\"tag\":0.1,\"version\":\"0xeeabcaf6cf907e0fd27a0d1f305313e9c1069c5d7f8729d227110012c9b37025\",\"version_history\":[\"0xeeabcaf6cf907e0fd27a0d1f305313e9c1069c5d7f8729d227110012c9b37025\"],\"reference_address\":\"\",\"reserve_address\":\"0x9cc060690705a13078634637b1d2a5f2fe1b8096\"}}]}]",
		"author": "aaa2aaab0fb041c5cb2a60a12291cbc3097352bb",
		"hash": "3799a13a481b5df87a4fb18db20286754a638d784d397f962ed4e4dba54cb52d"
	}
}

// Request (to Stem chain)
{
	"id": "1480899822",
	"jsonrpc": "2.0",
	"method": "getTransactionCountByBlockNumber",
	"params": {
		"branchId": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7",
		"txIndexPosition": 2,
		"tag": "latest"
	}
}

// Result (from Stem chain)
{
	"jsonrpc": "2.0",
	"id": "1480899822",
	"result": {
		"chain": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7",
		"version": "0000000000000000",
		"type": "0000000000000000",
		"timestamp": 1537426969582,
		"bodyHash": "441a7570b535215b9d25f1a16f05e6c12fb33151d5fa74203f49aa74733a713e",
		"bodyLength": 674,
		"signature": "1bafca750fb41b187b04194e6cfb21b19add3cc7ae2fdff37e6dfdef6cc19b6cd25684b00fa31f7de6b326592ab467d0eda85b0c6ec1b24681cf40173a920c57a7",
		"body": "[{\"method\":\"create\",\"params\":[{\"branchId\":\"a08ee962cd8b2bd0edbfee989c1a9f7884d26532\",\"branch\":{\"name\":\"YEED\",\"owner\":\"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\",\"symbol\":\"YEED\",\"property\":\"currency\",\"type\":\"immunity\",\"timestamp\":1536756751728,\"description\":\"YEED is the currency used inside YGGDRASH. The vitality of the new branch chain is decided by the amount of YEED, which will be consumed gradually.\",\"tag\":0.1,\"version\":\"0xeeabcaf6cf907e0fd27a0d1f305313e9c1069c5d7f8729d227110012c9b37025\",\"version_history\":[\"0xeeabcaf6cf907e0fd27a0d1f305313e9c1069c5d7f8729d227110012c9b37025\"],\"reference_address\":\"\",\"reserve_address\":\"0x9cc060690705a13078634637b1d2a5f2fe1b8096\"}}]}]",
		"author": "aaa2aaab0fb041c5cb2a60a12291cbc3097352bb",
		"hash": "4ab3e4397b8235820e3f3d8e0ed802daefd93ce68cb726dff1edd30603f74a6d"
	}
}
```
 
 
-----

#### getTransactionByHash

Returns the information about a transaction requested by transaction hash.

**Parameter**

`DATA`, 32 Bytes - hash of a transaction

```
params: {
 		"branchId": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7",
 		"txId": "f5912fde84c6a3a44b4e529077ca9bf28feccd847137e44a77cd17e9fb9c1353"
}
```

**Returns**

`Object` - A transaction object, or `null` when no transaction was found:

**Example**
 
```
// Request (to Stem chain)
{
	"id": "759202303",
	"jsonrpc": "2.0",
	"method": "getTransactionByHash",
	"params": {
		"branchId": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7",
		"txId": "f5912fde84c6a3a44b4e529077ca9bf28feccd847137e44a77cd17e9fb9c1353"
	}
}

// Result (from Stem chain)
{
	"jsonrpc": "2.0",
	"id": "759202303",
	"result": {
		"chain": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7",
		"version": "0000000000000000",
		"type": "0000000000000000",
		"timestamp": 1537427095442,
		"bodyHash": "1332ee7afc829126f40a50b42b075dd103e471e7446287af70923220ba45b9b7",
		"bodyLength": 651,
		"signature": "1bf43beb1f39c005e8372985a09cedb0fb9dec39619320bbb17fe44536ebb9518d437db0ff888cc2a9c5271d726ec412077fd060d81fada5e2d04e23a1777ada6c",
		"body": "[{\"method\":\"create\",\"params\":[{\"branchId\":\"17024be871734c7d595a0ac7c5dfcc79396c7d7b\",\"branch\":{\"name\":\"Sacred Water\",\"owner\":\"aaa2aaab0fb041c5cb2a60a12291cbc3097352bb\",\"symbol\":\"SW\",\"property\":\"reputation\",\"type\":\"immunity\",\"timestamp\":1537340256736,\"description\":\"Reputation Score Evaluation Chain. Contribute your time and benefit the ecosystem to earn a higher reputation score.\",\"tag\":0.1,\"version\":\"0xebe69520b56c81e1ad2ceec4c2f9ed9405376fa56e9a9713a026fb305989e042\",\"reference_address\":\"\",\"reserve_address\":\"0x2831de120827570cf8c7cfcb9b788c222e307de4\",\"version_history\":[\"0xebe69520b56c81e1ad2ceec4c2f9ed9405376fa56e9a9713a026fb305989e042\"]}}]}]",
		"author": "aaa2aaab0fb041c5cb2a60a12291cbc3097352bb",
		"hash": "f5912fde84c6a3a44b4e529077ca9bf28feccd847137e44a77cd17e9fb9c1353"
	}
}
```
 
-----

#### getTransactionByBlockHash

Returns information about a transaction by block hash and transaction index position.

**Parameter**

1. `DATA`, 32 Bytes - hash of a block.
2. `QUANTITY` - integer of the transaction index position.

```
params: [
   1,
   '0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf' 
]
```

**Returns**

See [getTransactionByHash]()

**Example**
  
```
// Request (to Stem chain)
{
	"id": "1141387996",
	"jsonrpc": "2.0",
	"method": "getTransactionByBlockHash",
	"params": {
		"branchId": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7",
		"txIndexPosition": 2,
		"blockId": "5ef71a90c6d99c7bc13bfbcaffb50cb89210678e99ed6626c9d2f378700b392c"
	}
}

// Result (from Stem chain)
{
	"jsonrpc": "2.0",
	"id": "1141387996",
	"result": {
		"chain": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7",
		"version": "0000000000000000",
		"type": "0000000000000000",
		"timestamp": 1537427094634,
		"bodyHash": "1332ee7afc829126f40a50b42b075dd103e471e7446287af70923220ba45b9b7",
		"bodyLength": 651,
		"signature": "1cb27a3625148c380ce0624b814c9f2c3ccad056351145c0d1e839328ed7a830b76b721423803e17f5090aa0814cf88c89919fb32254139b54c550253bcfe2c768",
		"body": "[{\"method\":\"create\",\"params\":[{\"branchId\":\"17024be871734c7d595a0ac7c5dfcc79396c7d7b\",\"branch\":{\"name\":\"Sacred Water\",\"owner\":\"aaa2aaab0fb041c5cb2a60a12291cbc3097352bb\",\"symbol\":\"SW\",\"property\":\"reputation\",\"type\":\"immunity\",\"timestamp\":1537340256736,\"description\":\"Reputation Score Evaluation Chain. Contribute your time and benefit the ecosystem to earn a higher reputation score.\",\"tag\":0.1,\"version\":\"0xebe69520b56c81e1ad2ceec4c2f9ed9405376fa56e9a9713a026fb305989e042\",\"reference_address\":\"\",\"reserve_address\":\"0x2831de120827570cf8c7cfcb9b788c222e307de4\",\"version_history\":[\"0xebe69520b56c81e1ad2ceec4c2f9ed9405376fa56e9a9713a026fb305989e042\"]}}]}]",
		"author": "aaa2aaab0fb041c5cb2a60a12291cbc3097352bb",
		"hash": "98f2d644cedc38362f28cb8b624287ed014563c97d4eb0048270b7381daea7a1"
	}
}
```
 
-----

#### sendTransaction

Creates new message call transaction or a contract creation, if the data field contains code.

**Parameter**

`Object` - The transaction object

**Returns**

`DATA`, 32 Bytes - the transaction hash, or the zero hash if the transaction is not yet available.

**Example**

```
// Request (to Stem chain)
{
	"id": "1634388869",
	"jsonrpc": "2.0",
	"method": "sendTransaction",
	"params": {
		"tx": {
			"chain": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7",
			"version": "0000000000000000",
			"type": "0000000000000000",
			"timestamp": 1537409777132,
			"bodyHash": "441a7570b535215b9d25f1a16f05e6c12fb33151d5fa74203f49aa74733a713e",
			"bodyLength": 674,
			"signature": "1c84b0d2894b312043a91c4e61b768d62040ed7f4717d99d9aad9f774a0632d0f113a344ef1ade11b6ecae2b021972032508158f8b0ff919857f5da0773fb73c94",
			"body": "[{\"method\":\"create\",\"params\":[{\"branchId\":\"a08ee962cd8b2bd0edbfee989c1a9f7884d26532\",\"branch\":{\"name\":\"YEED\",\"owner\":\"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\",\"symbol\":\"YEED\",\"property\":\"currency\",\"type\":\"immunity\",\"timestamp\":1536756751728,\"description\":\"YEED is the currency used inside YGGDRASH. The vitality of the new branch chain is decided by the amount of YEED, which will be consumed gradually.\",\"tag\":0.1,\"version\":\"0xeeabcaf6cf907e0fd27a0d1f305313e9c1069c5d7f8729d227110012c9b37025\",\"version_history\":[\"0xeeabcaf6cf907e0fd27a0d1f305313e9c1069c5d7f8729d227110012c9b37025\"],\"reference_address\":\"\",\"reserve_address\":\"0x9cc060690705a13078634637b1d2a5f2fe1b8096\"}}]}]",
			"author": "aaa2aaab0fb041c5cb2a60a12291cbc3097352bb",
			"hash": "d7cf6612214effa4013ac6fee62894c1a0f9b47b1ddcffb2a62a6398b200ff31"
		}
	}
} 

// Result (from Stem chain)
{
	"jsonrpc": "2.0",
	"id": "1634388869",
	"result": "d7cf6612214effa4013ac6fee62894c1a0f9b47b1ddcffb2a62a6398b200ff31"
}

// Request (to Yeed chain)
{
	"id": "905164516",
	"jsonrpc": "2.0",
	"method": "sendTransaction",
	"params": {
		"tx": {
			"chain": "a08ee962cd8b2bd0edbfee989c1a9f7884d26532",
			"version": "0000000000000000",
			"type": "0000000000000000",
			"timestamp": 1537409809998,
			"bodyHash": "0e6c084a898864eac534a089a79247de8cf3325d076803d90e0cf94e59cb6bd2",
			"bodyLength": 102,
			"signature": "1ca3861872ae2ff66ad2c2bb9f6909a32ffcdac43299160908afeb4afff3b2147c714d36fbbcd85ebb60cc89b08fa8fb2145c5ca673fa1e006a50d93659ea13e0e",
			"body": "[{\"method\":\"transfer\",\"params\":[{\"address\":\"e1980adeafbb9ac6c9be60955484ab1547ab0b76\",\"amount\":100}]}]",
			"author": "aaa2aaab0fb041c5cb2a60a12291cbc3097352bb",
			"hash": "a27cc5bd50672f04423110f76784d821536c24884b3f1425b25dba3831de3c56"
		}
	}
}

// Result (from Yeed chain)
{
	"jsonrpc": "2.0",
	"id": "905164516",
	"result": "a27cc5bd50672f04423110f76784d821536c24884b3f1425b25dba3831de3c56"
} 
```
  
-----

#### sendRawTransaction

Creates new message call transaction or a contract creation for signed transactions.

**Parameter**

`DATA`, The signed transaction data.

```
params: [
    "MDAwMDAwMDDefl5jdaRgKKIzV/pKUUBLyIzsEyZC3tU3JVTch7UJHAAAjbWEJtGkAAAAAAAAAAIcBVYKn9ycJe3+/lo0jbGC7ZsoPnNLOa+4J3haf3kiIy11MEuT1cR3SrLYid8GeJrbD9H6JAm8Qu601eckAiN36HsiaWQiOiIwIiwibmFtZSI6IlJhY2hhZWwiLCJhZ2UiOiIyNyJ9"
]
```

**Returns**

`DATA`, 32 Bytes - the transaction hash, or the zero hash if the transaction is not yet available.

**Example**
  
```
// Request
{
    "id":"1797225445",
    "jsonrpc":"2.0",
    "method":"sendRawTransaction",
    "params":{
                "rawTx":"MDAwMDAwMDDefl5jdaRgKKIzV/pKUUBLyIzsEyZC3tU3JVTch7UJHAAAjbWEJtGkAAAAAAAAAAIcBVYKn9ycJe3+/lo0jbGC7ZsoPnNLOa+4J3haf3kiIy11MEuT1cR3SrLYid8GeJrbD9H6JAm8Qu601eckAiN36HsiaWQiOiIwIiwibmFtZSI6IlJhY2hhZWwiLCJhZ2UiOiIyNyJ9"
             }
}

// Result
{
    "jsonrpc":"2.0",
    "id":"1797225445",
    "result":"3PAOrAaNlZYixCxMzeR25bYLsnJ2bykSxd5ZyDVzcgE="
}
```
 
-----

#### newPendingTransactionFilter

Creates a filter in the node, to notify when new pending transactions arrive.

**Parameter**

none 

**Returns**

`QUANTITY` - A filter id.

**Example**
  
```
// Request (to Stem chain)
{
	"id": "1823533023",
	"jsonrpc": "2.0",
	"method": "newPendingTransactionFilter",
	"params": {
		"branchId": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7"
	}
}

// Result (from Stem chain)
{
	"jsonrpc": "2.0",
	"id": "1823533023",
	"result": 0
}

// Request (To Yeed chain)
{
	"id": "813535546",
	"jsonrpc": "2.0",
	"method": "newPendingTransactionFilter",
	"params": {
		"branchId": "a08ee962cd8b2bd0edbfee989c1a9f7884d26532"
	}
}

// Result (from Yeed chain)
{
	"jsonrpc": "2.0",
	"id": "813535546",
	"result": 1
}
```
 
-----

#### getTransactionReceipt

Returns the TransactionReceipt of transaction hash.

**Parameter**

`DATA`, 32 Bytes - the transaction hash

**Returns**

`Object` - A TransactionReceipt object, or null when no TransactionReceipt was found:

**Example**

```
// Request (to Stem chain)
{
	"id": "1224098261",
	"jsonrpc": "2.0",
	"method": "getTransactionReceipt",
	"params": {
		"branchId": "fe7b7c93dd23f78e12ad42650595bc0f874c88f7",
		"txId": "d7cf6612214effa4013ac6fee62894c1a0f9b47b1ddcffb2a62a6398b200ff31"
	}
}

// Result (from Stem chain)
{
	"jsonrpc": "2.0",
	"id": "1224098261",
	"result": {
		"transactionHash": "d7cf6612214effa4013ac6fee62894c1a0f9b47b1ddcffb2a62a6398b200ff31",
		"transactionIndex": 1,
		"blockHash": "0xc6ef2fc5426d6ad6fd9e2a26abeab0aa2411b7ab17f30a99d3cb96aed1d1055b",
		"yeedUsed": 30000,
		"branchAddress": "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
		"txLog": {
			"method": "create",
			"a08ee962cd8b2bd0edbfee989c1a9f7884d26532": {
				"owner": "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94",
				"reference_address": "",
				"symbol": "YEED",
				"reserve_address": "0x9cc060690705a13078634637b1d2a5f2fe1b8096",
				"name": "YEED",
				"property": "currency",
				"description": "YEED is the currency used inside YGGDRASH. The vitality of the new branch chain is decided by the amount of YEED, which will be consumed gradually.",
				"tag": 0.1,
				"version_history": ["0xeeabcaf6cf907e0fd27a0d1f305313e9c1069c5d7f8729d227110012c9b37025"],
				"type": "immunity",
				"version": "0xeeabcaf6cf907e0fd27a0d1f305313e9c1069c5d7f8729d227110012c9b37025",
				"timestamp": 1536756751728
			}
		},
		"status": 1,
		"success": true
	}
}

// Request (to Yeed chain)
{
	"id": "1288216515",
	"jsonrpc": "2.0",
	"method": "getTransactionReceipt",
	"params": {
		"branchId": "a08ee962cd8b2bd0edbfee989c1a9f7884d26532",
		"txId": "a27cc5bd50672f04423110f76784d821536c24884b3f1425b25dba3831de3c56"
	}
}

// Result (from Stem chain)
{
	"jsonrpc": "2.0",
	"id": "1288216515",
	"result": {
		"transactionHash": "a27cc5bd50672f04423110f76784d821536c24884b3f1425b25dba3831de3c56",
		"transactionIndex": 1,
		"blockHash": "0xc6ef2fc5426d6ad6fd9e2a26abeab0aa2411b7ab17f30a99d3cb96aed1d1055b",
		"yeedUsed": 30000,
		"branchAddress": "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
		"txLog": {
			"amount": "100",
			"method": "transfer",
			"from": "aaa2aaab0fb041c5cb2a60a12291cbc3097352bb",
			"to": "e1980adeafbb9ac6c9be60955484ab1547ab0b76"
		},
		"status": 0,
		"success": false
	}
}
```
 
-----

#### getAllTransactionReceipt

Returns all TransactionReceipts.

**Parameter**

None

**Returns**

`HashMap of DATA`, List of all TransactionReceipts

**Example**

```
/ Request (to Stem chain)
{
  "id": "1890315358",
  "jsonrpc": "2.0",
  "method": "getTransactionReceipt",
  "params": {
    "txId": "2da7c6e0bbb35365a1b57c7cdb9b5a434a41c43b6374018d9303b87576c157a9"
  }
}

// Result (from Stem chain)
{
	"jsonrpc": "2.0",
	"id": "395916987",
	"result": {
		"76f2292411ea0c8233cc5302ccb7035d16e13d9b6f6c53cb26a8afd126cc9864": {
			"transactionHash": "76f2292411ea0c8233cc5302ccb7035d16e13d9b6f6c53cb26a8afd126cc9864",
			"transactionIndex": 1,
			"blockHash": "0xc6ef2fc5426d6ad6fd9e2a26abeab0aa2411b7ab17f30a99d3cb96aed1d1055b",
			"yeedUsed": 30000,
			"branchAddress": "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
			"txLog": {
				"method": "genesis",
				"fe7b7c93dd23f78e12ad42650595bc0f874c88f7": {
					"reference_address": "",
					"owner": "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94",
					"symbol": "STEM",
					"reserve_address": "0xcee3d4755e47055b530deeba062c5bd0c17eb00f",
					"name": "STEM",
					"property": "ecosystem",
					"description": "The Basis of the YGGDRASH Ecosystem. It is also an aggregate and a blockchain containing information of all Branch Chains.",
					"tag": 0.1,
					"version_history": ["0xcc9612ff91ff844938acdb6608e58506a2f21b8a5d77e88726c0897e8d1d02c0"],
					"type": "immunity",
					"version": "0xcc9612ff91ff844938acdb6608e58506a2f21b8a5d77e88726c0897e8d1d02c0",
					"timestamp": 1536755670444
				}
			},
			"status": 1,
			"success": true
		},
		"5a0d54a81a43142c1292fa35c6d333279cf06e2a9d0886c6a713acd6163e0d72": {
			"transactionHash": "5a0d54a81a43142c1292fa35c6d333279cf06e2a9d0886c6a713acd6163e0d72",
			"transactionIndex": 1,
			"blockHash": "0xc6ef2fc5426d6ad6fd9e2a26abeab0aa2411b7ab17f30a99d3cb96aed1d1055b",
			"yeedUsed": 30000,
			"branchAddress": "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
			"txLog": {
				"method": "create",
				"ee0adfb66a13b07d5520adb270f71979b508255d": {
					"owner": "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94",
					"reference_address": "",
					"symbol": "TEST1",
					"reserve_address": "0x2G5f8A319550f80f9D362ab2eE0D1f023EC665a3",
					"name": "TEST1",
					"property": "dex",
					"description": "TEST1",
					"tag": 0.1,
					"version_history": ["0xe1980adeafbb9ac6c9be60955484ab1547ab0b76"],
					"type": "immunity",
					"version": "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76",
					"timestamp": 1536756751728
				}
			},
			"status": 1,
			"success": true
		},
		"d7cf6612214effa4013ac6fee62894c1a0f9b47b1ddcffb2a62a6398b200ff31": {
			"transactionHash": "d7cf6612214effa4013ac6fee62894c1a0f9b47b1ddcffb2a62a6398b200ff31",
			"transactionIndex": 1,
			"blockHash": "0xc6ef2fc5426d6ad6fd9e2a26abeab0aa2411b7ab17f30a99d3cb96aed1d1055b",
			"yeedUsed": 30000,
			"branchAddress": "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
			"txLog": {
				"method": "create",
				"a08ee962cd8b2bd0edbfee989c1a9f7884d26532": {
					"owner": "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94",
					"reference_address": "",
					"symbol": "YEED",
					"reserve_address": "0x9cc060690705a13078634637b1d2a5f2fe1b8096",
					"name": "YEED",
					"property": "currency",
					"description": "YEED is the currency used inside YGGDRASH. The vitality of the new branch chain is decided by the amount of YEED, which will be consumed gradually.",
					"tag": 0.1,
					"version_history": ["0xeeabcaf6cf907e0fd27a0d1f305313e9c1069c5d7f8729d227110012c9b37025"],
					"type": "immunity",
					"version": "0xeeabcaf6cf907e0fd27a0d1f305313e9c1069c5d7f8729d227110012c9b37025",
					"timestamp": 1536756751728
				}
			},
			"status": 1,
			"success": true
		},
		"216a54011c7234651002ead4610d5cb06a19d79ef0278df5251fffe3795b9196": {
			"transactionHash": "216a54011c7234651002ead4610d5cb06a19d79ef0278df5251fffe3795b9196",
			"transactionIndex": 1,
			"blockHash": "0xc6ef2fc5426d6ad6fd9e2a26abeab0aa2411b7ab17f30a99d3cb96aed1d1055b",
			"yeedUsed": 30000,
			"branchAddress": "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
			"txLog": {
				"method": "create",
				"ee0adfb66a13b07d5520adb270f71979b508255d": {
					"owner": "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94",
					"reference_address": "",
					"symbol": "TEST1",
					"reserve_address": "0x2G5f8A319550f80f9D362ab2eE0D1f023EC665a3",
					"name": "TEST1",
					"property": "dex",
					"description": "TEST1",
					"tag": 0.1,
					"version_history": ["0xe1980adeafbb9ac6c9be60955484ab1547ab0b76"],
					"type": "immunity",
					"version": "0xe1980adeafbb9ac6c9be60955484ab1547ab0b76",
					"timestamp": 1536756751728
				}
			},
			"status": 1,
			"success": true
		}
	}
}

// Request (to Yeed chain)
{
	"id": "660112712",
	"jsonrpc": "2.0",
	"method": "getAllTransactionReceipt",
	"params": {
		"branchId": "a08ee962cd8b2bd0edbfee989c1a9f7884d26532"
	}
}

// Response (from Yeed chain)
{
	"jsonrpc": "2.0",
	"id": "660112712",
	"result": {
		"70b7ad44389664f3cb70ec7d9de975bd58e7a52d5810091f8415715deb5119c5": {
			"transactionHash": "70b7ad44389664f3cb70ec7d9de975bd58e7a52d5810091f8415715deb5119c5",
			"transactionIndex": 1,
			"blockHash": "0xc6ef2fc5426d6ad6fd9e2a26abeab0aa2411b7ab17f30a99d3cb96aed1d1055b",
			"yeedUsed": 30000,
			"branchAddress": "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
			"txLog": {
				"balance[0]": 1000000000,
				"method": "genesis",
				"frontier[0]": "c91e9d46dd4b7584f0b6348ee18277c10fd7cb94"
			},
			"status": 0,
			"success": false
		},
		"a27cc5bd50672f04423110f76784d821536c24884b3f1425b25dba3831de3c56": {
			"transactionHash": "a27cc5bd50672f04423110f76784d821536c24884b3f1425b25dba3831de3c56",
			"transactionIndex": 1,
			"blockHash": "0xc6ef2fc5426d6ad6fd9e2a26abeab0aa2411b7ab17f30a99d3cb96aed1d1055b",
			"yeedUsed": 30000,
			"branchAddress": "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
			"txLog": {
				"amount": "100",
				"method": "transfer",
				"from": "aaa2aaab0fb041c5cb2a60a12291cbc3097352bb",
				"to": "e1980adeafbb9ac6c9be60955484ab1547ab0b76"
			},
			"status": 0,
			"success": false
		}
	}
}
```

 
------
 
#### [Contract] query

Handles all queries that are dispatched to the contract

**Parameter**

`DATA`, string - query 

**Returns**

`DATA`, string - result of query

**Example**

```
//request
{
	"id": "706872067",
	"jsonrpc": "2.0",
	"method": "query",
	"params": ["{\"address\":\"fe7b7c93dd23f78e12ad42650595bc0f874c88f7\",\"method\":\"view\",\"params\":[{\"branchId\":\"a08ee962cd8b2bd0edbfee989c1a9f7884d26532\"}]}"]
}

//result
{
	"jsonrpc": "2.0",
	"id": "706872067",
	"result": "{\"result\":\"{\\\"name\\\":\\\"YEED\\\",\\\"owner\\\":\\\"c91e9d46dd4b7584f0b6348ee18277c10fd7cb94\\\",\\\"symbol\\\":\\\"YEED\\\",\\\"property\\\":\\\"currency\\\",\\\"type\\\":\\\"immunity\\\",\\\"timestamp\\\":1536756751728,\\\"description\\\":\\\"YEED is the currency used inside YGGDRASH. The vitality of the new branch chain is decided by the amount of YEED, which will be consumed gradually.\\\",\\\"tag\\\":0.1,\\\"version\\\":\\\"0xeeabcaf6cf907e0fd27a0d1f305313e9c1069c5d7f8729d227110012c9b37025\\\",\\\"version_history\\\":[\\\"0xeeabcaf6cf907e0fd27a0d1f305313e9c1069c5d7f8729d227110012c9b37025\\\"],\\\"reference_address\\\":\\\"\\\",\\\"reserve_address\\\":\\\"0x9cc060690705a13078634637b1d2a5f2fe1b8096\\\"}\"}"
}
```
 
------
 
#### [Peer] add
 
Add Peer
 
**Parameter**
 
`Object` - Peer object
 
**Returns**
 
`Object` -  Added peer object
 
**Example**
 
```
// request
{
	"id": "548925121",
	"jsonrpc": "2.0",
	"method": "add",
	"params": {
		"peer": {
			"host": "127.0.0.1",
			"port": 32918,
			"ynodeUri": "ynode://65bff16c@127.0.0.1:32918"
		}
	}
}

// result
{
	"jsonrpc": "2.0",
	"id": "548925121",
	"result": {
		"host": "127.0.0.1",
		"port": 32918,
		"ynodeUri": "ynode://65bff16c@127.0.0.1:32918"
	}
}
```
 
-----
 
#### [Peer] getAll
 
Returns all peers
  
**Parameter**
  
None
  
**Returns**
  
`DATA` - List of all peers
  
**Example**
  
```
// request
{
	"id": "1634791329",
	"jsonrpc": "2.0",
	"method": "getAll"
}

// result
{
	"jsonrpc": "2.0",
	"id": "1634791329",
	"result": [{
		"host": "localhost",
		"port": 32918,
		"ynodeUri": "ynode://9ea9225f0b7db3c697c0a2e09cdd65046899058d16f73378c1559d61aa3e10cd5dc9337142728f5a02faadafab2b926e2998d5bc2b62c2183fab75ca996de2ce@localhost:32918"
	}]
}
```
  
-----
 
#### [Peer] getAllActivePeer
 
Returns all active peers
  
**Parameter**
  
None
  
**Returns**
  
`DATA`- List of all active Peer as string
  
**Example**
  
```
// request
{
	"id": "1702019309",
	"jsonrpc": "2.0",
	"method": "getAllActivePeer"
}

// result
{
	"jsonrpc": "2.0",
	"id": "1702019309",
	"result": []
}
```
 
-----

#### [admin] nodeHello

Returns a clientHello message(with nonce) for managing node. 

**Parameter**
  
`Object` - Command object

**Returns**
  
`String` - String of Command object 
  
**Example**
  
```
// request
{
    "id":"142596201",
    "jsonrpc":"2.0",
    "method":"nodeHello",
    "params":{
        "command":{
            "header":"{\"timestamp\":\"00000166818E7D38\",\"nonce\":\"0000000000000000baabb165899f98a8\",\"bodyHash\":\"3717ec34f5b0345c3b480d9cd402f0be1111c0e04cb9dbe1da5b933e353a5bba\",\"bodyLength\":\"0000000000000018\"}",
            "signature":"1b9cd47ba5847be5830a25cb94c407eaad0bbabaf0ad1a984dfb1ed04ccbb96e9f3a8b188568ba46415bb6895c166797417547b1029e3074dfd358a12f982a015a",
            "body":"[{\"method\":\"nodeHello\"}]"
        }
    }
}

// result
{
    "jsonrpc": "2.0",
    "id": "142596201",
    "result": "{\"header\":{\"timestamp\":\"000001668a0a2bae\",\"nonce\":\"baabb165899f98a8b90b7251d88f2bcf\",\"bodyHash\":\"f5ef2617558e62b5a882187978f411a6ec1d437bb13a5d94e473e680d8ce7e7c\",\"bodyLength\":\"000000000000001a\"},\"signature\":\"1bab5fbd03a51cddd6b4aa0d2a68453b3073d2aa7aa58190073d1d6e647353bbaa0618d5e3f126fb5275a03f17b304b4a6afd293cf631fed6dfa31fb7c70db1c0d\",\"body\":[{\"method\":\"clientHello\"}]}"
}
```

-----

#### [admin] requestCommand

Returns a responseCommand message(with nonce) for managing node. 

**Parameter**
  
`Object` - Command object

**Returns**
  
`String` - String of Command object 
  
**Example**
  
```
// request
{
    "id":"142596201",
    "jsonrpc":"2.0",
    "method":"requestCommand",
    "params":{
        "command":{
            "header":"{\"timestamp\":\"00000166818E7D38\",\"nonce\":\"28ef9ba1d6a55167aaabb165899f988a\",\"bodyHash\":\"3717ec34f5b0345c3b480d9cd402f0be1111c0e04cb9dbe1da5b933e353a5bba\",\"bodyLength\":\"0000000000000016\"}",
            "signature":"1b9cd47ba5847be5830a25cb94c407eaad0bbabaf0ad1a984dfb1ed04ccbb96e9f3a8b188568ba46415bb6895c166797417547b1029e3074dfd358a12f982a015a",
            "body":"[{\"method\":\"setConfig\",\"params\":\"{network{port:32921},log{level:info}}\"}]"
        }
    }
}

or

{
    "id":"142596201",
    "jsonrpc":"2.0",
    "method":"requestCommand",
    "params":{
        "command":{
            "header":"{\"timestamp\":\"00000166818E7D38\",\"nonce\":\"28ef9ba1d6a55167aaabb165899f988a\",\"bodyHash\":\"3717ec34f5b0345c3b480d9cd402f0be1111c0e04cb9dbe1da5b933e353a5bba\",\"bodyLength\":\"0000000000000016\"}",
            "signature":"1b9cd47ba5847be5830a25cb94c407eaad0bbabaf0ad1a984dfb1ed04ccbb96e9f3a8b188568ba46415bb6895c166797417547b1029e3074dfd358a12f982a015a",
            "body":"[{\"method\":\"restart\"}]"
        }
    }
}

// result
{
    "jsonrpc": "2.0",
    "id": "142596201",
    "result": "{\"header\":{\"timestamp\":\"000001669b8df580\",\"nonce\":\"aaabb165899f988aecd8754326149367\",\"bodyHash\":\"2344c5b38540509e0babd9292553d001fde5bc4a52e8af09221fc7dedc493ee7\",\"bodyLength\":\"000000000000001e\"},\"signature\":\"1c36f6e720b32bd03d981388a0bc103ee5a4e4affee8481f175ab527ed4c189c045105aa4e99f78b64ed4e99f2d8a76fb430ebd88184e744378991a84f41b7650f\",\"body\":[{\"method\":\"responseCommand\"}]}"
}
```

-----
