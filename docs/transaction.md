# Transaction 
* 블록체인 데이터에 실행명령 집합 이다.

```json
[{
   "body":{
     "issuer" : "0x------ 어카운트(public key 의 hash)",
     "timestamp":"utc-0 시간 (integer)",
     "branch_id":"branch network address",
     "params":{
         "method":"method name",
         "param1":"param1 value",
         "param2":"param2 value",
         "param3":""
     }  
   },
   "hash":"transaction hash",
   "meta":{
      "tags":[
        "test",""
      ]
   },
   "sign":"issuer의 sign 값"
 }]

```

