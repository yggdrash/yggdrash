# JSON RPC Error Codes Improvement Proposal

Contents
- JSON RPC Standard errors
- Custom error codes
- Possible future error codes

### JSON RPC Standard errors
| Code             | Possible Return message                                         | Description                                                                                          |
| :--------------: | --------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| -32700           | Parse error                                                     | Invalid JSON was received by the server. An error occurred on the server while parsing the JSON text.|
| -32600           | Invalid Request                                                 | The JSON sent is not a valid Request object.                                                         |
| -32601           | Method not found                                                | The method does not exist / is not available.                                                        |
| -32602           | Invalid params                                                  | Invalid method parameter(s).                                                                         |
| -32603           | Internal error                                                  | Internal JSON-RPC error.                                                                             |
| -32000 to -32099 | Server error. Reserved for implementation-defined server-errors.|                                                                                                      |

### Custom error codes

| Code   | Message                             | Description                                                                                                    | Exception                |
| :----: | ----------------------------------- | -------------------------------------------------------------------------------------------------------------- | ------------------------ |
| -10000 | X doesn't exist                     | Should be used when the requested resource (i.e. TransactionHash, BlockHash, Account) are not be found.        | NonExistObjectException  |
| -10001 | Invalid signature                   | Should be used when an authentication by signature has failed.                                                 | InvalidSignatureException|
| -10002 | The size of data is not appropriate | Should be used when the size of address,transactionHash,blockHash, or transactionObject is too large or small. | WrongStructuredException |
| -10003 | Rejected                            | Should be used when an action was rejected.                                                                    | RejectedAccessException  |
| -10004 | X not created                       | Should be used for action which creates account, transaction or block.                                         | FailedOperationException |
| -10005 | Internal error                      | Internal error.                                                                                                | InternalErrorException   |

### Possible future error codes 

| Code   | Message                 | Description                                                                          | Exception |
| :----: | ----------------------- | ------------------------------------------------------------------------------------ | --------- |
| -10006 | Unconfirmed transaction | Should be used when the client request the resource by unconfirmed transaction hash. |           |
| -10007 | Requires Yeed           | Should be used for actions which require something else.                             |           |
| -10008 | Yeed too low            | Should be used when a to low value of Ether was given.                               |           |

