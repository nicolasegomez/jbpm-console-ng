/*
 * Copyright 2014 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jbpm.console.ng.ht.forms.client.editors.taskform.displayers;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.ErrorCallback;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jbpm.console.ng.ht.forms.api.FormRefreshCallback;
import org.jbpm.console.ng.ht.forms.client.editors.taskform.displayers.util.ActionRequest;
import org.jbpm.console.ng.ht.forms.client.editors.taskform.displayers.util.JSNIHelper;
import org.jbpm.console.ng.ht.forms.client.i18n.Constants;
import org.jbpm.console.ng.ht.forms.ht.api.HumanTaskFormDisplayer;
import org.jbpm.console.ng.ht.model.TaskKey;
import org.jbpm.console.ng.ht.model.TaskSummary;
import org.jbpm.console.ng.ht.model.events.TaskRefreshedEvent;
import org.jbpm.console.ng.ht.service.TaskServiceEntryPoint;
import org.uberfire.client.workbench.widgets.common.ErrorPopup;
import org.uberfire.security.Identity;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author salaboy
 */
public abstract class AbstractHumanTaskFormDisplayer implements HumanTaskFormDisplayer {
    public static final String ACTION_CLAIM_TASK = "claimTask";
    public static final String ACTION_START_TASK = "startTask";
    public static final String ACTION_RELEASE_TASK = "releaseTask";
    public static final String ACTION_SAVE_TASK = "saveTask";
    public static final String ACTION_COMPLETE_TASK = "completeTask";

    protected long taskId;
    protected String formContent;
    protected String opener;
    protected String taskName;

    final protected FlowPanel container = new FlowPanel();
    final protected FlowPanel buttonsContainer = new FlowPanel();
    final protected VerticalPanel formContainer = new VerticalPanel();
    protected Constants constants = GWT.create(Constants.class);

    @Inject
    protected Caller<TaskServiceEntryPoint> taskServices;

    @Inject
    protected Event<TaskRefreshedEvent> taskRefreshed;

    @Inject
    protected Identity identity;

    @Inject
    protected JSNIHelper jsniHelper;

    protected List<FormRefreshCallback> refreshCallbacks = new ArrayList<FormRefreshCallback>();


    protected abstract void initDisplayer();

    protected abstract void completeFromDisplayer();

    protected abstract void saveStateFromDisplayer();

    protected abstract void startFromDisplayer();

    protected abstract void claimFromDisplayer();

    protected abstract void releaseFromDisplayer();

    @Override
    public void init(TaskKey key, String formContent, String openerUrl) {
        this.taskId = key.getTaskId();
        this.formContent = formContent;
        this.opener = openerUrl;
        container.add(formContainer);
        container.add(buttonsContainer);

        if (formContent == null || formContent.length() == 0) {
            return;
        }
        taskServices.call(new RemoteCallback<TaskSummary>() {
            @Override
            public void callback(final TaskSummary task) {
                buttonsContainer.clear();
                taskName = task.getTaskName();

                if (task == null) {
                    return;
                }

                if (opener != null) {
                    injectEventListener(AbstractHumanTaskFormDisplayer.this);
                } else {
                    FlowPanel wrapperFlowPanel = new FlowPanel();
                    wrapperFlowPanel.setStyleName("wrapper form-actions");
                    buttonsContainer.add(wrapperFlowPanel);

                    if (task.getStatus().equals("Ready")) {
                        Button claimButton = new Button();
                        claimButton.setType(ButtonType.PRIMARY);
                        claimButton.setText(constants.Claim());
                        claimButton.addClickHandler(new ClickHandler() {
                            @Override
                            public void onClick(ClickEvent event) {
                                claimFromDisplayer();
                            }
                        });
                        wrapperFlowPanel.add(claimButton);
                        buttonsContainer.add(wrapperFlowPanel);
                    }

                    if (task.getStatus().equals("Reserved") && task.getActualOwner().equals(identity.getName())) {

                        Button releaseButton = new Button();
                        releaseButton.setText(constants.Release());
                        releaseButton.addClickHandler(new ClickHandler() {
                            @Override
                            public void onClick(ClickEvent event) {
                                releaseFromDisplayer();
                            }
                        });
                        wrapperFlowPanel.add(releaseButton);

                        Button startButton = new Button();
                        startButton.setType(ButtonType.PRIMARY);
                        startButton.setText(constants.Start());
                        startButton.addClickHandler(new ClickHandler() {
                            @Override
                            public void onClick(ClickEvent event) {
                                startFromDisplayer();
                            }
                        });
                        wrapperFlowPanel.add(startButton);

                        buttonsContainer.add(wrapperFlowPanel);
                    } else if (task.getStatus().equals("InProgress") && task.getActualOwner().equals(identity.getName())) {
                        Button saveButton = new Button();
                        saveButton.setText(constants.Save());
                        saveButton.addClickHandler(new ClickHandler() {
                            @Override
                            public void onClick(ClickEvent event) {
                                saveStateFromDisplayer();

                            }
                        });
                        wrapperFlowPanel.add(saveButton);

                        Button releaseButton = new Button();
                        releaseButton.setText(constants.Release());
                        releaseButton.addClickHandler(new ClickHandler() {
                            @Override
                            public void onClick(ClickEvent event) {
                                releaseFromDisplayer();
                            }
                        });
                        wrapperFlowPanel.add(releaseButton);

                        Button completeButton = new Button();
                        completeButton.setType(ButtonType.PRIMARY);
                        completeButton.setText(constants.Complete());
                        completeButton.addClickHandler(new ClickHandler() {
                            @Override
                            public void onClick(ClickEvent event) {
                                completeFromDisplayer();
                            }
                        });

                        wrapperFlowPanel.add(completeButton);
                        buttonsContainer.add(wrapperFlowPanel);

                    }
                }
                initDisplayer();
            }
        }, getUnexpectedErrorCallback()).getTaskDetails(taskId);
    }

    @Override
    public void complete(Map<String, Object> params) {
        taskServices.call(getCompleteTaskRemoteCallback(), getUnexpectedErrorCallback())
                .complete(taskId, identity.getName(), params);
    }

    @Override
    public void claim() {
        taskServices.call(getClaimTaskCallback(), getUnexpectedErrorCallback()).claim(taskId, identity.getName());
    }

    @Override
    public void release() {
        taskServices.call(getReleaseTaskRemoteCallback(), getUnexpectedErrorCallback()).release(taskId, identity.getName());
    }

    @Override
    public void saveState(Map<String, Object> state) {
        taskServices.call(getSaveTaskStateCallback(), getUnexpectedErrorCallback()).saveContent(taskId, state);
    }

    @Override
    public void start() {
        taskServices.call(getStartTaskRemoteCallback(), getUnexpectedErrorCallback()).start(taskId, identity.getName());
    }

    @Override
    public FlowPanel getContainer() {
        return container;
    }

    protected RemoteCallback getStartTaskRemoteCallback() {
        return new RemoteCallback<Void>() {
            @Override
            public void callback(Void nothing) {
                taskRefreshed.fire(new TaskRefreshedEvent(taskId));
                jsniHelper.notifySuccessMessage(opener, "Task: " + taskId + " was started!");
                refresh();
            }
        };
    }

    protected RemoteCallback getClaimTaskCallback() {
        return new RemoteCallback<Void>() {
            @Override
            public void callback(Void nothing) {
                taskRefreshed.fire(new TaskRefreshedEvent(taskId));
                jsniHelper.notifySuccessMessage(opener, "Task: " + taskId + " was claimed!");
                refresh();
            }
        };
    }

    protected RemoteCallback getSaveTaskStateCallback() {
        return new RemoteCallback<Long>() {
            @Override
            public void callback(Long contentId) {
                taskRefreshed.fire(new TaskRefreshedEvent(taskId));
                jsniHelper.notifySuccessMessage(opener, "Task: " + taskId + " was saved!");
                refresh();
            }
        };
    }

    protected RemoteCallback getReleaseTaskRemoteCallback() {
        return new RemoteCallback<Void>() {
            @Override
            public void callback(Void nothing) {
                taskRefreshed.fire(new TaskRefreshedEvent(taskId));
                jsniHelper.notifySuccessMessage(opener, "Task: " + taskId + " was released!");
                refresh();
            }
        };
    }

    protected RemoteCallback<Void> getCompleteTaskRemoteCallback() {
        return new RemoteCallback<Void>() {
            @Override
            public void callback(Void nothing) {

                taskRefreshed.fire(new TaskRefreshedEvent(taskId));
                jsniHelper.notifySuccessMessage(opener, "Task: " + taskId + " was completed!");
                taskServices.call(new RemoteCallback<Boolean>() {
                    @Override
                    public void callback(Boolean response) {
                        if (!response) {
                            //editPanelEvent.fire(new EditPanelEvent(taskId));
                            close();
                        }
                        refresh();
                    }
                }).existInDatabase(taskId);

            }
        };
    }

    protected ErrorCallback<Message> getUnexpectedErrorCallback() {
        return new ErrorCallback<Message>() {
            @Override
            public boolean error(Message message, Throwable throwable) {
                String notification = "Unexpected error encountered : " + throwable.getMessage();
                ErrorPopup.showMessage(notification);
                jsniHelper.notifyErrorMessage(opener, notification);
                return true;
            }
        };
    }

    @Override
    public void addFormRefreshCallback(FormRefreshCallback callback) {
        refreshCallbacks.add(callback);
    }

    protected void refresh(){
        for(FormRefreshCallback callback : refreshCallbacks){
            callback.refresh();
        }
    }

    @Override
    public void close() {
        for(FormRefreshCallback callback : refreshCallbacks){
            callback.close();
        }
    }

    protected void eventListener(String origin, String request) {
        if (origin == null || !origin.endsWith("//" + opener)) return;

        ActionRequest actionRequest = JsonUtils.safeEval(request);

        if (ACTION_CLAIM_TASK.equals(actionRequest.getAction())) claimFromDisplayer();
        else if (ACTION_START_TASK.equals(actionRequest.getAction())) startFromDisplayer();
        else if (ACTION_RELEASE_TASK.equals(actionRequest.getAction())) releaseFromDisplayer();
        else if (ACTION_SAVE_TASK.equals(actionRequest.getAction())) saveStateFromDisplayer();
        else if (ACTION_COMPLETE_TASK.equals(actionRequest.getAction())) completeFromDisplayer();
    }

    private native void injectEventListener(AbstractHumanTaskFormDisplayer fdp) /*-{
        function postMessageListener(e) {
            fdp.@org.jbpm.console.ng.ht.forms.client.editors.taskform.displayers.AbstractHumanTaskFormDisplayer::eventListener(Ljava/lang/String;Ljava/lang/String;)(e.origin, e.data);
        }

        if ($wnd.addEventListener) {
            $wnd.addEventListener("message", postMessageListener, false);
        } else {
            $wnd.attachEvent("onmessage", postMessageListener, false);
        }
    }-*/;
}
