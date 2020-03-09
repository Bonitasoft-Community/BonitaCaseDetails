package org.bonitasoft.casedetails;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.casedetails.CaseDetails.CaseDetailFlowNode;
import org.bonitasoft.casedetails.CaseDetails.CaseDetailProcessVariable;
import org.bonitasoft.casedetails.CaseDetails.ProcessInstanceDescription;
import org.bonitasoft.casedetails.CaseDetailsAPI.CaseHistoryParameter;
import org.bonitasoft.engine.api.ProcessAPI;
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

    protected static void loadVariables(CaseDetails caseDetails, CaseHistoryParameter caseHistoryParameter, ProcessAPI processAPI) {
        List<Map<String, Object>> listDataInstanceMap = new ArrayList<>();

        for (ProcessInstanceDescription processInstanceDescription : caseDetails.listProcessInstances) {
            // maybe an archived ID
            try {
                ProcessDefinition processDefinition = processAPI.getProcessDefinition(processInstanceDescription.processDefinitionId);
                List<DataInstance> listDataInstances = null;
                List<Map<String, Object>> listArchivedDataInstances = null;
                List<Long> listSourceId = new ArrayList<Long>();
                listSourceId.add(processInstanceDescription.processInstanceId);
                // collect each list
                if (processInstanceDescription.isActive) {
                    listDataInstances = processAPI.getProcessDataInstances(processInstanceDescription.processInstanceId, 0, 1000);

                    //listArchivedDataInstances = processAPI.getArchivedProcessDataInstances(processInstanceDescription.id, 0, 1000);
                    listArchivedDataInstances = loadArchivedProcessVariables(caseDetails, listSourceId, processAPI);

                } else {
                    listDataInstances = new ArrayList<DataInstance>();
                    // listArchivedDataInstances = processAPI.getArchivedProcessDataInstances(processInstanceDescription.id, 0, 1000);
                    listArchivedDataInstances = loadArchivedProcessVariables(caseDetails, listSourceId, processAPI);
                }

               // completeListDataInstanceMap(listDataInstanceMap, ScopeVariable.PROCESS, null, listDataInstances, processDefinition, processInstanceDescription.id, "");
               // completeListDataInstanceMap(listDataInstanceMap, ScopeVariable.PROCESS, StatusVariable.ARCHIVED, listArchivedDataInstances, processDefinition, processInstanceDescription.id, "");

            } catch (Exception e) {

            }

        }

        // collect local variable in activity - attention, the same sourceinstanceid can come up multiple time

        for (CaseDetailFlowNode caseDetailflowNode : caseDetails.listCaseDetailFlowNodes) {
            try {
                if (caseDetailflowNode.activityInstance != null) {

                    List<DataInstance> listDataInstances = processAPI.getActivityDataInstances(caseDetailflowNode.activityInstance.getId(), 0, 1000);
                    // the getActivityDataInstances return PROCESS_INSTANCE variable !!!
                    for (DataInstance dataInstance : listDataInstances) {
                        if (!dataInstance.getContainerType().equals("PROCESS_INSTANCE")) {
                            CaseDetailProcessVariable processVariable = caseDetails.addProcessVariableDetail();
                            processVariable.scopeVariable = CaseDetails.ScopeVariable.LOCAL;
                            processVariable.containerId = caseDetailflowNode.activityInstance.getParentContainerId();
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
                if (caseDetailflowNode.archFlownNodeInstance != null) {
                    List<Long> listSourceId = new ArrayList<Long>();
                    listSourceId.add(caseDetailflowNode.archFlownNodeInstance.getSourceObjectId());

                    loadArchivedProcessVariables(caseDetails, listSourceId, processAPI);
                    // List<ArchivedDataInstance> listArchivedDataInstances =  processAPI.getArchivedActivityDataInstances( activitySourceInstanceId, 0, 1000);

                }
            } catch (Exception e) {
                logger.severe("Exception " + e.getMessage());
                caseDetails.listEvents.add(new BEvent(eventLoadVariableFailed, e, "FlowNodeId[" + caseDetailflowNode.getId() + "]"));

            }
        }

        return;
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
    private static List<Map<String, Object>> loadArchivedProcessVariables(CaseDetails caseDetails, List<Long> sourceId,
            ProcessAPI processAPI) {
        // the PROCESSAPI load as archive the current value.
        // Load the archived : do that in the table
        int maxCount = 200;
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sqlRequest = "";
        List<Map<String, Object>> listArchivedDataInstanceMap = new ArrayList<Map<String, Object>>();
        try {
            // logger.info("Connect to [" + sqlDataSourceName + "]
            // loaddomainename[" + domainName + "]");

            List<String> listColumnName = new ArrayList<String>();
            listColumnName.add("INTVALUE");
            listColumnName.add("LONGVALUE");
            listColumnName.add("SHORTTEXTVALUE");
            listColumnName.add("BOOLEANVALUE");
            listColumnName.add("DOUBLEVALUE");
            listColumnName.add("FLOATVALUE");
            listColumnName.add("BLOBVALUE");
            listColumnName.add("CLOBVALUE");

            sqlRequest = "select NAME , CLASSNAME, CONTAINERID, SOURCEOBJECTID, ID,";
            for (String columnName : listColumnName)
                sqlRequest += columnName + ", ";

            sqlRequest += " ARCHIVEDATE from ARCH_DATA_INSTANCE where CONTAINERID in (";
            // generate a ? per item
            for (int i = 0; i < sourceId.size(); i++) {
                if (i > 0)
                    sqlRequest += ",";
                sqlRequest += " ? ";
            }
            sqlRequest += ") ORDER BY ARCHIVEDATE";

            con = CaseDetailsToolbox.getConnection();

            pstmt = con.prepareStatement(sqlRequest);

            for (int i = 0; i < sourceId.size(); i++) {
                pstmt.setObject(i + 1, sourceId.get(i));
            }

            rs = pstmt.executeQuery();
            while (rs.next() && listArchivedDataInstanceMap.size() < maxCount) {
                CaseDetailProcessVariable processVariable = caseDetails.addProcessVariableDetail();
                processVariable.isArchived = true;

                processVariable.name = rs.getString("NAME");
                processVariable.dateArchived = new Date((Long) rs.getLong("ARCHIVEDATE"));
                processVariable.containerId = rs.getLong("CONTAINERID");
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
        return listArchivedDataInstanceMap;

    }
    /*
     * private static void completeListDataInstanceMap(List<Map<String, Object>> listDataInstanceMap, ScopeVariable scopeVariable, StatusVariable
     * statusVariable, List<?> listDataInstances, ProcessDefinition processDefinition, Long processId, String contextInfo) {
     * for (Object dataInstance : listDataInstances) {
     * Map<String, Object> mapDataInstance = new HashMap<String, Object>();
     * mapDataInstance.put("processname", processDefinition.getName());
     * mapDataInstance.put("processversion", processDefinition.getVersion());
     * mapDataInstance.put("processinstance", processId);
     * mapDataInstance.put("scope", scopeVariable.toString());
     * mapDataInstance.put("contextinfo", contextInfo);
     * listDataInstanceMap.add(mapDataInstance);
     * if (dataInstance instanceof DataInstance) {
     * mapDataInstance.put("name", ((DataInstance) dataInstance).getName());
     * mapDataInstance.put("description", ((DataInstance) dataInstance).getDescription());
     * mapDataInstance.put("type", ((DataInstance) dataInstance).getClassName());
     * mapDataInstance.put("datearchived", null);
     * mapDataInstance.put("status", StatusVariable.ACTIF.toString());
     * mapDataInstance.put("scope", scopeVariable.toString());
     * mapDataInstance.put("value", getValueToDisplay(((DataInstance) dataInstance).getValue()));
     * }
     * if (dataInstance instanceof ArchivedDataInstance) {
     * mapDataInstance.put("name", ((ArchivedDataInstance) dataInstance).getName());
     * mapDataInstance.put("description", ((ArchivedDataInstance) dataInstance).getDescription());
     * mapDataInstance.put("type", ((ArchivedDataInstance) dataInstance).getClassName());
     * mapDataInstance.put("datearchived", null);
     * mapDataInstance.put("status", StatusVariable.ARCHIVED.toString());
     * mapDataInstance.put("scope", scopeVariable.toString());
     * mapDataInstance.put("value", getValueToDisplay(((DataInstance) dataInstance).getValue()));
     * }
     * if (dataInstance instanceof Map) {
     * mapDataInstance.putAll((Map<String, Object>) dataInstance);
     * mapDataInstance.put("processinstance", processId);
     * mapDataInstance.put("processname", processDefinition == null ? "" : processDefinition.getName());
     * mapDataInstance.put("processversion", processDefinition == null ? "" : processDefinition.getVersion());
     * //mapDataInstance.put("name", variable.get("name") );
     * //mapDataInstance.put("description", variable.get("description"));
     * //mapDataInstance.put("type", variable.get("type"));
     * //mapDataInstance.put("datearchived", variable.get("datearchived"));
     * // mapDataInstance.put("value", variable.get("value"));
     * mapDataInstance.put("status", statusVariable.toString());
     * mapDataInstance.put("scope", scopeVariable.toString());
     * // String jsonSt = new JsonBuilder(variable.getValue()).toPrettyString();
     * }
     * /*
     * Object dataValueJson = (jsonSt==null || jsonSt.length()==0) ?
     * null : new JsonSlurper().parseText(jsonSt);
     * mapDataInstance.put("value", dataValueJson);
     * }
     * }
     */
}
