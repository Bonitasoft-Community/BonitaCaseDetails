package org.bonitasoft.casedetails;

import org.bonitasoft.casedetails.CaseDetails.CaseDetailComment;
import org.bonitasoft.casedetails.CaseDetails.ProcessInstanceDescription;
import org.bonitasoft.casedetails.CaseDetailsAPI.CaseHistoryParameter;
import org.bonitasoft.casedetails.CaseDetailsAPI.LOADCOMMENTS;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.comment.ArchivedComment;
import org.bonitasoft.engine.bpm.comment.ArchivedCommentsSearchDescriptor;
import org.bonitasoft.engine.bpm.comment.Comment;
import org.bonitasoft.engine.bpm.comment.SearchCommentsDescriptor;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

public class CaseComments {

    private final static BEvent eventLoadCommentFailed = new BEvent(CaseComments.class.getName(), 1, Level.ERROR, "Load Comment failed",
            "Loading comments failed",
            "Result will not contains comment",
            "Check exception");

    /** utility class should privatise the constructor */
    private CaseComments() {
    }

    /**
     * @param caseDetails
     * @param caseHistoryParameter
     * @param processAPI
     * @param businessDataAPI
     * @param apiSession
     */
    protected static void loadComments(CaseDetails caseDetails, LOADCOMMENTS loadComments, CaseHistoryParameter caseHistoryParameter, ProcessAPI processAPI) {
        for (ProcessInstanceDescription processInstanceDescription : caseDetails.listProcessInstances) {
            try {
                SearchOptionsBuilder sob = new SearchOptionsBuilder(0, 10000);
                sob.filter(SearchCommentsDescriptor.PROCESS_INSTANCE_ID, processInstanceDescription.processInstanceId);
                 SearchResult<Comment> searchComments = processAPI.searchComments(sob.done());

                for (Comment comment : searchComments.getResult()) {
                    if (loadComments== LOADCOMMENTS.ONLYUSERS && comment.getUserId() == null)
                        continue;
                    CaseDetailComment caseDetailComment = caseDetails.createInstanceCaseDetailComment();
                    caseDetailComment.processInstanceId = processInstanceDescription.processInstanceId;
                    caseDetailComment.comment = comment;
                }

            } catch (Exception e) {
                caseDetails.listEvents.add(new BEvent(eventLoadCommentFailed, e, "ProcessInstance[" + processInstanceDescription + "]"));
            }
            try {
                SearchOptionsBuilder sob = new SearchOptionsBuilder(0, 10000);
                sob.filter(ArchivedCommentsSearchDescriptor.PROCESS_INSTANCE_ID, processInstanceDescription.processInstanceId);
                SearchResult<ArchivedComment> searchComments = processAPI.searchArchivedComments(sob.done());

                for (ArchivedComment comment : searchComments.getResult()) {
                    CaseDetailComment caseDetailComment = caseDetails.createInstanceCaseDetailComment();
                    caseDetailComment.processInstanceId = processInstanceDescription.processInstanceId;
                    caseDetailComment.archivedComment = comment;
                }

            } catch (Exception e) {
                caseDetails.listEvents.add(new BEvent(eventLoadCommentFailed, e, "ProcessInstance[" + processInstanceDescription + "]"));
            }
        }
    }
}
