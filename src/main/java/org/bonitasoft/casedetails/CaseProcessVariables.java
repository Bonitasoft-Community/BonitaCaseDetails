package org.bonitasoft.casedetails;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bonitasoft.casedetails.CaseDetails.CaseDetailFlowNode;
import org.bonitasoft.casedetails.CaseDetails.CaseDetailVariable;
import org.bonitasoft.casedetails.CaseDetails.ProcessInstanceDescription;
import org.bonitasoft.casedetails.CaseDetails.ScopeVariable;
import org.bonitasoft.casedetails.CaseDetailsAPI.CaseHistoryParameter;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.data.ArchivedDataInstance;
import org.bonitasoft.engine.bpm.data.DataInstance;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

/* -------------------------------------------------------------------- */
/*                                                                      */
/* ProcessVariable manipulation */
/*                                                                      */
/* -------------------------------------------------------------------- */

public class CaseProcessVariables {

    final static Logger logger = Logger.getLogger(CaseProcessVariables.class.getName());

    private final static BEvent eventLoadArchivedVariableFailed = new BEvent(CaseProcessVariables.class.getName(), 1, Level.ERROR, "Load archived variable failed",
            "The load failed",
            "Result will not contains the archived variable",
            "Check exception");

    private final static BEvent eventLoadVariableFailed = new BEvent(CaseProcessVariables.class.getName(), 2, Level.ERROR, "Load variable failed",
            "Error when variables are loaded",
            "Result will not contains variables",
            "Check exception");

    /**
     * @param caseDetails
     * @param caseHistoryParameter
     * @param processAPI
     */
    protected static void loadVariables(CaseDetails caseDetails, CaseHistoryParameter caseHistoryParameter, ProcessAPI processAPI) {

        loadProcessVariables(caseDetails, caseHistoryParameter, processAPI);
        loadLocalVariables(caseDetails, caseHistoryParameter, processAPI);

    }

    /**
     * load Process Variable, archive and active
     * 
     * @param caseDetails
     * @param caseHistoryParameter
     * @param processAPI
     */
    private static void loadProcessVariables(CaseDetails caseDetails, CaseHistoryParameter caseHistoryParameter, ProcessAPI processAPI) {
        for (ProcessInstanceDescription processInstanceDescription : caseDetails.listProcessInstances) {
            // maybe an archived ID
            try {

                // List<CaseDetailProcessVariable> listArchivedDataInstances = null;

                // ------------------ Active
                if (processInstanceDescription.isActive) {
                    List<DataInstance> listDataInstances = processAPI.getProcessDataInstances(processInstanceDescription.processInstanceId, 0, 1000);
                    for (DataInstance dataInstance : listDataInstances) {
                        CaseDetailVariable processVariable = caseDetails.createInstanceProcessVariableDetail();
                        processVariable.processInstanceId = processInstanceDescription.processInstanceId;
                        processVariable.isArchived = false;
                        processVariable.id = dataInstance.getId();
                        processVariable.name = dataInstance.getName();
                        processVariable.description = dataInstance.getDescription();
                        processVariable.dataInstance = dataInstance;
                        processVariable.dateArchived = null;
                        processVariable.scopeVariable = ScopeVariable.PROCESS;
                        processVariable.value = dataInstance.getValue();
                    }
                }

                // ----------------- archive
                // the method retrieve only one value per archiveProcessData, not all the history
                if (caseHistoryParameter.loadArchivedHistoryProcessVariable)
                    loadArchivedProcessVariables(caseDetails, Arrays.asList(processInstanceDescription.processInstanceId), processAPI);
                else {
                    List<ArchivedDataInstance> listArchivedDataInstances = processAPI.getArchivedProcessDataInstances(processInstanceDescription.processInstanceId, 0, 1000);
                    for (ArchivedDataInstance archivedDataInstance : listArchivedDataInstances) {
                        CaseDetailVariable processVariable = caseDetails.createInstanceProcessVariableDetail();
                        processVariable.processInstanceId = processInstanceDescription.processInstanceId;
                        processVariable.isArchived = true;
                        processVariable.id = archivedDataInstance.getSourceObjectId();
                        processVariable.name = archivedDataInstance.getName();
                        processVariable.description = archivedDataInstance.getDescription();
                        processVariable.archivedDataInstance = archivedDataInstance;
                        processVariable.dateArchived = archivedDataInstance.getArchiveDate();
                        processVariable.scopeVariable = ScopeVariable.PROCESS;
                        processVariable.value = archivedDataInstance.getValue();
                    }
                }

            } catch (Exception e) {

            }

        }
    }

    /**
     * @param caseDetails
     * @param caseHistoryParameter
     * @param processAPI
     */
    private static void loadLocalVariables(CaseDetails caseDetails, CaseHistoryParameter caseHistoryParameter, ProcessAPI processAPI) {

        //--------------------------------------  collect local variable in activity - attention, the same sourceinstanceid can come up multiple time
        Set<Long> registerSourceId = new HashSet<>();
        for (CaseDetailFlowNode caseDetailflowNode : caseDetails.listCaseDetailFlowNodes) {
            try {
                
                // active
                if (caseDetailflowNode.activityInstance != null) {
                    if (registerSourceId.contains(caseDetailflowNode.activityInstance.getId()))
                        continue;
                    registerSourceId.add(caseDetailflowNode.activityInstance.getId());
                    List<DataInstance> listDataInstances = processAPI.getActivityDataInstances(caseDetailflowNode.activityInstance.getId(), 0, 1000);
                    // the getActivityDataInstances return PROCESS_INSTANCE variable !!!
                    for (DataInstance dataInstance : listDataInstances) {
                        if (!dataInstance.getContainerType().equals("PROCESS_INSTANCE")) {
                            CaseDetailVariable processVariable = caseDetails.createInstanceProcessVariableDetail();
                            processVariable.scopeVariable = CaseDetails.ScopeVariable.LOCAL;
                            processVariable.processInstanceId = caseDetailflowNode.activityInstance.getParentContainerId();
                            processVariable.activityId = caseDetailflowNode.activityInstance.getId();
                            processVariable.contextInfo = caseDetailflowNode.activityInstance.getName() + "(" + caseDetailflowNode.activityInstance.getId() + ")";
                            processVariable.dataInstance = dataInstance;
                            processVariable.name = dataInstance.getName();
                            processVariable.description = dataInstance.getDescription();
                            processVariable.typeVariable = dataInstance.getClassName();
                            processVariable.dateArchived = null;
                            processVariable.isArchived = false;
                            processVariable.value = dataInstance.getValue();
                        }
                    }

                }
                
                // archive
                if (caseDetailflowNode.archFlownNodeInstance != null) {
                    if (registerSourceId.contains(caseDetailflowNode.archFlownNodeInstance.getSourceObjectId()))
                        continue;
                    registerSourceId.add(caseDetailflowNode.archFlownNodeInstance.getSourceObjectId());

                    if (caseHistoryParameter.loadArchivedHistoryProcessVariable) {
                        List<CaseDetailVariable> listLocalVariable = loadArchivedProcessVariables(caseDetails, Arrays.asList(caseDetailflowNode.archFlownNodeInstance.getSourceObjectId()), processAPI);
                        // complete the processInstanceId
                        for (CaseDetailVariable localVariable : listLocalVariable)
                            localVariable.processInstanceId = caseDetailflowNode.getProcessInstanceId();
                    } else {
                        List<ArchivedDataInstance> listArchivedDataInstances = processAPI.getArchivedActivityDataInstances(caseDetailflowNode.archFlownNodeInstance.getSourceObjectId(), 0, 1000);
                        for (ArchivedDataInstance archiveDataInstance : listArchivedDataInstances) {
                            if (!archiveDataInstance.getContainerType().equals("PROCESS_INSTANCE")) {
                                CaseDetailVariable processVariable = caseDetails.createInstanceProcessVariableDetail();
                                processVariable.scopeVariable = CaseDetails.ScopeVariable.LOCAL;
                                processVariable.processInstanceId = caseDetailflowNode.archFlownNodeInstance.getProcessInstanceId();
                                processVariable.activityId = caseDetailflowNode.archFlownNodeInstance.getSourceObjectId();
                                processVariable.contextInfo = caseDetailflowNode.archFlownNodeInstance.getName() + "(" + caseDetailflowNode.archFlownNodeInstance.getSourceObjectId() + ")";
                                processVariable.archivedDataInstance = archiveDataInstance;
                                processVariable.name = archiveDataInstance.getName();
                                processVariable.description = archiveDataInstance.getDescription();
                                processVariable.typeVariable = archiveDataInstance.getClassName();
                                processVariable.dateArchived = archiveDataInstance.getArchiveDate();
                                processVariable.isArchived = true;
                                processVariable.value = archiveDataInstance.getValue();
                            }
                        }
                    }

                }
            } catch (Exception e) {
                logger.severe("Exception " + e.getMessage());
                caseDetails.listEvents.add(new BEvent(eventLoadVariableFailed, e, "FlowNodeId[" + caseDetailflowNode.getId() + "]"));

            }
        }
    }

    /**
     * load ArchivesProcessVariable
     * The API does not work for theses variable, load them in the database
     * 
     * @param caseDetails
     * @param sourceId
     * @param processAPI
     * @return
     */
    private static List<CaseDetailVariable> loadArchivedProcessVariables(CaseDetails caseDetails, List<Long> sourceId,
            ProcessAPI processAPI) {
        // the PROCESSAPI load as archive the current value.
        // Load the archived : do that in the table
        int maxCount = 200;
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        StringBuilder sqlRequest = new StringBuilder();
        List<CaseDetailVariable> listProcessVariable = new ArrayList<>();
        try {
            // logger.info("Connect to [" + sqlDataSourceName + "]
            // loaddomainename[" + domainName + "]");
            List<Object> listParameters = new ArrayList<>();

            List<String> listColumnName = new ArrayList<>();
            listColumnName.add("INTVALUE");
            listColumnName.add("LONGVALUE");
            listColumnName.add("SHORTTEXTVALUE");
            listColumnName.add("BOOLEANVALUE");
            listColumnName.add("DOUBLEVALUE");
            listColumnName.add("FLOATVALUE");
            listColumnName.add("BLOBVALUE");
            listColumnName.add("CLOBVALUE");

            sqlRequest.append("select NAME , CLASSNAME, CONTAINERID, CONTAINERTYPE, SOURCEOBJECTID, ID, ");
            for (String columnName : listColumnName)
                sqlRequest.append(columnName + ", ");

            sqlRequest.append(" ARCHIVEDATE from ARCH_DATA_INSTANCE where CONTAINERID in (");
            // generate a ? per item
            for (int i = 0; i < sourceId.size(); i++) {
                if (i > 0)
                    sqlRequest.append(",");
                sqlRequest.append(" ? ");
                listParameters.add(sourceId.get(i));
            }
            sqlRequest.append(") ");
            if (caseDetails.tenantId != null) {
                sqlRequest.append(" and TENANTID=? ");
                listParameters.add(caseDetails.tenantId);

            }
            sqlRequest.append(" ORDER BY ARCHIVEDATE");

            con = CaseDetailsToolbox.getConnection();

            pstmt = con.prepareStatement(sqlRequest.toString());

            for (int i = 0; i < listParameters.size(); i++) {
                pstmt.setObject(i + 1, listParameters.get(i));
            }

            rs = pstmt.executeQuery();
            while (rs.next() && listProcessVariable.size() < maxCount) {
                CaseDetailVariable processVariable = caseDetails.createInstanceProcessVariableDetail();
                listProcessVariable.add(processVariable);
                processVariable.isArchived = true;
                processVariable.id = rs.getLong("ID");;

                processVariable.name = rs.getString("NAME");
                String containerType = rs.getString("CONTAINERTYPE");
                processVariable.scopeVariable = "ACTIVITY_INSTANCE".equals(containerType) ? ScopeVariable.LOCAL : ScopeVariable.PROCESS;
                processVariable.dateArchived = new Date((Long) rs.getLong("ARCHIVEDATE"));
                // save in the processInstanceId the ContainerId : in case of a process Variable, this is the process, else the activityId
                if (ScopeVariable.PROCESS.equals(processVariable.scopeVariable))
                    processVariable.processInstanceId = rs.getLong("CONTAINERID");
                else
                    processVariable.activityId = rs.getLong("CONTAINERID");
                processVariable.sourceId = rs.getLong("SOURCEOBJECTID");
                processVariable.typeVariable = rs.getString("CLASSNAME");

                Object value = null;
                String valueSt = null;
                for (String columnName : listColumnName) {
                    if (value == null) {
                        value = rs.getObject(columnName);
                        if (value != null) {
                            if ("java.util.Date".equals(processVariable.typeVariable) && value instanceof Long) {
                                processVariable.value = new Date((Long) value);
                            } else if (value instanceof Clob) {
                                long length = ((Clob) value).length();
                                processVariable.value = ((Clob) value).getSubString(1, (int) length);
                            } else if ("java.lang.String".equals(processVariable.typeVariable)) {
                                valueSt = value.toString();
                                // format is clob14:'value'
                                int pos = valueSt.indexOf(":");
                                if (pos != -1) {
                                    valueSt = valueSt.substring(pos + 3);
                                    valueSt = valueSt.substring(0, valueSt.length() - 1);
                                    valueSt = "\"" + valueSt + "\"";
                                }
                                processVariable.value = valueSt;
                            } else
                                processVariable.value = value.toString();
                        }
                    }
                }
            }

        } catch (final Exception e) {
            caseDetails.listEvents.add(new BEvent(eventLoadArchivedVariableFailed, e, "SqlRequest [" + sqlRequest + "]"));

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
        return listProcessVariable;

    }

}
