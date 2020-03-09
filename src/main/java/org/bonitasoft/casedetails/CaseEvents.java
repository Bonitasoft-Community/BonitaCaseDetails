package org.bonitasoft.casedetails;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bonitasoft.casedetails.CaseDetails.CaseDetailFlowNode;
import org.bonitasoft.casedetails.CaseDetails.MessageContent;
import org.bonitasoft.casedetails.CaseDetails.MessageDetail;
import org.bonitasoft.casedetails.CaseDetails.ProcessInstanceDescription;
import org.bonitasoft.casedetails.CaseDetails.SignalDetail;
import org.bonitasoft.casedetails.CaseDetailsAPI.CaseHistoryParameter;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.flownode.CatchEventDefinition;
import org.bonitasoft.engine.bpm.flownode.CatchMessageEventTriggerDefinition;
import org.bonitasoft.engine.bpm.flownode.CorrelationDefinition;
import org.bonitasoft.engine.bpm.flownode.EventCriterion;
import org.bonitasoft.engine.bpm.flownode.EventInstance;
import org.bonitasoft.engine.bpm.flownode.FlowElementContainerDefinition;
import org.bonitasoft.engine.bpm.flownode.FlowNodeDefinition;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

/* -------------------------------------------------------------------- */
/*                                                                      */
/* Events manipulation */
/*                                                                      */
/* -------------------------------------------------------------------- */

public class CaseEvents {

    final static Logger logger = Logger.getLogger(CaseEvents.class.getName());

    private final static BEvent eventProcessDesignFailed = new BEvent(CaseEvents.class.getName(), 1, Level.ERROR, "Can't load Process Design",
            "To load the event definition, the process design has to be loaded. This failed",
            "result will not contains the details of event",
            "Check exception ");

    protected static void loadEvents(CaseDetails caseDetails, CaseHistoryParameter caseHistoryParameter, ProcessAPI processAPI, IdentityAPI identityAPI) {

        // ------------------------------ events
        // List<Map<String, Object>> listSignals = new ArrayList<Map<String, Object>>();
        // List<Map<String, Object>> listMessages = new ArrayList<Map<String, Object>>();
        for (ProcessInstanceDescription processInstance : caseDetails.listProcessInstances) {
            final List<EventInstance> listEventInstance = processAPI.getEventInstances(processInstance.processInstanceId, 0,
                    1000, EventCriterion.NAME_ASC);
            for (final EventInstance eventInstance : listEventInstance) {
                CaseDetailFlowNode flowNodeDetail = caseDetails.getFlowNodeById(eventInstance.getId());

                if (flowNodeDetail == null) {
                    flowNodeDetail = caseDetails.addFlowNodeDetails();
                    flowNodeDetail.activityInstance = eventInstance;
                    // mapActivity.put(cstPerimeter, "ARCHIVED");
                }

                flowNodeDetail.dateFlowNode = eventInstance.getLastUpdateDate();

                DesignProcessDefinition designProcessDefinition;
                try {
                    designProcessDefinition = processAPI
                            .getDesignProcessDefinition(eventInstance.getProcessDefinitionId());

                    FlowElementContainerDefinition flowElementContainerDefinition = designProcessDefinition
                            .getFlowElementContainer();
                    FlowNodeDefinition flowNodeDefinition = flowElementContainerDefinition
                            .getFlowNode(eventInstance.getFlownodeDefinitionId());
                    if (flowNodeDefinition instanceof CatchEventDefinition) {
                        CatchEventDefinition catchEventDefinition = (CatchEventDefinition) flowNodeDefinition;
                        if (catchEventDefinition.getSignalEventTriggerDefinitions() != null) {
                            SignalDetail signalDetail = caseDetails.addSignal();
                            signalDetail.eventInstance = eventInstance;
                            signalDetail.listSignalEvent = catchEventDefinition.getSignalEventTriggerDefinitions();

                        } // end signal detection
                        if (catchEventDefinition.getMessageEventTriggerDefinitions() != null) {
                            MessageDetail messageDetail = caseDetails.addMessageDetail();
                            messageDetail.eventInstance = eventInstance;
                            for (CatchMessageEventTriggerDefinition msgTrigger : catchEventDefinition.getMessageEventTriggerDefinitions()) {
                                MessageContent msgDetailContent = messageDetail.addMessageContent();
                                msgDetailContent.messageEvent = msgTrigger;

                                List<String> listCorrelationValue = getCorrelationValue(eventInstance);

                                List<CorrelationDefinition> listCorrelations = msgTrigger.getCorrelations();
                                for (int i = 0; i < listCorrelations.size(); i++) {
                                    CorrelationDefinition correlationDefinition = listCorrelations.get(i);

                                    msgDetailContent.mapCorrelationValues.put(correlationDefinition.getKey().getName(), i < listCorrelationValue.size() ? listCorrelationValue.get(i) : null);
                                }

                            }

                        } // end message detection
                    }
                    // ActivityDefinition activityDefinition= processAPI.getDef
                    // CatchEventDefinition.getSignalEventTriggerDefinitions().getSignalName()

                } catch (ProcessDefinitionNotFoundException e) {
                    caseDetails.listEvents.add(new BEvent(eventProcessDesignFailed, e, "ProcessDefinitionId[" + eventInstance.getProcessDefinitionId() + "]"));
                }
            }
        }
    }

    /**
     * search the correlation Values
     * 
     * @param eventInstance
     * @return
     */
    private static List<String> getCorrelationValue(EventInstance eventInstance) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<String> listCorrelationValue = new ArrayList<>();
        try {

            // Get the VALUE for the correlation
            // SELECT CORRELATION1, CORRELATION2, CORRELATION3, CORRELATION4,
            // CORRELATION5 FROM WAITING_EVENT where FLOWNODEINSTANCEID=60004
            con = CaseDetailsToolbox.getConnection();
            List<String> listColumnName = new ArrayList<>();

            for (int i = 1; i <= 5; i++) {
                listColumnName.add("CORRELATION" + i);
            }
            String sqlRequest = " select ";
            for (int i = 0; i < listColumnName.size(); i++) {
                sqlRequest += listColumnName.get(i) + ", ";
            }
            sqlRequest += " FLOWNODEINSTANCEID  FROM WAITING_EVENT where FLOWNODEINSTANCEID=?";

            pstmt = con.prepareStatement(sqlRequest);
            pstmt.setObject(1, eventInstance.getId());

            rs = pstmt.executeQuery();
            // expect only one record
            if (!rs.next())
                return listCorrelationValue;
            for (int i = 0; i < listColumnName.size(); i++) {
                String result = rs.getString(i + 1);
                // form is msgKeyId-$-3002
                if (result != null) {
                    int pos = result.indexOf("-$-");
                    if (pos != -1)
                        result = result.substring(pos + "-$-".length());
                }
                listCorrelationValue.add(result);
            }

        } catch (final Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            final String exceptionDetails = sw.toString();
            logger.severe("loadArchivedProcessVariables : " + e.toString() + " : " + exceptionDetails);

        } finally {
            if (rs != null) {
                try {
                    rs.close();
                    rs = null;
                } catch (final SQLException localSQLException) {
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                    pstmt = null;
                } catch (final SQLException localSQLException) {
                }
            }
            if (con != null) {
                try {
                    con.close();
                    con = null;
                } catch (final SQLException localSQLException1) {
                }
            }
        }
        return listCorrelationValue;

    }
}
