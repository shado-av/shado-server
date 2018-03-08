var total_num_fleet = 0;
var total_num_team = 0;
var total_num_exo = 0;
var total_num_task= 0;
var num_phases = 2;
var operator_names=["Dispatcher","Operations Management Specialist","Artificially Intelligent Agent"];
var task_names = ["Communicating","Actuation","Directive_Mandatory","Directive_Courtesy_1","Directive_Courtesy_2","Record Keeping","Referencing"];
var priorities = [[4,7],[5,5],[2,5],[5,3],[3,4],[3,2],[3,4],[3,2],[3,1]];
var exo_factor_names =["Medical","Weather"];
var exo_factor_types =["add_task","long_serv"];
var arrival_param = [[0.033333,0.1],[0.033333,0.1],[0.033333,0.1],[0.033333,0.1],[0.033333,0.1],[0.033333,0.1],[0.033333,0.1],[0.033333,0.1],];
var service_param = [[0.5,2],[0.5,2],[0.5,2],[0.5,2],[0.5,2],[0.5,2],[0.5,2],[0.5,2],[0.5,2],[0.5,2]];
var expire_param = [[0,0.184],[0,0.184],[0,0.184],[0,0.184],[0,0.184],[0,0.184],[0,0.184],[0,0.184],[0,0.184]];
var expire_param_exo = [[0,0.184],[0,0.184],[0,0.184],[0,0.184],[0,0.184],[0,0.184],[0,0.184],[0,0.184],[0,0.184]];
var affect_by_IROPS = [[0,1],[0,1],[0,1],[0,1],[0,1],[0,1],[0,1],[0,1],[0,1]];
var human_error_prob = [[0.003,0.00008,0.007],[0.003,0.00008,0.007],[0.003,0.00008,0.007],[0.003,0.00008,0.007],[0.003,0.00008,0.007],[0.003,0.00008,0.007],[0.003,0.00008,0.007]];

var sample = {
    "numHours": 8,
    "traffic":[0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5],
    "numReps": 2,
    "numRemoteOp" : 7,
    "numTeams": 3,
    "numvehicles": [1,3,9,7],
    "autolvl":2,
    "numPhases":2,

    "hasExogenous":[1,2],
    "exNames":["Medical","Weather"],
    "exTypes":["add_task","long_serv"],

    "failThreshold":0.5,
    "opStrats":"STF",
    "opNames":["Dispatcher","Operations Management Specialist","AIDA"],
    "opTasks":[[0,1,2,3],[0,2],[0,1,2,3]],
    "teamComm":["N","S","F"],
    "teamSize":[3,2,2],
    "fleetTypes": 4,
    "fleetHetero":[[0,1,2,3],[0,1],[1,2,3],[0]],

    "numTaskTypes":4,
    "taskNames":["Communication","Paperwork","Signal Response Management","Communicating Internally"],
    "taskPrty":[[3,5],[2,2],[3,4],[4,3]],
    "arrDists":["E","E","E","E"],
    "arrPms":[[0,0.03333],[0,0.03333],[0,0.03333],[0,0.03333]],
    "serDists":["U","U","U","U"],
    "serPms":[[0,0.5],[0,0.5],[0,0.5],[0,0.5]],
    "expDists":["E","E","E","E"],
    "expPmsLo":[[0,0.184 ],[0,0.184 ],[0,0.184 ],[0,0.184 ]],
    "expPmsHi":[[0,0.184 ],[0,0.184 ],[0,0.184 ],[0,0.184 ]],
    "affByTraff":[[0,1],[0,1],[0,1],[0,1]],
    "teamCoordAff":[0,1,0,1],
    "humanError":[[0.07,0.02,0.17],[0.003,0.0008,0.007],[0.0004,0.00008,0.007],[0.09,0.06,0.13]]

};

function submit(){
    var num_teams = document.getElementById("num_teams").value;
    var num_exo = document.getElementById("num_exo").value;
    var has_exo = [0,0];
    if(num_exo !== 0){
        has_exo[0] = 1;
        has_exo[1] = num_exo;
    }
    var traffic = [];
    var num_hours = parseInt( document.getElementById("num_hours").value);
    for(var i = 0 ; i < num_hours; i++ ){
        if(document.getElementById("traffic_levels").value === 0.5)
            traffic[i] = 0.5;
        if(document.getElementById("traffic_levels").value === 0)
            traffic[i] = 0.5;
        if(document.getElementById("traffic_levels").value === 1)
            traffic[i] = 0.5;
    }
    var num_fleet = document.getElementById("num_fleet_types").value;
    var fleet_size = [];
    var fleet_hetero = [];
    for(var i = 0; i < num_fleet; i++){
        var fleet_size_id =  "fleet_type_size_" +i;
        var fleet_hetero_id = "fleet_type_" +i;
        fleet_size.add(document.getElementById(fleet_size_id).value);
        //?
        fleet_hetero.add( document.getElementById(fleet_hetero_id).value)
    }

    var out = {
        "numHours":  num_hours,
        "traffic":[0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5],
        "numReps": parseInt(document.getElementById("num_replications").value),
        "numRemoteOp" : 7,
        "numTeams": parseInt(document.getElementById("num_teams").value),
        "numvehicles": [1,3,9,7],
        "autolvl":2,
        "numPhases":parseInt(document.getElementById("num_phases").value),

        "hasExogenous":[1,2],
        "exNames":["Medical","Weather"],
        "exTypes":["add_task","long_serv"],

        "failThreshold":0.5,
        "opStrats":"STF",
        "opNames":["Dispatcher","Operations Management Specialist","AIDA"],
        "opTasks":[[0,1,2,3],[0,2],[0,1,2,3]],
        "teamComm":["N","S","F"],
        "teamSize":[3,2,2],
        "fleetTypes": parseInt(document.getElementById("num_fleet_types").value),
        "fleetHetero":[[0,1,2,3],[0,1],[1,2,3],[0]],

        "numTaskTypes":4,
        "taskNames":["Communication","Paperwork","Signal Response Management","Communicating Internally"],
        "taskPrty":[[3,5],[2,2],[3,4],[4,3]],
        "arrDists":["E","E","E","E"],
        "arrPms":[[0,0.03333],[0,0.03333],[0,0.03333],[0,0.03333]],
        "serDists":["U","U","U","U"],
        "serPms":[[0,0.5],[0,0.5],[0,0.5],[0,0.5]],
        "expDists":["E","E","E","E"],
        "expPmsLo":[[0,0.184 ],[0,0.184 ],[0,0.184 ],[0,0.184 ]],
        "expPmsHi":[[0,0.184 ],[0,0.184 ],[0,0.184 ],[0,0.184 ]],
        "affByTraff":[[0,1],[0,1],[0,1],[0,1]],
        "teamCoordAff":[0,1,0,1],
        "humanError":[[0.07,0.02,0.17],[0.003,0.0008,0.007],[0.0004,0.00008,0.007],[0.09,0.06,0.13]]
    };



  $.ajax({
      type: "POST",
      url: "http://localhost:8080/shado/testpost",
      // The key needs to match your method's input parameter (case-sensitive).
      data: JSON.stringify(sample),
      contentType: "application/json; charset=utf-8",
      dataType: "json",
      success: function(msg){
          alert(msg);
          console.log("response received");
          console.log(msg);
          // move(100);
          //  var obj = JSON.stringify(msg)
          // // var tempParseData = obj;
          // obj = JSON.parse(obj);
          // console.log(obj);
          // alert(obj);
          // if(msg.status == 'success')
          alert("PARAMETERS SUBMITTED!");
      },
      failure: function(errMsg) {
          alert(errMsg);
      }
  });

    // alert("SHADO params SUBMITTED!");


}

function popFleetTypes(){
  var num_fleet = document.getElementById("num_fleet_types");
  var selected_fleet_num = num_fleet.options[num_fleet.selectedIndex].value;
  // if(document.getElementById("header_fleet_type").isHidden)
  $('.fleet_type').remove();
  total_num_fleet = selected_fleet_num;
  // alert("total_num_fleet"+total_num_fleet);
  for(var i = 0; i < total_num_fleet; i++){
    $( ".fleet_option" ).append( "<label class='fleet_type' id='lbl_fleet_"+i+"' value ='Fleet Type"+i+"'>Fleet Type "+i+"'s Tasks </label> "+
    " <input class='fleet_type'id='fleet_type_"+i+"' value='0,1,2,3,4,5,6,7'placeholder='0,1,2,3,4,5,6,7'></input>"+
    "   <label class='fleet_type' id='lbl_fleet_size"+i+"' value ='Fleet Type "+i+"'>   size </label> "+
    "<input class='fleet_type'id='fleet_type_size_"+i+"' value ='2'placeholder='2'></input><br class='fleet_type'><br class='fleet_type'>");
  }

}


function popTeamTypes(){
  var num_teams = document.getElementById("num_teams");
  var selected_num_teams = num_teams.options[num_teams.selectedIndex].value;
  // if(document.getElementById("header_fleet_type").isHidden)
    $('.team_type').remove();
  total_num_team = selected_num_teams;
  // alert("total_num_fleet"+total_num_fleet);
  for(var i = 0; i < total_num_team; i++){
    $( ".team_option" ).append( "<label class='team_type' id='lbl_team_name_"+i+"' value =''>  Operator Team "+i+"'s Name </label> "+
    "<input class='team_type'id='team_name_"+i+"' value='"+operator_names[i]+"' placeholder='"+operator_names[i]+"'></input>"+
    "<label class='team_type'id='lbl_team_"+i+"' value =''> Tasks </label> "+
    " <input class='team_type'id='team_type_"+i+"' value='0,1,2,3,4,5,6,7' placeholder='0,1,2,3,4,5,6,7'></input>"+
    "   <lable class='team_type'id='lbl_team_size"+i+"' value =''>   size </label> "+
    "<input class='team_type'id='team_type_size"+i+"' value='2'placeholder='2'></input>"+
    "  <select class='team_type'id='select_team_comm_"+i+"'> "+
    "<option class='team_type'selected disabled hidden>Choose Team Communication</option>"+
    "<option value='N'>None</option> "+
    "<option value='L'>Low</option>"+
    "<option value='F'>Full</option> </select> "+
    "<select class='team_type' id='select_team_strat_"+i+"'> "+
    "<option selected disabled hidden>Choose Team Strategy</option>"+
    "<option value='N'>FIFO</option> "+
    "<option value='L'>Shortest Tast First</option>"+
    "<option value='F'>Priority</option> </select><br><br>");
}
}


function popExoFactor(){
  var num_exo = document.getElementById("num_exo");
  var selected_num_exo = num_exo.options[num_exo.selectedIndex].value;
  // if(document.getElementById("header_fleet_type").isHidden)
  $('.exo_type').remove();
  total_num_exo = selected_num_exo;
  // alert("total_num_fleet"+total_num_fleet);
  for(var i = 0; i < total_num_exo; i++){
    $( ".exo_option" ).append( "<label class='exo_type'id='lbl_exo_"+i+"' value ='Exogenous Factor Type"+i+"'>Exo Type"+i+"'s Name </label> "+
    " <input class='exo_type'class='exo_type'class='exo_type'id='exo_type_"+i+"' value='"+exo_factor_names[i]+"' placeholder='"+exo_factor_names[i]+"'></input>"+
    "   <lable class='exo_type'class='exo_type'id='lbl_exo_size"+i+"' value =''>   type </label> "+
    "<input class='exo_type'id='exo_type_size"+i+"' value='"+exo_factor_types[i]+"'placeholder='"+exo_factor_types[i]+"'></input><br><br>");
  }
}

function popTask(){
  var num_task = document.getElementById("num_task");
  var selected_num_task = num_task.options[num_task.selectedIndex].value;
  if(total_num_task!=0)
    $('.task_type').remove();
  total_num_task = selected_num_task;
  for(var i = 0; i < total_num_task; i++){
    $( ".task_option" ).append( "<label class='task_type'id='lbl_name_"+i+"' value ='Exogenous Factor Type"+i+"'>Name </label> "+
    "<input class = 'task_type' id='txt_task_name"+i+"' value='"+task_names[i]+"'placeholder='"+task_names[i]+"'></input>"+
    "<lable class = 'task_type'id='lbl_priority_"+i+"' value ='Exogenous Factor Type"+i+"'>Priority </label> "+
    "<input class = 'task_type' id='txt_priority_"+i+"' size = '4' value='"+priorities[i]+"' placeholder='"+priorities[i]+"'></input>"+
    "<lable class = 'task_type'id='lbl_arr_pm_dist_"+i+"' value ='Arrival Dist"+i+"'>Arrival Dist </label>"+
    "<input class = 'task_type' id='txt_arr_pm_dist_"+i+"' size = '3'value='E' placeholder='E'></input>"+
    "<lable class = 'task_type'id='lbl_serv_pm_dist_"+i+"' value ='Service Dist"+i+"'>Service Dist </label>"+
    "<input class = 'task_type' id='txt_serv_pm_dist_"+i+"'size = '3' value='U' placeholder='U'></input>"+
    "<lable class = 'task_type'id='lbl_exp_pm_dist_"+i+"' value ='Expire Dist"+i+"'>Expire Dist </label>"+
    "<input class = 'task_type'id='txt_exp_pm_dist_"+i+"' size = '3'value='E' placeholder='E'></input><br><br>"+
    "<lable class = 'task_type'id='lbl_arr_pm_"+i+"' value =''>Arrival Param </label> "+
    "<input class = 'task_type'id='txt_arr_pm_"+i+"'  value='"+arrival_param[i]+"' placeholder='"+arrival_param[i]+"'></input>"+
    "<lable class = 'task_type'id='lbl_serv_pm_"+i+"' value =''>Service Param </label> "+
    "<input class = 'task_type'id='txt_serv_pm_"+i+"'  value='"+service_param[i]+"' placeholder='"+service_param[i]+"'></input>"+
    "<lable class = 'task_type'id='lbl_expire_pm_default_"+i+"' value =''>Expire Param Default </label> "+
    "<input class = 'task_type'id='txt_expire_pm_default"+i+"'  value='"+expire_param[i]+"' placeholder='"+expire_param[i]+"'></input>"+
    "<lable class = 'task_type'id='lbl_expire_pm_exo_"+i+"' value =''>Expire Param High Traffic</label> "+
    "<input class = 'task_type'id='txt_expire_pm_exo_"+i+"'  value='"+expire_param_exo[i]+"' placeholder='"+expire_param_exo[i]+"'></input><br><br>"+
    "<lable class = 'task_type'id='lbl_affect_by_IROP_"+i+"' value =''>Affected By IROPS</label> "+
    "<input class = 'task_type'id='txt_affect_by_IROP_"+i+"' size='5' value='"+affect_by_IROPS[i]+"' placeholder='"+affect_by_IROPS[i]+"'></input>"+
    "<lable class = 'task_type'id='lbl_human_err_"+i+"' value =''>Human Error Probability</label> "+
    "<input class = 'task_type'id='txt_human_err_"+i+"' value='"+human_error_prob[i]+"' placeholder='"+human_error_prob[i]+"'></input>"+
    "<br><br>----------------------------------------------------------------------------------------------------------------------------------------------------<br><br>");
  }
}
