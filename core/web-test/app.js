var parse_text_tree =[];
var node_mapping_tree =[];
var user_node_tree_select = [];
var select_node_choice = {
    node_choices:{

    }
};
var tempTreedata = {};
var tempParseData = {};
var edgeArr =[];
var wordArr = [];

var treeOptionsEdge = [];
var treeOptionsWord = [];
var treeOptionsid = [];

// document.getElementById("getsql").style.visibility = 'visible';

function clearTextBox() {
    showProgress();
    document.getElementById("textBox").value = "";
    var xhttp = new XMLHttpRequest();
    var url = "http://localhost:4567/parse/clear" ;
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4 ) {
            console.log("Response Received");
            hideProgress();
            if(this.responseText ==="OK")
            document.getElementById("response").innerText = "TREE CLEARED";
            // alert(this.responseText);
        }
    };
    // xhttp = new XDomainRequest();
    xhttp.open("GET", url, true);
    xhttp.send();
    // alert("Post to Clear");
}

function generate(){
    // move(40);
    var input = "";
    if(document.getElementById("textBox").value ==="")
         input = "return title of articles after 1970";
    else
         input = document.getElementById("textBox").value;
    showProgress();
    $.ajax({

        type: "POST",
        url: "http://localhost:4567/parse/text",
        // The key needs to match your method's input parameter (case-sensitive).
        data: JSON.stringify({ text: input }),
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function(msg){
            hideProgress();
            console.log("response received");
            // move(100);
            var obj = JSON.stringify(msg)
            tempParseData = obj;
            obj = JSON.parse(obj);
            console.log(obj);
            $.each(obj.tree.nodes, function (id,nodes) {
                console.log(this);
                wordArr.push(this.word);
                // tempParseEdgeData = this.edges;
                // $.each(this.nodes, function () {
                //     wordArr.push(this);
                // });
            });
            $.each(obj.tree.edges,function(){
                edgeArr.push(this);
            });
            document.getElementById("viewTree").style.visibility = 'visible';
            document.getElementById("hideTree").style.visibility = 'visible';
            drawTree();
            // console.log(tempParseEdgeData);
            // console.log("***wordArr***");
            // console.log(wordArr);
            // console.log("***edgeArr***");
            // console.log(edgeArr);

            // document.getElementById("response").innerText =  "Tree Example: id: "+ obj.tree.nodes[1].id +
            //     "\n Word: "+ obj.tree.nodes[1].word +
            //     "\n Type :" + obj.tree.nodes[1].type;
            // console.log("SUCCESS: POST intput parse tree ");
        },
        failure: function(errMsg) {
            hideProgress();
            alert(errMsg);
        }
    });
    // hideProgress();
}


function visualizeTree() {
    
}

function showParseTreeJSON(input) {

    //TODO: uncomment to enable remote call
    // $.getJSON("http://localhost:4567/parse/text", function(data){
    $.getJSON("test-input/parse_text_output.json", function(data){
        var items = [];
        // data = JSON.stringify(data);
        // data = JSON.parse(data);
        var wordIndex = 0;
        console.log(data);
        $.each(data.tree, function (id,nodes) {
            items.push( "<h3 id='" + id + "'> >" + id+" : "+wordArr[wordIndex]+ "</h3>" );
            console.log(nodes);
            $.each(nodes, function (id,node) {
                items.push( "<button  class='pure-button' id='" + node.type + "'>" + node.val + "</button>" );
            });
            wordIndex ++;
        });

        $( "<ul/>", {
            "class": "node_choice_output",
            html: items.join( "" )
        }).appendTo( "body" );
        // var obj = JSON.stringify(msg);
        // obj = JSON.parse(obj);
        // console.log(obj);
    });
}

function mapNodeChoice(){
    showProgress();
    // $.getJSON("http://localhost:4567/map/node_choices", function(data){
        $.getJSON("test-input/node_choices_output.json", function(data){
            hideProgress();
            var items = [];
            console.log(data);
            var nodeCtr = 0;
            var innerCtr = 0;
            var wordIndex = 0;
            $.each(data.node_choices, function (id,nodes) {
                items.push( "<h3 class='user-node-select' >" + id+" : "+wordArr[wordIndex]+  "</h3>" );

                console.log(nodes);
                innerCtr =0;
                $.each(nodes, function (id,node) {

                    var type = node.type;
                    var val = node.val;
                    var treeId = nodeCtr+innerCtr;
                    console.log("Id: "+treeId+", Type: "+ type +" ,Val: "+ val);
                    node_mapping_tree.push([treeId,type,val]);
                    items.push( "<button onclick='selectNodeChoice(" + treeId +")' class='pure-button user-node-select' " +
                        " id='"+ treeId +"'>"+"("+type+")"+ val + "</button>" );
                    innerCtr ++;
                });
                nodeCtr += 10;
                wordIndex++;
            });

            $( "<ul/>", {
                "class": "node_choice_output",
                html: items.join( "" )
            }).appendTo( "#nodeSuccess" );
            document.getElementById("nodeSuccess").style.visibility = 'visible';
            // var obj = JSON.stringify(msg);
            // obj = JSON.parse(obj);
            // console.log(obj);
        });
    // }
    // var node_choice_output = JSON.parse(tree_choice_output.json);
}

function selectNodeChoice(treeId) {
    user_node_tree_select.push(treeId);
    document.getElementById(treeId).style.backgroundColor="#00b4cc";

    console.log("Added: Id: " +treeId);
}
function generateUserNodeChoice() {
    console.log(user_node_tree_select);
    for (i = 0; i < user_node_tree_select.length; i++) {
        console.log("USER SELECT: "+user_node_tree_select[i]);
        for (j = 0; j < node_mapping_tree.length; j++) {
            if(user_node_tree_select[i] == node_mapping_tree[j][0]){

            // console.log("USER SELECT-> Id: " + node_mapping_tree[j][0] +
            //     ", Type:" + node_mapping_tree[j][1] +
            //     ", Val:" + node_mapping_tree[j][2]);

                var id = Math.floor(user_node_tree_select[i]/10);
                var type =node_mapping_tree[j][1];
                var val = node_mapping_tree[j][2];
                // var jsonObj = {};
                select_node_choice.node_choices[id] = {
                        "type": type,
                        "val" : val
                }
            }
        }
    }
    //GET The input
    document.getElementById("nodeSuccess").textContent= "Node mapping choice submitted!";
    console.log(select_node_choice);
    showProgress();
    //ajax post
    $.ajax({
        type: "POST",
        url: "http://localhost:4567/map/select_node_choices",
        // The key needs to match your method's input parameter (case-sensitive).
        data: JSON.stringify(select_node_choice),
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function(msg){
            hideProgress();
            console.log("response received");
            if(msg.responseText === "OK")
                console.log("POST SUCCESS: Map/select_noce_choices ");
        },
        failure: function(errMsg) {
            hideProgress();
            alert(errMsg);
        }
    });
    // hideProgress();
    [].forEach.call(document.querySelectorAll('.user-node-select'), function (el) {
        el.remove();
    });
    hideTree();
    document.getElementById("treeSelection").style.visibility = 'visible';
}

//Get and Display Trees
function GetTreeChoice() {
    showProgress();
    // TODO: uncomment to enable remote call
    document.getElementById("treeSelect").style.visibility = 'visible';
    // $.getJSON("http://localhost:4567/adjust/tree_choices", function(data){
    $.getJSON("test-input/tree_choice_output.json", function(data){
        tempTreedata = data;
        hideProgress();
        var items = [];
        var treeSelect = [];
        console.log(data);
        var treeCtr = 0;
        var innerCtr = 0;
        $.each(data.trees, function (edges,nodes) {

            // items.push( "<h3 class='user-tree-select' >" + treeCtr+ "</h3>" );
            treeSelect.push("<option>"+ treeCtr + "</option>");
            // console.log(this.edges);

            //For Drawing Trees
            var treeOpEdgeArr = [];
            var treeOpWordArr = [];
            var treeOpIdArr = [];
            for(i = 0; i < this.edges.length; i++)
                treeOpEdgeArr.push(this.edges[i]);
            for(i = 0; i < this.nodes.length; i++) {
                treeOpWordArr.push(this.nodes[i].word);
                treeOpIdArr.push(this.nodes[i].id);
            }
            // console.log(edgeArr);

            treeOptionsEdge.push(treeOpEdgeArr);
            treeOptionsWord.push(treeOpWordArr);
            treeOptionsid.push(treeOpIdArr);
            $.each(data.trees.nodes, function (id,node) {
                items.push( "<button onclick='selectNodeChoice(" + treeId +")' class='pure-button user-node-select' " +
                    " id='"+ treeId +"'>"+ val + "</button>" );
                innerCtr ++;
            });
            treeCtr += 1;
        });
        console.log(treeOptionsEdge);
        console.log(treeOptionsWord);
        console.log(treeOptionsid);
        $( "<select>",{
            "id":"select-tree",
            "class":  "user-tree-select",
            html : treeSelect.join(""),
        }).appendTo("#treeSelect");
        $( "<ul/>", {
            "class": "node_choice_output",
            html: items.join( "" )
        }).appendTo( "#sqlQuery" );

        // document.getElementById("nodeSuccess").style.visibility = 'visible';
    });
}


function postTreeChoice() {
    showProgress();
    document.getElementById("getsql").style.visibility = 'visible';
    [].forEach.call(document.querySelectorAll('.user-tree-select'), function (el) {
        el.style.visibility = 'hidden';
    });
    var treeChoice = document.getElementById("select-tree").value;
    var treeObj = {
        "tree":{}
    };
    // TODO: uncomment to enable remote call

    // $.getJSON("http://localhost:4567/adjust/tree_choices", function(data){
    // $.getJSON("test-input/tree_choice_output.json", function(data){
        if(tempTreedata != null) {
            var data = tempTreedata;
            // console.log(data);
            var treeCtr = 0;
            $.each(data.trees, function (edges, nodes) {
                if (treeChoice == treeCtr) {
                    console.log("Post: Tree: #" + treeCtr);
                    treeObj.tree = {
                        "edges": this.edges,
                        "nodes": this.nodes
                    };
                }
                treeCtr++;
            });
            console.log("Tree selected:");
            console.log(treeObj);
        }

           // document.getElementById("nodeSuccess").style.visibility = 'visible';

    //Post to server
    $.ajax({
        type: "POST",
        url: "http://localhost:4567/adjust/select_tree_choice",
        // The key needs to match your method's input parameter (case-sensitive).
        data: JSON.stringify(treeObj),
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function(msg){
            hideProgress();
            console.log("response received");
            if(msg.responseText === "OK")
                document.getElementById("getsql").style.visibility = 'visible';
                console.log("POST SUCCESS: Map/select_noce_choices ");
        },
        failure: function(errMsg) {
            hideProgress();
            alert(errMsg);
        }
    });
    hideTreeOptions();
    hideProgress();
    // console.log("Tree Chosen: " +treeChoice);
    document.getElementById("txtTreeChoiceSubmit").style.visibility = 'visible';
}


function getSql() {
    // document.getElementById("sqlQuery").textContent = "test";
    $.getJSON("test-input/translate_to_sql_output.json", function(data){
        // $.getJSON("http://localhost:4567/process/translate_to_sql", function(data){
        var sqlQuery = data.sql;

        document.getElementById("sqlQuery").textContent = sqlQuery;
    });

}


function showProgress() {
    document.getElementById("progress").style.visibility = 'visible';
}
function hideProgress() {
    document.getElementById("progress").style.visibility = 'hidden';
}
function drawTree() {
    document.getElementById("container").style.display = "block";
    /**
     * This is a basic example on how to instantiate sigma. A random graph is
     * generated and stored in the "graph" variable, and then sigma is instantiated
     * directly with the graph.
     *
     * The simple instance of sigma is enough to make it render the graph on the on
     * the screen, since the graph is given directly to the constructor.
     */
    var i,
        s,
        N = wordArr.length,
        E = edgeArr.length,
        midX = Math.floor(N/2);
        midY= Math.floor(E/2);
        g = {
            nodes: [],
            edges: []
        };
    g.nodes.push({
        id: 'n' + 0,
        label: wordArr[0],
        x: midX,
        y: Math.floor(midY/2),
        size: 2,
        color: '#666'});
    // Generate a random graph:
    for (i = 1; i < N; i++)
        g.nodes.push({
            id: 'n' + i,
            label: wordArr[i],
            x: midX + i % 3,
            y: midY + i % 4,
            size: 2,
            color: '#666'
        });

    for (i = 0; i < E; i++)
        g.edges.push({
            id: 'e' + i,
            source: 'n' + edgeArr[i][0],
            target: 'n' + edgeArr[i][1],
            size: 5,
            color: '#ccc'
        });

    // Instantiate sigma:
    s = new sigma({
        graph: g,
        container: 'graph-container'
    });
}
function hideTree() {
    document.getElementById("container").style.display = 'none';
}
function viewTree() {
    document.getElementById("container").style.display = 'block';
}

function drawTreeOptions() {

    var treeIndex = document.getElementById("select-tree").value;
    console.log("Draw Tree #: "+ treeIndex);
    var i,
        s,
        N = treeOptionsWord[treeIndex].length,
        E = treeOptionsEdge[treeIndex].length,
        midX = Math.floor(N/2);
        midY= Math.floor(E/2);
    g = {
        nodes: [],
        edges: []
    };
    s = new sigma({
        graph: g,
        container: 'tree-option-graph-container'
    });
    g.nodes.push({
        id: 'n' + 0,
        label: treeOptionsWord[treeIndex][0],
        x: midX,
        y: Math.floor(midY/2),
        size: 2,
        color: '#666'});
    // Generate a random graph:
    for (i = 1; i < N; i++)
        g.nodes.push({
            id: 'n' +treeOptionsid[treeIndex][i] ,
            label: treeOptionsWord[treeIndex][i],
            x: midX + i % 3,
            y: midY + i % 4,
            size: 2,
            color: '#666'
        });

    for (i = 0; i < E; i++)
        g.edges.push({
            id: 'e' + i,
            source: 'n' + treeOptionsEdge[treeIndex][i][0],
            target: 'n' + treeOptionsEdge[treeIndex][i][1],
            size: 10,
            color: '#ccc'
        });

    // Instantiate sigma:
    s = new sigma({
        graph: g,
        container: 'tree-option-graph-container'
    });
}
function hideTreeOptions() {
    document.getElementById("tree-option-container").style.visibility = 'hidden';
}


function testParseText(){
    move(50);
    console.log("***TEST***\nparse/text");
    // var input = "Return the authors, where the papers of the author in VLDB after 2000 is more than Bob";
    var input = "return title of articles after 2000";
    $.ajax({
        type: "POST",
        url: "http://localhost:4567/parse/text",
        // The key needs to match your method's input parameter (case-sensitive).
        data: JSON.stringify({ text: input }),
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function(msg){
            console.log("response received");
            move(100);
            var obj = JSON.stringify(msg)
            obj = JSON.parse(obj);
            console.log(obj);
            document.getElementById("response").innerText =  "id: "+ obj.tree.nodes[1].id +
                "\n Word: "+ obj.tree.nodes[1].word +
                "\n Type :" + obj.tree.nodes[1].type;
            console.log("SUCCESS: POST intput parse tree ");
        },
        failure: function(errMsg) {
            alert(errMsg);
        }
    });
}

function insertImplicitNode() {
    $.ajax({
        type: "POST",
        url: "http://localhost:4567/process/insert_implicit_nodes",
        // The key needs to match your method's input parameter (case-sensitive).
        data: JSON.stringify(""),
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function(msg){
            console.log("response received");
            if(msg.responseText === "OK")
                console.log("POST SUCCESS: Map/select_noce_choices ");
        },
        failure: function(errMsg) {
            alert(errMsg);
        }
    });
}
function test() {
    // testParseText();
    mapNodeChoice();
}

// function testSelectNodeChoice(input){
//
// }

// function testSlectTreeChoice(input) {
//
// }