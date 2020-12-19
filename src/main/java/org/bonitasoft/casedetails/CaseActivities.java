package org.bonitasoft.casedetails;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bonitasoft.casedetails.CaseDetails.CaseDetailFlowNode;
import org.bonitasoft.casedetails.CaseDetails.ProcessInstanceDescription;
import org.bonitasoft.casedetails.CaseDetailsAPI.CaseHistoryParameter;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.actor.ActorInstance;
import org.bonitasoft.engine.bpm.connector.ConnectorInstance;
import org.bonitasoft.engine.bpm.connector.ConnectorInstancesSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ActivityStates;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ArchivedHumanTaskInstance;
import org.bonitasoft.engine.bpm.flownode.FlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.FlowNodeInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.GatewayInstance;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.identity.UserNotFoundException;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

/* -------------------------------------------------------------------- */
/*                                                                      */
/* Activities manipulation */
/*                                                                      */
/* -------------------------------------------------------------------- */

public class CaseActivities {

    final static Logger logger = Logger.getLogger(CaseActivities.class.getName());

  
    private final static BEvent eventContractTasksFailed = new BEvent(CaseActivities.class.getName(), 1, Level.ERROR, "Can't load Task Contract", "The call to retrieve contract on an archived human task failed",
            "result will not contains the contract on that task",
            "Check exception");

    private final static BEvent eventSearchTaskFailed = new BEvent(CaseActivities.class.getName(), 2, Level.ERROR, "Search task failed", "The call to search tasks failed",
            "result will not contains tasks",
            "Check exception ");
    private final static BEvent eventSearchArchivedTasksFailed = new BEvent(CaseActivities.class.getName(), 1, Level.ERROR, "Can't load ArchivedTask Contract", "The call to search archived tasks failed",
            "result will not contains archived tasks",
            "Check exception ");

    /**
     * this is a utility class
     * Default Constructor.
     */
    private CaseActivities() {};

    protected static void loadActivities(CaseDetails caseDetails, CaseHistoryParameter caseHistoryParameter, ProcessAPI processAPI, IdentityAPI identityAPI) {

        // ---------------------------- Activities
        // keep the list of FlownodeId returned: the event should return the
        // same ID and then it's necessary to merge them

        // multi instance task : if the task is declare as a multi instance,
        // it considere as finish ONLY when we see the
        // MULTI_INSTANCE_ACTIVITY / completed

        // Active tasks
        SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 1000);
        // searchOptionsBuilder.filter(ActivityInstanceSearchDescriptor.PROCESS_INSTANCE_ID,
        // processInstanceId);

        if (caseHistoryParameter.loadSubProcess) {
            searchOptionsBuilder.filter(FlowNodeInstanceSearchDescriptor.ROOT_PROCESS_INSTANCE_ID,
                    caseDetails.rootCaseId);
        } else {
            searchOptionsBuilder.filter(FlowNodeInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID,
                    caseDetails.rootCaseId);
        }
        searchOptionsBuilder.sort(FlowNodeInstanceSearchDescriptor.LAST_UPDATE_DATE, Order.ASC);
        
        // SearchResult<ActivityInstance> searchActivity =
        // processAPI.searchActivities(searchOptionsBuilder.done());
        // the mutitinstance may be active and the task already archived
        Set<Long> multiInstanceTasks = new HashSet<>();
        try {
            final SearchResult<FlowNodeInstance> searchFlowNode = processAPI.searchFlowNodeInstances(searchOptionsBuilder.done());

            for (final FlowNodeInstance activityInstance : searchFlowNode.getResult()) {
                CaseDetailFlowNode flowNodeDetail = caseDetails.createInstanceFlowNodeDetails();
                flowNodeDetail.activityInstance = activityInstance;

                flowNodeDetail.dateFlowNode = activityInstance.getLastUpdateDate();
                if (multiInstanceTasks.contains( activityInstance.getParentContainerId()))
                {
                    // this is part of a multi instance
                    flowNodeDetail.isMultiInstanciationTask = true;
                }
                // Human task
                if (activityInstance instanceof HumanTaskInstance) {
                    HumanTaskInstance humanTaskInstance = (HumanTaskInstance) activityInstance;
                    
                    Long actorId = humanTaskInstance.getActorId();
                    if (actorId!=null)
                    {
                        try {
                            flowNodeDetail.actor = processAPI.getActor(actorId);
                        }
                        catch(Exception e) {};
                        SearchResult<User> searchCandidates = processAPI.searchUsersWhoCanExecutePendingHumanTask(humanTaskInstance.getId(), new SearchOptionsBuilder(0,10000).done());
                        flowNodeDetail.nbCandidates= searchCandidates.getCount();
                        
                    }
                    flowNodeDetail.assigneeId = humanTaskInstance.getAssigneeId();
                    if (flowNodeDetail.assigneeId > 0) {
                        try {
                            flowNodeDetail.assigneeUser = identityAPI.getUser(flowNodeDetail.assigneeId);
                        } catch (Exception e) {
                        }

                    }
                }
                    
                    
                if (activityInstance instanceof GatewayInstance) {
                    // an active gatewaty does not have any date...
                    flowNodeDetail.dateFlowNode = null;
                }

                logger.fine("##### FLOWNODE Activity[" + activityInstance.getName() + "] Class["
                        + activityInstance.getClass().getName() + "]");

                if ("MULTI_INSTANCE_ACTIVITY".equals(activityInstance.getType().toString())) {
                    caseDetails.listMultiInstanceActivity.add(activityInstance.getFlownodeDefinitionId());
                    multiInstanceTasks.add( activityInstance.getId() );
                }

                if (activityInstance.getExecutedBy() != 0) {
                    try {
                        flowNodeDetail.userExecutedBy = identityAPI.getUser(activityInstance.getExecutedBy());
                    } catch (final UserNotFoundException ue) {
                    }
                }
                
                if (caseHistoryParameter.loadContract && activityInstance instanceof HumanTaskInstance) {
                    try {
                        // by definition, a human task is READY and does not be executed, so no Contract value
                        // mapActivity.put("contract", getContractValues(null, null, (HumanTaskInstance) activityInstance, null, processAPI));
                    } catch (Exception e) {
                        // caseDetails.errorMessage="Error during get case history " + e.toString();
                    }
                }
                
                if (activityInstance.getState().equals( ActivityStates.FAILED_STATE)) {
                    // search and load connectors
                    SearchOptionsBuilder sobConnector = new SearchOptionsBuilder(0,100);
                    sobConnector.filter( ConnectorInstancesSearchDescriptor.CONTAINER_ID, activityInstance.getId());
                    sobConnector.sort(ConnectorInstancesSearchDescriptor.EXECUTION_ORDER,Order.ASC);
                    SearchResult<ConnectorInstance> searchConnectors = processAPI.searchConnectorInstances( sobConnector.done());
                    flowNodeDetail.connectors = searchConnectors.getResult();
                }

            }
        } catch (SearchException e1) {
            caseDetails.listEvents.add(new BEvent(eventSearchTaskFailed, e1, "During search on caseId [" + caseDetails.rootCaseId + "]"));

        }

        logger.fine("#### casehistory on processInstanceId[" + caseDetails.rootCaseId + "] : found ["
                + caseDetails.listCaseDetailFlowNodes.size() + "] activity");

        // ------------------- archived   
        // Attention, same activity will be returned multiple time and search based on ROOT process instance does not works
        Set<Long> setActivitiesRetrieved = new HashSet<>();
        for (ProcessInstanceDescription processInstance : caseDetails.listProcessInstances) {

            searchOptionsBuilder = new SearchOptionsBuilder(0, 1000);
            if (caseHistoryParameter.loadSubProcess) {
                searchOptionsBuilder.filter(ArchivedFlowNodeInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID,
                        processInstance.processInstanceId);
                // bug : not working
                // searchOptionsBuilder.filter(ArchivedFlowNodeInstanceSearchDescriptor.ROOT_PROCESS_INSTANCE_ID,
                // caseHistoryParameter.caseId);
            } else {
                searchOptionsBuilder.filter(ArchivedFlowNodeInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID,
                        processInstance.processInstanceId);
            }

            final SearchResult<ArchivedFlowNodeInstance> searchActivityArchived;
            try {
                searchActivityArchived = processAPI.searchArchivedFlowNodeInstances(searchOptionsBuilder.done());

                //  get the list of Aborted task : a aborted task has a READY, but the READY does't not contains contract
                // the point is there is a READY when the task is created,
                // and a READY + CONTRACT when the task is executed ! the first READY is overlap and we lost the information when the task was READY actually
                //  (second status should be EXECUTED actually)
                
                Set<Long> abordedTask = new HashSet<>();

                for (final ArchivedFlowNodeInstance flowNodeInstance : searchActivityArchived.getResult()) {
                    if ((ActivityStates.ABORTED_STATE.equalsIgnoreCase(flowNodeInstance.getState())))
                        abordedTask.add( flowNodeInstance.getSourceObjectId() );
                }
                for (final ArchivedFlowNodeInstance flowNodeInstance : searchActivityArchived.getResult()) {
                    if (setActivitiesRetrieved.contains(flowNodeInstance.getId()))
                        continue;

                    CaseDetailFlowNode flowNodeDetail = caseDetails.createInstanceFlowNodeDetails();
                    flowNodeDetail.archFlownNodeInstance = flowNodeInstance;

                    setActivitiesRetrieved.add(flowNodeInstance.getId());
                    if (flowNodeInstance.getExecutedBy() != 0) {
                        try {
                            flowNodeDetail.userExecutedBy = identityAPI.getUser(flowNodeInstance.getExecutedBy());
                        } catch (final UserNotFoundException ue) {
                        } 
                    }
                    
                    if (multiInstanceTasks.contains( flowNodeInstance.getParentContainerId()))
                    {
                        // this is part of a multi instance
                        flowNodeDetail.isMultiInstanciationTask = true;
                    }
                 
                    if ("MULTI_INSTANCE_ACTIVITY".equals(flowNodeInstance.getType().toString())) {
                        caseDetails.listMultiInstanceActivity.add(flowNodeInstance.getFlownodeDefinitionId());
                        multiInstanceTasks.add( flowNodeInstance.getSourceObjectId() );
                    }

                    // only on archived READY state
                    if (caseHistoryParameter.loadContract 
                            && flowNodeInstance instanceof ArchivedHumanTaskInstance
                            && (! abordedTask.contains( flowNodeInstance.getSourceObjectId() ))
                            && (ActivityStates.READY_STATE.equalsIgnoreCase(flowNodeInstance.getState())))
                        try {
                            flowNodeDetail.listContractValues = CaseContract.getContractTaskValues(caseDetails, caseHistoryParameter, (ArchivedHumanTaskInstance) flowNodeInstance, processAPI);
                        } catch (Exception e) {
                            caseDetails.listEvents.add(new BEvent(eventContractTasksFailed, e, "During activity [" + flowNodeInstance.getId() + "]"));
                        }
                }
            } catch (SearchException e1) {
                caseDetails.listEvents.add(new BEvent(eventSearchArchivedTasksFailed, e1, "During search on caseId [" + processInstance.processInstanceId + "]"));

            }
        } // end loop processinstance
    }
}
