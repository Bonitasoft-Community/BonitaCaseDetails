package org.bonitasoft.casedetails;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.BusinessDataAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.json.simple.JSONValue;

@SuppressWarnings("deprecation")
public class CaseDetailsAPI {

    final static Logger logger = Logger.getLogger(CaseDetailsAPI.class.getName());

    private final static String LOGGER_LABEL = "CaseDetails ##";

    private final static BEvent eventMissingCaseId = new BEvent(CaseDetailsAPI.class.getName(), 1, Level.APPLICATIONERROR, "No Case ID",
            "A case ID must be give, no one is given",
            "The result can't be calculated",
            "Give a case ID");

    private final static BEvent eventNoCaseId = new BEvent(CaseDetailsAPI.class.getName(), 2, Level.APPLICATIONERROR, "Case ID does not exist",
            "This case ID does not exists",
            "No result will be provided",
            "Give a existing (archived or active) case ID");

    private final static BEvent eventCaseDetailsFailed = new BEvent(CaseDetailsAPI.class.getName(), 3, Level.ERROR, "Error loading case",
            "An error arrived during the case Details operation",
            "No details result",
            "Give a existing (archived or active) case ID");

    /**
     * ----------------------------------------------------------------
     * getCaseDetails
     * 
     * @return
     */
    public static class CaseHistoryParameter {

        /**
         * Please provide the tenant Id to be consistent
         */
        public Long tenantId;
        public Long caseId;
        public boolean loadSubProcess = true;
        public boolean loadContract = true;
        
        public boolean loadBdmVariables = true;
        public boolean loadContentBdmVariables = true;

        /** Process and local variable */
        public boolean loadProcessVariables = true;
        public boolean loadArchivedProcessVariable = true;
        public boolean loadArchivedHistoryProcessVariable = true;
        
        public boolean loadActivities = true;
        public boolean loadArchivedActivities = true;
        
        public boolean loadEvents = true;
        public boolean loadTimers = true;
        public boolean loadDocuments = true;

        public boolean contractInJsonFormat = false;

        public String searchIndex1;
        public String searchIndex2;
        public String searchIndex3;
        public String searchIndex4;
        public String searchIndex5;

        public static CaseHistoryParameter getInstanceFromJson(String jsonSt) {
            CaseHistoryParameter caseHistoryParameter = new CaseHistoryParameter();
            if (jsonSt == null)
                return caseHistoryParameter;

            @SuppressWarnings("unchecked")
            final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);

            caseHistoryParameter.caseId = CaseDetailsToolbox.jsonToLong(jsonHash.get("caseId"), null);
            caseHistoryParameter.searchIndex1 = CaseDetailsToolbox.jsonToString(jsonHash.get("search1"), "");
            caseHistoryParameter.searchIndex2 = CaseDetailsToolbox.jsonToString(jsonHash.get("search2"), "");
            caseHistoryParameter.searchIndex3 = CaseDetailsToolbox.jsonToString(jsonHash.get("search3"), "");
            caseHistoryParameter.searchIndex4 = CaseDetailsToolbox.jsonToString(jsonHash.get("search4"), "");
            caseHistoryParameter.searchIndex5 = CaseDetailsToolbox.jsonToString(jsonHash.get("search5"), "");
            caseHistoryParameter.loadSubProcess = CaseDetailsToolbox.jsonToBoolean(jsonHash.get("showSubProcess"), false);
            caseHistoryParameter.loadBdmVariables = CaseDetailsToolbox.jsonToBoolean(jsonHash.get("loadBdmVariables"), false);
            // map the history and the archive on the same
            caseHistoryParameter.loadArchivedProcessVariable = CaseDetailsToolbox.jsonToBoolean(jsonHash.get("showArchivedData"), false);
            caseHistoryParameter.loadArchivedHistoryProcessVariable = CaseDetailsToolbox.jsonToBoolean(jsonHash.get("showArchivedData"), false);
            return caseHistoryParameter;
        }

    }

    /**
     * getCaseDetails
     * 
     * @param caseHistoryParameter
     * @param processAPI
     * @param identityAPI
     * @param businessDataAPI
     * @return
     */
    public CaseDetails getCaseDetails(CaseHistoryParameter caseHistoryParameter, ProcessAPI processAPI, IdentityAPI identityAPI, BusinessDataAPI businessDataAPI, APISession apiSession) {

        // Activities
        logger.info(LOGGER_LABEL+"############### start caseDetail v1.0 on [" + caseHistoryParameter.caseId + "] ShowSubProcess["
                + caseHistoryParameter.loadSubProcess + "]");
        
        CaseDetails caseDetails = new CaseDetails(caseHistoryParameter.tenantId, caseHistoryParameter.caseId, this);
        try {

            if (caseHistoryParameter.caseId == null) {
                caseDetails.listEvents.add(eventMissingCaseId);
                return caseDetails;
            }

            // load process now
            CaseProcessInstance.loadProcessInstances(caseDetails, caseHistoryParameter, processAPI,identityAPI);

            if (caseDetails.listProcessInstances.isEmpty()) {
                caseDetails.listEvents.add(new BEvent(eventNoCaseId, "Given CaseId[" + caseHistoryParameter.caseId+"]"));

            }
            // 
            // load activity now
            if (caseHistoryParameter.loadActivities)
                CaseActivities.loadActivities(caseDetails, caseHistoryParameter, processAPI, identityAPI);

            if (caseHistoryParameter.loadEvents)
                CaseEvents.loadEvents(caseDetails, caseHistoryParameter, processAPI, identityAPI);

            // -------------------------------------------- search the timer
            if (caseHistoryParameter.loadTimers)
                CaseTimers.loadTimers(caseDetails, caseHistoryParameter, processAPI);

            // -------------------------------------------- Variables
            if (caseHistoryParameter.loadProcessVariables)
                CaseProcessVariables.loadVariables(caseDetails, caseHistoryParameter, processAPI);
            if (caseHistoryParameter.loadBdmVariables)
                CaseDetailsBDMVariables.loadVariables(caseDetails, caseHistoryParameter, processAPI, businessDataAPI, apiSession);

            // -------------------------------------------- Documents
            if (caseHistoryParameter.loadDocuments)
                CaseDetailsDocuments.loadDocuments(caseDetails, caseHistoryParameter, processAPI);

        } catch (final Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            logger.severe(LOGGER_LABEL+"Error during get CaseHistory" + e.toString() + " at " + sw.toString());
            caseDetails.listEvents.add(new BEvent(eventCaseDetailsFailed, e, "Case Id[" + caseDetails.rootCaseId + "]"));

        }
        return caseDetails;
    }
    /* ******************************************************************************** */
    /*                                                                                  */
    /*  CaseAPI need to access multiple time to a ProcessDefinitionId                   */
    /*                                                                                  */
    /* Cache used is accessible, then caller can use it too.                            */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */
    private Map<Long,ProcessDefinition> cacheProcessDefinition = new HashMap<>(); 
    public ProcessDefinition getProcessDefinition( long processDefinitionId, ProcessAPI processAPI ) throws ProcessDefinitionNotFoundException {
        ProcessDefinition processDefinition = cacheProcessDefinition.get( processDefinitionId);
        if (processDefinition!=null)
            return processDefinition;
         processDefinition = processAPI.getProcessDefinition(processDefinitionId);
        cacheProcessDefinition.put( processDefinitionId, processDefinition);
        return processDefinition;
        
        
    }
    
    public Map<Long,ProcessDefinition> getCacheProcessDefinition() {
        return cacheProcessDefinition;
    }
    public void clearCache() {
        cacheProcessDefinition.clear();
    }
    
    
    
}
