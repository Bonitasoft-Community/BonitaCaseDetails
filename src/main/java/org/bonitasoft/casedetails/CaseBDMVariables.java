package org.bonitasoft.casedetails;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.casedetails.CaseDetails.CaseDetailVariable;
import org.bonitasoft.casedetails.CaseDetails.ProcessInstanceDescription;
import org.bonitasoft.casedetails.CaseDetails.ScopeVariable;
import org.bonitasoft.casedetails.CaseDetailsAPI.CaseHistoryParameter;
import org.bonitasoft.engine.api.BusinessDataAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bdm.BusinessObjectDAOFactory;
import org.bonitasoft.engine.bdm.Entity;
import org.bonitasoft.engine.bdm.dao.BusinessObjectDAO;
import org.bonitasoft.engine.business.data.BusinessDataReference;
import org.bonitasoft.engine.business.data.MultipleBusinessDataReference;
import org.bonitasoft.engine.business.data.SimpleBusinessDataReference;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.json.simple.JSONValue;

/* -------------------------------------------------------------------- */
/*                                                                      */
/* ProcessVariable manipulation */
/*                                                                      */
/* -------------------------------------------------------------------- */

@SuppressWarnings("deprecation")
public class CaseBDMVariables {

    final static Logger logger = Logger.getLogger(CaseBDMVariables.class.getName());

    private final static BEvent eventLoadBdmContentFailed = new BEvent(CaseBDMVariables.class.getName(), 1, Level.ERROR, "Load BDM Content failed",
            "Loading the content of a BDM failed",
            "Result will not contains the BDM content",
            "Check exception");

    private final static BEvent eventLoadBdmFailed = new BEvent(CaseBDMVariables.class.getName(), 2, Level.ERROR, "Load BDM variable failed",
            "Error when BDM is loaded",
            "Result will not contains the BDM",
            "Check exception");

    /**
     * utility class should privatise the constructors
     * Default Constructor.
     */
    private CaseBDMVariables() {
    }

    /**
     * @param caseDetails
     * @param caseHistoryParameter
     * @param processAPI
     * @param businessDataAPI
     * @param apiSession
     */
    @SuppressWarnings({})
    protected static void loadVariables(CaseDetails caseDetails, CaseHistoryParameter caseHistoryParameter, ProcessAPI processAPI, BusinessDataAPI businessDataAPI, APISession apiSession) {

        if (businessDataAPI == null)
            return;
        for (ProcessInstanceDescription processInstanceDescription : caseDetails.listProcessInstances) {
            // BDM 3009

            Long sourceId = processInstanceDescription.processInstanceId;
            if (!processInstanceDescription.isActive)
                sourceId = processInstanceDescription.archivedProcessInstanceId;

            List<BusinessDataReference> listBdmReference = new ArrayList<>();

            listBdmReference.addAll(businessDataAPI.getProcessBusinessDataReferences(sourceId, 0, 1000));
            try {
                Map<String, Serializable> map = processAPI.getArchivedProcessInstanceExecutionContext(sourceId);
                for (String key : map.keySet()) {
                    if (map.get(key) instanceof BusinessDataReference) {
                        // we got an archive Business Data Reference !
                        // logger.info(">>> Detect["+key+"] businessVariable");
                        BusinessDataReference bde = (BusinessDataReference) map.get(key);
                        listBdmReference.add(bde);
                    }
                }
            } catch (Exception e) {
                caseDetails.listEvents.add(new BEvent(eventLoadBdmFailed, e, ""));

            }

            for (BusinessDataReference businessDataReference : listBdmReference) {
                boolean isMultiple=false;
                List<Long> listStorageIds = new ArrayList<>();

                if (businessDataReference instanceof SimpleBusinessDataReference) {
                    // if null, add it even to have a result (bdm name + null)
                    listStorageIds.add(((SimpleBusinessDataReference) businessDataReference).getStorageId());
                    isMultiple=false;
                } else if (businessDataReference instanceof MultipleBusinessDataReference) {
                    // this is a multiple data
                    isMultiple=true;
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
                    // if we don't have a session, it's not possible to load the BDM Object.... so, if loadContentBdmVariable is false, this is not an issue
                    BusinessObjectDAO dao = null;
                    for (Long storageId : listStorageIds) {
                        if (storageId == null) {
                            continue;
                        }
                        CaseDetailVariable bdmVariable = caseDetails.getProcessVariable( businessDataReference.getName());
                        if (bdmVariable==null)
                            bdmVariable= caseDetails.createInstanceProcessVariableDetail();
                        bdmVariable.name = businessDataReference.getName();
                        bdmVariable.processInstanceId = processInstanceDescription.processInstanceId;
                        bdmVariable.scopeVariable = ScopeVariable.BDM;
                        bdmVariable.bdmName = businessDataReference.getType();
                        bdmVariable.listPersistenceId.add( storageId );
                        bdmVariable.bdmIsMultiple=isMultiple;
                        Entity dataBdmEntity = null;
                        if (caseHistoryParameter.loadContentBdmVariables) {
                            if (dao==null)
                                dao = daoFactory.createDAO(apiSession, classDao);

                            // method findByPersistenceId exist, but is not declare in the interface
                            try {
                                Method m = dao.getClass().getDeclaredMethod("findByPersistenceId", Long.class);
                                dataBdmEntity = (Entity) m.invoke(dao, storageId);
                                bdmVariable.value = JSONValue.toJSONString(dataBdmEntity);
                            } catch (Exception e) {
                                caseDetails.listEvents.add(new BEvent(eventLoadBdmContentFailed, e, "BDMName[" + businessDataReference.getName() + "] StorageId[" + storageId + "]"));

                            }
                        }

                    }
                } catch (Exception e) {
                    caseDetails.listEvents.add(new BEvent(eventLoadBdmFailed, e, "BDMName[" + businessDataReference == null ? null : businessDataReference.getName() + "]"));

                }
            } // end loop on all BDM
        } // end loop all processinstance
    } // end collect BDM

}
