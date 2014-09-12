/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jbpm.console.ng.ht.client.editors.taskadmin;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Label;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.ErrorCallback;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jbpm.console.ng.bd.service.DataServiceEntryPoint;
import org.jbpm.console.ng.ht.client.i18n.Constants;
import org.jbpm.console.ng.ht.model.TaskSummary;
import org.jbpm.console.ng.ht.model.events.TaskRefreshedEvent;
import org.jbpm.console.ng.ht.model.events.TaskSelectionEvent;
import org.jbpm.console.ng.ht.service.TaskServiceEntryPoint;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.client.annotations.WorkbenchScreen;
import org.kie.uberfire.client.common.popups.errors.ErrorPopup;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.mvp.UberView;
import org.uberfire.lifecycle.OnOpen;
import org.uberfire.lifecycle.OnStartup;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.security.Identity;
import org.uberfire.client.workbench.events.BeforeClosePlaceEvent;

@Dependent
@WorkbenchScreen(identifier = "Task Admin")
public class TaskAdminPresenter {

    private Constants constants = GWT.create(Constants.class);

    public interface TaskAdminView extends UberView<TaskAdminPresenter> {

        void displayNotification(String text);

        Label getUsersGroupsControlsPanel();

        Button getForwardButton();
        
        TextBox getUserOrGroupText();
        
    }

    @Inject
    private PlaceManager placeManager;

    @Inject
    private TaskAdminView view;

    @Inject
    private Identity identity;

    @Inject
    Caller<TaskServiceEntryPoint> taskServices;

    @Inject
    private Caller<DataServiceEntryPoint> dataServices;

    @Inject
    private Event<BeforeClosePlaceEvent> closePlaceEvent;

    private PlaceRequest place;

    private long currentTaskId = 0;
    
    @Inject
    private Event<TaskRefreshedEvent> taskRefreshed;

    @OnStartup
    public void onStartup(final PlaceRequest place) {
        this.place = place;
    }

    @WorkbenchPartTitle
    public String getTitle() {
        return constants.Task_Admin();
    }

    @WorkbenchPartView
    public UberView<TaskAdminPresenter> getView() {
        return view;
    }

    public void forwardTask(String entity) {
        taskServices.call(new RemoteCallback<Void>() {
            @Override
            public void callback(Void nothing) {
                view.displayNotification("Task was succesfully forwarded");
                taskRefreshed.fire( new TaskRefreshedEvent( currentTaskId ) );
                refreshTaskPotentialOwners();
            }

        }, new ErrorCallback<Message>() {
          @Override
          public boolean error( Message message, Throwable throwable ) {
              ErrorPopup.showMessage("Unexpected error encountered : " + throwable.getMessage());
              return true;
          }
      }).delegate( currentTaskId, identity.getName(), entity );
    }

    public void refreshTaskPotentialOwners() {
        List<Long> taskIds = new ArrayList<Long>(1);
        taskIds.add(currentTaskId);
        taskServices.call(new RemoteCallback<Map<Long, List<String>>>() {
            @Override
            public void callback(Map<Long, List<String>> ids) {
                if (ids.isEmpty()) {
                    view.getUsersGroupsControlsPanel().setText(constants.No_Potential_Owners());
                } else {
                    view.getUsersGroupsControlsPanel().setText(("" + ids.get(currentTaskId).toString()));
                }
            }
        }, new ErrorCallback<Message>() {
              @Override
              public boolean error( Message message, Throwable throwable ) {
                  ErrorPopup.showMessage("Unexpected error encountered : " + throwable.getMessage());
                  return true;
              }
          }).getPotentialOwnersForTaskIds(taskIds);
        
        taskServices.call(new RemoteCallback<TaskSummary>() {
            @Override
            public void callback(TaskSummary ts) {
                if (ts == null) return;
                view.getForwardButton().setEnabled(true);
                view.getUserOrGroupText().setEnabled(true);
            }
        }, new ErrorCallback<Message>() {
              @Override
              public boolean error( Message message, Throwable throwable ) {
                  ErrorPopup.showMessage("Unexpected error encountered : " + throwable.getMessage());
                  return true;
              }
          }).getTaskDetails(currentTaskId);

    }

    @OnOpen
    public void onOpen() {

        this.currentTaskId = Long.parseLong(place.getParameter("taskId", "0").toString());
        refreshTaskPotentialOwners();
    }
    
    public void onTaskSelectionEvent( @Observes final TaskSelectionEvent event ) {
        this.currentTaskId = event.getTaskId();
        refreshTaskPotentialOwners();
    }
    
    public void onTaskRefreshedEvent(@Observes TaskRefreshedEvent event){
        if(currentTaskId == event.getTaskId()){
            refreshTaskPotentialOwners();
        }
    }

    public void close() {
        closePlaceEvent.fire(new BeforeClosePlaceEvent(this.place));
    }

}
