// Copyright 2011 Lawrence Kesteloot

(function ($) {

// Submit the form with a command.
var submitCommand = function (command) {
    $("#command").val(command);
    $("#form").submit();
};

// Submit this hand-entered move.
var makeMove = function (move) {
    $("#moveTextField").val(move);
    submitCommand("submitMove");
};

// Set up a button to submit a command.
var configureButton = function (buttonId, command) {
    $("#" + buttonId).click(function () {
        submitCommand(command);
    });
};

// Look at the checkbox and update the at-risk display accordingly.
var updateShowAtRisk = function () {
    var showAtRisk = $("#showAtRiskCheckbox").val();
    if (showAtRisk) {
        for (var position in g_atRisk) {
            var count = g_atRisk[position];

            $("#" + position).addClass("highlight" + count);
        }
    } else {
        $("td.highlight1").removeClass("highlight1");
        $("td.highlight2").removeClass("highlight2");
        $("td.highlight3").removeClass("highlight3");
        $("td.highlight4").removeClass("highlight4");
        $("td.highlight5").removeClass("highlight5");
    }
};

// Run when page loads.
$(function () {
    var source = null;

    // Focus text input field.
    $("#moveTextField").focus();

    // Add click handler to all squares.
    $("td.square").click(function () {
        var id = $(this).attr("id");

        if (source === null) {
            var legalMoves = g_legalMoves[id];
            if (legalMoves) {
                source = id;
                $(this).addClass("clicked");

                for (var i = 0; i < legalMoves.length; i++) {
                    var destination = legalMoves[i];
                    $("#" + destination).addClass("destination");
                }
            }
        } else {
            if (source === id) {
                // Clicked on source, cancel.
                $("td.clicked").removeClass("clicked");
                $("td.destination").removeClass("destination");
                source = null;
            } else {
                makeMove(source + "-" + id);
            }
        }
    });

    // Set up the buttons.
    configureButton("makeMoveButton", "computerMove");
    configureButton("undoButton", "undo");
    configureButton("redoButton", "redo");
    configureButton("newGameButton", "newGame");
    configureButton("new960GameButton", "new960Game");

    $("#moveTextField").keypress(function (event) {
        // Bind Enter to submitting the move.
        if (event.which == 13) {
            submitCommand("submitMove");
        }
    });

    // Show pressure when asked.
    $("#showAtRiskCheckbox").click(updateShowAtRisk);
    updateShowAtRisk();

    // Move if auto-play is on.
    if (g_playMove) {
        setTimeout(function () {
            submitCommand("computerMove");
        }, 100);
    }
});

})(jQuery);
