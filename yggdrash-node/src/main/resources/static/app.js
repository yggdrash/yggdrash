var stompClient = null;

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    if (connected) {
        $("#blockTable").show();
        $("#txTable").show();
    }
    else {
        $("#blockTable").hide();
        $("#txTable").hide();
    }
    $("#blocks").html("");
    $("#txs").html("");
}

function connect() {
    var socket = new SockJS('/yggdrash-websocket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/blocks', function (message) {
            showBlocks(JSON.parse(message.body));
        });
        stompClient.subscribe('/topic/txs', function (message) {
            showTxs(JSON.parse(message.body));
        });
    });
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    setConnected(false);
    console.log("Disconnected");
}

function showBlocks(block) {
    $("#blocks").append("<tr><td>" + block.hash + "</td></tr>");
}

function showTxs(tx) {
    $("#txs").append("<tr><td>" + tx.txHash + "</td></tr>");
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $( "#connect" ).click(function() { connect(); });
    $( "#disconnect" ).click(function() { disconnect(); });
});