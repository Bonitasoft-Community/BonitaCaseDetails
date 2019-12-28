package org.bonitasoft.tools.Process;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.contract.ContractDefinition;
import org.bonitasoft.engine.bpm.contract.FileInputValue;
import org.bonitasoft.engine.bpm.contract.InputDefinition;
import org.bonitasoft.engine.bpm.flownode.ArchivedHumanTaskInstance;
import org.bonitasoft.engine.bpm.flownode.UserTaskDefinition;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.exception.ContractDataNotFoundException;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.tools.Process.CaseDetails.ProcessInstanceDescription;
/* -------------------------------------------------------------------- */
/*                                                                      */
/* Contract manipulation */
/*                                                                      */
/* -------------------------------------------------------------------- */
import org.bonitasoft.tools.Process.CaseDetailsAPI.CaseHistoryParameter;

public class CaseContract {

    final static Logger logger = Logger.getLogger(CaseContract.class.getName());

    private final static BEvent LOAD_CONTRACT_CONTENT_FAILED = new BEvent(CaseContract.class.getName(), 1, Level.ERROR, "Contrat load failed",
            "Loading the contract failed",
            "Result will not contains the contract content",
            "Check exception");

    /**
     * @param processInstance : the case may be archived, or not, so use this object where both information are available.
     * @param processAPI
     * @return
     */
    public static List<Map<String, Serializable>> getContractInstanciationValues(CaseDetails caseDetails, CaseHistoryParameter caseHistoryParameter, ProcessInstanceDescription processInstance, ProcessAPI processAPI) {
        List<Map<String, Serializable>> listValues = new ArrayList<Map<String, Serializable>>();
        ContractDefinition processContract;
        try {
            processContract = processAPI.getProcessContract(processInstance.processDefinitionId);

            for (InputDefinition inputDefinition : processContract.getInputs()) {
                Map<String, Serializable> contractInput = new HashMap<String, Serializable>();
                Serializable value = processAPI.getProcessInputValueAfterInitialization(processInstance.processInstanceId, inputDefinition.getName());
                contractInput.put("name", inputDefinition.getName());
                contractInput.put("value", translateContractValue(value, caseHistoryParameter.contractInJsonFormat));
                contractInput.put("type", value == null ? null : value.getClass().getName());

                listValues.add(contractInput);
            }
        } catch (ProcessDefinitionNotFoundException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe("During getContractInstanciationValues : " + e.toString() + " at " + sw.toString());
            caseDetails.listEvents.add(new BEvent(LOAD_CONTRACT_CONTENT_FAILED, e, "Process Instance [" + processInstance.processInstanceId + "]"));
        } catch (ContractDataNotFoundException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe("During getContractInstanciationValues : " + e.toString() + " at " + sw.toString());
            caseDetails.listEvents.add(new BEvent(LOAD_CONTRACT_CONTENT_FAILED, e, "Process Instance [" + processInstance.processInstanceId + "]"));
        }
        return listValues;
    }

    /**
     * @param processInstance
     * @param processAPI
     * @return
     */
    public static List<Map<String, Serializable>> getContractTaskValues(CaseDetails caseDetails, CaseHistoryParameter caseHistoryParameter, ArchivedHumanTaskInstance archivedTaskInstance, ProcessAPI processAPI) {
        List<Map<String, Serializable>> listValues = new ArrayList<Map<String, Serializable>>();

        try {
            DesignProcessDefinition pdef = processAPI.getDesignProcessDefinition(archivedTaskInstance.getProcessDefinitionId());
            UserTaskDefinition task = (UserTaskDefinition) pdef.getFlowElementContainer().getActivity(archivedTaskInstance.getName());
            ContractDefinition contractDefinition = task.getContract();

            for (InputDefinition inputDefinition : contractDefinition.getInputs()) {
                Map<String, Serializable> contractInput = new HashMap<String, Serializable>();
                Serializable value = processAPI.getUserTaskContractVariableValue(archivedTaskInstance.getSourceObjectId(), inputDefinition.getName());
                contractInput.put("name", inputDefinition.getName());
                contractInput.put("value", translateContractValue(value, caseHistoryParameter.contractInJsonFormat));
                listValues.add(contractInput);
            }

        } catch (ProcessDefinitionNotFoundException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe("During getContractInstanciationValues : " + e.toString() + " at " + sw.toString());
            caseDetails.listEvents.add(new BEvent(LOAD_CONTRACT_CONTENT_FAILED, e, "ArchivedTaskId[" + archivedTaskInstance.getId() + "]"));
        } catch (ContractDataNotFoundException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe("During getContractInstanciationValues : " + e.toString() + " at " + sw.toString());
            caseDetails.listEvents.add(new BEvent(LOAD_CONTRACT_CONTENT_FAILED, e, "ArchivedTaskId[" + archivedTaskInstance.getId() + "]"));

        }
        return listValues;
    }

    /**
     * FileInput toString is not correct, and can't be JSON. So, we have to translate it...
     * 
     * @param value
     * @saveInJsonFormat
     *                   format : "dd/MM/yyyy" ==> LocalDate
     * @return
     */
    public static Serializable translateContractValue(Serializable value, boolean saveInJsonFormat) {

        if (value instanceof Map) {
            HashMap<String, Serializable> valueTranslated = new HashMap<String, Serializable>();
            for (String key : ((Map<String, Serializable>) value).keySet()) {
                valueTranslated.put(key, translateContractValue((Serializable) ((Map) value).get(key), saveInJsonFormat));
            }
            return valueTranslated;
        } else if (value instanceof List) {
            ArrayList<Serializable> valueTranslated = new ArrayList<Serializable>();
            for (Serializable valueIt : ((List<Serializable>) value)) {
                valueTranslated.add(translateContractValue(valueIt, saveInJsonFormat));
            }
            return valueTranslated;
        } else if (value instanceof FileInputValue) {
            HashMap<String, Serializable> valueTranslated = new HashMap<String, Serializable>();
            valueTranslated.put("fileName", ((FileInputValue) value).getFileName());
            valueTranslated.put("contentType", ((FileInputValue) value).getContentType());
            // valueTranslated.put("content", ((FileInputValue) value).getContent());
            return valueTranslated;
        } else if (value instanceof LocalDate && saveInJsonFormat) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd");
            return formatter.format((LocalDate) value);
        } else if (value instanceof LocalDateTime && saveInJsonFormat) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd'T'HH24:mm:ss");
            return formatter.format((LocalDateTime) value);
        } else if (value instanceof OffsetDateTime && saveInJsonFormat) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd'T'HH24:mm:ss'Z'");
            return formatter.format((OffsetDateTime) value);
        } else if (value instanceof Date && saveInJsonFormat) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd'T'HH24:mm:ss'Z'");
            return sdf.format((Date) value);
        } else
            return value;
    }
}
