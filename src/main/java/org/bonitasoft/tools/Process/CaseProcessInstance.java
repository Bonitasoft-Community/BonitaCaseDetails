package org.bonitasoft.tools.Process;

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

import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.tools.Process.CaseDetails.ProcessInstanceDescription;
import org.bonitasoft.tools.Process.CaseDetailsAPI.CaseHistoryParameter;

/* -------------------------------------------------------------------- */
/*                                                                      */
/* Processinstance manipulation */
/*                                                                      */
/* -------------------------------------------------------------------- */

public class CaseProcessInstance {
    
    final static Logger logger = Logger.getLogger(CaseContract.class.getName());

    
    /**
     * load all processinstance declaration
     * 
     * @param rootProcessInstanceId
     * @param loadSubProcess
     * @param processAPI
     * @return
     */
    protected static void loadProcessInstances(CaseDetails caseDetails,  CaseHistoryParameter caseHistoryParameter,
            ProcessAPI processAPI) {
      
        caseDetails.listProcessInstances = getAllProcessInstance(caseDetails.rootCaseId,
                caseHistoryParameter.loadSubProcess, processAPI);
        for (ProcessInstanceDescription processInstanceDescription : caseDetails.listProcessInstances) {
            try {
                processInstanceDescription.processDefinition = processAPI.getProcessDefinition(processInstanceDescription.processDefinitionId);


                if (processInstanceDescription.callerId != null && processInstanceDescription.callerId > 0) {
                    boolean foundIt = false;
                    try {
                        ActivityInstance act = processAPI.getActivityInstance(processInstanceDescription.callerId);
                        foundIt = true;
                        processInstanceDescription.parentActivity = act;
                        processInstanceDescription.parentActivityName = act.getName();
                        processInstanceDescription.parentProcessInstanceId = act.getParentProcessInstanceId(); 
                        processInstanceDescription.parentProcessDefinitionId = act.getProcessDefinitionId(); 
                        
                        processInstanceDescription.parentProcessDefinition = processAPI.getProcessDefinition(act.getProcessDefinitionId());

                    } catch (Exception e) {
                    }
                    if (!foundIt) { // maybe archived
                        try {
                            ArchivedActivityInstance act = processAPI.getArchivedActivityInstance(processInstanceDescription.callerId);
                            foundIt = true;
                            processInstanceDescription.archiveParentActivity= act;
                            processInstanceDescription.parentActivityName = act.getName();
                            processInstanceDescription.parentProcessInstanceId = act.getProcessInstanceId(); 

                            ProcessDefinition parentProcessDefinition = processAPI.getProcessDefinition(act.getProcessDefinitionId());
                            processInstanceDescription.parentProcessDefinitionId = act.getProcessDefinitionId(); 
                            
                            processInstanceDescription.parentProcessDefinition = processAPI.getProcessDefinition(act.getProcessDefinitionId());

                        } catch (Exception e) {
                        }
                    }

                }
                

            } catch (Exception e) {
                final StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                logger.severe("During loadProcessinstance : " + e.toString() + " at " + sw.toString());

            }
        }

        if (caseHistoryParameter.loadContract)
        {
            for (ProcessInstanceDescription processInstanceDescription :caseDetails.listProcessInstances )
            {
                // processInstanceMap.put("contract", getContractValuesBySql(processInstanceDescription.processDefinitionId, processInstanceDescription.id, null, processAPI));
                processInstanceDescription.contractInstanciation = CaseContract.getContractInstanciationValues(caseDetails,caseHistoryParameter, processInstanceDescription, processAPI);
                
            }            
        }
        return ;
    }

  
    
  /**
   * 
   * @param rootProcessInstance
   * @param showSubProcess
   * @param processAPI
   * @return
   */
    private static List<ProcessInstanceDescription> getAllProcessInstance(long rootProcessInstance,
            boolean showSubProcess, ProcessAPI processAPI) {
        List<ProcessInstanceDescription> listProcessInstances = new ArrayList<ProcessInstanceDescription>();

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sqlRequest = "";
        try {
            sqlRequest = "select ID, PROCESSDEFINITIONID, CALLERID,  STARTDATE, ENDDATE  from PROCESS_INSTANCE where ROOTPROCESSINSTANCEID = ?";

            // search all process instance like with the root
            con = CaseDetailsToolbox.getConnection();
            pstmt = con.prepareStatement(sqlRequest);
            pstmt.setLong(1, rootProcessInstance);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                ProcessInstanceDescription processInstanceDescription = new ProcessInstanceDescription();
                processInstanceDescription.processInstanceId = rs.getLong(1);
                processInstanceDescription.processDefinitionId = rs.getLong(2);
                processInstanceDescription.callerId = rs.getLong(3);
                processInstanceDescription.startDate = getDate( rs.getLong( 4 ));
                processInstanceDescription.endDate = getDate( rs.getLong( 5 ));

                processInstanceDescription.isActive = true;
                if (showSubProcess || processInstanceDescription.processInstanceId == rootProcessInstance)
                    listProcessInstances.add(processInstanceDescription);
            }
            rs.close();
            rs = null;
            pstmt.close();
            pstmt = null;
            sqlRequest = "select SOURCEOBJECTID, PROCESSDEFINITIONID, CALLERID, STARTDATE, ENDDATE  from ARCH_PROCESS_INSTANCE where ROOTPROCESSINSTANCEID = ?";
            pstmt = con.prepareStatement(sqlRequest);
            pstmt.setLong(1, rootProcessInstance);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                ProcessInstanceDescription processInstanceDescription = new ProcessInstanceDescription();
                processInstanceDescription.processInstanceId = rs.getLong(1);
                processInstanceDescription.processDefinitionId = rs.getLong(2);
                processInstanceDescription.callerId = rs.getLong(3);
                
                processInstanceDescription.startDate = getDate( rs.getLong( 4 ));
                processInstanceDescription.endDate = getDate( rs.getLong( 5 ));
                
                processInstanceDescription.isActive = false;
                //maybe in double?
                boolean alreadyExist = false;
                for (ProcessInstanceDescription current : listProcessInstances) {
                    if (current.processInstanceId == processInstanceDescription.processInstanceId)
                        alreadyExist = true;
                }
                if (!alreadyExist)
                    if (showSubProcess || processInstanceDescription.processInstanceId == rootProcessInstance)
                        listProcessInstances.add(processInstanceDescription);

            }
            rs.close();
            rs = null;
            pstmt.close();
            pstmt = null;
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

        return listProcessInstances;
    }

 
    private static Date getDate( Long value ) {
        return value==null ? null: new Date( value );
    }
}
