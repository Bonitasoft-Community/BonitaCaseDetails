package org.bonitasoft.casedetails;

import java.util.List;
import java.util.logging.Logger;

import org.bonitasoft.casedetails.CaseDetails.CaseDetailDocument;
import org.bonitasoft.casedetails.CaseDetails.ProcessInstanceDescription;
import org.bonitasoft.casedetails.CaseDetailsAPI.CaseHistoryParameter;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.document.Document;
import org.bonitasoft.engine.bpm.document.DocumentCriterion;
import org.bonitasoft.engine.bpm.document.DocumentException;
import org.bonitasoft.engine.bpm.process.ProcessInstanceNotFoundException;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

/* -------------------------------------------------------------------- */
/*                                                                      */
/* ProcessVariable manipulation */
/*                                                                      */
/* -------------------------------------------------------------------- */

public class CaseDetailsDocuments {

    final static Logger logger = Logger.getLogger(CaseDetailsDocuments.class.getName());

    private final static BEvent eventLoadDocumentFailed = new BEvent(CaseDetailsDocuments.class.getName(), 1, Level.ERROR, "Load document failed",
            "Loading the content of a document failed",
            "Result will not contains document content",
            "Check exception");


    /** utility class should privatise the constructor */
    private CaseDetailsDocuments() {}
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
                caseDetails.listEvents.add( new BEvent(eventLoadDocumentFailed,  e, "ProcessInstance["+processInstanceDescription+"]"));
            }
        }
    }
}
