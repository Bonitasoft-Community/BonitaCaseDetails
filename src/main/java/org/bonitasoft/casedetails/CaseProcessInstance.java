package org.bonitasoft.casedetails;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.bonitasoft.casedetails.CaseDetails.ProcessInstanceDescription;
import org.bonitasoft.casedetails.CaseDetailsAPI.CaseHistoryParameter;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance;
import org.bonitasoft.engine.identity.UserNotFoundException;

/* -------------------------------------------------------------------- */
/*                                                                      */
/* Processinstance manipulation */
/*                                                                      */
/* -------------------------------------------------------------------- */

public class CaseProcessInstance {

    final static Logger logger = Logger.getLogger(CaseProcessInstance.class.getName());

    /**
     * load all processinstance declaration
     * 
     * @param rootProcessInstanceId
     * @param loadSubProcess
     * @param processAPI
     * @return
     */
    protected static void loadProcessInstances(CaseDetails caseDetails, CaseHistoryParameter caseHistoryParameter,
            ProcessAPI processAPI, IdentityAPI identityAPI) {

        caseDetails.listProcessInstances = getAllProcessInstance(caseDetails.tenantId, caseDetails.rootCaseId, caseHistoryParameter.loadSubProcess, processAPI);

        for (ProcessInstanceDescription processInstanceDescription : caseDetails.listProcessInstances) {
            try {
                processInstanceDescription.processDefinition = caseDetails.getCaseDetailsAPI().getProcessDefinition(processInstanceDescription.processDefinitionId, processAPI);

                if (processInstanceDescription.callerId != null && processInstanceDescription.callerId > 0) {
                    boolean foundIt = false;
                    try {
                        ActivityInstance act = processAPI.getActivityInstance(processInstanceDescription.callerId);
                        foundIt = true;
                        processInstanceDescription.parentActivity = act;
                        processInstanceDescription.parentActivityName = act.getName();
                        processInstanceDescription.parentProcessInstanceId = act.getParentProcessInstanceId();
                        processInstanceDescription.parentProcessDefinitionId = act.getProcessDefinitionId();

                        processInstanceDescription.parentProcessDefinition = caseDetails.getCaseDetailsAPI().getProcessDefinition(act.getProcessDefinitionId(), processAPI);

                    } catch (Exception e) {
                    }
                    if (!foundIt) { // maybe archived
                        try {
                            ArchivedActivityInstance act = processAPI.getArchivedActivityInstance(processInstanceDescription.callerId);
                            processInstanceDescription.archiveParentActivity = act;
                            processInstanceDescription.parentActivityName = act.getName();
                            processInstanceDescription.parentProcessInstanceId = act.getProcessInstanceId();

                            processInstanceDescription.parentProcessDefinitionId = act.getProcessDefinitionId();

                            processInstanceDescription.parentProcessDefinition = caseDetails.getCaseDetailsAPI().getProcessDefinition(act.getProcessDefinitionId(), processAPI);

                        } catch (Exception e) {
                            // not not manage this error
                        }
                    }

                }

            } catch (Exception e) {
                final StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                logger.severe("During loadProcessinstance : " + e.toString() + " at " + sw.toString());

            }
        }

        /**
         * load the user who started the case
         */
        for (ProcessInstanceDescription processInstanceDescription : caseDetails.listProcessInstances) {
            Long userId = null;
            if (processInstanceDescription.processInstance != null)
                userId = processInstanceDescription.processInstance.getStartedBy();
            else if (processInstanceDescription.archProcessInstance != null)
                userId = processInstanceDescription.archProcessInstance.getStartedBy();

            if (userId != null) {
                try {
                    processInstanceDescription.userCreatedBy = identityAPI.getUser(userId);
                } catch (UserNotFoundException e) {
                    final StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    logger.severe("During searching userName[" + processInstanceDescription.callerId + "] : " + e.toString() + " at " + sw.toString());

                }

            }
        }

        /**
         * load contracts
         */
        if (caseHistoryParameter.loadContract) {
            for (ProcessInstanceDescription processInstanceDescription : caseDetails.listProcessInstances) {
                processInstanceDescription.contractInstanciation = CaseContract.getContractInstanciationValues(caseDetails, caseHistoryParameter, processInstanceDescription, processAPI);

            }
        }
    }

    /**
     * @param rootProcessInstance
     * @param showSubProcess
     * @param processAPI
     * @return
     */
    private static List<ProcessInstanceDescription> getAllProcessInstance(Long tenantId, long rootProcessInstance,
            boolean showSubProcess, ProcessAPI processAPI) {
        List<ProcessInstanceDescription> listProcessInstances = new ArrayList<>();

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sqlRequest = "";
        try {
            sqlRequest = "select ID, ROOTPROCESSINSTANCEID, PROCESSDEFINITIONID, CALLERID,  STARTDATE, ENDDATE, TENANTID  from PROCESS_INSTANCE where ROOTPROCESSINSTANCEID = ?";
            if (tenantId != null) {
                sqlRequest += " and TENANTID=?";
            }

            // search all process instance like with the root
            con = CaseDetailsToolbox.getConnection();
            pstmt = con.prepareStatement(sqlRequest);
            pstmt.setLong(1, rootProcessInstance);
            if (tenantId!=null)
                pstmt.setLong(2, tenantId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                ProcessInstanceDescription processInstanceDescription = new ProcessInstanceDescription();
                processInstanceDescription.processInstanceId = rs.getLong(1);
                processInstanceDescription.rootProcessInstanceId = rs.getLong(2);
                processInstanceDescription.processDefinitionId = rs.getLong(3);
                processInstanceDescription.callerId = rs.getLong(4);
                processInstanceDescription.startDate = getDate(rs.getLong(5));
                processInstanceDescription.endDate = getDate(rs.getLong(6));
                processInstanceDescription.tenantId = rs.getLong(7);
                
                processInstanceDescription.isActive = true;
                if (showSubProcess || processInstanceDescription.processInstanceId == rootProcessInstance)
                    listProcessInstances.add(processInstanceDescription);
            }
            rs.close();
            rs = null;
            pstmt.close();
            pstmt = null;
            sqlRequest = "select ID, SOURCEOBJECTID, ROOTPROCESSINSTANCEID, PROCESSDEFINITIONID, CALLERID, STARTDATE, ENDDATE, TENANTID  from ARCH_PROCESS_INSTANCE where ROOTPROCESSINSTANCEID = ?";
            if (tenantId != null) {
                sqlRequest += " and TENANTID=?";
            }

            pstmt = con.prepareStatement(sqlRequest);
            pstmt.setLong(1, rootProcessInstance);
            if (tenantId!=null)
                pstmt.setLong(2, tenantId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                ProcessInstanceDescription processInstanceDescription = new ProcessInstanceDescription();
                processInstanceDescription.archivedProcessInstanceId = rs.getLong(1);
                processInstanceDescription.processInstanceId = rs.getLong(2);
                processInstanceDescription.rootProcessInstanceId = rs.getLong(3);
                processInstanceDescription.processDefinitionId = rs.getLong(4);
                processInstanceDescription.callerId = rs.getLong(5);
                processInstanceDescription.startDate = getDate(rs.getLong(6));
                // there is multiple record, but only one has a endDate (the terminal state, maybe 6 or a different one)
                Long enDateLong = rs.getLong(7);
                if (enDateLong>0)
                    processInstanceDescription.endDate = getDate(enDateLong);
                processInstanceDescription.tenantId = rs.getLong(8);

                processInstanceDescription.isActive = false;
                //maybe in double?
                boolean alreadyExist = false;
                for (ProcessInstanceDescription current : listProcessInstances) {
                    if (current.processInstanceId == processInstanceDescription.processInstanceId) {
                        alreadyExist = true;
                        // but update the endDate ?
                        if (processInstanceDescription.endDate !=null)
                            current.endDate = processInstanceDescription.endDate;
                    }
                }
                if (!alreadyExist)
                    if (showSubProcess || processInstanceDescription.processInstanceId == rootProcessInstance)
                        listProcessInstances.add(processInstanceDescription);

            }
            rs.close();
            rs = null;
            pstmt.close();
            pstmt = null;

            for (ProcessInstanceDescription processInstance : listProcessInstances) {
                if (processInstance.isActive)
                    processInstance.processInstance = processAPI.getProcessInstance(processInstance.processInstanceId);
                else
                    processInstance.archProcessInstance = processAPI.getArchivedProcessInstance(processInstance.archivedProcessInstanceId);
            }
        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe("During getAllProcessInstance : " + e.toString() + " at " + sw.toString());

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
                    // do not manage this error
                }
            }
            if (con != null) {
                try {
                    con.close();
                    con = null;
                } catch (final SQLException localSQLException1) {
                    // do not manage this error
                }
            }
        }

        return listProcessInstances;
    }

    private static Date getDate(Long value) {
        return value == null ? null : new Date(value);
    }
}
