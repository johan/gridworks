var DOM = {};

DOM.bind = function(elmt) {
    var map = {};
    
    for (var i = 0; i < elmt.length; i++) {
        DOM._bindDOMElement(elmt[i], map);
    }
    
    return map;
};

DOM._bindDOMElement = function(elmt, map) {
    var bind = elmt.getAttribute("bind");
    if (bind !== null && bind.length > 0) {
        map[bind] = $(elmt);
        elmt.removeAttribute("bind");
    }
    
    if (elmt.hasChildNodes()) {
        DOM._bindDOMChildren(elmt, map);
    }
};

DOM._bindDOMChildren = function(elmt, map) {
    var node = elmt.firstChild;
    while (node !== null) {
        var node2 = node.nextSibling;
        if (node.nodeType == 1) {
            DOM._bindDOMElement(node, map);
        }
        node = node2;
    }
};