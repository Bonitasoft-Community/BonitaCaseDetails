package org.bonitasoft.tools.Process;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.bonitasoft.engine.bpm.document.Document;
import org.bonitasoft.engine.bpm.document.DocumentCriterion;
import org.bonitasoft.engine.bpm.flownode.ActivityStates;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ArchivedHumanTaskInstance;
import org.bonitasoft.engine.bpm.flownode.CatchEventDefinition;
import org.bonitasoft.engine.bpm.flownode.EventCriterion;
import org.bonitasoft.engine.bpm.flownode.EventInstance;
import org.bonitasoft.engine.bpm.flownode.FlowElementContainerDefinition;
import org.bonitasoft.engine.bpm.flownode.FlowNodeDefinition;
import org.bonitasoft.engine.bpm.flownode.TimerEventTriggerInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstanceNotFoundException;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstanceNotFoundException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.identity.UserNotFoundException;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.tools.Process.CaseDetails.CaseDetailFlowNode;
import org.bonitasoft.tools.Process.CaseDetails.ProcessInstanceDescription;


public class DecoJsonCaseDetails {
    
    
    CaseDetails caseDetails;
    
    
public final static String cstActivityName = "activityName";
public final static String cstActivityDescription = "description";
public final static String cstActivityDisplayDescription = "displaydescription";

public final static String cstPerimeter = "perimeter";
public final static String cstPerimeter_V_ACTIVE = "ACTIVE";
public final static String cstPerimeter_V_ARCHIVED = "ARCHIVED";

public final static String cstActivityId = "activityId";
public final static String cstActivitySourceId = "activitySourceId";
public final static String cstActivityIdDesc = "activityIdDesc";
public final static String cstTriggerId = "triggerid";
public final static String cstJobName = "jobName";
public final static String cstActivityFlownodeDefId = "FlownodeDefId";
public final static String cstActivityType = "type";
public final static String cstActivityState = "state";
public final static String cstActivityDate = "activityDate";
public final static String cstActivityDateHuman = "humanActivityDateSt";
public final static String cstActivitySourceObjectId = "SourceObjectId";
public final static String cstActivityTimerDate = "timerDate";
public final static String cstActivityParentContainer = "parentcontainer";
public final static String cstActivityExpl = "expl";

public final static String cstActivityDateBegin = "dateBegin";
public final static String cstActivityDateBeginHuman = "dateBeginSt";

public final static String cstActivityDateEnd = "dateEnd";
public final static String cstActivityDateEndHuman = "dateEndSt";

public final static String cstActivityIsTerminal = "isTerminal";
public final static String cstActivityJobIsStillSchedule = "jobIsStillSchedule";
public final static String cstActivityJobScheduleDate = "jobScheduleDate";

// message
public final static String cstActivityMessageName = "messageName";
public final static String cstActivityMessageCorrelationList = "correlations";
public final static String cstActivityMessageContentList = "contents";

public final static String cstActivityMessageVarName = "msgVarName";
public final static String cstActivityMessageVarValue = "msgVarValue";

public final static String cstActivityCorrelationDefinition = "corrDefinition";

// signal
public final static String cstActivitySignalName = "signalName";

public final static String cstCaseId = "caseId";
public final static String cstRootCaseId = "rootCaseId";
public final static String cstRealCaseId = "realCaseId";
public final static String cstCaseProcessInfo = "processInfo";
public final static String cstCaseStartDateSt = "startDateSt";

public final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");

    public DecoJsonCaseDetails(CaseDetails caseDetails) {
        this.caseDetails = caseDetails;
    }

  

    /**
     * get map of the collect operation
     * 
     * @return
     */
    public Map<String, Object> getMap() {
        Map<String, Object> result = new HashMap<String, Object>();
        //result.put("title", title);
        /*
        List<Map<String,Object>> listDetails = new ArrayList<Map<String,Object>>();
        for (ProcessInstanceDescription processInstanceDescription : caseDetails.listProcessInstances) {
            try {
                ProcessDefinition processDefinition = processAPI.getProcessDefinition(processInstanceDescription.processDefinitionId);

        Map<String, Object> processInstanceMap = new HashMap<String, Object>();
        listDetails.add(processInstanceMap);
        caseDetails.processInstanceList.listIds.add(processInstanceDescription.id);

        processInstanceMap.put("id", processInstanceDescription.id);

        processInstanceMap.put("processname", processDefinition.getName());
        processInstanceMap.put("processversion", processDefinition.getVersion());
        if (processInstanceDescription.callerId != null && processInstanceDescription.callerId > 0) {
            boolean foundIt = false;
            try {
                ActivityInstance act = processAPI.getActivityInstance(processInstanceDescription.callerId);
                foundIt = true;
                processInstanceMap.put("parentact", act.getName());
                long ppid = act.getParentProcessInstanceId();

                processInstanceMap.put("parentid", ppid);;
                ProcessDefinition parentProcessDefinition = processAPI.getProcessDefinition(act.getProcessDefinitionId());

                processInstanceMap.put("parentprocessname", parentProcessDefinition.getName());
                processInstanceMap.put("parentprocessversion", parentProcessDefinition.getVersion());
            } catch (Exception e) {
            }
            if (!foundIt) { // maybe archived
                try {
                    ArchivedActivityInstance act = processAPI.getArchivedActivityInstance(processInstanceDescription.callerId);
                    foundIt = true;
                    processInstanceMap.put("parentact", act.getName());
                    long ppid = act.getProcessInstanceId();

                    processInstanceMap.put("parentid", ppid);;
                    ProcessDefinition parentProcessDefinition = processAPI.getProcessDefinition(act.getProcessDefinitionId());

                    processInstanceMap.put("parentprocessname", parentProcessDefinition.getName());
                    processInstanceMap.put("parentprocessversion", parentProcessDefinition.getVersion());
                } catch (Exception e) {
                }
            }

        }
        processInstanceMap.put("status", processInstanceDescription.isActive ? "ACTIF" : "ARCHIVED");
        if (caseHistoryParameter.showContract)
        {
            // processInstanceMap.put("contract", getContractValuesBySql(processInstanceDescription.processDefinitionId, processInstanceDescription.id, null, processAPI));
            processInstanceMap.put("contract", getContractInstanciationValues(processInstanceDescription, processAPI));
            
        }
        public final static String cstStatus = "status";
        
        
        // another
        

        final Map<String, Object> caseDetailsMap = new HashMap<String, Object>();
        caseDetailsMap.put("errormessage", caseDetails.errorMessage);
        final List<Map<String, Object>> listActivities = new ArrayList<Map<String, Object>>();
        final Map<Long, Map<String, Object>> mapActivities = new HashMap<Long, Map<String, Object>>();

        for (CaseDetailFlowNode flowNodeDetail : caseDetails.listCaseDetailFlowNodes)
        {
            final HashMap<String, Object> mapActivity = new HashMap<String, Object>();

            if (flowNodeDetail.activityInstance!=null)
                mapActivity.put(cstPerimeter, cstPerimeter_V_ACTIVE);
            else
                mapActivity.put(cstPerimeter, cstPerimeter_V_ARCHIVED);

            mapActivity.put(cstActivityName, flowNodeDetail.getName());
            mapActivity.put(cstActivityId,  flowNodeDetail.activityInstance.getId());
            mapActivity.put(cstActivityIdDesc,  flowNodeDetail.getId());
            mapActivity.put(cstActivityDescription,  flowNodeDetail.getDescription());
            mapActivity.put(cstActivityDisplayDescription,  flowNodeDetail.getDisplayDescription());

            if (flowNodeDetail.dateFlowNode != null) {
                mapActivity.put(cstActivityDate, flowNodeDetail.dateFlowNode.getTime());
                mapActivity.put(cstActivityDateHuman, getDisplayDate(flowNodeDetail.dateFlowNode));
            }
            // mapActivity.put("isterminal",
            // activityInstance.().toString());
            mapActivity.put(cstActivityType,  flowNodeDetail.getType().toString());
            mapActivity.put(cstActivityState,  flowNodeDetail.getState().toString());
            mapActivity.put(cstActivityFlownodeDefId,  flowNodeDetail.getFlownodeDefinitionId());
            mapActivity.put(cstActivityParentContainer,  flowNodeDetail.getParentContainerId());
            final Set<Long> listMultiInstanceActivity = new HashSet<Long>();

            if ("MULTI_INSTANCE_ACTIVITY".equals( flowNodeDetail.getType().toString())) {
                listMultiInstanceActivity.add( flowNodeDetail.getFlownodeDefinitionId());
            }
            mapActivity.put(cstActivityExpl,
                    "FlowNode :" +  flowNodeDetail.getFlownodeDefinitionId() + "] ParentContainer["
                            +  flowNodeDetail.getParentContainerId() + "] RootContainer["
                            +  flowNodeDetail.getRootContainerId() + "]");

            if ( flowNodeDetail.userExecutedBy != null) {
                final String userExecuted =  flowNodeDetail.userExecutedBy.getUserName() + " ("
                            + flowNodeDetail.userExecutedBy.getId() + ")";
                    mapActivity.put("ExecutedBy", userExecuted);
            }
            listActivities.add(mapActivity);
            mapActivities.put(flowNodeDetail.getId(), mapActivity);

        }
        
        
        // ------------------- archived   
        // Attention, same activity will be returned multiple time
        Set<Long> setActivitiesRetrieved = new HashSet<Long>();
        
        for (ProcessInstanceDescription processInstance : caseDetails.listProcessInstances) {

            SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 1000);
            if (caseHistoryParameter.showSubProcess) {
                searchOptionsBuilder.filter(ArchivedFlowNodeInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID,
                        processInstanceId);
                // bug : not working
                // searchOptionsBuilder.filter(ArchivedFlowNodeInstanceSearchDescriptor.ROOT_PROCESS_INSTANCE_ID,
                // caseHistoryParameter.caseId);
            } else {
                searchOptionsBuilder.filter(ArchivedFlowNodeInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID,
                        processInstanceId);
            }

            final SearchResult<ArchivedFlowNodeInstance> searchActivityArchived;
            searchActivityArchived = processAPI.searchArchivedFlowNodeInstances(searchOptionsBuilder.done());
            for (final ArchivedFlowNodeInstance flownNodeInstance : searchActivityArchived.getResult()) {
                if (setActivitiesRetrieved.contains(flownNodeInstance.getId()))
                    continue;
                setActivitiesRetrieved.add(flownNodeInstance.getId());

                final HashMap<String, Object> mapActivity = new HashMap<String, Object>();
                mapActivity.put(cstPerimeter, cstPerimeter_V_ARCHIVED);
                mapActivity.put(cstActivityName, flownNodeInstance.getName());
                mapActivity.put(cstActivityId, flownNodeInstance.getId());
                mapActivity.put(cstActivitySourceId, flownNodeInstance.getSourceObjectId());
                mapActivity.put(cstActivityIdDesc, flownNodeInstance.getSourceObjectId() + " ( " + flownNodeInstance.getId() + ")");
                mapActivity.put(cstActivityDescription, flownNodeInstance.getDescription());
                mapActivity.put(cstActivityDisplayDescription, flownNodeInstance.getDisplayDescription());

                final Date date = flownNodeInstance.getArchiveDate();
                mapActivity.put(cstActivityDate, date.getTime());
                mapActivity.put(cstActivityDateHuman, getDisplayDate(date));
                mapActivity.put(cstActivityIsTerminal, flownNodeInstance.isTerminal() ? "Terminal" : "");
                mapActivity.put(cstActivityType, flownNodeInstance.getType().toString());
                mapActivity.put(cstActivityState, flownNodeInstance.getState().toString());
                mapActivity.put(cstActivityFlownodeDefId, flownNodeInstance.getFlownodeDefinitionId());
                mapActivity.put("parentactivityid", flownNodeInstance.getParentActivityInstanceId());
                mapActivity.put(cstActivityParentContainer, flownNodeInstance.getParentContainerId());
                mapActivity.put(cstActivitySourceObjectId, flownNodeInstance.getSourceObjectId());
                mapActivity.put(cstActivityExpl,
                        "FlowNode :" + flownNodeInstance.getFlownodeDefinitionId() + "] ParentActivityInstanceId["
                                + flownNodeInstance.getParentActivityInstanceId() + "] ParentContainer["
                                + flownNodeInstance.getParentContainerId() + "] RootContainer["
                                + flownNodeInstance.getRootContainerId() + "] Source["
                                + flownNodeInstance.getSourceObjectId() + "]");

                if (flownNodeInstance.getExecutedBy() != 0) {
                    try {
                        final User user = identityAPI.getUser(flownNodeInstance.getExecutedBy());

                        final String userExecuted = (user != null ? user.getUserName() : "unknow") + " ("
                                + flownNodeInstance.getExecutedBy() + ")";
                        mapActivity.put("ExecutedBy", userExecuted);
                    } catch (final UserNotFoundException ue) {
                        mapActivity.put("ExecutedBy", "UserNotFound id=" + flownNodeInstance.getExecutedBy());
                        caseDetails.put("errormessage", "UserNotFound id=" + flownNodeInstance.getExecutedBy());

                    } ;
                }
                // only on archived READY state
                if (caseHistoryParameter.showContract && flownNodeInstance instanceof ArchivedHumanTaskInstance && ("ready".equalsIgnoreCase(flownNodeInstance.getState().toString())))
                    try {
                        List<Map<String, Serializable>> listContractValues = getContractTaskValues( (ArchivedHumanTaskInstance) flownNodeInstance, processAPI);
                        if (listContractValues != null && listContractValues.size() > 0)
                            mapActivity.put("contract", listContractValues);
                    } catch (Exception e) {
                        caseDetails.put("errormessage", "Error during get case history " + e.toString());
                    }

                logger.info("#### casehistory Activity[" + mapActivity + "]");

                listActivities.add(mapActivity);
                mapActivities.put(flownNodeInstance.getId(), mapActivity);

            }
        }
        // ------------------------------ events
        List<Map<String, Object>> listSignals = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> listMessages = new ArrayList<Map<String, Object>>();
        for (Long processInstanceId : loadAllprocessInstances.listIds) {
            final List<EventInstance> listEventInstance = processAPI.getEventInstances(processInstanceId, 0,
                    1000, EventCriterion.NAME_ASC);
            for (final EventInstance eventInstance : listEventInstance) {
                Map<String, Object> mapActivity = null;
                if (mapActivities.containsKey(eventInstance.getId()))
                    mapActivity = mapActivities.get(eventInstance.getId());
                else {
                    mapActivity = new HashMap<String, Object>();
                    listActivities.add(mapActivity);
                    mapActivity.put(cstPerimeter, "ARCHIVED");
                    mapActivity.put(cstActivityName, eventInstance.getName());
                    mapActivity.put(cstActivityId, eventInstance.getId());
                    mapActivity.put(cstActivityIdDesc, eventInstance.getId());

                    mapActivity.put(cstActivityDescription, eventInstance.getDescription());
                    mapActivity.put(cstActivityDisplayDescription, eventInstance.getDisplayDescription());
                    mapActivity.put(cstActivityIsTerminal, "");

                }

                Date date = eventInstance.getLastUpdateDate();
                if (date != null) {
                    mapActivity.put(cstActivityDate, date.getTime());
                    mapActivity.put(cstActivityDateHuman, getDisplayDate(date));
                }
                mapActivity.put(cstActivityType, eventInstance.getType().toString());
                mapActivity.put(cstActivityState, eventInstance.getState().toString());
                mapActivity.put(cstActivityFlownodeDefId, eventInstance.getFlownodeDefinitionId());
                mapActivity.put(cstActivityParentContainer, eventInstance.getParentContainerId());

                mapActivity.put(cstActivityExpl,
                        "EventInstance :" + eventInstance.getFlownodeDefinitionId() + "] ParentContainer["
                                + eventInstance.getParentContainerId() + "] RootContainer["
                                + eventInstance.getRootContainerId() + "]");

                DesignProcessDefinition designProcessDefinition = processAPI
                        .getDesignProcessDefinition(eventInstance.getProcessDefinitionId());
                FlowElementContainerDefinition flowElementContainerDefinition = designProcessDefinition
                        .getFlowElementContainer();
                FlowNodeDefinition flowNodeDefinition = flowElementContainerDefinition
                        .getFlowNode(eventInstance.getFlownodeDefinitionId());
                if (flowNodeDefinition instanceof CatchEventDefinition) {
                    CatchEventDefinition catchEventDefinition = (CatchEventDefinition) flowNodeDefinition;
                    if (catchEventDefinition.getSignalEventTriggerDefinitions() != null) {
                        SignalOperations.collectSignals(catchEventDefinition, eventInstance, listSignals);

                    } // end signal detection
                    if (catchEventDefinition.getMessageEventTriggerDefinitions() != null) {
                        MessageOperations.collectMessage(catchEventDefinition, eventInstance, listMessages);

                    } // end message detection
                }
                // ActivityDefinition activityDefinition= processAPI.getDef
                // CatchEventDefinition.getSignalEventTriggerDefinitions().getSignalName()
            }
        }
        caseDetails.put("signals", listSignals);
        caseDetails.put("messages", listMessages);

        // -------------------------------------------- search the timer
        List<Map<String, Object>> listTimers = new ArrayList<Map<String, Object>>();
        for (Long processInstanceId : loadAllprocessInstances.listIds) {

            SearchResult<TimerEventTriggerInstance> searchTimer = processAPI.searchTimerEventTriggerInstances(
                    processInstanceId, new SearchOptionsBuilder(0, 100).done());
            if (searchTimer.getResult() != null)
                for (TimerEventTriggerInstance triggerInstance : searchTimer.getResult()) {
                    Map<String, Object> eventTimer = new HashMap<String, Object>();

                    eventTimer.put(cstActivityJobIsStillSchedule, "Yes");
                    eventTimer.put(cstTriggerId, triggerInstance.getId());
                    eventTimer.put(cstActivityId, triggerInstance.getEventInstanceId());
                    eventTimer.put(cstActivityIdDesc, triggerInstance.getEventInstanceId());
                    eventTimer.put(cstActivityName, triggerInstance.getEventInstanceName());
                    eventTimer.put(cstActivityTimerDate, triggerInstance.getExecutionDate() == null ? ""
                            : sdf.format(triggerInstance.getExecutionDate()));

                    // update the activity : a timer is still active
                    if (mapActivities.containsKey(triggerInstance.getEventInstanceId())) {
                        Map<String, Object> mapActivity = mapActivities.get(triggerInstance.getEventInstanceId());
                        mapActivity.put(cstActivityJobIsStillSchedule, "Yes");
                        mapActivity.put(cstActivityJobScheduleDate, triggerInstance.getExecutionDate() == null ? ""
                                : sdf.format(triggerInstance.getExecutionDate()));
                        mapActivity.put(cstTriggerId, triggerInstance.getExecutionDate() == null ? ""
                                : sdf.format(triggerInstance.getExecutionDate()));

                    }
                    listTimers.add(eventTimer);
                }
        }
       
        caseDetails.put("timers", listTimers);

        // --- set the activities now that we updated it
        sortTheList(listActivities, cstActivityDate);

        caseDetails.put("activities", listActivities);

        // -------------------------- Calcul the Active list
        Map<Long, Map<String, Object>> mapActive = new HashMap<Long, Map<String, Object>>();
        for (Map<String, Object> activity : listActivities) {
            if (cstPerimeter_V_ARCHIVED.equals(activity.get(cstPerimeter)))
                continue;
            Long idActivity = (Long) activity.get(cstActivityId);
            mapActive.put(idActivity, activity);
        }
        final long currentTime = System.currentTimeMillis();
        // ok, now we have in Map all the last state for each activity
        for (Map<String, Object> activity : mapActive.values()) {
            listActivitiesActives.add(activity);
            if (ActivityStates.INITIALIZING_STATE.equals(activity.get(cstActivityState))) {
                if (currentTime - ((Long) activity.get(cstActivityDate)) > 1000 * 60)
                    activity.put("ACTIONEXECUTE", true);
            }
            if (ActivityStates.READY_STATE.equals(activity.get(cstActivityState)))
                activity.put("ACTIONEXECUTE", true);
        }
        
        caseDetails.put("actives", listActivitiesActives);
        logger.info("ACTIVE:" + listActivitiesActives.toString());

        // process instance
        caseDetails.put("processintances", loadAllprocessInstances.listDetails);

        // -------------------------------------------- Variables
        List<Map<String, Object>> listDataInstanceMap = new ArrayList<Map<String, Object>>();

        // process variables
        listDataInstanceMap.addAll(loadProcessVariables(caseHistoryParameter.caseId, caseHistoryParameter.showSubProcess, mapActivities, processAPI));
        listDataInstanceMap.addAll(loadBdmVariables(caseHistoryParameter.caseId, caseHistoryParameter, apiSession, businessDataAPI, processAPI));

        sortTheList(listDataInstanceMap, "processinstance;name;datearchived");

        caseDetails.put("variables", listDataInstanceMap);

        // -------------------------------------------- Documents
        List<Map<String, Object>> listDocumentsMap = new ArrayList<Map<String, Object>>();

        List<ProcessInstanceDescription> listProcessInstances = getAllProcessInstance(caseHistoryParameter.caseId,
                caseHistoryParameter.showSubProcess, processAPI);

        for (ProcessInstanceDescription processInstanceDescription : listProcessInstances) {
            List<Document> listDocuments = processAPI.getLastVersionOfDocuments(processInstanceDescription.id, 0, 1000,
                    DocumentCriterion.NAME_ASC);
            if (listDocuments != null) {
                for (Document document : listDocuments) {
                    Map<String, Object> documentMap = new HashMap<String, Object>();
                    listDocumentsMap.add(documentMap);

                    ProcessDefinition processDefinition = processAPI.getProcessDefinition(processInstanceDescription.processDefinitionId);

                    documentMap.put("processname", processDefinition.getName());
                    documentMap.put("processversion", processDefinition.getVersion());
                    documentMap.put("processinstance", processInstanceDescription.id);

                    documentMap.put("name", document.getName());
                    documentMap.put("id", document.getId());
                    documentMap.put("hascontent", document.hasContent());
                    documentMap.put("contentstorageid", document.getContentStorageId());
                    documentMap.put("url", document.getUrl());
                    documentMap.put("contentfilename", document.getContentFileName());
                    documentMap.put("contentmimetype", document.getContentMimeType());
                    documentMap.put("docindex", Integer.valueOf(document.getIndex()));
                    documentMap.put("creationdate",
                            document.getCreationDate() == null ? "" : sdf.format(document.getCreationDate()));
                }
            }
        }
        sortTheList(listDocumentsMap, "processinstance;name;docindex");
        caseDetails.put("documents", listDocumentsMap);

        // ---------------------------------- Synthesis
        final Map<Long, Map<String, Object>> mapSynthesis = new HashMap<Long, Map<String, Object>>();
        for (final Map<String, Object> mapActivity : listActivities) {

            final Long flowNodedefid = Long.valueOf((Long) mapActivity.get(cstActivityFlownodeDefId));
            if (mapSynthesis.get(flowNodedefid) != null) {
                continue; // already analysis
            }
            final String type = (String) mapActivity.get(cstActivityType);
            if ("BOUNDARY_EVENT".equals(type)) {
                continue; // don't keep this kind of activity
            }

            // analysis this one !
            final HashMap<String, Object> oneSynthesisLine = new HashMap<String, Object>();
            mapSynthesis.put(flowNodedefid, oneSynthesisLine);

            oneSynthesisLine.put(cstActivityName, mapActivity.get(cstActivityName));
            oneSynthesisLine.put(cstActivityType, mapActivity.get(cstActivityType));

            String expl = "";
            boolean isReady = false;
            boolean isInitializing = false;
            boolean isExecuting = false;
            boolean isCancelled = false;
            boolean isCompleted = false;
            boolean isReallyTerminated = false;
            boolean isFailed = false;

            // in case of a simple task, there are only one record. In case
            // of MultInstance, there are one per instance
            final HashMap<Long, TimeCollect> timeCollectPerSource = new HashMap<Long, TimeCollect>();
            // calculate the line : check in the list all related event
            // to make the relation, the SOURCE (
            // activityInstance.getSourceObjectId()) is necessary.
            // in case of a multi instance, we will have multiple
            // initializing / executing but with different source :
            // Instance 1 : initializing source="344"
            // instance 2 : initializing source ="345"
            // Instance 1 executing : source ="344"
            // Instance 2 executing : source ="345"
            // then we collect the time per source

            // ------------------- sub loop
            for (final Map<String, Object> mapRunActivity : listActivities) {
                if (!mapRunActivity.get(cstActivityFlownodeDefId).equals(flowNodedefid)) {
                    continue;
                }

                expl += "Found state[" + mapRunActivity.get(cstActivityState) + "]";
                Long key = (Long) mapRunActivity.get(cstActivitySourceObjectId);
                if (key == null) {
                    key = (Long) mapRunActivity.get(cstActivityId);
                }
                TimeCollect timeCollect = timeCollectPerSource.get(key);

                if (timeCollect == null) {
                    timeCollect = new TimeCollect();
                    timeCollect.activitySourceObjectId = (Long) mapRunActivity.get(cstActivitySourceObjectId);
                    timeCollect.activityId = (Long) mapRunActivity.get(cstActivityId);
                    timeCollect.activityType = (String) mapRunActivity.get(cstActivityType);
                    timeCollectPerSource.put(key, timeCollect);
                }

                // min and max
                Long timeActivity = (Long) mapRunActivity.get(cstActivityDate);
                if (timeActivity != null) {
                    if (timeCollect.timeEntry == null || (timeActivity < timeCollect.timeEntry))
                        timeCollect.timeEntry = timeActivity;
                    if (timeCollect.timeFinish == null || (timeActivity > timeCollect.timeFinish))
                        timeCollect.timeFinish = timeActivity;
                }

                if ("initializing".equals(mapRunActivity.get(cstActivityState))
                        || "executing".equals(mapRunActivity.get(cstActivityState))) {
                    // attention : multiple initializing or executing,
                    // specialy in a Call Activity. get the min !
                    // Long timeSynthesis = (Long)
                    // oneSynthesisLine.get(cstActivityDateBegin);
                    logger.info("##### Synthesis Init activity[" + oneSynthesisLine.get(cstActivityName) + " "
                            + timeCollect.toString());
                }

                if ("ready".equals(mapRunActivity.get(cstActivityState))) {
                    timeCollect.timeUserExecute = (Long) mapRunActivity.get(cstActivityDate);
                    isReady = true;
                }
                if ("failed".equals(mapRunActivity.get(cstActivityState))) {
                    isFailed = true;

                }
                if (("completed".equals(mapRunActivity.get(cstActivityState))
                        || "cancelled".equals(mapRunActivity.get(cstActivityState)))
                        && mapRunActivity.get(cstActivityDate) instanceof Long) {
                    isReallyTerminated = true;
                    // attention ! if the task is a MULTI The task is
                    // considere
                    if (listMultiInstanceActivity.contains(flowNodedefid)) {
                        if ("MULTI_INSTANCE_ACTIVITY".equals(mapRunActivity.get(cstActivityType))) {
                            isReallyTerminated = true;
                        } else {
                            isReallyTerminated = false;
                        }
                    }
                }

                if (ActivityStates.INITIALIZING_STATE.equals(mapRunActivity.get(cstActivityState))) {
                    isInitializing = true;
                }
                if (ActivityStates.EXECUTING_STATE.equals(mapRunActivity.get(cstActivityState))) {
                    isExecuting = true;
                }
                if (ActivityStates.READY_STATE.equals(mapRunActivity.get(cstActivityState))) {
                    isReady = true;
                }
                if (ActivityStates.COMPLETED_STATE.equals(mapRunActivity.get(cstActivityState))
                        && isReallyTerminated) {
                    isCompleted = true;
                }
                if (ActivityStates.CANCELLED_STATE.equals(mapRunActivity.get(cstActivityState))
                        && isReallyTerminated) {
                    isCancelled = true;
                }

            } // end run sub activity lool
              // build the activity synthesis
            long mintimeInitial = -1;
            long maxtimeComplete = -1;
            long sumTimeEnterConnector = -1; // a marker
            long sumTimeWaitUser = -1; // a marker
            long sumTimeFinishConnector = 0;
            for (final TimeCollect timeCollect : timeCollectPerSource.values()) {
                if (timeCollect.timeEntry != null) {
                    if (mintimeInitial == -1 || timeCollect.timeEntry < mintimeInitial) {
                        mintimeInitial = timeCollect.timeEntry;
                    }
                }
                if (timeCollect.timeFinish != null) {
                    if (maxtimeComplete == -1 || timeCollect.timeFinish > maxtimeComplete) {
                        maxtimeComplete = timeCollect.timeFinish;
                        // automatic task : we have only a timeInitial and a
                        // timeComplete
                    }
                }

                // USER TASK
                // timeEntry initializing.reachedStateDate or
                // initializing.archivedDate API: YES
                // timeAvailable ready.reachedStateDate API : No
                // timeUserExecute ready.archivedDate API : YES
                // timeFinish Completed.archivedDate API : YES
                // ==> No way to calculated the time of input connector or
                // the time the task is waiting

                // Service TASK
                // timeEntry initializing.archivedDate API: YES
                // timeAvailable API : No
                // timeUserExecute API : No
                // timeFinish Completed.archivedDate API : YES

                if (timeCollect.timeAvailable == null) {
                    timeCollect.timeAvailable = timeCollect.timeEntry;
                }

                if (timeCollect.timeUserExecute == null) {
                    timeCollect.timeUserExecute = timeCollect.timeAvailable;
                }

                // multi instance is not part of the sum calculation
                if ("MULTI_INSTANCE_ACTIVITY".equals(timeCollect.activityType)) {
                    continue;
                }
                if (timeCollect.timeEntry != null && timeCollect.timeAvailable != null) {
                    if (sumTimeEnterConnector == -1) {
                        sumTimeEnterConnector = 0;
                    }
                    sumTimeEnterConnector += timeCollect.timeAvailable - timeCollect.timeEntry;
                }

                if (timeCollect.timeUserExecute != null && timeCollect.timeAvailable != null) {
                    if (sumTimeWaitUser == -1) {
                        sumTimeWaitUser = 0;
                    }
                    sumTimeWaitUser += timeCollect.timeUserExecute - timeCollect.timeAvailable;
                }
                if (timeCollect.timeFinish != null && timeCollect.timeUserExecute != null) {
                    if (sumTimeFinishConnector == -1) {
                        sumTimeFinishConnector = 0;
                    }
                    sumTimeFinishConnector += timeCollect.timeFinish - timeCollect.timeUserExecute;
                }
            
            }
            // it's possible to not have any time (an active gateway has not
            // time)
            if (mintimeInitial != -1) {
                oneSynthesisLine.put(cstActivityDateBegin, mintimeInitial);
                oneSynthesisLine.put(cstActivityDateBeginHuman, getDisplayDate(mintimeInitial));
            }
            if (isReallyTerminated) {
                oneSynthesisLine.put(cstActivityDateEnd, maxtimeComplete);
                oneSynthesisLine.put(cstActivityDateEndHuman, getDisplayDate(maxtimeComplete));
            }

            if (isInitializing) {
                oneSynthesisLine.put(cstActivityState, "initializing");
            }
            if (isExecuting) {
                oneSynthesisLine.put(cstActivityState, "Executing");
            }
            if (isReady) {
                oneSynthesisLine.put(cstActivityState, "ready");
            }
            if (isFailed) {
                oneSynthesisLine.put(cstActivityState, "failed");
            }
            if (isCompleted) {
                oneSynthesisLine.put(cstActivityState, "completed");
            }
            if (isCancelled) {
                oneSynthesisLine.put(cstActivityState, "cancelled");
            }
            if (isFailed) {
                oneSynthesisLine.put(cstActivityState, "failed");
            }

            // now build the synthesis
            expl += "timeEnterConnector[" + sumTimeEnterConnector + "] timeUser[" + sumTimeWaitUser
                    + "] timeFinishConnector[" + sumTimeFinishConnector + "]";
            oneSynthesisLine.put(cstActivityExpl, expl);

            oneSynthesisLine.put("enterconnector", sumTimeEnterConnector);
            oneSynthesisLine.put("user", sumTimeWaitUser);
            // case of gateway or automatic task
            oneSynthesisLine.put("finishconnector", sumTimeFinishConnector);

            logger.info("Calcul time:" + expl);

            // onAnalysis.put("end", (timeCompleted - timeCompleted));
        }
        // Then process instance information

        final List<Map<String, Object>> listSynthesis = new ArrayList<Map<String, Object>>();

        // built the timeline

        final Date currentDate = new Date();
        final List<ActivityTimeLine> listTimeline = new ArrayList<ActivityTimeLine>();
        for (final Map<String, Object> oneSynthesisLine : mapSynthesis.values()) {
            listSynthesis.add(oneSynthesisLine);
            if (oneSynthesisLine.get(cstActivityDateBegin) == null) {
                continue;
            }
            listTimeline
                    .add(ActivityTimeLine.getActivityTimeLine((String) oneSynthesisLine.get(cstActivityName),
                            new Date((Long) oneSynthesisLine.get(cstActivityDateBegin)),
                            oneSynthesisLine.get(cstActivityDateEnd) == null ? currentDate
                                    : new Date((Long) oneSynthesisLine.get(cstActivityDateEnd))));
        }
        // now order all by the time
        Collections.sort(listTimeline, new Comparator<ActivityTimeLine>() {

            public int compare(final ActivityTimeLine s1, final ActivityTimeLine s2) {
                final Long d1 = s1.getDateLong();
                final Long d2 = s2.getDateLong();
                return d1 > d2 ? 1 : -1;
            }
        });
        // and order the list
        sortTheList(listSynthesis, cstActivityDateBegin);
        caseDetails.put("synthesis", listSynthesis);

        final String timeLineChart = CaseGraphDisplay.getActivityTimeLine("Activity", listTimeline);
        // logger.info("Return CHART>>" + timeLineChart + "<<");

        caseDetails.put("chartTimeline", timeLineChart);

        // ----------------------------------- overview
        boolean oneProcessInstanceIsFound = false;
        try {
            final ProcessInstance processInstance = processAPI.getProcessInstance(caseHistoryParameter.caseId);
            oneProcessInstanceIsFound = true;
            caseDetails.put(cstCaseId, processInstance.getId());
            caseDetails.put("caseState", "ACTIF");
            caseDetails.put(cstCaseStartDateSt,
                    processInstance.getStartDate() == null ? "" : getDisplayDate(processInstance.getStartDate()));
            caseDetails.put("endDateSt",
                    processInstance.getEndDate() == null ? "" : getDisplayDate(processInstance.getEndDate()));
            caseDetails.put("stringIndex",
                    " " + getDisplayString(processInstance.getStringIndexLabel(1)) + ":["
                            + getDisplayString(processInstance.getStringIndex1()) + "] "
                            + getDisplayString(processInstance.getStringIndexLabel(2)) + ":["
                            + getDisplayString(processInstance.getStringIndex2())
                            + "] 3:[" + getDisplayString(processInstance.getStringIndex3()) + "] 4:["
                            + getDisplayString(processInstance.getStringIndex4()) + "] 5:["
                            + getDisplayString(processInstance.getStringIndex5()) + "]");
            final ProcessDefinition processDefinition = processAPI
                    .getProcessDefinition(processInstance.getProcessDefinitionId());
            caseDetails.put(cstCaseProcessInfo,
                    processDefinition.getName() + " (" + processDefinition.getVersion() + ")");

        } catch (final ProcessInstanceNotFoundException e1) {
            logger.info("processinstance [" + caseHistoryParameter.caseId + "] not found (not active) ");

        } catch (final Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("During getProcessInstance : " + e.toString() + " at " + sw.toString());
            caseDetails.put("errormessage", "Error during get case history " + e.toString());
        }

        try {

            // search by the source
            if (!oneProcessInstanceIsFound) {
                final ArchivedProcessInstance archivedProcessInstance = processAPI
                        .getFinalArchivedProcessInstance(caseHistoryParameter.caseId);
                logger.info(
                        "Case  [" + caseHistoryParameter.caseId + "]  found by getFinalArchivedProcessInstance ? "
                                + (archivedProcessInstance == null ? "No" : "Yes"));
                if (archivedProcessInstance != null) {
                    oneProcessInstanceIsFound = true;
                    caseDetails.put("caseState", "ARCHIVED");
                    caseDetails.put("caseId", archivedProcessInstance.getSourceObjectId());
                    caseDetails.put("archiveCaseId", archivedProcessInstance.getId());

                }
                caseDetails.put("startDateSt", archivedProcessInstance.getStartDate() == null ? ""
                        : getDisplayDate(archivedProcessInstance.getStartDate()));
                caseDetails.put("endDateSt", archivedProcessInstance.getEndDate() == null ? ""
                        : getDisplayDate(archivedProcessInstance.getEndDate()));

                caseDetails.put("archivedDateSt", getDisplayDate(archivedProcessInstance.getArchiveDate()));
                final ProcessDefinition processDefinition = processAPI
                        .getProcessDefinition(archivedProcessInstance.getProcessDefinitionId());
                caseDetails.put("processdefinition",
                        processDefinition.getName() + " (" + processDefinition.getVersion() + ")");
            }

        } catch (final ArchivedProcessInstanceNotFoundException e1) {
            logger.info("Case found by getFinalArchivedProcessInstance ? exception so not found");
        } catch (final Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe("During getArchivedProcessInstance : " + e.toString() + " at " + sw.toString());
            caseDetails.put("errormessage", "Error during get case history " + e.toString());

        } ;
        if (!oneProcessInstanceIsFound) {
            caseDetails.put("errormessage", "The caseId [" + caseHistoryParameter.caseId + "] does not exist");
        }
        return caseDetailsMap;
    }

        */
        return result;
    }

            /* -------------------------------------------------------------------- */
            /*                                                                      */
            /* Private */
            /*                                                                      */
            /* -------------------------------------------------------------------- */
            /**
             * @param listToSort
             * @param attributName : list of attribut separate par ; Example: name;docindex,processinstance .
             *        If name hjave the same value, compare docindex and so on.
             */
            private static void sortTheList(List<Map<String, Object>> listToSort, final String attributName) {

                Collections.sort(listToSort, new Comparator<Map<String, Object>>() {

                    public int compare(final Map<String, Object> s1, final Map<String, Object> s2) {

                        StringTokenizer st = new StringTokenizer(attributName, ";");
                        while (st.hasMoreTokens()) {
                            String token = st.nextToken();
                            Object d1 = s1.get(token);
                            Object d2 = s2.get(token);
                            if (d1 != null && d2 != null) {
                                int comparaison = 0;
                                if (d1 instanceof String)
                                    comparaison = ((String) d1).compareTo(((String) d2));
                                if (d1 instanceof Integer)
                                    comparaison = ((Integer) d1).compareTo(((Integer) d2));
                                if (d1 instanceof Long)
                                    comparaison = ((Long) d1).compareTo(((Long) d2));
                                if (comparaison != 0)
                                    return comparaison;
                            }
                            // one is null, or both are null : continue
                        }
                        return 0;
                    }
                });;
            }
            

            private static String getDisplayDate(final Object dateObj) {
                if (dateObj == null) {
                    return "";
                }
                if (dateObj instanceof Long) {
                    return sdf.format(new Date((Long) dateObj)); // +"("+dateObj+")";
                }
                if (dateObj instanceof Date) {
                    return sdf.format((Date) dateObj); // +"("+ (
                                                       // (Date)dateObj).getTime()+")"
                                                       // ;
                }
                return "-";
            }  
}
