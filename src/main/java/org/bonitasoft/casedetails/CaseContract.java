package org.bonitasoft.casedetails;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.casedetails.CaseDetails.ProcessInstanceDescription;
import org.bonitasoft.casedetails.CaseDetailsAPI.CaseHistoryParameter;
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

public class CaseContract {

    final static Logger logger = Logger.getLogger(CaseContract.class.getName());

    private final static String LOGGERLABEL="CaseContract";
    
    private final static BEvent eventLoadContractContentFailed = new BEvent(CaseContract.class.getName(), 1, Level.ERROR, "Contrat load failed",
            "Loading the contract failed",
            "Result will not contains the contract content",
            "Check exception");

    /**
     * this is a utility class
     * Default Constructor.
     */
    private CaseContract() {};
    /**
     * @param processInstance : the case may be archived, or not, so use this object where both information are available.
     * @param processAPI
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Serializable> getContractInstanciationValues(CaseDetails caseDetails, CaseHistoryParameter caseHistoryParameter, ProcessInstanceDescription processInstance, ProcessAPI processAPI) {
        Map<String, Serializable> contractInputTransformed = new HashMap<String, Serializable>();
        ContractDefinition processContract;
        try {
            processContract = processAPI.getProcessContract(processInstance.processDefinitionId);
            
            HashMap<String, Serializable> contractInput = new HashMap<>();

            for (InputDefinition inputDefinition : processContract.getInputs()) {
                Serializable value = processAPI.getProcessInputValueAfterInitialization(processInstance.processInstanceId, inputDefinition.getName());
                contractInput.put(inputDefinition.getName(), value );
            }
            contractInputTransformed = (Map<String, Serializable> ) translateContractValue(contractInput, caseHistoryParameter.contractInJsonFormat);
            return  contractInputTransformed;

        } catch (ProcessDefinitionNotFoundException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe("During getContractInstanciationValues : " + e.toString() + " at " + sw.toString());
            caseDetails.listEvents.add(new BEvent(eventLoadContractContentFailed, e, "Process Instance [" + processInstance.processInstanceId + "]"));
        } catch (ContractDataNotFoundException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe("During getContractInstanciationValues : " + e.toString() + " at " + sw.toString());
            caseDetails.listEvents.add(new BEvent(eventLoadContractContentFailed, e, "Process Instance [" + processInstance.processInstanceId + "]"));
        }
        return contractInputTransformed;
    }

    /**
     * @param processInstance
     * @param processAPI
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Serializable> getContractTaskValues(CaseDetails caseDetails, CaseHistoryParameter caseHistoryParameter, ArchivedHumanTaskInstance archivedTaskInstance, ProcessAPI processAPI) {
        Map<String, Serializable> contractInputTransformed = new HashMap<String, Serializable>();

        try {
            DesignProcessDefinition pdef = processAPI.getDesignProcessDefinition(archivedTaskInstance.getProcessDefinitionId());
            UserTaskDefinition task = (UserTaskDefinition) pdef.getFlowElementContainer().getActivity(archivedTaskInstance.getName());
            ContractDefinition contractDefinition = task.getContract();
            HashMap<String, Serializable> contractInput = new HashMap<String, Serializable>();

            for (InputDefinition inputDefinition : contractDefinition.getInputs()) {
                Serializable value = processAPI.getUserTaskContractVariableValue(archivedTaskInstance.getSourceObjectId(), inputDefinition.getName());
                contractInput.put(inputDefinition.getName(), value );
            }
            contractInputTransformed = (Map<String, Serializable> ) translateContractValue(contractInput, caseHistoryParameter.contractInJsonFormat);
            return  contractInputTransformed;

        } catch (ProcessDefinitionNotFoundException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe(LOGGERLABEL+"During getContractInstanciationValues : " + e.toString() + " at " + sw.toString());
            caseDetails.listEvents.add(new BEvent(eventLoadContractContentFailed, e, "ArchivedTaskId[" + archivedTaskInstance.getId() + "]"));
        } catch (ContractDataNotFoundException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe(LOGGERLABEL+"During getContractInstanciationValues : " + e.toString() + " at " + sw.toString());
            caseDetails.listEvents.add(new BEvent(eventLoadContractContentFailed, e, "ArchivedTaskId[" + archivedTaskInstance.getId() + "]"));

        }
        return contractInputTransformed;
    }

    /**
     * opposite of getContractTaskValues 
     * @param listContracts
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Serializable> recalculateContractValue( Map<String, Serializable> listContracts) {
        
        return (Map<String, Serializable>) untranslateContractValue(null, listContracts);
        
    }
    /**
     * FileInput toString is not correct, and can't be JSON. So, we have to translate it...
     * 
     * @param value
     * @saveInJsonFormat
     *                   format : "dd/MM/yyyy" ==> LocalDate
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Serializable translateContractValue(Serializable value, boolean saveInJsonFormat) {

        if (value instanceof Map) {
           // 
            // transform to:
           //  "coffeeNameInput" : {
           //      "type": "java.lang.String",
           //      "value": "macchiato" 
           //  },
           //  "datePreparation" : {
           //    "type": "java.time.LocalDate",
           //    "value": "2019-12-27"
           //  },
            HashMap<String, Map<String,Serializable>> valueTranslated = new HashMap<String, Map<String,Serializable>>();
            for (String key : ((Map<String, Serializable>) value).keySet()) {
                Map<String,Serializable> oneRecord = new HashMap<String,Serializable> ();
                Object valueRecord= ((Map<String, Serializable>) value).get( key );
                if (valueRecord!=null)
                    oneRecord.put("type", valueRecord.getClass().getName());
                oneRecord.put("value", translateContractValue((Serializable) ((Map) value).get(key), saveInJsonFormat));
                valueTranslated.put( key, oneRecord );
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
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return formatter.format((LocalDate) value);
        } else if (value instanceof LocalDateTime && saveInJsonFormat) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'kk:mm:ss");
            return formatter.format((LocalDateTime) value);
        } else if (value instanceof OffsetDateTime && saveInJsonFormat) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'kk:mm:ss'Z'");
            return formatter.format((OffsetDateTime) value);
        } else if (value instanceof Date && saveInJsonFormat) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            return sdf.format((Date) value);
        } else
            return value;
    }
    
    @SuppressWarnings({ "unchecked" })
    private static Serializable untranslateContractValue(String type, Object value) {
        try
        {
        if (value==null)
            return null;
        if (FileInputValue.class.getCanonicalName().equalsIgnoreCase(type))
        {
            // file type from Json : return null, we don't have the content of the process
            return null;
        }
        if (value instanceof Map) {
            HashMap<String, Serializable> valueTranslated = new HashMap<String, Serializable>();
            for (String key : ((Map<String, Serializable>) value).keySet()) {
                Serializable valueKey=((Map<String, Serializable>) value).get( key );
                if (valueKey instanceof Map) {
                    Map<String,Serializable> oneRecord = (Map<String, Serializable>) ((Map<String, Serializable>) value).get( key );
                    String typeRecord = (String) oneRecord.get( "type");
                    Object valueRecord = oneRecord.get("value");
                    valueTranslated.put(key,  untranslateContractValue(typeRecord, valueRecord));
                }
                else {
                    valueTranslated.put(key, valueKey);
                }
            }
            return valueTranslated;
        } else if (value instanceof List) {
            ArrayList<Serializable> valueTranslated = new ArrayList<Serializable>();
            for (Serializable valueIt : ((List<Serializable>) value)) {
                valueTranslated.add( untranslateContractValue("", valueIt));
            }
            return valueTranslated;
        } else if (value instanceof FileInputValue) {
            HashMap<String, Serializable> valueTranslated = new HashMap<String, Serializable>();
            valueTranslated.put("fileName", ((FileInputValue) value).getFileName());
            valueTranslated.put("contentType", ((FileInputValue) value).getContentType());
            // valueTranslated.put("content", ((FileInputValue) value).getContent());
            return valueTranslated;
        } else if ("java.time.LocalDate".equals(type)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return LocalDate.parse(value.toString(), formatter);
        } else if ("java.time.LocalDateTime".equals( type ) ) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'kk:mm:ss");
            return LocalDateTime.parse( value.toString(), formatter);
        } else if ("java.time.OffsetDateTime".equals(type)) {
            /* DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'kk:mm:ss'Z'").withZone(ZoneId.of("Europe/Paris"));
            return OffsetDateTime.parse(value.toString(), formatter);
            */
            
            DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'kk:mm:ss'Z'");
            LocalDateTime datetime = LocalDateTime.parse(value.toString(), formatter2);
            ZonedDateTime zoned = datetime.atZone(ZoneId.of("Europe/Paris"));
            OffsetDateTime result = zoned.toOffsetDateTime();
            return result;
        } else if ("java.lang.Boolean".equals(type)) {
            return Boolean.valueOf("true".equals(value.toString()));
        } else if ("java.util.Date".equals(type)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH24:mm:ss'Z'");
                return sdf.parse(value.toString());
        } else if ("java.lang.Integer".equals(type)) {
            try {
                return Integer.valueOf( value.toString() );
            } catch(Exception e ) {
              return null;
            }
        } else if ("java.lang.Long".equals(type)) {
            try {
                return Long.valueOf( value.toString() );
            } catch(Exception e ) {
              return null;
            }
        } else
            return (Serializable) value;
        }
        catch(Exception e ) {
            logger.severe("CaseContract.untranslateContractValue exception Value["+value+"] type["+type+"] Exception:"+e.getMessage());
            return null;
        }
    }

}
