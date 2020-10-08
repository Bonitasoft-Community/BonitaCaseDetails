package org.bonitasoft.casedetails;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.casedetails.CaseDetails.CaseDetailDocument;
import org.bonitasoft.casedetails.CaseDetails.ProcessInstanceDescription;
import org.bonitasoft.casedetails.CaseDetailsAPI.CaseHistoryParameter;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.document.Document;
import org.bonitasoft.engine.bpm.document.DocumentCriterion;
import org.bonitasoft.engine.bpm.document.DocumentException;
import org.bonitasoft.engine.bpm.process.ProcessInstanceNotFoundException;
import org.bonitasoft.engine.business.data.BusinessDataReference;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

/* -------------------------------------------------------------------- */
/*                                                                      */
/* ProcessVariable manipulation */
/*                                                                      */
/* -------------------------------------------------------------------- */

public class CaseDocuments {

    final static Logger logger = Logger.getLogger(CaseDocuments.class.getName());

    private final static BEvent eventLoadDocumentFailed = new BEvent(CaseDocuments.class.getName(), 1, Level.ERROR, "Load document failed",
            "Loading the content of a document failed",
            "Result will not contains document content",
            "Check exception");

    /** utility class should privatise the constructor */
    private CaseDocuments() {
    }

    /**
     * @param caseDetails
     * @param caseHistoryParameter
     * @param processAPI
     * @param businessDataAPI
     * @param apiSession
     */
    protected static void loadDocuments(CaseDetails caseDetails, CaseHistoryParameter caseHistoryParameter, ProcessAPI processAPI) {
        for (ProcessInstanceDescription processInstanceDescription : caseDetails.listProcessInstances) {
            List<Document> listDocuments = new ArrayList<>();
            try {
                Long sourceId = processInstanceDescription.processInstanceId;
                if (!processInstanceDescription.isActive)
                    sourceId = processInstanceDescription.archivedProcessInstanceId;
                listDocuments.addAll(processAPI.getLastVersionOfDocuments(processInstanceDescription.processInstanceId, 0, 1000, DocumentCriterion.NAME_ASC));

                try {
                    // but the archive is based on the sourceArchivedid ...
                    Map<String, Serializable> map = processAPI.getArchivedProcessInstanceExecutionContext(sourceId);
                    for (String key : map.keySet()) {
                        if (map.get(key) instanceof Document) {
                            // we got an archive Business Data Reference !
                            listDocuments.add((Document) map.get(key));
                        }
                        if (map.get(key) instanceof List) {
                            List listDoc = (List) map.get(key);
                            for (Object subRef : listDoc) {
                                if (subRef instanceof Document)
                                    // we got an archive Business Data Reference !
                                    listDocuments.add((Document) subRef);
                            }
                        }
                    }
                } catch (Exception e) {
                    caseDetails.listEvents.add(new BEvent(eventLoadDocumentFailed, e, ""));

                }

                for (Document document : listDocuments) {

                    CaseDetailDocument caseDetailDocument = caseDetails.createInstanceCaseDetailDocument();
                    caseDetailDocument.processInstanceId = processInstanceDescription.processInstanceId;
                    caseDetailDocument.document = document;
                    
                }

            } catch (ProcessInstanceNotFoundException | DocumentException e) {
                caseDetails.listEvents.add(new BEvent(eventLoadDocumentFailed, e, "ProcessInstance[" + processInstanceDescription + "]"));
            }
        }
    }
}
