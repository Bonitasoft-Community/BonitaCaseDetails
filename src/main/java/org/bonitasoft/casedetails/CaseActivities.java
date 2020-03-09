package org.bonitasoft.casedetails;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.bonitasoft.casedetails.CaseDetails.CaseDetailFlowNode;
import org.bonitasoft.casedetails.CaseDetails.ProcessInstanceDescription;
import org.bonitasoft.casedetails.CaseDetailsAPI.CaseHistoryParameter;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.flownode.ActivityStates;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ArchivedHumanTaskInstance;
import org.bonitasoft.engine.bpm.flownode.FlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.FlowNodeInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.GatewayInstance;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.identity.UserNotFoundException;
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

        // SearchResult<ActivityInstance> searchActivity =
        // processAPI.searchActivities(searchOptionsBuilder.done());
        try {

            final SearchResult<FlowNodeInstance> searchFlowNode = processAPI.searchFlowNodeInstances(searchOptionsBuilder.done());

            for (final FlowNodeInstance activityInstance : searchFlowNode.getResult()) {
                CaseDetailFlowNode flowNodeDetail = caseDetails.addFlowNodeDetails();
                flowNodeDetail.activityInstance = activityInstance;

                flowNodeDetail.dateFlowNode = activityInstance.getLastUpdateDate();
                if (activityInstance instanceof GatewayInstance) {
                    // an active gatewaty does not have any date...
                    flowNodeDetail.dateFlowNode = null;
                }

                logger.fine("##### FLOWNODE Activity[" + activityInstance.getName() + "] Class["
                        + activityInstance.getClass().getName() + "]");

                if ("MULTI_INSTANCE_ACTIVITY".equals(activityInstance.getType().toString())) {
                    caseDetails.listMultiInstanceActivity.add(activityInstance.getFlownodeDefinitionId());
                }

                if (activityInstance.getExecutedBy() != 0) {
                    try {
                        flowNodeDetail.userExecutedBy = identityAPI.getUser(activityInstance.getExecutedBy());
                    } catch (final UserNotFoundException ue) {
                    } ;

                }
                if (caseHistoryParameter.loadContract && activityInstance instanceof HumanTaskInstance)
                    try {
                        // by definition, a human task is READY and does not be executed, so no Contract value
                        // mapActivity.put("contract", getContractValues(null, null, (HumanTaskInstance) activityInstance, null, processAPI));
                    } catch (Exception e) {
                        // caseDetails.errorMessage="Error during get case history " + e.toString();
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

                for (final ArchivedFlowNodeInstance flownNodeInstance : searchActivityArchived.getResult()) {
                    if (setActivitiesRetrieved.contains(flownNodeInstance.getId()))
                        continue;

                    CaseDetailFlowNode flowNodeDetail = caseDetails.addFlowNodeDetails();
                    flowNodeDetail.archFlownNodeInstance = flownNodeInstance;

                    setActivitiesRetrieved.add(flownNodeInstance.getId());
                    if (flownNodeInstance.getExecutedBy() != 0) {
                        try {
                            flowNodeDetail.userExecutedBy = identityAPI.getUser(flownNodeInstance.getExecutedBy());
                        } catch (final UserNotFoundException ue) {
                        } ;
                    }
                    // only on archived READY state
                    if (caseHistoryParameter.loadContract && flownNodeInstance instanceof ArchivedHumanTaskInstance && (ActivityStates.READY_STATE.equalsIgnoreCase(flownNodeInstance.getState())))
                        try {
                            flowNodeDetail.listContractValues = CaseContract.getContractTaskValues(caseDetails, caseHistoryParameter, (ArchivedHumanTaskInstance) flownNodeInstance, processAPI);
                        } catch (Exception e) {
                            caseDetails.listEvents.add(new BEvent(eventContractTasksFailed, e, "During activity [" + flownNodeInstance.getId() + "]"));
                        }
                }
            } catch (SearchException e1) {
                caseDetails.listEvents.add(new BEvent(eventSearchArchivedTasksFailed, e1, "During search on caseId [" + processInstance.processInstanceId + "]"));

            }
        } // end loop processinstance
    }
}
