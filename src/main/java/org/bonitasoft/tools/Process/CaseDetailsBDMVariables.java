package org.bonitasoft.tools.Process;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.BusinessDataAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bdm.BusinessObjectDAOFactory;
import org.bonitasoft.engine.bdm.Entity;
import org.bonitasoft.engine.bdm.dao.BusinessObjectDAO;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.business.data.BusinessDataReference;
import org.bonitasoft.engine.business.data.MultipleBusinessDataReference;
import org.bonitasoft.engine.business.data.SimpleBusinessDataReference;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.tools.Process.CaseDetails.CaseDetailProcessVariable;
import org.bonitasoft.tools.Process.CaseDetails.ProcessInstanceDescription;
import org.bonitasoft.tools.Process.CaseDetails.ScopeVariable;
import org.bonitasoft.tools.Process.CaseDetailsAPI.CaseHistoryParameter;
import org.json.simple.JSONValue;

/* -------------------------------------------------------------------- */
/*                                                                      */
/* ProcessVariable manipulation */
/*                                                                      */
/* -------------------------------------------------------------------- */

public class CaseDetailsBDMVariables {

    final static Logger logger = Logger.getLogger(CaseProcessVariables.class.getName());

    private final static BEvent LOAD_BDM_CONTENT_FAILED = new BEvent(CaseDetailsBDMVariables.class.getName(), 1, Level.ERROR, "Load BDM Content failed",
            "Loading the content of a BDM failed",
            "Result will not contains the BDM content",
            "Check exception");

    private final static BEvent LOAD_BDM_FAILED = new BEvent(CaseDetailsBDMVariables.class.getName(), 2, Level.ERROR, "Load BDM variable failed",
            "Error when BDM is loaded",
            "Result will not contains the BDM",
            "Check exception");

    /**
     * @param caseDetails
     * @param caseHistoryParameter
     * @param processAPI
     * @param businessDataAPI
     * @param apiSession
     */
    protected static void loadVariables(CaseDetails caseDetails, CaseHistoryParameter caseHistoryParameter, ProcessAPI processAPI, BusinessDataAPI businessDataAPI, APISession apiSession) {

        if (businessDataAPI==null)
            return;
        for (ProcessInstanceDescription processInstanceDescription : caseDetails.listProcessInstances) {
            // BDM
            List<BusinessDataReference> listBdmReference = businessDataAPI.getProcessBusinessDataReferences(processInstanceDescription.processInstanceId, 0, 1000);
            for (BusinessDataReference businessDataReference : listBdmReference) {

                List<Long> listStorageIds = new ArrayList<Long>();
                Object collectListBdm = null;
                if (businessDataReference instanceof SimpleBusinessDataReference) {
                    collectListBdm = null;
                    // if null, add it even to have a result (bdm name + null)
                    listStorageIds.add(((SimpleBusinessDataReference) businessDataReference).getStorageId());
                } else if (businessDataReference instanceof MultipleBusinessDataReference) {
                    // this is a multiple data
                    collectListBdm = new ArrayList<Object>();
                    if (((MultipleBusinessDataReference) businessDataReference).getStorageIds() == null)
                        listStorageIds.add(null); // add a null value to have a
                                                  // result (bdm name + null) and
                                                  // geet the resultBdm as null
                    else {
                        listStorageIds.addAll(((MultipleBusinessDataReference) businessDataReference).getStorageIds());
                    }
                }

                // now we get a listStorageIds
                try {
                    String classDAOName = businessDataReference.getType() + "DAO";
                    @SuppressWarnings("rawtypes")
                    Class classDao = Class.forName(classDAOName);
                    if (classDao == null) {
                        // a problem here...
                        continue;
                    }

                    BusinessObjectDAOFactory daoFactory = new BusinessObjectDAOFactory();

                    @SuppressWarnings("unchecked")
                    BusinessObjectDAO dao = daoFactory.createDAO(apiSession, classDao);
                    for (Long storageId : listStorageIds) {
                        if (storageId == null) {
                            continue;
                        }
                        CaseDetailProcessVariable bdmVariable = caseDetails.addProcessVariableDetail();
                        bdmVariable.name = businessDataReference.getName();
                        bdmVariable.containerId = processInstanceDescription.processDefinitionId;
                        bdmVariable.scopeVariable = ScopeVariable.BDM;
                        Entity dataBdmEntity = null;
                        if (caseHistoryParameter.loadContentBdmVariables) {
                            // method findByPersistenceId exist, but is not declare in the interface
                            try {
                                Method m = dao.getClass().getDeclaredMethod("findByPersistenceId", Long.class);
                                dataBdmEntity = (Entity) m.invoke(dao, storageId);
                                bdmVariable.value = JSONValue.toJSONString(dataBdmEntity);
                            } catch (Exception e) {
                                caseDetails.listEvents.add(new BEvent(LOAD_BDM_CONTENT_FAILED, e, "BDMName[" + businessDataReference.getName() + "] StorageId[" + storageId + "]"));

                            }
                        }

                    }
                } catch (Exception e) {
                    caseDetails.listEvents.add(new BEvent(LOAD_BDM_FAILED, e, "BDMName[" + businessDataReference == null ? null : businessDataReference.getName() + "]"));

                }
            } // end loop on all BDM
        } // end loop all processinstance
        return;
    } // end collect BDM

}
