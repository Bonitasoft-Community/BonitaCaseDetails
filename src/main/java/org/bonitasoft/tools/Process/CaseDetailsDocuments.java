package org.bonitasoft.tools.Process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.BusinessDataAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.document.Document;
import org.bonitasoft.engine.bpm.document.DocumentCriterion;
import org.bonitasoft.engine.bpm.document.DocumentException;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessInstanceNotFoundException;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.tools.Process.CaseDetails.CaseDetailDocument;
import org.bonitasoft.tools.Process.CaseDetails.ProcessInstanceDescription;
import org.bonitasoft.tools.Process.CaseDetailsAPI.CaseHistoryParameter;

/* -------------------------------------------------------------------- */
/*                                                                      */
/* ProcessVariable manipulation */
/*                                                                      */
/* -------------------------------------------------------------------- */

public class CaseDetailsDocuments {

    final static Logger logger = Logger.getLogger(CaseProcessVariables.class.getName());

    private final static BEvent LOAD_DOCUMENT_FAILED = new BEvent(CaseDetailsDocuments.class.getName(), 1, Level.ERROR, "Load document failed",
            "Loading the content of a document failed",
            "Result will not contains document content",
            "Check exception");


    /**
     * @param caseDetails
     * @param caseHistoryParameter
     * @param processAPI
     * @param businessDataAPI
     * @param apiSession
     */
    protected static void loadDocuments(CaseDetails caseDetails, CaseHistoryParameter caseHistoryParameter, ProcessAPI processAPI) {
        for (ProcessInstanceDescription processInstanceDescription : caseDetails.listProcessInstances) {
            List<Document> listDocuments;
            try {
                listDocuments = processAPI.getLastVersionOfDocuments(processInstanceDescription.processInstanceId, 0, 1000,
                        DocumentCriterion.NAME_ASC);
           
            if (listDocuments != null) {
                for (Document document : listDocuments) {
                    
                    CaseDetailDocument caseDetailDocument = caseDetails.addCaseDetailDocument();
                    caseDetailDocument.processInstanceid = processInstanceDescription.processInstanceId;
                    caseDetailDocument.document = document;
                }
                    
            }
            } catch (ProcessInstanceNotFoundException | DocumentException e) {
                // TODO Auto-generated catch block
                caseDetails.listEvents.add( new BEvent(LOAD_DOCUMENT_FAILED,  e, "ProcessInstance["+processInstanceDescription+"]"));
            }
        }
    }
}
