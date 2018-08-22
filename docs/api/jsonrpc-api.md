#JSON RPC
**Contents**
* [JSON RPC API]()
  * [JavaScript API]()
  * [JSON-RPC Endpoint]()
  * [Curl Examples Explained]()
  * [JSON-RPC methods]()
  * [JSON RPC API Reference]()
     * [createAccount](#create-account)
        * [Parameters]()
        * [Returns]()
        * [Example]()
     * [accounts](#accounts)
        * [Parameters]()
        * [Returns]()
        * [Example]()
     * [balanceOf](#balance-of)
        * [Parameters]()
        * [Returns]()
        * [Example]()
     * [getBalance](#get-balance)
        * [Parameters]()
        * [Returns]()
        * [Example]()
     * [blockNumber](#block-number)
        * [Parameters]()
        * [Returns]()
        * [Example]()
     * [getBlockByHash](#get-block-by-hash)
        * [Parameters]()
        * [Returns]()
        * [Example]()
     * [getBlockByNumber](#get-block-by-number)
        * [Parameters]()
        * [Returns]()
        * [Example]()
     * [newBlockFilter](#new-block-filter)
        * [Parameters]()
        * [Returns]()
        * [Example]()
     * [getTransactionCount](#get-transaction-count)
        * [Parameters]()
        * [Returns]()
        * [Example]()
     * [getBlockTransactionCountByHash](#get-block-transaction-count-by-hash)
        * [Parameters]()
        * [Returns]()
        * [Example]()
     * [getBlockTransactionCountByNumber](#get-block-transaction-count-by-number)
        * [Parameters]()
        * [Returns]()
        * [Example]()
     * [getTransactionByHash](#get-transaction-by-hash)
        * [Parameters]()
        * [Returns]()
        * [Example]()
     * [getTransactionByBlockHashAndIndex](#get-transaction-by-block-hash-and-index)
        * [Parameters]()
        * [Returns]()
        * [Example]()
     * [getTransactionByBlockNumberAndIndex](#get-transaction-by-block-number-and-index)
        * [Parameters]()
        * [Returns]()
        * [Example]()
     * [getTransactionReceipt](#get-transaction-receipt)
        * [Parameters]()
        * [Returns]()
        * [Example]()
     * [sendTransaction](#send-transaction)
        * [Parameters]()
        * [Returns]()
        * [Example]()
     * [sendRawTransaction](#send-raw-transaction)
        * [Parameters]()
        * [Returns]()
        * [Example]()
     * [newPendingTransactionFilter](#new-pending-transaction-filter)
        * [Parameters]()
        * [Returns]()
        * [Example]()
     * [getTransactionReceipt](#get-transaction-receipt)
        * [Parameters]()
        * [Returns]()
        * [Example]()
     * [getAllTransactionReceipt](#get-all-transaction-receipt)
        * [Parameters]()
        * [Returns]()
        * [Example]()
 


#JSON RPC API 

[JSON](http://json.org/) is a lightweight data-interchange format. 
It can represent numbers, strings, ordered sequences of values, and collections of name/value pairs.

[JSON-RPC](https://www.jsonrpc.org/specification) is a stateless, light-weight remote procedure call (RPC) protocol. 
Primarily this specification defines several data structures and the rules around their processing. 
It is transport agnostic in that the concepts can be used within the same process, over sockets, over HTTP, or in many various message passing environments. 
It uses JSON ([RFC 4627](http://www.ietf.org/rfc/rfc4627.txt)) as data format.

##JavaScript API

##JSON-RPC Endpoint

Default JSON-RPC endpoints : 

| Client | URL                                            |
| :----: | :--------------------------------------------: |
| Java   | [http://localhost:8080](http://localhost:8080) |

##Curl Examples Explained

The curl options below might return a response where the node complains about the content type, 
this is because the --data option sets the content type to application/x-www-form-urlencoded . 
If your node does complain, manually set the header by placing -H "Content-Type: application/json" at the start of the call.

The examples also do not include the URL/IP & port combination which must be the last argument given to curl e.x. 127.0.0.1:8080

##JSON-RPC methods

* [createAccount](#create-account)
* [accounts](#accounts)
* [balanceOF](#balance-of)
* [getBalance](#get-balance)
* [blockNumber](#block-number)
* [getBlockByHash](#get-block-by-hash)
* [getBlockByNumber](#get-block-by-number)
* [newBlockFilter](#new-block-filter)
* [getTransactionCount](#get-transaction-count)
* [getBlockTransactionCountByHash](#get-block-transaction-count-by-hash)
* [getBlockTransactionCountByNumber](#get-block-transaction-count-by-number)
* [getTransactionByHash](#get-transaction-by-hash)
* [getTransactionByBlockHashAndIndex](#get-transaction-by-block-hash-and-index)
* [getTransactionByBlockNumberAndIndex](#get-transaction-by-block-number-and-index)
* [getTransactionReceipt](#get-transaction-receipt)
* [sendTransaction](#send-transaction)
* [sendRawTransaction](#send-raw-transaction)
* [newPendingTransactionFilter](#new-pending-transaction-filter)
* [getTransactionReceipt](#get-transaction-receipt)
* [getAllTransactionReceipt](#get-all-transaction-receipt)


##JSON RPC API Reference
  
#### createAccount 

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
{
	"address":"0xe1980adeafbb9ac6c9be60955484ab1547ab0b76",
	"method":"balanceOf",
	"params":[{"address":"0xe1980adeafbb9ac6c9be60955484ab1547ab0b76"}]
}
```

**Returns**

`String of DATA` - balance of address

**Example**

```
// Request
{
    "id":"1890315358",
    "jsonrpc":"2.0",
    "method":"balanceOf",
    "params":{"data":"{\"address\":\"0xe1980adeafbb9ac6c9be60955484ab1547ab0b76\",\"method\":\"balanceOf\",\"params\":[{\"address\":\"0xe1980adeafbb9ac6c9be60955484ab1547ab0b76\"}]}"}
}

// Response
{
    "jsonrpc":"2.0",
    "id":"1890315358",
    "result":"{\"result\":\"8\"}"
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
    "id":"1192709666",
    "jsonrpc":"2.0",
    "method":"blockNumber"
}

// Result
{
    "jsonrpc":"2.0",
    "id":"1192709666",
    "result":0
}
```
  
-----

#### getBlockByHash

Returns information about a block by hash.

**Parameter**

1. `DATA`, 32 Bytes - Hash of a block.
2. `Boolean` - If `true` it returns the full transaction objects, if `false` only the hashes of the transactions.

```
params: [
   '0x2Aa4BCaC31F7F67B9a15681D5e4De2FBc778066A',
   true
]
```

**Returns**

`Object` - A block object, or `null` when no block was found:


**Example**

```
// Request
{
    "id":"1758660442",
    "jsonrpc":"2.0",
    "method":"getBlockByHash",
    "params":{
                "address":"0x2Aa4BCaC31F7F67B9a15681D5e4De2FBc778066A",
                "bool":true
             }
}

// Result
{
    "jsonrpc":"2.0",
    "id":"991236886",
    "result":{
                "header":{
                            "type":"AAAAAA==",
                            "version":"AAAAAA==",
                            "prevBlockHash":null,
                            "merkleRoot":"IFUzinQY/o3s74my9JmeRtJ0UOdlyJL12o7YhmmaO+A=",
                            "timestamp":1532490268914,
                            "dataSize":3,
                            "signature":"G8eZI4PWKUKdNui0ee0CVnHo0hhkcKWTMKx0bpoMVkHQOCu7TxEQMFyoB4tTPs/I6eO+N2/6sK56M+0+4gvvj28=",
                            "index":0,
                            "blockHash":"Ju2hMxEBQfQuDTufXmJBEDXOD6K0v7eL+HjipSvjGc4="
                         },
                "data":{
                            "transactionList":[...],
                            "size":3,
                            "merkleRoot":"IFUzinQY/o3s74my9JmeRtJ0UOdlyJL12o7YhmmaO+A="
                        }
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
// Request
{
    "id":"1759269633",
    "jsonrpc":"2.0",
    "method":"getBlockByNumber",
    "params":{
                "bool":true,
                "hashOfBlock":"0xbbF5029Fd710d227630c8b7d338051B8E76d50B3"
             }
}

// Result
{
    "jsonrpc":"2.0",
    "id":"991236886",
    "result":{
                "header":{
                            "type":"AAAAAA==",
                            "version":"AAAAAA==",
                            "prevBlockHash":null,
                            "merkleRoot":"IFUzinQY/o3s74my9JmeRtJ0UOdlyJL12o7YhmmaO+A=",
                            "timestamp":1532490268914,
                            "dataSize":3,
                            "signature":"G8eZI4PWKUKdNui0ee0CVnHo0hhkcKWTMKx0bpoMVkHQOCu7TxEQMFyoB4tTPs/I6eO+N2/6sK56M+0+4gvvj28=",
                            "index":0,
                            "blockHash":"Ju2hMxEBQfQuDTufXmJBEDXOD6K0v7eL+HjipSvjGc4="
                         },
                "data":{
                            "transactionList":[...],
                            "size":3,
                            "merkleRoot":"IFUzinQY/o3s74my9JmeRtJ0UOdlyJL12o7YhmmaO+A="
                        }
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

#### getTransactionCount

Returns the number of transactions sent from an address.

**Parameter**

`DATA`, 32 Bytes - hash of a block

```
params: [
   '0x407d73d8a49eeb85d32cf465507dd71d507100c1'
]
```

**Returns**

`QUANTITY` - integer of the number of transactions in this block.

**Example**
  
```
// Request
{
    "id":"396122987",
    "jsonrpc":"2.0",
    "method":"getTransactionCount",
    "params":{
                "address":"0x407d73d8a49eeb85d32cf465507dd71d507100c1",
                "tag":"latest"
             }
}

// Result
{
    "jsonrpc":"2.0",
    "id":"396122987",
    "result":1
}
```
 
-----

#### getBlockTransactionCountByHash

Returns the number of transactions in a block from a block matching the given block hash.

**Parameter**

`DATA`, 32 Bytes - hash of a block

**Returns**

`QUANTITY` - integer of the number of transactions in this block.

**Example**
  
```
// Request
{
    "id":"2002392382",
    "jsonrpc":"2.0",
    "method":"getBlockTransactionCountByHash",
    "params":{
                "hashOfBlock":"0xbd729cb4ecbcbd3fc66bedb43dbb856f5e71ebefff95fc9503b92921b8466bab"
             }
}

// Result
{
    "jsonrpc":"2.0",
    "id":"2002392382",
    "result":3
}
```
 
-----

#### getBlockTransactionCountByNumber

Returns the number of transactions in a block matching the given block number.

**Parameter**

`QUANTITY|TAG` - integer of a block number, or the string `"earliest"`, `"latest"` or `"pending"`, as in the default block parameter.

```
params: [
   "latest"
]
```

**Returns**

`QUANTITY` - integer of the number of transactions in this block.

**Example**
  
```
// Request
{
    "id":"446057403",
    "jsonrpc":"2.0",
    "method":"getBlockTransactionCountByNumber",
    "params":{
                "blockNumber":1
             }
}

// Result
{
    "jsonrpc":"2.0",
    "id":"446057403",
    "result":4
}
```
 
-----

#### getTransactionByHash

Returns the information about a transaction requested by transaction hash.

**Parameter**

`DATA`, 32 Bytes - hash of a transaction

```
params: [
   "0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf"
]
```

**Returns**

`Object` - A transaction object, or `null` when no transaction was found:

**Example**
 
```
// Request
{
    "id":"1860630157",
    "jsonrpc":"2.0",
    "method":"getTransactionByHash",
    "params":{
                "hashOfTx":"0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf"
             }
}

// Result
{
    "jsonrpc":"2.0",
    "id":"478987002",
    "result":{
                "header":{
                            "type":"AAAAAA==","
                            version":"AAAAAA==",
                            "dataHash":"hFxESRGFle2H2AXpEul4BKnrYGnXsqrZQTcxA4bW4eE=",
                            "timestamp":160992683505627,
                            "dataSize":85,
                            "signature":"HBWog1oLhusOpum914dUUS8WFQvnvpu+6JzEeeEpJDaiK+JYYgHrvNP9pCyTE29oCJYl68G/rYgNmZba2FZZXQU=",
                            "address":"qqKqqw+wQcXLKmChIpHLwwlzUrs=",
                            "hashString":"b32712df40cc2075f4b2ae15da1039ca2af17a6d57342ecf7e7e90c321a315ca",
                            "hash":"sycS30DMIHX0sq4V2hA5yirxem1XNC7Pfn6QwyGjFco=",
                            "pubKey":"BJ6pIl8LfbPGl8Ci4JzdZQRomQWNFvczeMFVnWGqPhDNXckzcUJyj1oC+q2vqyuSbimY1bwrYsIYP6t1yplt4s4=",
                            "addressToString":"aaa2aaab0fb041c5cb2a60a12291cbc3097352bb",
                            "signDataHash":"6lA759wPtw0DU/rAqE03f+jhKmDeU7ulaDfu6H4Wkkk="
                         },
                "data":"{
                            "operator":"transfer",
                            "to":"0x9843DC167956A0e5e01b3239a0CE2725c0631392",
                            "value":100}
                        }"
             }
}
```
 
-----

#### getTransactionByBlockHashAndIndex

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
// Request
{
    "id":"1897986394",
    "jsonrpc":"2.0",
    "method":"getTransactionByBlockHashAndIndex",
    "params":{
                "txIndexPosition":1,
                "hashOfBlock":"0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf"
             }
}

// Result
{
    "jsonrpc":"2.0",
    "id":"478987002",
    "result":{
                "header":{
                            "type":"AAAAAA==","
                            version":"AAAAAA==",
                            "dataHash":"hFxESRGFle2H2AXpEul4BKnrYGnXsqrZQTcxA4bW4eE=",
                            "timestamp":160992683505627,
                            "dataSize":85,
                            "signature":"HBWog1oLhusOpum914dUUS8WFQvnvpu+6JzEeeEpJDaiK+JYYgHrvNP9pCyTE29oCJYl68G/rYgNmZba2FZZXQU=",
                            "address":"qqKqqw+wQcXLKmChIpHLwwlzUrs=",
                            "hashString":"b32712df40cc2075f4b2ae15da1039ca2af17a6d57342ecf7e7e90c321a315ca",
                            "hash":"sycS30DMIHX0sq4V2hA5yirxem1XNC7Pfn6QwyGjFco=",
                            "pubKey":"BJ6pIl8LfbPGl8Ci4JzdZQRomQWNFvczeMFVnWGqPhDNXckzcUJyj1oC+q2vqyuSbimY1bwrYsIYP6t1yplt4s4=",
                            "addressToString":"aaa2aaab0fb041c5cb2a60a12291cbc3097352bb",
                            "signDataHash":"6lA759wPtw0DU/rAqE03f+jhKmDeU7ulaDfu6H4Wkkk="
                         },
                "data":"{
                            "operator":"transfer",
                            "to":"0x9843DC167956A0e5e01b3239a0CE2725c0631392",
                            "value":100
                        }"
             }
}
```
 
-----

#### getTransactionByBlockNumberAndIndex

Returns information about a transaction by block number and transaction index position.

**Parameter**

1. `QUANTITY|TAG` - a block number, or the string `"earliest"`, `"latest"` or `"pending"`, as in the default block parameter.
2. `QUANTITY` - the transaction index position.

```
params: [
   1
   235 // 0
]
```

**Returns**

See [getTransactionByHash]()

**Example**
  
```
// Request
{
    "id":"1977444593",
    "jsonrpc":"2.0",
    "method":"getTransactionByBlockNumberAndIndex",
    "params":{
                "txIndexPosition":1,
                "blockNumber":235
             }
}

// Result
{
    "jsonrpc":"2.0",
    "id":"478987002",
    "result":{
                "header":{
                            "type":"AAAAAA==","
                            version":"AAAAAA==",
                            "dataHash":"hFxESRGFle2H2AXpEul4BKnrYGnXsqrZQTcxA4bW4eE=",
                            "timestamp":160992683505627,
                            "dataSize":85,
                            "signature":"HBWog1oLhusOpum914dUUS8WFQvnvpu+6JzEeeEpJDaiK+JYYgHrvNP9pCyTE29oCJYl68G/rYgNmZba2FZZXQU=",
                            "address":"qqKqqw+wQcXLKmChIpHLwwlzUrs=",
                            "hashString":"b32712df40cc2075f4b2ae15da1039ca2af17a6d57342ecf7e7e90c321a315ca",
                            "hash":"sycS30DMIHX0sq4V2hA5yirxem1XNC7Pfn6QwyGjFco=",
                            "pubKey":"BJ6pIl8LfbPGl8Ci4JzdZQRomQWNFvczeMFVnWGqPhDNXckzcUJyj1oC+q2vqyuSbimY1bwrYsIYP6t1yplt4s4=",
                            "addressToString":"aaa2aaab0fb041c5cb2a60a12291cbc3097352bb",
                            "signDataHash":"6lA759wPtw0DU/rAqE03f+jhKmDeU7ulaDfu6H4Wkkk="
                         },
                "data":"{
                            "operator":"transfer",
                            "to":"0x9843DC167956A0e5e01b3239a0CE2725c0631392",
                            "value":100
                        }"
             }
}
```
 
-----

#### getTransactionReceipt

Returns the receipt of a transaction by transaction hash.

**Parameter**

`DATA`, 32 Bytes - hash of a transaction

```
params: [
   '0xbd729cb4ecbcbd3fc66bedb43dbb856f5e71ebefff95fc9503b92921b8466bab'
]
```

**Returns**

`Object` - A transaction receipt object, or `null` when no receipt was found:

**Example**
  
```
// Request
{
    "id":"981029532",
    "jsonrpc":"2.0",
    "method":"getTransactionReceipt",
    "params":{
                "hashOfTx":"0xbd729cb4ecbcbd3fc66bedb43dbb856f5e71ebefff95fc9503b92921b8466bab"
             }
}

// Result
{
    "jsonrpc":"2.0",
    "id":"17020410",
    "result":{
                "transactionHash":"0xb903239f8543d04b5dc1ba6579132b143087c68db1b2168786408fcbce568238",
                "transactionIndex":1,
                "blockHash":"0xc6ef2fc5426d6ad6fd9e2a26abeab0aa2411b7ab17f30a99d3cb96aed1d1055b",
                "YeedUsed":30000,
                "branchAddress":"0xb60e8dd61c5d32be8058bb8eb970870f07233155",
                "txLog":[],
                "status":1
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
// Request
{
  "id": "401735777",
  "jsonrpc": "2.0",
  "method": "sendTransaction",
  "params": {
    "tx": {     
      "type": "AAAAAQ==",
      "version": "AAAAAQ==",
      "dataHash": "xIg9gIny5ZGj8bgGfqlkpJGPq6/tCcLpaPZ6xncgLMg=",
      "dataSize": 103,
      "timestamp": 95282891139918,      
      "signature": "G7HymgvvgSCWiTEl2uyxzOvuglUNLzPN+ismFLYg+D4+I4S8IJDC5FFdlE76SH0WKnosL96BlhHL7agNlxRKtmk=",
      "data": "{\"method\":\"transfer\",\"params\":[{\"address\":\"aaa2aaab0fb041c5cb2a60a12291cbc3097352bb\"},{\"amount\":5000}]}",
    }
  }
}

// Result
{
  "jsonrpc": "2.0",
  "id": "401735777",
  "result": "2da7c6e0bbb35365a1b57c7cdb9b5a434a41c43b6374018d9303b87576c157a9"
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
// Request
{
    "id":"1708484786",
    "jsonrpc":"2.0",
    "method":"newPendingTransactionFilter"
}

// Result
{
    "jsonrpc":"2.0",
    "id":"1708484786",
    "result":6
}
```
 
-----

## getTransactionReceipt

Returns the TransactionRecipt of transaction hash.

**Parameter**

`DATA`, 32 Bytes - the transaction hash

**Returns**

`Object` - A TransactionReceipt object, or null when no TransactionReceipt was found:

**Example**

```
// Request
{
  "id": "1890315358",
  "jsonrpc": "2.0",
  "method": "getTransactionReceipt",
  "params": {
    "hashOfTx": "2da7c6e0bbb35365a1b57c7cdb9b5a434a41c43b6374018d9303b87576c157a9"
  }
}

// Result
{
  "jsonrpc": "2.0",
  "id": "1890315358",
  "result": {
    "transactionHash": "2da7c6e0bbb35365a1b57c7cdb9b5a434a41c43b6374018d9303b87576c157a9",
    "transactionIndex": 1,
    "blockHash": "0xc6ef2fc5426d6ad6fd9e2a26abeab0aa2411b7ab17f30a99d3cb96aed1d1055b",
    "yeedUsed": 30000,
    "branchAddress": "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
    "txLog": {
      "amount": "5000",
      "from": "6f19c769c78513a3a60a3618c6a11eb9a886086a",
      "to": "aaa2aaab0fb041c5cb2a60a12291cbc3097352bb"
    },
    "status": 1
  }
}
```
 
-----

## getTransactionReceipt

Returns all TransactionReceipts.

**Parameter**

None

**Returns**

`HashMap of DATA`, List of all TransactionReceipts

**Example**

```
// Request
{
  "id": "1890315358",
  "jsonrpc": "2.0",
  "method": "getAllTransactionReceipt",
  "params": {
     
  }
}

// Result
{
  "jsonrpc": "2.0",
  "id": "1890315358",
  "result": {
    "2da7c6e0bbb35365a1b57c7cdb9b5a434a41c43b6374018d9303b87576c157a9": {
      "transactionHash": "2da7c6e0bbb35365a1b57c7cdb9b5a434a41c43b6374018d9303b87576c157a9",
      "transactionIndex": 1,
      "blockHash": "0xc6ef2fc5426d6ad6fd9e2a26abeab0aa2411b7ab17f30a99d3cb96aed1d1055b",
      "yeedUsed": 30000,
      "branchAddress": "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
      "txLog": {
        "amount": "5000",
        "from": "6f19c769c78513a3a60a3618c6a11eb9a886086a",
        "to": "aaa2aaab0fb041c5cb2a60a12291cbc3097352bb"
      },
      "status": 1
    }
  }
}
```







































