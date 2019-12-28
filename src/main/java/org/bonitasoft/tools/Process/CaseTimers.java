package org.bonitasoft.tools.Process;

import java.util.logging.Logger;

import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.flownode.TimerEventTriggerInstance;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.tools.Process.CaseDetails.ProcessInstanceDescription;
import org.bonitasoft.tools.Process.CaseDetails.TimerDetail;
import org.bonitasoft.tools.Process.CaseDetailsAPI.CaseHistoryParameter;

/* -------------------------------------------------------------------- */
/*                                                                      */
/* Activities manipulation */
/*                                                                      */
/* -------------------------------------------------------------------- */

public class CaseTimers {

    final static Logger logger = Logger.getLogger(CaseTimers.class.getName());

    private final static BEvent SEARCH_TIMER_FAILED = new BEvent(CaseTimers.class.getName(), 1, Level.ERROR, "Search timer failed",
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
                caseDetails.listEvents.add(new BEvent(SEARCH_TIMER_FAILED, e, "ProcessInstance [" + processInstanceDescription.processInstanceId + "]"));
            }
        }
    }
}
