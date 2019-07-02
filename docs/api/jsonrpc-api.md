# JSON RPC
**Contents**
* [JSON RPC API](#json-rpc-api)
  * [JavaScript API](#javascript-api)
  * [JSON-RPC Endpoint](#json-rpc-endpoint)
  * [Curl Examples Explained](#curl-examples-explained)
  * [JSON RPC API Reference](#json-rpc-api-reference)
     * [Branch](#branch)
        * [getBranches](#getbranches)
        * [getValidators](#getvalidators)
     * [Block](#block)   
         * [blockNumber](#blocknumber)
         * [getBlockByHash](#getblockbyhash)
         * [getBlockByNumber](#getblockbynumber)        
     * [Transaction](#transaction)
         * [getTransactionCountByBlockHash](#gettransactioncountbyblockhash)
         * [getTransactionCountByBlockNumber](#gettransactioncountbyblocknumber)
         * [getTransactionByHash](#gettransactionbyhash)
         * [getTransactionByBlockHash](#gettransactionbyblockhash)
         * [getTransactionByBlockNumber](#gettransactionbyblocknumber)
         * [getTransactionReceipt](#gettransactionreceipt)
         * [sendTransaction](#sendtransaction)
         * [sendRawTransaction](#sendrawtransaction)
         * [newPendingTransactionFilter](#newpendingtransactionfilter)
         * [getTransactionReceipt](#gettransactionreceipt)     
     * [Contract](#contract)
         * [query](#query)
     * [Log](#log)
         * [getLog](#getlog)
         * [getLogs](#getlogs)
         * [curIndex](#curlindex)
     * [Peer](#peer)
         * [getAllActivePeer](#getallactivepeer)
     * [Admin](#admin)        
         * [nodeHello](#nodehello)
         * [requestCommand](#requestcommand) 

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

| Client | Category    | URL                                                                            |
| :----: | :---------- | :----------------------------------------------------------------------------- |
|        | Branch      | [http://localhost:8080/api/branch](http://localhost:8080/api/branch)           |
|        | Block       | [http://localhost:8080/api/block](http://localhost:8080/api/block)             |
|        | Transaction | [http://localhost:8080/api/transaction](http://localhost:8080/api/transaction) |
|        | Contract    | [http://localhost:8080/api/contract](http://localhost:8080/api/contract)       |
|        | Log         | [http://localhost:8080/api/log](http://localhost:8080/api/log)                 |
|        | Peer        | [http://localhost:8080/api/peer](http://localhost:8080/api/peer)               |
|        | Admin       | [http://localhost:8080/api/admin](http://localhost:8080/api/admin)             |

## Curl Examples Explained

The curl options below might return a response where the node complains about the content type, 
this is because the --data option sets the content type to application/x-www-form-urlencoded . 
If your node does complain, manually set the header by placing -H "Content-Type: application/json" at the start of the call.

The examples also do not include the URL/IP & port combination which must be the last argument given to curl e.x. 127.0.0.1:8080

## JSON RPC API Reference

### Branch 

- **URL** : _`/api/branch`_ 

  
-----

#### getBranches

Returns the spec of all running branches in the node.

**Parameter**
  
none
  
**Returns**
  
`DATA`-  The branch objects.
  
**Example** 

 ```
// Request
{  
   "id":"1538349352",
   "jsonrpc":"2.0",
   "method":"getBranches"
}
 
// Result
{  
   "jsonrpc":"2.0",
   "id":"1538349352",
   "result":{  
      "63589382e2e183e2a6969ebf57bd784dcb29bd43":{  
         "name":"YGGDRASH",
         "symbol":"YGGDRASH",
         "property":"platform",
         "description":"TRUST-based Multi-dimensional Blockchains",
         "contracts":[  
            {  
               "contractVersion":"178b44b22d8c6d5bb08175fa2fcab15122ca8d1e",
               "init":{  

               },
               "description":"The Basis of the YGGDRASH Ecosystem. It is also an aggregate and a blockchain containing information of all Branch Chains.",
               "name":"STEM",
               "isSystem":true
            },
            {  
               "contractVersion":"6a2371e34b780dd39bd56002b1d96c23689cc5dc",
               "isSystem":true,
               "init":{  
                  "alloc":{  
                     "101167aaf090581b91c08480f6e559acdd9a3ddd":{  
                        "balance":"1000000000000000000000"
                     },
                     "ffcbff030ecfa17628abdd0ff1990be003da35a2":{  
                        "balance":"1000000000000000000000"
                     },
                     "b0aee21c81bf6057efa9a321916f0f1a12f5c547":{  
                        "balance":"1000000000000000000000"
                     },
                     "1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e":{  
                        "balance":"1000000000000000000000"
                     },
                     "57c6510966903044581c148bb67eb47dbbeebef1":{  
                        "balance":"1000000000000000000000"
                     },
                     "d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95":{  
                        "balance":"1000000000000000000000"
                     },
                     "cee3d4755e47055b530deeba062c5bd0c17eb00f":{  
                        "balance":"994000000000000000000000"
                     },
                     "4e5cbe1d0db35add81e7f2840eeb250b5b469161":{  
                        "balance":"994000000000000000000000"
                     }
                  }
               },
               "description":"YEED is the currency used inside YGGDRASH. The vitality of the new branch chain is decided by the amount of YEED, which will be consumed gradually.",
               "name":"YEED"
            },
            {  
               "contractVersion":"30783a1311b9c68dd3a92596d650ae6914b01658",
               "isSystem":true,
               "init":{  
                  "validators":[  
                     "101167aaf090581b91c08480f6e559acdd9a3ddd",
                     "ffcbff030ecfa17628abdd0ff1990be003da35a2",
                     "b0aee21c81bf6057efa9a321916f0f1a12f5c547",
                     "4e5cbe1d0db35add81e7f2840eeb250b5b469161",
                     "77283a04b3410fe21ba5ed04c7bd3ba89e70b78c",
                     "9911fb4663637706811a53a0e0b4bcedeee38686",
                     "2ee2eb80c93d031147c21ba8e2e0f0f4a33f5312",
                     "51e2128e8deb622c2ec6dc38f9d895f0be044eb4",
                     "047269a50640ed2b0d45d461488c13abad1e0fac",
                     "21640f2116389a3e37462fd6b68b969e490b6a50",
                     "63fef4912dc8b0781351b18eb9be450638ea2c17",
                     "34e3b1fb13c865fd558e7aa1081377bb5dca43cb",
                     "75fad0fa463b34e659d2bb3b9324b32faed67863",
                     "af79407d6c55c950c09ba4a30f8eae55f05508a2",
                     "f871740aedb55e0d4f110502d5221c4a648f4c27",
                     "f0a714707c286ba16e32fe18e90c9e13922edf5d",
                     "d6f2eae80c8dfedc279111b4ebf2298de9d62b02",
                     "f18db7b37206c58eb7ff8b6a0bc903f76dfcd0d8",
                     "ff50d378b0d6f642826efd475508f372aa2e858a",
                     "ab5506912430d4fc539f21afb0f47d2244cdfa76",
                     "70507099ee03b3e67b5b343d483e0e835018db4b",
                     "e58d0858512a047b2debbcfeab318bb5eeec7dee",
                     "5519a46223c025707a12dda2d3fffe9634b274c0",
                     "8a99217f44c65287a9cc12523b80c3e57782afb1",
                     "fc534627231364101088709fdf030d99f1c52d38"
                  ]
               },
               "name":"DPoA",
               "description":"This contract is for a validator."
            }
         ],
         "timestamp":"000001674dc56231",
         "consensus":{  
            "algorithm":"pbft",
            "period":"* * * * * *"
         }
      }
   }
}
```
 
-----
 
#### getValidators

Returns the validators of all running branches in the node

**Parameter**

`DATA`, 20 Bytes - branchId

**Returns**

`Array`- A list of all validators

**Example**

```
// Request
{  
   "id":"182278868",
   "jsonrpc":"2.0",
   "method":"getValidators",
   "params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43"
   }
 }
 
// Result
{  
   "jsonrpc":"2.0",
   "id":"182278868",
   "result":[  
      "101167aaf090581b91c08480f6e559acdd9a3ddd",
      "e58d0858512a047b2debbcfeab318bb5eeec7dee",
      "9911fb4663637706811a53a0e0b4bcedeee38686",
      "ffcbff030ecfa17628abdd0ff1990be003da35a2",
      "21640f2116389a3e37462fd6b68b969e490b6a50",
      "34e3b1fb13c865fd558e7aa1081377bb5dca43cb",
      "af79407d6c55c950c09ba4a30f8eae55f05508a2",
      "75fad0fa463b34e659d2bb3b9324b32faed67863",
      "4e5cbe1d0db35add81e7f2840eeb250b5b469161",
      "2ee2eb80c93d031147c21ba8e2e0f0f4a33f5312",
      "70507099ee03b3e67b5b343d483e0e835018db4b",
      "b0aee21c81bf6057efa9a321916f0f1a12f5c547",
      "047269a50640ed2b0d45d461488c13abad1e0fac",
      "d6f2eae80c8dfedc279111b4ebf2298de9d62b02",
      "ab5506912430d4fc539f21afb0f47d2244cdfa76",
      "ff50d378b0d6f642826efd475508f372aa2e858a",
      "5519a46223c025707a12dda2d3fffe9634b274c0",
      "f18db7b37206c58eb7ff8b6a0bc903f76dfcd0d8",
      "77283a04b3410fe21ba5ed04c7bd3ba89e70b78c",
      "f871740aedb55e0d4f110502d5221c4a648f4c27",
      "63fef4912dc8b0781351b18eb9be450638ea2c17",
      "51e2128e8deb622c2ec6dc38f9d895f0be044eb4",
      "8a99217f44c65287a9cc12523b80c3e57782afb1",
      "fc534627231364101088709fdf030d99f1c52d38",
      "f0a714707c286ba16e32fe18e90c9e13922edf5d"
   ]
}
```
 
------

### Block

- **URL** : _`/api/block`_
 
 
-----

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

1. `DATA`, 20 bytes - BranchId.
2. `DATA`, 32 Bytes - Hash of a block.
3. `Boolean` - If `true` it returns the full transaction objects, if `false` only the hashes of the transactions.

```
"params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "blockId":"a726170b6aa17429b79854dfff1519ac338deabc345c1a99a3a727fbc358c555",
      "bool":true
}
```

**Returns**

`Object` - A block object, or `null` when no block was found:

**Example**

```
/// Request 
{  
   "id":"65210285",
   "jsonrpc":"2.0",
   "method":"getBlockByHash",
   "params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "blockId":"a726170b6aa17429b79854dfff1519ac338deabc345c1a99a3a727fbc358c555",
      "bool":true
   }
}
 
 // Result
 {  
    "jsonrpc":"2.0",
    "id":"65210285",
    "result":{  
       "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
       "version":"0000000000000000",
       "type":"0000000000000000",
       "prevBlockId":"6720d6fa7fb34265aed9fa6f791fdab9193209671b080b6b0b7519c67424cdd0",
       "index":5806,
       "timestamp":"2019-06-28T02:11:29.211Z",
       "merkleRoot":"0000000000000000000000000000000000000000000000000000000000000000",
       "bodyLength":0,
       "txSize":0,
       "signature":"1b04a8af7d6fb0313c9d8e302288894485499c2576668955f687dd9914b071b90b6fde2cbbe1d1dc0b538ebd6dfe72c8e7cc1a7c6fc6c18a14c1d016854d6f4722",
       "body":[],
       "author":"2ee2eb80c93d031147c21ba8e2e0f0f4a33f5312",
       "blockId":"a726170b6aa17429b79854dfff1519ac338deabc345c1a99a3a727fbc358c555",
       "consensusMessages":"{\"prePrepare\":{\"type\":\"PREPREPA\",\"viewNumber\":5806,\"seqNumber\":5806,\"hash\":\"a726170b6aa17429b79854dfff1519ac338deabc345c1a99a3a727fbc358c555\",\"signature\":\"1b4395dfdc8a581c0dde3dc2d60694b03d33c594813c94ffb97ee1eec7b079862e0785e16af53dbcf11c3fb4df81bb78e9a1d20286f7711f2e748e5b309b390855\",\"block\":{\"header\":{\"chain\":\"63589382e2e183e2a6969ebf57bd784dcb29bd43\",\"version\":\"0000000000000000\",\"type\":\"0000000000000000\",\"prevBlockHash\":\"6720d6fa7fb34265aed9fa6f791fdab9193209671b080b6b0b7519c67424cdd0\",\"index\":\"00000000000016ae\",\"timestamp\":\"0000016b9bda153b\",\"merkleRoot\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"bodyLength\":\"0000000000000000\"},\"signature\":\"1b04a8af7d6fb0313c9d8e302288894485499c2576668955f687dd9914b071b90b6fde2cbbe1d1dc0b538ebd6dfe72c8e7cc1a7c6fc6c18a14c1d016854d6f4722\",\"body\":[]}},\"prepareList\":[{\"type\":\"PREPAREM\",\"viewNumber\":5806,\"seqNumber\":5806,\"hash\":\"a726170b6aa17429b79854dfff1519ac338deabc345c1a99a3a727fbc358c555\",\"signature\":\"1b4b120dcfbfca700ab9daa9291028f27e1a3f86db300a6e5e6ed17ae64b06c993660c310ef19f63419afcf43644ef246a8fd3888d4ea98ed579581814bd84360b\"},{\"type\":\"PREPAREM\",\"viewNumber\":5806,\"seqNumber\":5806,\"hash\":\"a726170b6aa17429b79854dfff1519ac338deabc345c1a99a3a727fbc358c555\",\"signature\":\"1bf61e23085a3953f15e50c216bda32f8cd1e9972b5c736f93deff4b332d82cd9f0157346f79f626fed2b6cdcfd1d1672cf0b3d02f17040281c8d3f96c739b1ae6\"},{\"type\":\"PREPAREM\",\"viewNumber\":5806,\"seqNumber\":5806,\"hash\":\"a726170b6aa17429b79854dfff1519ac338deabc345c1a99a3a727fbc358c555\",\"signature\":\"1c0c6b42c0ab673941182dfa67878447f56b546876c8a441c93ea4db03d33b7f0933d8eb5483afbf9962b2f619c0127515cbfc7ecc71bc1d7ba0d93287c6b496bf\"},{\"type\":\"PREPAREM\",\"viewNumber\":5806,\"seqNumber\":5806,\"hash\":\"a726170b6aa17429b79854dfff1519ac338deabc345c1a99a3a727fbc358c555\",\"signature\":\"1c0d9a1aece7d1c30f57d3ee12b6537f0d3f1ab388b33774f1019fa017b1b6cf176d163ba63770fea2097d8088c914a31f79a1e837c0f197d04c87b14e4433499a\"},{\"type\":\"PREPAREM\",\"viewNumber\":5806,\"seqNumber\":5806,\"hash\":\"a726170b6aa17429b79854dfff1519ac338deabc345c1a99a3a727fbc358c555\",\"signature\":\"1c987220ed31860e9f680789e7790d5a309fb5d95e5ec3ce11608a7766c483b7d1752ea6900e790ee8b586ec7b748e6b4f851455b1c5719f207af8570ee117f436\"}],\"commitList\":[{\"type\":\"COMMITMS\",\"viewNumber\":5806,\"seqNumber\":5806,\"hash\":\"a726170b6aa17429b79854dfff1519ac338deabc345c1a99a3a727fbc358c555\",\"signature\":\"1b733d171c5cd2095cab473b09b9cf84fb87ccdccf5fce20366617a653d75a06816a6b42d5bb956064034304a27a76507291f6475d998ae0ec87ac85985da3d19f\"},{\"type\":\"COMMITMS\",\"viewNumber\":5806,\"seqNumber\":5806,\"hash\":\"a726170b6aa17429b79854dfff1519ac338deabc345c1a99a3a727fbc358c555\",\"signature\":\"1bb400a607135a80f536a6bd19095d7e4dd06ea7294793734ea894d26e3cee2b547598e8647233cbc37a936326d948d590ee1be4b72a9e619e088128300f841ccb\"},{\"type\":\"COMMITMS\",\"viewNumber\":5806,\"seqNumber\":5806,\"hash\":\"a726170b6aa17429b79854dfff1519ac338deabc345c1a99a3a727fbc358c555\",\"signature\":\"1bef88311ee002386a9d5586665b57d90a750528d27eea888023d329362eb55d602c35021a0fc77d02e39f212d275cbb213cb3cd8064f4544fa3c11dcc3e16efa3\"},{\"type\":\"COMMITMS\",\"viewNumber\":5806,\"seqNumber\":5806,\"hash\":\"a726170b6aa17429b79854dfff1519ac338deabc345c1a99a3a727fbc358c555\",\"signature\":\"1c573078bf89a7908521306876b14ccbf7cfba40483fc63b133252c271c895958205fc3e2ee2cc580cd82468b634995ad957504f2d5e71d81a48c0bf44e9a061a4\"},{\"type\":\"COMMITMS\",\"viewNumber\":5806,\"seqNumber\":5806,\"hash\":\"a726170b6aa17429b79854dfff1519ac338deabc345c1a99a3a727fbc358c555\",\"signature\":\"1cd97ee762f929d88618bfee7939fb6c17a36b746ad95e925de1bac9c1822fffea33f4a455208dab7cbe0e56105e6012ff1136f7129c2da4e35888eb007ebf6971\"}]}"
    }
 }
```
  
-----

#### getBlockByNumber

Returns information about a block by block number.

**Parameter**

1. `DATA`, 20 bytes - BranchId. 
2. `QUANTITY|TAG` - integer of a block number, or the string `"earliest"`, `"latest"` or `"pending"`, as in the default block parameter.
3. `Boolean` - If `true` it returns the full transaction objects, if `false` only the hashes of the transactions.

```
params: [
    "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
    "numOfBlock":6830,
    "bool":true
]
```

**Returns**

See [getBlockByHash]()

**Example**

```
// Request
{  
   "id":"2001111864",
   "jsonrpc":"2.0",
   "method":"getBlockByNumber",
   "params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "numOfBlock":6830,
      "bool":true
   }
}
 
// Result 
{  
   "jsonrpc":"2.0",
   "id":"2001111864",
   "result":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "version":"0000000000000000",
      "type":"0000000000000000",
      "prevBlockId":"bc0a19cbf558985cd2630f0f171d65c6da9d58561087d19db700b2aa3b149e7e",
      "index":6830,
      "timestamp":"2019-06-28T02:28:33.086Z",
      "merkleRoot":"0000000000000000000000000000000000000000000000000000000000000000",
      "bodyLength":0,
      "txSize":0,
      "signature":"1c7c8cc5c37a6851cdfad2798abdd3f6c6de759da4b497ed34ecae682f2dcece4258bdadb65689c4d5069e0a469ddb93ad79b25267fe4c99f043f414b39774fc44",
      "body":[],
      "author":"047269a50640ed2b0d45d461488c13abad1e0fac",
      "blockId":"d2f03a22b83d096dd384a3b3d22c79397c8895158b66d988e3e67d970d547e15",
      "consensusMessages":"{\"prePrepare\":{\"type\":\"PREPREPA\",\"viewNumber\":6830,\"seqNumber\":6830,\"hash\":\"d2f03a22b83d096dd384a3b3d22c79397c8895158b66d988e3e67d970d547e15\",\"signature\":\"1c546bdeffaad86d6c9076072cde171cca882261e3b532fd147af430ad0bbf05c11aa306ad74eda63ee8b9fc15c4e1a032b02ea29cec1c6953e07222238c518a34\",\"block\":{\"header\":{\"chain\":\"63589382e2e183e2a6969ebf57bd784dcb29bd43\",\"version\":\"0000000000000000\",\"type\":\"0000000000000000\",\"prevBlockHash\":\"bc0a19cbf558985cd2630f0f171d65c6da9d58561087d19db700b2aa3b149e7e\",\"index\":\"0000000000001aae\",\"timestamp\":\"0000016b9be9b4be\",\"merkleRoot\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"bodyLength\":\"0000000000000000\"},\"signature\":\"1c7c8cc5c37a6851cdfad2798abdd3f6c6de759da4b497ed34ecae682f2dcece4258bdadb65689c4d5069e0a469ddb93ad79b25267fe4c99f043f414b39774fc44\",\"body\":[]}},\"prepareList\":[{\"type\":\"PREPAREM\",\"viewNumber\":6830,\"seqNumber\":6830,\"hash\":\"d2f03a22b83d096dd384a3b3d22c79397c8895158b66d988e3e67d970d547e15\",\"signature\":\"1bc295b99caa9550600f30e4dc992504a65f09e912cafffca6282238b5a716a6e161e89dc1cde838dc09359b8fd392dbceb6ec19462f54f59b44b342605ea641fb\"},{\"type\":\"PREPAREM\",\"viewNumber\":6830,\"seqNumber\":6830,\"hash\":\"d2f03a22b83d096dd384a3b3d22c79397c8895158b66d988e3e67d970d547e15\",\"signature\":\"1bf8c94174cdd23e4fe77b4340ccf4b60ea5a3fa59fbe9c9f5f4a24cfc701552243642009591d62ed2eb0c99cdb4ee2db8a2a0e006e68fb97c0bcd907ea068299f\"},{\"type\":\"PREPAREM\",\"viewNumber\":6830,\"seqNumber\":6830,\"hash\":\"d2f03a22b83d096dd384a3b3d22c79397c8895158b66d988e3e67d970d547e15\",\"signature\":\"1c01108771614283e70e59dd9efa8d5211ef8991470ef8e811a6c1639c447378ff03154bc17cef85566009cea678a55ad5914e3103401cfb1ebacf53f08c6c0077\"},{\"type\":\"PREPAREM\",\"viewNumber\":6830,\"seqNumber\":6830,\"hash\":\"d2f03a22b83d096dd384a3b3d22c79397c8895158b66d988e3e67d970d547e15\",\"signature\":\"1c0a454cb1bc01fd5009d65c900518e6fca4c04dc9d21d80cee956ad716b19399a115d01b5763af86a5d5717a9949deb3757493fc010ce65db4eb71e1e2841de42\"},{\"type\":\"PREPAREM\",\"viewNumber\":6830,\"seqNumber\":6830,\"hash\":\"d2f03a22b83d096dd384a3b3d22c79397c8895158b66d988e3e67d970d547e15\",\"signature\":\"1cedb1b51ae62df3e23f48112b25d24619bc32b7f903b10ab861b39283faac940d7afc4cd5f2829ff139102312ec59d895791062a9dd0ee6da999f6649e92b45e1\"}],\"commitList\":[{\"type\":\"COMMITMS\",\"viewNumber\":6830,\"seqNumber\":6830,\"hash\":\"d2f03a22b83d096dd384a3b3d22c79397c8895158b66d988e3e67d970d547e15\",\"signature\":\"1b1b9d4744c9d92a957d192ef705a27abf7d15d4098a6c57bc5ef1e1b3cca993e61d65f281104267ccad8327f72888e57d526811a803dde071ba01608c7153e096\"},{\"type\":\"COMMITMS\",\"viewNumber\":6830,\"seqNumber\":6830,\"hash\":\"d2f03a22b83d096dd384a3b3d22c79397c8895158b66d988e3e67d970d547e15\",\"signature\":\"1b63e8ccf383ee474421e5cad8aa51660a008201e6f42f1846abeac4d1065778e0537a7d66ae870e4ded0c09325ecb85344de8779b0e79f29574a946b19d51c20a\"},{\"type\":\"COMMITMS\",\"viewNumber\":6830,\"seqNumber\":6830,\"hash\":\"d2f03a22b83d096dd384a3b3d22c79397c8895158b66d988e3e67d970d547e15\",\"signature\":\"1bedf224a61cf99adb9a93a50b58811af5c35d3b3c3bdb80916e6b88318f09d25123a32691ccb939144b20e989f1054600aa08830ab7ebe3ce84bee88cad5c4f6f\"},{\"type\":\"COMMITMS\",\"viewNumber\":6830,\"seqNumber\":6830,\"hash\":\"d2f03a22b83d096dd384a3b3d22c79397c8895158b66d988e3e67d970d547e15\",\"signature\":\"1c30104357a5db0c79a54d9eb3eee1c463ecf4ecb0d1f8fca46a97fa5762a87512162a07ec38e29ef1f357a20753626bc062eb77313b75a6f83ef2be702ae8e7f9\"},{\"type\":\"COMMITMS\",\"viewNumber\":6830,\"seqNumber\":6830,\"hash\":\"d2f03a22b83d096dd384a3b3d22c79397c8895158b66d988e3e67d970d547e15\",\"signature\":\"1c662152a5bf5a66f944483e4615e82323575e98c62c6eeec6c91281ef9d78ed646acd1dc98db6fc3df6df6d2af9e7c014eaa2fed4f472311e9f7b4d80eaf853f7\"}]}"
   }
}
```
  
-----

### Transaction

- **URL** : _`/api/transaction`_

 
-----

#### getTransactionCountByBlockHash

Returns the number of transactions in a block from a block matching the given block hash.

**Parameter**

`DATA`, 32 Bytes - hash of a block

**Returns**

`QUANTITY` - integer of the number of transactions in this block.

**Example**
  
```
// Request 
{  
   "id":"562768530",
   "jsonrpc":"2.0",
   "method":"getTransactionCountByBlockHash",
   "params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "blockId":"7e18144fd6f943572b09e2e0ba6f22e68bf5fef3d8fd1d3d640276bca081a0dd"
   }
}

// Result 
{  
   "jsonrpc":"2.0",
   "id":"562768530",
   "result":15
}
```
 
-----

#### getTransactionCountByBlockNumber

Returns the number of transactions in a block matching the given block number.

**Parameter**

1. `DATA`, 20 bytes - BranchId.
2. `QUANTITY|TAG` - integer of a block number, or the string `"earliest"`, `"latest"` or `"pending"`, as in the default block parameter.
3. `QUANTITY` - the transaction index position.

```
"params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "blockNumber":7293
}

"params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "blockNumber":7293,
      "txIndexPosition":2
}
```

**Returns**

`QUANTITY` - integer of the number of transactions in this block.

**Example**
  
```
// Request 
{  
   "id":"1792293295",
   "jsonrpc":"2.0",
   "method":"getTransactionCountByBlockNumber",
   "params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "blockNumber":7293
   }
}

// Result
{  
   "jsonrpc":"2.0",
   "id":"1792293295",
   "result":15
}

// Request
{  
   "id":"1109859650",
   "jsonrpc":"2.0",
   "method":"getTransactionByBlockNumber",
   "params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "blockNumber":7293,
      "txIndexPosition":2
   }
}

// Result
{  
   "jsonrpc":"2.0",
   "id":"1109859650",
   "result":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "version":"0000000000000000",
      "type":"0000000000000000",
      "timestamp":1561689372950,
      "bodyHash":"4365a707da15a6bb7427f67453c8b70fe6757b17cb3159cc1324123cd8f7e7a6",
      "bodyLength":152,
      "signature":"1bda6b072adcf11aac3edc92523bed27a0d178d0b834765efba4c209df67e20cbf45172101b0f092181dd335ecaa28fc3de817e0682a65d84ed84f11e46d5053d8",
      "body":"{\"contractVersion\":\"6a2371e34b780dd39bd56002b1d96c23689cc5dc\",\"method\":\"transfer\",\"params\":{\"to\":\"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\",\"amount\":1}}",
      "author":"101167aaf090581b91c08480f6e559acdd9a3ddd",
      "txId":"ce1b1c0a2f5076a41e6077134cf9fb9042a1a566c769f2fa81b90abe4b19f078"
   }
}

// Request
{  
   "id":"654256854",
   "jsonrpc":"2.0",
   "method":"getTransactionByBlockNumber",
   "params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "tag":"latest",
      "txIndexPosition":2
   }
}

// Result
{  
   "jsonrpc":"2.0",
   "id":"654256854",
   "result":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "version":"0000000000000000",
      "type":"0000000000000000",
      "timestamp":1561689372950,
      "bodyHash":"4365a707da15a6bb7427f67453c8b70fe6757b17cb3159cc1324123cd8f7e7a6",
      "bodyLength":152,
      "signature":"1bda6b072adcf11aac3edc92523bed27a0d178d0b834765efba4c209df67e20cbf45172101b0f092181dd335ecaa28fc3de817e0682a65d84ed84f11e46d5053d8",
      "body":"{\"contractVersion\":\"6a2371e34b780dd39bd56002b1d96c23689cc5dc\",\"method\":\"transfer\",\"params\":{\"to\":\"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\",\"amount\":1}}",
      "author":"101167aaf090581b91c08480f6e559acdd9a3ddd",
      "txId":"ce1b1c0a2f5076a41e6077134cf9fb9042a1a566c769f2fa81b90abe4b19f078"
   }
}
```
 
-----

#### getTransactionByHash

Returns the information about a transaction requested by transaction hash.

**Parameter**

`DATA`, 32 Bytes - hash of a transaction

```
"params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "txId":"bce793985a1cde7791acbbeb16037d7b86b967df4213329b2e3cc45995fecd68"
}
```

**Returns**

`Object` - A transaction object, or `null` when no transaction was found:

**Example**
 
```
// Request 
{  
   "id":"1947689513",
   "jsonrpc":"2.0",
   "method":"getTransactionByHash",
   "params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "txId":"bce793985a1cde7791acbbeb16037d7b86b967df4213329b2e3cc45995fecd68"
   }
}

// Result
{  
   "jsonrpc":"2.0",
   "id":"1947689513",
   "result":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "version":"0000000000000000",
      "type":"0000000000000000",
      "timestamp":1561689373153,
      "bodyHash":"4365a707da15a6bb7427f67453c8b70fe6757b17cb3159cc1324123cd8f7e7a6",
      "bodyLength":152,
      "signature":"1b24835190feef4c08d16962c7e58090254f799bcb1b188dee5c0b59aa71befcbd336ec1e6013c2b56da956f5c81aa4f94d4f63bb9bbeef61117b4e0b5e9f07a0e",
      "body":"{\"contractVersion\":\"6a2371e34b780dd39bd56002b1d96c23689cc5dc\",\"method\":\"transfer\",\"params\":{\"to\":\"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\",\"amount\":1}}",
      "author":"101167aaf090581b91c08480f6e559acdd9a3ddd",
      "txId":"bce793985a1cde7791acbbeb16037d7b86b967df4213329b2e3cc45995fecd68"
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
   '7e18144fd6f943572b09e2e0ba6f22e68bf5fef3d8fd1d3d640276bca081a0dd' 
]
```

**Returns**

See [getTransactionByHash]()

**Example**
  
```
// Request
{  
   "id":"1129606082",
   "jsonrpc":"2.0",
   "method":"getTransactionByBlockHash",
   "params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "blockId":"7e18144fd6f943572b09e2e0ba6f22e68bf5fef3d8fd1d3d640276bca081a0dd",
      "txIndexPosition":2
   }
}

// Result
{  
   "jsonrpc":"2.0",
   "id":"1129606082",
   "result":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "version":"0000000000000000",
      "type":"0000000000000000",
      "timestamp":1561689372950,
      "bodyHash":"4365a707da15a6bb7427f67453c8b70fe6757b17cb3159cc1324123cd8f7e7a6",
      "bodyLength":152,
      "signature":"1bda6b072adcf11aac3edc92523bed27a0d178d0b834765efba4c209df67e20cbf45172101b0f092181dd335ecaa28fc3de817e0682a65d84ed84f11e46d5053d8",
      "body":"{\"contractVersion\":\"6a2371e34b780dd39bd56002b1d96c23689cc5dc\",\"method\":\"transfer\",\"params\":{\"to\":\"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\",\"amount\":1}}",
      "author":"101167aaf090581b91c08480f6e559acdd9a3ddd",
      "txId":"ce1b1c0a2f5076a41e6077134cf9fb9042a1a566c769f2fa81b90abe4b19f078"
   }
}
```
 
-----

#### sendTransaction

Creates new message call transaction or a contract creation, if the data field contains code.

**Parameter**

`Object` - The transaction object

**Returns**

`DATA` - An object with transaction hash, status, and logs.

**Example**

```
// Request 
{  
   "id":"1805101289",
   "jsonrpc":"2.0",
   "method":"sendTransaction",
   "params":{  
      "tx":{  
         "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
         "version":"0000000000000000",
         "type":"0000000000000000",
         "timestamp":1561690711866,
         "bodyHash":"200cc3d2044283d0ee51309423a8454ccc9bf66cf2b7c59f36920b4099bd1ead",
         "bodyLength":154,
         "signature":"1c0d7f648c5e5da994d45cc32b5ab34594f1a5e1ec53a99380e61f4e75fe76799302825d32119c3ea6c46d427346ba7e3b0f1e4bc71b48957931bc8bfd537f7ac2",
         "body":"{\"contractVersion\":\"6a2371e34b780dd39bd56002b1d96c23689cc5dc\",\"method\":\"transfer\",\"params\":{\"to\":\"e1980adeafbb9ac6c9be60955484ab1547ab0b76\",\"amount\":100}}",
         "author":"2277af4fac56f6bab7dde6e28f21ae0d16d6a81e",
         "txId":"abbe2cd619a867e0b82fa87ab385f520e8af54bbb1694059dc540ac9faa4cbe9"
      }
   }
} 

// Result
{  
   "jsonrpc":"2.0",
   "id":"1805101289",
   "result":{  
      "txHash":"abbe2cd619a867e0b82fa87ab385f520e8af54bbb1694059dc540ac9faa4cbe9",
      "status":true,
      "logs":{}
   }
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

`DATA` - the transaction hash, or logs of the error transaction,

**Example**
  
```
// Request
{  
   "id":"382853993",
   "jsonrpc":"2.0",
   "method":"sendRawTransaction",
   "params":{  
      "rawTx":"Y1iTguLhg+Kmlp6/V714TcspvUMAAAAAAAAAAAAAAAAAAAAAAAABa5wGwTYgDMPSBEKD0O5RMJQjqEVMzJv2bPK3xZ82kgtAmb0erQAAAAAAAACaG7AbGdTmut5WvvXfrMzuXwR1nxhcpA2q07QB6ChfOBQnVk5NeAj4Ta/WsqHN5ETfBUlLcLhtxmC+zQibI4uBVf57ImNvbnRyYWN0VmVyc2lvbiI6IjZhMjM3MWUzNGI3ODBkZDM5YmQ1NjAwMmIxZDk2YzIzNjg5Y2M1ZGMiLCJtZXRob2QiOiJ0cmFuc2ZlciIsInBhcmFtcyI6eyJ0byI6ImUxOTgwYWRlYWZiYjlhYzZjOWJlNjA5NTU0ODRhYjE1NDdhYjBiNzYiLCJhbW91bnQiOjEwMH19"
   }
}

// Result
{  
   "jsonrpc":"2.0",
   "id":"382853993",
   "result":"bhCzaumxN4o1tdJks0rhvNtj4jhBTby/2ilb735W1Qo="
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
   "id":"858550036",
   "jsonrpc":"2.0",
   "method":"newPendingTransactionFilter",
   "params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43"
   }
}

// Result
{  
   "jsonrpc":"2.0",
   "id":"858550036",
   "result":0
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
// Request 
{  
   "id":"99723131",
   "jsonrpc":"2.0",
   "method":"getTransactionReceipt",
   "params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "txId":"bce793985a1cde7791acbbeb16037d7b86b967df4213329b2e3cc45995fecd68"
   }
}

// Result
{  
   "jsonrpc":"2.0",
   "id":"99723131",
   "result":{  
      "txId":"bce793985a1cde7791acbbeb16037d7b86b967df4213329b2e3cc45995fecd68",
      "blockId":"7e18144fd6f943572b09e2e0ba6f22e68bf5fef3d8fd1d3d640276bca081a0dd",
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "txLog":[  
         "Transfer from 101167aaf090581b91c08480f6e559acdd9a3ddd to 1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e value 1 fee 0 "
      ],
      "status":"SUCCESS",
      "issuer":"101167aaf090581b91c08480f6e559acdd9a3ddd",
      "contractVersion":"6a2371e34b780dd39bd56002b1d96c23689cc5dc",
      "blockHeight":7293,
      "methodName":null
   }
}
```
 
-----

#### getRawTransaction

Returns the information about a raw transaction requested by transaction hash.

**Parameter**

1. `DATA`, 20 bytes - branchId
2. `DATA`, 32 Bytes - the transaction hash

**Returns**

`Object` - A raw transaction object, or `null` when no raw transaction was found:

**Example**

```
// Request
{  
   "id":"1963736878",
   "jsonrpc":"2.0",
   "method":"getRawTransaction",
   "params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "txId":"bce793985a1cde7791acbbeb16037d7b86b967df4213329b2e3cc45995fecd68"
   }
}

// Result
{  
   "jsonrpc":"2.0",
   "id":"1963736878",
   "result":"63589382e2e183e2a6969ebf57bd784dcb29bd43000000000000000000000000000000000000016b9bf0b9e14365a707da15a6bb7427f67453c8b70fe6757b17cb3159cc1324123cd8f7e7a600000000000000981b24835190feef4c08d16962c7e58090254f799bcb1b188dee5c0b59aa71befcbd336ec1e6013c2b56da956f5c81aa4f94d4f63bb9bbeef61117b4e0b5e9f07a0e7b22636f6e747261637456657273696f6e223a2236613233373165333462373830646433396264353630303262316439366332333638396363356463222c226d6574686f64223a227472616e73666572222c22706172616d73223a7b22746f223a2231613063646561643364316431646265656638343866656639303533623466306165303664623965222c22616d6f756e74223a317d7d"
}
```
 
-----

#### getRawTransactionHeader

Returns the information about a raw transaction header requested by transaction hash.

**Parameter**

1. `DATA`, 20 bytes - branchId
2. `DATA`, 32 Bytes - the transaction hash

**Returns**

`Object` - A raw transaction header object, or `null` when no raw transaction header was found:

**Example**

```
// Request
{  
   "id":"1346968245",
   "jsonrpc":"2.0",
   "method":"getRawTransactionHeader",
   "params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "txId":"bce793985a1cde7791acbbeb16037d7b86b967df4213329b2e3cc45995fecd68"
   }
}

// Result
{  
   "jsonrpc":"2.0",
   "id":"1346968245",
   "result":"63589382e2e183e2a6969ebf57bd784dcb29bd43000000000000000000000000000000000000016b9bf0b9e14365a707da15a6bb7427f67453c8b70fe6757b17cb3159cc1324123cd8f7e7a600000000000000981b24835190feef4c08d16962c7e58090254f799bcb1b188dee5c0b59aa71befcbd336ec1e6013c2b56da956f5c81aa4f94d4f63bb9bbeef61117b4e0b5e9f07a0e"
}
```
 
------

### Contract

- **URL** : _`/api/contract`_

 
-----
 
#### query

Handles all queries that are dispatched to the contract

**Parameter**

`DATA`, string - query 

**Returns**

`DATA`, string - result of query

**Example**

- TotalSupply
- BalanceOf
- Allowance

```
// Request  
{  
   "id":"858711201",
   "jsonrpc":"2.0",
   "method":"query",
   "params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "contractVersion":"6a2371e34b780dd39bd56002b1d96c23689cc5dc",
      "method":"totalSupply",
      "params":null
   }
}
// Result
{  
   "jsonrpc":"2.0",
   "id":"858711201",
   "result":"1994000000000000000000000"
}

// Request
{  
   "id":"711450074",
   "jsonrpc":"2.0",
   "method":"query",
   "params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "contractVersion":"6a2371e34b780dd39bd56002b1d96c23689cc5dc",
      "method":"balanceOf",
      "params":{  
         "address":"cee3d4755e47055b530deeba062c5bd0c17eb00f"
      }
   }
}
// Result
{  
   "jsonrpc":"2.0",
   "id":"711450074",
   "result":"994000000000000000000000"
}

// Request
{  
   "id":"126654732",
   "jsonrpc":"2.0",
   "method":"query",
   "params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "contractVersion":"6a2371e34b780dd39bd56002b1d96c23689cc5dc",
      "method":"allowance",
      "params":{  
         "owner":"cee3d4755e47055b530deeba062c5bd0c17eb00f",
         "spender":"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e"
      }
   }
}
// Result
{  
   "jsonrpc":"2.0",
   "id":"126654732",
   "result":"0"
}
```
 
------

### Log

- **URL** : _`/api/log`_

 
-----

#### getLog

Returns the log of the index

**Parameter**

1. `DATA`, 20 bytes - branchId
2. `QUANTITY` - integer of log index 

**Returns**

`DATA`, string - result of log string

**Example**

```
// Request
{  
   "id":"1039655387",
   "jsonrpc":"2.0",
   "method":"getLog",
   "params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "index":5
   }
}
// Result
{  
   "jsonrpc":"2.0",
   "id":"1039655387",
   "result":"{\"to\":\"d2a5721e80dc439385f3abc5aab0ac4ed2b1cd95\",\"balance\":\"1000000000000000000000\"}"
}
```
 
------

#### getLogs

Returns the logs of offset from start
 
**Parameter**
 
1. `DATA`, 20 bytes - branchId
2. `QUANTITY` - integer of start index
3. `QUANTITY` - integer of offset 
 
**Returns**
 
`DATA`, string - A list of log string
 
**Example**

```
// Request
{  
   "id":"1276014628",
   "jsonrpc":"2.0",
   "method":"getLogs",
   "params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43",
      "start":0,
      "offset":5
   }
}
// Result
{  
   "jsonrpc":"2.0",
   "id":"1276014628",
   "result":[  
      "{\"to\":\"101167aaf090581b91c08480f6e559acdd9a3ddd\",\"balance\":\"1000000000000000000000\"}",
      "{\"to\":\"ffcbff030ecfa17628abdd0ff1990be003da35a2\",\"balance\":\"1000000000000000000000\"}",
      "{\"to\":\"b0aee21c81bf6057efa9a321916f0f1a12f5c547\",\"balance\":\"1000000000000000000000\"}",
      "{\"to\":\"1a0cdead3d1d1dbeef848fef9053b4f0ae06db9e\",\"balance\":\"1000000000000000000000\"}",
      "{\"to\":\"57c6510966903044581c148bb67eb47dbbeebef1\",\"balance\":\"1000000000000000000000\"}"
   ]
}
```
 
------

#### curIndex

Returns the current index of logStore

**Parameter**
 
`DATA`, 20 bytes - branchId

**Returns**
 
`QUANTITY` - integer of a current index of branch
 
**Example**

```
// Request
{  
   "id":"737589190",
   "jsonrpc":"2.0",
   "method":"curIndex",
   "params":{  
      "branchId":"63589382e2e183e2a6969ebf57bd784dcb29bd43"
   }
}
// Result
{  
   "jsonrpc":"2.0",
   "id":"737589190",
   "result":617
}
```
  
-----

### Peer

- **URL** : _`/api/peer`_

 
-----

#### getAllActivePeer
 
Returns all active peers
  
**Parameter**
  
None
  
**Returns**
  
`DATA`- A List of active peers
  
**Example**
  
```
// Request
{  
   "id":"217115873",
   "jsonrpc":"2.0",
   "method":"getAllActivePeer"
}
// Result
{  
   "jsonrpc":"2.0",
   "id":"217115873",
   "result":[  
      "ynode://d2d340076503834aa58b57df05354a7929f5ed3c@127.0.0.1:32918",
      "ynode://77283a04b3410fe21ba5ed04c7bd3ba89e70b78c@127.0.0.1:32901"
   ]
}
```
 
-----

### Admin

- **URL** : _`/api/admin`_

 
-----

#### nodeHello

Returns a clientHello message(with nonce) for managing node. 

**Parameter**
  
`Object` - Command object

**Returns**
  
`String` - String of Command object 
  
**Example**
  
```
// Request
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
// Result
{
    "jsonrpc": "2.0",
    "id": "142596201",
    "result": "{\"header\":{\"timestamp\":\"000001668a0a2bae\",\"nonce\":\"baabb165899f98a8b90b7251d88f2bcf\",\"bodyHash\":\"f5ef2617558e62b5a882187978f411a6ec1d437bb13a5d94e473e680d8ce7e7c\",\"bodyLength\":\"000000000000001a\"},\"signature\":\"1bab5fbd03a51cddd6b4aa0d2a68453b3073d2aa7aa58190073d1d6e647353bbaa0618d5e3f126fb5275a03f17b304b4a6afd293cf631fed6dfa31fb7c70db1c0d\",\"body\":[{\"method\":\"clientHello\"}]}"
}
```
 
-----

#### requestCommand

Returns a responseCommand message(with nonce) for managing node. 

**Parameter**
  
`Object` - Command object

**Returns**
  
`String` - String of Command object 
  
**Example**
  
```
// Request
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
// Request
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
// Result
{
    "jsonrpc": "2.0",
    "id": "142596201",
    "result": "{\"header\":{\"timestamp\":\"000001669b8df580\",\"nonce\":\"aaabb165899f988aecd8754326149367\",\"bodyHash\":\"2344c5b38540509e0babd9292553d001fde5bc4a52e8af09221fc7dedc493ee7\",\"bodyLength\":\"000000000000001e\"},\"signature\":\"1c36f6e720b32bd03d981388a0bc103ee5a4e4affee8481f175ab527ed4c189c045105aa4e99f78b64ed4e99f2d8a76fb430ebd88184e744378991a84f41b7650f\",\"body\":[{\"method\":\"responseCommand\"}]}"
}
```
 
-----
