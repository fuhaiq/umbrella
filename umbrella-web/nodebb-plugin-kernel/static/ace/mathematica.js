define(function(require, exports, module) {
"use strict";

var oop = require("../lib/oop");
var TextMode = require("../mode/text").Mode;
var MathematicaHighlightRules = require("./mathematica_highlight_rules").MathematicaHighlightRules;

var Mode = function() {
    this.HighlightRules = MathematicaHighlightRules;
};
oop.inherits(Mode, TextMode);

(function() {
    
    this.createWorker = function(session) {
        return null;
    };

    this.$id = "ace/mode/mathematica";
}).call(Mode.prototype);

exports.Mode = Mode;
});