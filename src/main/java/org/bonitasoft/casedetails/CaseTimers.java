package org.bonitasoft.casedetails;

import java.util.logging.Logger;

import org.bonitasoft.casedetails.CaseDetails.ProcessInstanceDescription;
import org.bonitasoft.casedetails.CaseDetails.TimerDetail;
import org.bonitasoft.casedetails.CaseDetailsAPI.CaseHistoryParameter;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.flownode.TimerEventTriggerInstance;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

/* -------------------------------------------------------------------- */
/*                                                                      */
/* Activities manipulation */
/*                                                                      */
/* -------------------------------------------------------------------- */

public class CaseTimers {

    final static Logger logger = Logger.getLogger(CaseTimers.class.getName());

    private final static BEvent eventSearchTimerFailed = new BEvent(CaseTimers.class.getName(), 1, Level.ERROR, "Search timer failed",
            "Search timers of a case failed",
            "result will not contains the details of timers",
            "Check exception");

    /**
     * Load timers
     * 
     * @param caseDetails
     * @param caseHistoryParameter
     * @param processAPI
     */
    protected static void loadTimers(CaseDetails caseDetails, CaseHistoryParameter caseHistoryParameter, ProcessAPI processAPI) {
        for (ProcessInstanceDescription processInstanceDescription : caseDetails.listProcessInstances) {

            SearchResult<TimerEventTriggerInstance> searchTimer;
            try {
                searchTimer = processAPI.searchTimerEventTriggerInstances(processInstanceDescription.processInstanceId, new SearchOptionsBuilder(0, 1000).done());

                if (searchTimer.getResult() != null)
                    for (TimerEventTriggerInstance triggerInstance : searchTimer.getResult()) {
                        TimerDetail timerDetails = caseDetails.addTimerDetail();

                        timerDetails.jobIsStillSchedule = true;
                        timerDetails.triggerId = triggerInstance.getId();
                        timerDetails.activityId = triggerInstance.getEventInstanceId();
                        timerDetails.timerName = triggerInstance.getEventInstanceName();
                        timerDetails.triggerDate = triggerInstance.getExecutionDate();
                    }
            } catch (SearchException e) {
                caseDetails.listEvents.add(new BEvent(eventSearchTimerFailed, e, "ProcessInstance [" + processInstanceDescription.processInstanceId + "]"));
            }
        }
    }
}
