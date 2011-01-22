// Copyright 2011 Lawrence Kesteloot

(function ($) {

// Submit the form with a command.
var submitCommand = function (command, autoPlay) {
    $("#command").val(command);
    $("#autoPlayCheckbox").val(autoPlay ? "on" : "off");
    $("#form").submit();
};

// Run when page loads.
$(function () {
    // Configure buttons.
    $("#onePlayerButton").click(function () {
        submitCommand("newGame", false);
    });
    $("#twoPlayerButton").click(function () {
        submitCommand("newGame", true);
    });
});

})(jQuery);
