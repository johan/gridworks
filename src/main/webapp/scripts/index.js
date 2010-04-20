function onClickUploadFileButton(evt) {
    var projectName = $("#project-name-input")[0].value;
    if (! $.trim(projectName).length) {
        window.alert("You must specify a project name.");
        
        evt.preventDefault();
        return false;
    } else {
        $("#file-upload-form").attr("action", 
            "/command/create-project-from-upload?" + [
                "split-into-columns=" + $("#split-into-columns-input")[0].checked,
                "separator=" + $("#separator-input")[0].value,
                "ignore=" + $("#ignore-input")[0].value,
                "header-lines=" + $("#header-lines-input")[0].value,
                "skip=" + $("#skip-input")[0].value,
                "limit=" + $("#limit-input")[0].value,
                "guess-value-type=" + $("#guess-value-type-input")[0].checked
            ].join("&"));
    }
}

function formatDate(d) {
    var yesterday = Date.today().add({ days: -1 });
    var today = Date.today();
    var tomorrow = Date.today().add({ days: 1 });
    if (d.between(today, tomorrow)) {
        return "Today " + d.toString("h:mm tt");
    } else if (d.between(yesterday, today)) {
        return "Yesterday " + d.toString("h:mm tt");
    } else {
        return d.toString("ddd, MMM d, yyyy");
    }
}

function isThereNewRelease() {
    var thisRevision = GridworksVersion.revision;
    
    var revision_pattern = /r([0-9]+)/;
    
    if (!revision_pattern.test(thisRevision)) { // probably "trunk"
        return false;
    }

    var latestRevision = GridworksReleases.releases[0].revision;
    
    var thisRev = parseInt(revision_pattern.exec(thisRevision)[1],10);
    var latestRev = parseInt(revision_pattern.exec(GridworksReleases.releases[0].revision)[1],10);
    
    return latestRev > thisRev;
}

function renderProjects(data) {
    var projects = [];
    for (var n in data.projects) {
        if (data.projects.hasOwnProperty(n)) {
            var project = data.projects[n];
            project.id = n;
            project.date = Date.parseExact(project.modified, "yyyy-MM-ddTHH:mm:ssZ");
            projects.push(project);
        }
    }
    projects.sort(function(a, b) { return b.date.getTime() - a.date.getTime(); });
    
    var container = $("#projects-container").empty();
    if (!projects.length) {
        $('<div>')
            .addClass("message")
            .text("No existing project. Use form on left to create.")
            .appendTo(container);
    } else {
        var table = $(
            '<table><tr>' +
                '<th>Project Name</th>' +
                '<th>Last Modified</th>' +
                '<th></th>' +
            '</tr></table>'
        ).appendTo(container)[0];
        
        var renderProject = function(project) {
            var tr = table.insertRow(table.rows.length);
            tr.className = "project " + (table.rows.length % 2 ? "even" : "odd");
            
            $('<a></a>')
                .text(project.name)
                .attr("href", "/project.html?project=" + project.id)
                .appendTo(tr.insertCell(tr.cells.length));
                
            $('<span></span>')
                .text(formatDate(project.date))
                .addClass("last-modified")
                .appendTo(tr.insertCell(tr.cells.length));
            
            $('<a></a>')
                .addClass("delete-project")
                .attr("title","Delete this project")
                .attr("href","")
                .html("<img src='/images/close.png' />")
                .click(function() {
                    if (window.confirm("Are you sure you want to delete project \"" + project.name + "\"?")) {
                        $.ajax({
                            type: "POST",
                            url: "/command/delete-project",
                            data: { "project" : project.id },
                            dataType: "json",
                            success: function (data) {
                                if (data && typeof data.code != 'undefined' && data.code == "ok") {
                                    fetchProjects();
                                }
                            }
                        });                    
                    }
                    return false;
                }).appendTo(tr.insertCell(tr.cells.length));
        };
    
        for (var i = 0; i < projects.length; i++) {
            renderProject(projects[i]);
        }
    }
}

function fetchProjects() {
    $.getJSON(
        "/command/get-all-project-metadata",
        null,
        function(data) {
            renderProjects(data);
        },
        "json"
    );
}

function onLoad() {
    fetchProjects();
    
    $("#form-tabs").tabs();
    $("#upload-file-button").click(onClickUploadFileButton);
    $("#more-options-link").click(function() {
        $("#more-options-controls").hide();
        $("#more-options").show();
    });
    
    $("#gridworks-version").text(
        GridworksVersion.version + "-" + GridworksVersion.revision
    );
    if (isThereNewRelease()) {
        $('<div id="version-message">' +
            'New version "' + GridworksReleases.releases[0].description + '" <a href="' + GridworksReleases.homepage + '">available for download here</a>.' +
          '</div>').appendTo(document.body);
    }
}

$(onLoad);
