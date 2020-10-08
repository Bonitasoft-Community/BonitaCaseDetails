package org.bonitasoft.casedetails;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.bonitasoft.engine.bpm.actor.ActorInstance;
import org.bonitasoft.engine.bpm.comment.ArchivedComment;
import org.bonitasoft.engine.bpm.comment.Comment;
import org.bonitasoft.engine.bpm.connector.ConnectorInstance;
import org.bonitasoft.engine.bpm.data.ArchivedDataInstance;
import org.bonitasoft.engine.bpm.data.DataInstance;
import org.bonitasoft.engine.bpm.document.Document;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedLoopActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedMultiInstanceActivityInstance;
import org.bonitasoft.engine.bpm.flownode.CatchMessageEventTriggerDefinition;
import org.bonitasoft.engine.bpm.flownode.CatchSignalEventTriggerDefinition;
import org.bonitasoft.engine.bpm.flownode.EventInstance;
import org.bonitasoft.engine.bpm.flownode.FlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.FlowNodeType;
import org.bonitasoft.engine.bpm.flownode.GatewayInstance;
import org.bonitasoft.engine.bpm.flownode.LoopActivityInstance;
import org.bonitasoft.engine.bpm.flownode.MultiInstanceActivityInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.log.event.BEvent;
import org.json.simple.JSONValue;

public class CaseDetails {

    public List<BEvent> listEvents = new ArrayList<>();

    public Long tenantId;
    // List<Long> listProcessInstanceId  = new ArrayList<Long>();
    public Long rootCaseId;

    private CaseDetailsAPI caseDetailAPI;

    protected CaseDetails(Long tenantId, Long rootCaseId, CaseDetailsAPI caseDetailAPI) {
        this.tenantId = tenantId;
        this.rootCaseId = rootCaseId;
        this.caseDetailAPI = caseDetailAPI;
    }

    public CaseDetailsAPI getCaseDetailsAPI() {
        return caseDetailAPI;
    }

    /* ************************************************************************ */
    /*                                                                          */
    /* Process Instance */
    /*                                                                          */
    /* ************************************************************************ */

    public List<ProcessInstanceDescription> listProcessInstances = new ArrayList<>();

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

        public ProcessInstance processInstance = null;
        public ArchivedProcessInstance archProcessInstance = null;

        public Long tenantId;
        /**
         * information are load from the database.
         */
        public long processInstanceId;

        public Long rootProcessInstanceId;

        /**
         * in case of a ArchiveProcessInstance, the Internal ID. This information is mandatory to call the getArchivedProcessInstance method
         */
        public Long archivedProcessInstanceId = null;
        /**
         * If the case is created by a user, this is the one
         */
        public User userCreatedBy;
        /**
         * CallerId is the ProcessInstanceId who call this processInstance (it's a Parent, but in the Database, name is callerId)
         */
        public Long callerId;
        public Date startDate = null;
        public Date endDate = null;
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

    /**
     * Create an register the instance
     * @return
     */
    public CaseDetailFlowNode createInstanceFlowNodeDetails() {
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

        public FlowNodeInstance activityInstance;
        public ArchivedFlowNodeInstance archFlownNodeInstance;

        public User userExecutedBy;
        public Date dateFlowNode = null;

        public Map<String, Serializable> listContractValues;

        /**
         * In case of a Human Tasks
         */
        public ActorInstance actor;
        public long nbCandidates;
        public long assigneeId;
        public User assigneeUser;

        /**
         * when the case is failed, then the list of connector can be loaded
         */
        public List<ConnectorInstance> connectors;

        public String getName() {
            return activityInstance != null ? activityInstance.getName() : archFlownNodeInstance.getName();
        }

        public String getDisplayName() {
            return activityInstance != null ? activityInstance.getDisplayName() : archFlownNodeInstance.getDisplayName();
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

     
        /**
         * This information is use to link all task together, archive and current. They share the same ID.
         * @return
         */
        public Long getSourceObjectId() {
            return activityInstance != null ? activityInstance.getId() : archFlownNodeInstance.getSourceObjectId();

        }
        
        /**
         * To get the processInstanceId, it's mandatory to parse all flownode.
         * When the flownode is part of a ITERATION, then the parentContainterId is the TaskId, not the processInstanceId
         * @return
         */
        public Long getProcessInstanceId( ) {
            return activityInstance != null ? activityInstance.getParentProcessInstanceId() : archFlownNodeInstance.getProcessInstanceId();
        }
        public Long getParentContainerId() {
            return activityInstance != null ? activityInstance.getParentContainerId() : archFlownNodeInstance.getParentContainerId();
        }

        public Long getRootContainerId() {
            return activityInstance != null ? activityInstance.getRootContainerId() : archFlownNodeInstance.getRootContainerId();
        }

        public Date getReachedStateDate() {
            return activityInstance != null ? activityInstance.getReachedStateDate() : archFlownNodeInstance.getReachedStateDate();
        }

        public FlowNodeType getFlowNodeType() {
            return activityInstance != null ? activityInstance.getType() : archFlownNodeInstance.getType();
        }

        public Long getExecutedBy() {
            return activityInstance != null ? activityInstance.getExecutedBy() : archFlownNodeInstance.getExecutedBy();
        }

        public Long getExecutedBySubstitute() {
            return activityInstance != null ? activityInstance.getExecutedBySubstitute() : archFlownNodeInstance.getExecutedBySubstitute();
        }

        public Integer getLoopCounter() {
            if (activityInstance instanceof LoopActivityInstance)
                return ((LoopActivityInstance) activityInstance).getLoopCounter();

            if (archFlownNodeInstance instanceof ArchivedLoopActivityInstance) {
                return ((ArchivedLoopActivityInstance) archFlownNodeInstance).getLoopCounter();
            }
            return null;
        }
        public Integer getNumberOfInstances() {
            if (activityInstance instanceof MultiInstanceActivityInstance) {
                return ((MultiInstanceActivityInstance) archFlownNodeInstance).getNumberOfInstances();
            }
            if (archFlownNodeInstance instanceof ArchivedMultiInstanceActivityInstance) {
                return ((ArchivedMultiInstanceActivityInstance) archFlownNodeInstance).getNumberOfInstances();
            }
            return null;
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
            return archFlownNodeInstance != null;
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

    /**
     * Create an register the instance
     * @return
     */
    public SignalDetail createInstanceSignal() {
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

        /**
         * Create an register the instance
         * @return
         */
        public MessageContent createInstanceMessageContent() {
            MessageContent messageContent = new MessageContent();
            listMessageContent.add(messageContent);
            return messageContent;
        }
    }

    /**
     * Create an register the instance
     * @return
     */
    public MessageDetail createInstanceMessageDetail() {
        MessageDetail messageDetail = new MessageDetail();
        listMessages.add(messageDetail);
        return messageDetail;
    }

    public class MessageContent {

        CatchMessageEventTriggerDefinition messageEvent;
        Map<String, Object> mapCorrelationValues = new HashMap<>();
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

    /**
     * Create an register the instance
     * @return
     */
    public TimerDetail createInstanceTimerDetail() {
        TimerDetail timerDetail = new TimerDetail();
        listTimers.add(timerDetail);
        return timerDetail;
    }

    /* ************************************************************************ */
    /*                                                                          */
    /* ProcessVariable */
    /*                                                                          */
    /* ************************************************************************ */

    public List<CaseDetailVariable> listVariables = new ArrayList<>();

    public enum ScopeVariable {
        BDM, PROCESS, LOCAL
    }

    /**
     * A caseDetailVariable is a Process or a BDM variable (they are all variables isn't?)
     * 
     * @author Firstname Lastname
     */
    public class CaseDetailVariable {

        public boolean isArchived = false;
        public Long id;
        public String name;
        public String description;
        public Date dateArchived;
        public Long processInstanceId;
        public Long activityId;
        public Long sourceId;
        public String typeVariable;

        public String contextInfo;
        // for a Process Variable
        public Serializable value;

        // for a BDM
        public String bdmName;
        public boolean bdmIsMultiple;
        public List<Long> listPersistenceId = new ArrayList<>();

        public ScopeVariable scopeVariable;

        /**
         * If data are loaded from the Database, both of this value will be null then.
         */
        public DataInstance dataInstance = null;
        public ArchivedDataInstance archivedDataInstance = null;

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

        public String toString() {
            return name+" "+scopeVariable+" "+getValueToDisplay() +" PID["+processInstanceId+ (scopeVariable==ScopeVariable.LOCAL ? "] Activity["+activityId:"") +"]";
        };
    }

    /**
     * Create an register the instance
     * @return
     */
    public CaseDetailVariable createInstanceProcessVariableDetail() {
        CaseDetailVariable processVariable = new CaseDetailVariable();
        listVariables.add(processVariable);
        return processVariable;
    }

    public CaseDetailVariable getProcessVariable(String processVariableName) {
        for (CaseDetailVariable processVariable : listVariables) {
            if (processVariable.name.equalsIgnoreCase(processVariableName))
                return processVariable;
        }
        return null;
    }

    public void addProcessVariableDetails(List<CaseDetailVariable> listToAdd) {
        listVariables.addAll(listToAdd);
    }

    /* ************************************************************************ */
    /*                                                                          */
    /* Documents */
    /*                                                                          */
    /* ************************************************************************ */

    public List<CaseDetailDocument> listDocuments = new ArrayList<>();

    public class CaseDetailDocument {

        public long processInstanceId;
        public Document document;
    }
    /**
     * Create an register the instance
     * @return
     */
    public CaseDetailDocument createInstanceCaseDetailDocument() {
        CaseDetailDocument documentVariable = new CaseDetailDocument();
        listDocuments.add(documentVariable);
        return documentVariable;
    }

      /* ************************************************************************ */
    /*                                                                          */
    /* Comments */
    /*                                                                          */
    /* ************************************************************************ */

    public List<CaseDetailComment> listComments = new ArrayList<>();

    public class CaseDetailComment {

        public long processInstanceId;
        public Comment comment;
        public ArchivedComment archivedComment;
    }
    public CaseDetailComment createInstanceCaseDetailComment() {
        CaseDetailComment commentVariable = new CaseDetailComment();
        listComments.add(commentVariable);
        return commentVariable;
    }

   
}
