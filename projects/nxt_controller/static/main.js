// Copyright 2011 Lawrence Kesteloot

(function ($) {

var WIDTH = 1024;
var HEIGHT = 768;
var TAU = Math.PI*2;
var g_canvas;
var g_startX = null;
var g_startY = null;
var g_touchX;
var g_touchY;
var g_touchedNode = null;
var g_movingNode = null;
var g_executingNode = null;
var g_executeTime = 0; // For rotation, in seconds.

// Nodes are objects with the following attributes: x, y (center), radius, color,
// label, onBegin function, and nextNode.
var g_nodes = [];

var sendCommand = function (parameters) {
    $.ajax({
        contentType: "application/json; charset=UTF-8",
        data: JSON.stringify(parameters),
        dataType: "json",
        type: "POST",
        url: "/api/",
        error: function (request, textStatus, errorThrown) {
            alert("Error: " + request.status + " (" + textStatus + ")");
        },
        success: function (data, textStatus) {
            // Nothing.
        }
    });
};

var setSpeed = function (value, max, motor) {
    var speedFraction = value / max;
    var speed = 500 + 500*speedFraction;

    var parameters = {
        command: "setSpeed",
        motor: motor,
        speed: speed
    };

    sendCommand(parameters);
};

/**
 * Returns color string (like "rgb(12,69,123)" from HSV values). HSV are in the range
 * 0-1.
 */
function hsv2rgb(h,s,v) {
    // Adapted from http://www.easyrgb.com/math.html
    // hsv values = 0 - 1, rgb values = 0 - 255
    var r, g, b;
    var RGB = new Array();

    if (s == 0) {
        r = g = b = Math.round(v*255);
    } else {
        // h must be < 1
        var var_h = h * 6;
        if (var_h == 6) var_h = 0;
        //Or ... var_i = floor( var_h )
        var var_i = Math.floor( var_h );
        var var_1 = v*(1-s);
        var var_2 = v*(1-s*(var_h-var_i));
        var var_3 = v*(1-s*(1-(var_h-var_i)));
        if (var_i == 0) {
            r = v; 
            g = var_3; 
            b = var_1;
        } else if (var_i == 1) {
            r = var_2;
            g = v;
            b = var_1;
        } else if (var_i == 2) {
            r = var_1;
            g = v;
            b = var_3
        } else if (var_i == 3) {
            r = var_1;
            g = var_2;
            b = v;
        } else if (var_i == 4) {
            r = var_3;
            g = var_1;
            b = v;
        } else {
            r = v;
            g = var_1;
            b = var_2
        }

        //rgb results = 0 ÷ 255  
        r = Math.round(r * 255);
        g = Math.round(g * 255);
        b = Math.round(b * 255);
    }

    return "rgb(" + r + "," + g + "," + b + ")";
};

var draw = function () {
    var g = g_canvas.getContext('2d');

    // XXX maybe "g_canvas.width = g_canvas.width" would clear too.
    /// g.clearRect(0, 0, WIDTH, HEIGHT);

    g.fillStyle = "white";
    g.fillRect(0, 0, WIDTH, HEIGHT);

    for (var i = 0; i < g_nodes.length; i++) {
        var node = g_nodes[i];

        g.fillStyle = node.color;
        g.beginPath();
        g.arc(node.x, node.y, node.radius, 0, TAU, false);
        g.closePath();
        g.fill();

        if (g_executingNode === node) {
            g.lineWidth = 5;
            /*
            for (var j = 0; j < 10; j++) {
                var offsetRadians = g_executeTime;
                var startRadians = TAU/10*j + offsetRadians;
                var endRadians = TAU/10*(j + 0.5) + offsetRadians;

                g.strokeStyle = hsv2rgb(j / 10, 1, 1);
                g.beginPath();
                g.arc(node.x, node.y, node.radius, startRadians, endRadians, false);
                g.stroke();
            }
            */
            for (var j = 0; j < 20; j++) {
                var offsetRadians = g_executeTime;
                var radians = TAU/20*j + offsetRadians;
                var dx = Math.cos(radians);
                var dy = Math.sin(radians);

                g.strokeStyle = hsv2rgb(j / 20, 1, 1);
                g.beginPath();
                g.moveTo(node.x + dx*(node.radius + 5), node.y + dy*(node.radius + 5));
                g.lineTo(node.x + dx*(node.radius + 20), node.y + dy*(node.radius + 20));
                g.stroke();
            }
        } else if (g_movingNode === node) {
            g.strokeStyle = "black";
            g.lineWidth = 5;
            g.beginPath();
            g.arc(node.x, node.y, node.radius, 0, TAU, false);
            g.closePath();
            g.stroke();
        }

        g.fillStyle = "white";
        g.font = "28pt sans-serif";
        g.textAlign = "center";
        g.textBaseline = "middle";
        g.fillText(node.label, node.x, node.y);

        if (node.nextNode !== null) {
            var dx = node.nextNode.x - node.x;
            var dy = node.nextNode.y - node.y;
            var length = Math.sqrt(dx*dx + dy*dy);
            var arrowLength = length - node.radius - node.nextNode.radius - 20;
            if (arrowLength > 0) {
                dx /= length;
                dy /= length;
                var tailX = node.x + dx*(node.radius + 10);
                var tailY = node.y + dy*(node.radius + 10);
                var headX = tailX + dx*arrowLength;
                var headY = tailY + dy*arrowLength;

                // Line
                g.strokeStyle = "black";
                g.lineWidth = 5;
                g.beginPath();
                g.moveTo(tailX, tailY);
                g.lineTo(headX, headY);
                // Arrow head.
                g.moveTo(headX - dy*15 - dx*20, headY + dx*15 - dy*20);
                g.lineTo(headX, headY);
                g.lineTo(headX + dy*15 - dx*20, headY - dx*15 - dy*20);
                g.stroke();

                // Highlight
                /*
                if (node.nextNode === g_executingNode && g_executeTime < 0.250) {
                    g.strokeStyle = "red";
                    for (var j = 0; j < 5; j++) {
                        var x = tailX + (headX - tailX)*(j/5 + g_executeTime);
                        var y = tailY + (headY - tailY)*(j/5 + g_executeTime);

                        g.strokeStyle = hsv2rgb(j / 5, 1, 1);
                        g.beginPath();
                        g.moveTo(x - dy*5, y + dx*5);
                        g.lineTo(x - dy*20, y + dx*20);
                        g.stroke();
                        g.beginPath();
                        g.moveTo(x + dy*5, y - dx*5);
                        g.lineTo(x + dy*20, y - dx*20);
                        g.stroke();
                    }
                }
                */
            }
        }
    }
};

var executeNode = function (node) {
    g_executingNode = node;
    g_executeTime = 0;
    var intervalId = setInterval(function () {
        g_executeTime += 0.050;
        draw();
    }, 50);
    node.onBegin(function () {
        clearInterval(intervalId);
        g_executingNode = null;
        if (node.onFinish) {
            node.onFinish();
        }
        draw();

        if (node.nextNode !== null) {
            executeNode(node.nextNode);
        }
    });
    draw();
};

var touchStart = function (pageX, pageY) {
    g_touchedNode = null;

    for (var i = 0; i < g_nodes.length; i++) {
        var node = g_nodes[i];

        var dx = pageX - node.x;
        var dy = pageY - node.y;
        if (dx*dx + dy*dy < node.radius*node.radius) {
            g_touchedNode = node;
            g_startX = node.x;
            g_startY = node.y;
            g_touchX = pageX;
            g_touchY = pageY;
            break;
        }
    }

    draw();
};

var touchMove = function (pageX, pageY) {
    if (g_touchedNode !== null) {
        var movedX = pageX - g_touchX;
        var movedY = pageY - g_touchY;
        if (g_movingNode !== null || movedX <= -5 || movedX >= 5 || movedY <= -5 || movedY >= 5) {
            g_movingNode = g_touchedNode;
            g_movingNode.x = g_startX + movedX;
            g_movingNode.y = g_startY + movedY;
            draw();
        }
    }
};

var touchEnd = function (pageX, pageY) {
    if (g_touchedNode !== null) {
        if (g_movingNode === null) {
            // It's a tap.
            executeNode(g_touchedNode);
        }

        g_touchedNode = null;
        g_movingNode = null;

        draw();
    }
};

var makeNodes = function () {
    g_nodes = [
        {
            x: 300,
            y: 300,
            radius: 50,
            color: 'rgb(0,200,0)',
            label: 'Go',
            onBegin: function (onEnd) {
                sendCommand({
                    command: 'forward',
                    motor: 'A'
                });
                setTimeout(onEnd, 250);
            },
            nextNode: null
        },
        {
            x: 500,
            y: 300,
            radius: 50,
            color: 'rgb(50,50,250)',
            label: 'Wait',
            onBegin: function (onEnd) {
                setTimeout(onEnd, 1000);
            },
            nextNode: null
        },
        {
            x: 700,
            y: 300,
            radius: 50,
            color: 'rgb(200,0,0)',
            label: 'Stop',
            onBegin: function (onEnd) {
                sendCommand({
                    command: 'stop',
                    motor: 'A'
                });
                setTimeout(onEnd, 250);
            },
            nextNode: null
        },
    ];

    if (true) {
        g_nodes[0].nextNode = g_nodes[1];
        g_nodes[1].nextNode = g_nodes[2];
    }

    if (false) {
        g_nodes[3] = {
            x: 500,
            y: 500,
            radius: 50,
            color: 'rgb(50,50,250)',
            label: 'Wait',
            onBegin: function (onEnd) {
                setTimeout(onEnd, 1000);
            },
            nextNode: null
        };
        g_nodes[2].nextNode = g_nodes[3];
        g_nodes[3].nextNode = g_nodes[0];
        g_nodes[0].y += 100;
        g_nodes[2].y += 100;
    }
};

// Run at page load.
$(function () {
    g_canvas = $("canvas")[0];

    makeNodes();

    g_canvas.addEventListener("touchstart", function (event) {
        event.preventDefault();
        touchStart(event.targetTouches[0].pageX, event.targetTouches[0].pageY);
    }, false);
    g_canvas.addEventListener("touchmove", function (event) {
        event.preventDefault();
        touchMove(event.targetTouches[0].pageX, event.targetTouches[0].pageY);
    }, false);
    g_canvas.addEventListener("touchend", function (event) {
        event.preventDefault();
        touchEnd(event.targetTouches[0].pageX, event.targetTouches[0].pageY);
    }, false);
    g_canvas.addEventListener("mousedown", function (event) {
        event.preventDefault();
        touchStart(event.pageX, event.pageY);
    }, false);
    g_canvas.addEventListener("mousemove", function (event) {
        event.preventDefault();
        touchMove(event.pageX, event.pageY);
    }, false);
    g_canvas.addEventListener("mouseup", function (event) {
        event.preventDefault();
        touchEnd(event.pageX, event.pageY);
    }, false);

    draw();
});

})(jQuery);

