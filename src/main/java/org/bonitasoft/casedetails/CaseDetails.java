package org.bonitasoft.casedetails;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bonitasoft.engine.bpm.data.DataInstance;
import org.bonitasoft.engine.bpm.document.Document;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.CatchMessageEventTriggerDefinition;
import org.bonitasoft.engine.bpm.flownode.CatchSignalEventTriggerDefinition;
import org.bonitasoft.engine.bpm.flownode.EventInstance;
import org.bonitasoft.engine.bpm.flownode.FlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.FlowNodeType;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.log.event.BEvent;
import org.json.simple.JSONValue;

public class CaseDetails {

    public List<BEvent> listEvents = new ArrayList<>();

    // List<Long> listProcessInstanceId  = new ArrayList<Long>();
    public long rootCaseId;

    public CaseDetails(long rootCaseId) {
        this.rootCaseId = rootCaseId;
    }

    /* ************************************************************************ */
    /*                                                                          */
    /* Process Instance */
    /*                                                                          */
    /* ************************************************************************ */

    public List<ProcessInstanceDescription> listProcessInstances = new ArrayList<ProcessInstanceDescription>();

    /**
     * Container for the processInstanceList
     */
    /*
     * public class ProcessInstanceList {
     * List<Map<String, Object>> listDetails = new ArrayList<Map<String, Object>>();;
     * List<Long> listIds = new ArrayList<Long>();;
     * }
     */
    public static class ProcessInstanceDescription {


        ProcessInstance processInstance = null;
        ArchivedProcessInstance archProcessInstance = null;

        /**
         * information are load from the database.
         */
        public long processInstanceId;
        /**
         * If the case is created by a user, this is the one
         */
        public User  userCreatedBy;
        /**
         * CallerId is the ProcessInstanceId who call this processInstance (it's a Parent, but in the Database, name is callerId)
         */
        public Long callerId;
        public Date startDate;
        public Date endDate;
        public Long processDefinitionId;
        public ProcessDefinition processDefinition;
        public boolean isActive;

        /**
         * User Name who start the case
         */
        
        public Map<String, Serializable> contractInstanciation;
        /*
         * if this processinstance is a subprocess, this is the Parent Activity who call this process
         */
        public Long parentProcessInstanceId;
        public ActivityInstance parentActivity;
        public ArchivedActivityInstance archiveParentActivity;
        public String parentActivityName;
        public Long parentProcessDefinitionId;
        public ProcessDefinition parentProcessDefinition;

    }

    /* ************************************************************************ */
    /*                                                                          */
    /* Activity Instance */
    /*                                                                          */
    /* ************************************************************************ */

    public List<CaseDetailFlowNode> listCaseDetailFlowNodes = new ArrayList<>();

    final Set<Long> listMultiInstanceActivity = new HashSet<>();

    public CaseDetailFlowNode addFlowNodeDetails() {
        CaseDetailFlowNode flowNode = new CaseDetailFlowNode();
        listCaseDetailFlowNodes.add(flowNode);
        return flowNode;
    }

    public CaseDetailFlowNode getFlowNodeById(Long flowNodeId) {
        for (CaseDetailFlowNode flowNodeDetail : listCaseDetailFlowNodes)
            if (flowNodeDetail.getId().equals(flowNodeId))
                return flowNodeDetail;
        return null;
    }

    public class CaseDetailFlowNode {

        public FlowNodeInstance activityInstance; // todo rename flowNodeInstance
        public ArchivedFlowNodeInstance archFlownNodeInstance;

        public User userExecutedBy;
        public Date dateFlowNode = null;

        public Map<String, Serializable> listContractValues;

        
       

        public String getName() {
            return activityInstance != null ? activityInstance.getName() : archFlownNodeInstance.getName();
        }

        public Long getId() {
            return activityInstance != null ? activityInstance.getId() : archFlownNodeInstance.getId();
        }

        public String getDescription() {
            return activityInstance != null ? activityInstance.getDescription() : archFlownNodeInstance.getDescription();
        }

        public String getDisplayDescription() {
            return activityInstance != null ? activityInstance.getDisplayDescription() : archFlownNodeInstance.getDisplayDescription();
        }

        public FlowNodeType getType() {
            return activityInstance != null ? activityInstance.getType() : archFlownNodeInstance.getType();
        }

        public String getState() {
            return activityInstance != null ? activityInstance.getState() : archFlownNodeInstance.getState();
        }

        public Long getFlownodeDefinitionId() {
            return activityInstance != null ? activityInstance.getFlownodeDefinitionId() : archFlownNodeInstance.getFlownodeDefinitionId();
        }

        public Long getParentContainerId() {
            return activityInstance != null ? activityInstance.getParentContainerId() : archFlownNodeInstance.getParentContainerId();
        }

        public Long getRootContainerId() {
            return activityInstance != null ? activityInstance.getRootContainerId() : archFlownNodeInstance.getRootContainerId();
        }
        
        public Date getDate() {
            if (activityInstance != null)
                return activityInstance.getLastUpdateDate();
            if (archFlownNodeInstance != null)
                return archFlownNodeInstance.getArchiveDate();
            return null;
        }
        public Map<String, Serializable> getListContractValues() {
            return listContractValues;
        }
        public ArchivedFlowNodeInstance getArchFlownNodeInstance() { 
            return archFlownNodeInstance;
    }
        public FlowNodeInstance getActivityInstance() { 
            return activityInstance;
    }
        public boolean isArchived() {
            return archFlownNodeInstance!=null;
        }
    }

    
    /* ************************************************************************ */
    /*                                                                          */
    /* Signal Instance */
    /*                                                                          */
    /* ************************************************************************ */

    public List<SignalDetail> listSignals = new ArrayList<>();

    public class SignalDetail {

        EventInstance eventInstance;
        List<CatchSignalEventTriggerDefinition> listSignalEvent;
    }

    public SignalDetail addSignal() {
        SignalDetail signalDetail = new SignalDetail();
        listSignals.add(signalDetail);
        return signalDetail;
    }

    /* ************************************************************************ */
    /*                                                                          */
    /* Message Instance */
    /*                                                                          */
    /* ************************************************************************ */

    public List<MessageDetail> listMessages = new ArrayList<>();

    public class MessageDetail {

        EventInstance eventInstance;
        CatchSignalEventTriggerDefinition signalEvent;

        List<MessageContent> listMessageContent = new ArrayList<>();

        public MessageContent addMessageContent() {
            MessageContent messageContent = new MessageContent();
            listMessageContent.add(messageContent);
            return messageContent;
        }
    }

    public MessageDetail addMessageDetail() {
        MessageDetail messageDetail = new MessageDetail();
        listMessages.add(messageDetail);
        return messageDetail;
    }

    public class MessageContent {

        CatchMessageEventTriggerDefinition messageEvent;
        Map<String, Object> mapCorrelationValues = new HashMap<String, Object>();
    }

    /* ************************************************************************ */
    /*                                                                          */
    /* Timer */
    /*                                                                          */
    /* ************************************************************************ */

    public List<TimerDetail> listTimers = new ArrayList<>();

    public class TimerDetail {

        boolean jobIsStillSchedule;
        Long triggerId;
        Long activityId;
        String timerName;
        Date triggerDate;

    }

    public TimerDetail addTimerDetail() {
        TimerDetail timerDetail = new TimerDetail();
        listTimers.add(timerDetail);
        return timerDetail;
    }

    /* ************************************************************************ */
    /*                                                                          */
    /* ProcessVariable */
    /*                                                                          */
    /* ************************************************************************ */

    public List<CaseDetailProcessVariable> listVariables = new ArrayList<>();

    public enum ScopeVariable {
        BDM, PROCESS, LOCAL
    }

    public class CaseDetailProcessVariable {

        boolean isArchived = false;

        String name;
        String description;
        Date dateArchived;
        Long containerId;
        Long sourceId;
        String typeVariable;

        String contextInfo;
        Object value;

        ScopeVariable scopeVariable;

        DataInstance dataInstance;

        public Object getValueToDisplay() {
            if (value == null)
                return null;
            if (value instanceof Long || value instanceof Double || value instanceof Float || value instanceof Integer)
                return value;
            Object valueJson = JSONValue.toJSONString(value);
            // last controle...
            if (valueJson == "")
                return value;
            return valueJson;
        }

    }

    public CaseDetailProcessVariable addProcessVariableDetail() {
        CaseDetailProcessVariable processVariable = new CaseDetailProcessVariable();
        listVariables.add(processVariable);
        return processVariable;
    }

    /* ************************************************************************ */
    /*                                                                          */
    /* Documents */
    /*                                                                          */
    /* ************************************************************************ */

    List<CaseDetailDocument> listDocuments = new ArrayList<>();

    public class CaseDetailDocument {

        public long processInstanceid;
        public Document document;
    }

    public CaseDetailDocument addCaseDetailDocument() {
        CaseDetailDocument documentVariable = new CaseDetailDocument();
        listDocuments.add(documentVariable);
        return documentVariable;
    }
}
