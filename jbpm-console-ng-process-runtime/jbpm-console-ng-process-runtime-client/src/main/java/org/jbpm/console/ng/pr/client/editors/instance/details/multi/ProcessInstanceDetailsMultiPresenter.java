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
package org.jbpm.console.ng.pr.client.editors.instance.details.multi;

import java.util.List;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.IsWidget;
import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.ErrorCallback;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jbpm.console.ng.bd.service.DataServiceEntryPoint;
import org.jbpm.console.ng.bd.service.KieSessionEntryPoint;
import org.jbpm.console.ng.gc.client.experimental.details.AbstractTabbedDetailsPresenter;
import org.jbpm.console.ng.gc.client.experimental.details.AbstractTabbedDetailsView.TabbedDetailsView;
import org.jbpm.console.ng.pr.client.editors.diagram.ProcessDiagramUtil;
import org.jbpm.console.ng.pr.client.i18n.Constants;
import org.jbpm.console.ng.pr.model.NodeInstanceSummary;
import org.jbpm.console.ng.pr.model.ProcessInstanceSummary;
import org.jbpm.console.ng.pr.model.events.ProcessInstanceSelectionEvent;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.uberfire.client.common.popups.errors.ErrorPopup;
import org.uberfire.client.annotations.DefaultPosition;
import org.uberfire.client.annotations.WorkbenchMenu;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.client.annotations.WorkbenchScreen;
import org.uberfire.client.mvp.UberView;
import org.uberfire.client.workbench.events.ChangeTitleWidgetEvent;
import org.uberfire.lifecycle.OnClose;
import org.uberfire.lifecycle.OnStartup;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.workbench.model.Position;
import org.uberfire.workbench.model.menu.MenuFactory;
import org.uberfire.workbench.model.menu.MenuItem;
import org.uberfire.workbench.model.menu.Menus;
import org.uberfire.workbench.model.menu.impl.BaseMenuCustom;

@Dependent
@WorkbenchScreen(identifier = "Process Instance Details Multi", preferredWidth = 500)
public class ProcessInstanceDetailsMultiPresenter extends AbstractTabbedDetailsPresenter {

    public interface ProcessInstanceDetailsMultiView
            extends TabbedDetailsView<ProcessInstanceDetailsMultiPresenter> {

        IsWidget getOptionsButton();

        IsWidget getRefreshButton();

        IsWidget getCloseButton();
    }

    @Inject
    public ProcessInstanceDetailsMultiView view;

    private Constants constants = GWT.create( Constants.class );

    @Inject
    private Caller<KieSessionEntryPoint> kieSessionServices;

    @Inject
    private Caller<DataServiceEntryPoint> dataServices;

    @Inject
    private Event<ProcessInstanceSelectionEvent> processInstanceSelected;

    @Inject
    private Event<ChangeTitleWidgetEvent> changeTitleWidgetEvent;

    private String selectedDeploymentId = "";

    private int selectedProcessInstanceStatus = 0;

    private String selectedProcessDefName = "";

    @WorkbenchPartView
    public UberView<ProcessInstanceDetailsMultiPresenter> getView() {
        return view;
    }

    @DefaultPosition
    public Position getPosition() {
        return Position.EAST;
    }



    @WorkbenchPartTitle
    public String getTitle() {
        return constants.Details();
    }

    @OnStartup
    public void onStartup( final PlaceRequest place ) {
        super.onStartup( place );
    }

    public void onProcessSelectionEvent( @Observes ProcessInstanceSelectionEvent event ) {
        selectedItemId = String.valueOf( event.getProcessInstanceId() );
        selectedItemName = event.getProcessDefId();
        selectedDeploymentId = event.getDeploymentId();
        selectedProcessInstanceStatus = event.getProcessInstanceStatus();
        selectedProcessDefName = event.getProcessDefName();

        changeTitleWidgetEvent.fire( new ChangeTitleWidgetEvent( this.place, String.valueOf( selectedItemId ) + " - " + selectedProcessDefName ) );

        view.getTabPanel().selectTab( 0 );
    }

    public void refresh() {
        processInstanceSelected.fire( new ProcessInstanceSelectionEvent( selectedDeploymentId, Long.valueOf( selectedItemId ), selectedItemName, selectedProcessDefName, selectedProcessInstanceStatus ) );
    }

    public void signalProcessInstance() {
        PlaceRequest placeRequestImpl = new DefaultPlaceRequest( "Signal Process Popup" );
        placeRequestImpl.addParameter( "processInstanceId", selectedItemId );
        placeManager.goTo( placeRequestImpl );

    }

    public void abortProcessInstance() {
        dataServices.call( new RemoteCallback<ProcessInstanceSummary>() {
            @Override
            public void callback( ProcessInstanceSummary processInstance ) {
                if ( processInstance.getState() == ProcessInstance.STATE_ACTIVE ||
                        processInstance.getState() == ProcessInstance.STATE_PENDING ) {
                    if ( Window.confirm( "Are you sure that you want to abort the process instance?" ) ) {
                        final long processInstanceId = Long.parseLong( selectedItemId );
                        kieSessionServices.call( new RemoteCallback<Void>() {
                            @Override
                            public void callback( Void v ) {
                                refresh();
                            }
                        }, new ErrorCallback<Message>() {
                            @Override
                            public boolean error( Message message,
                                                  Throwable throwable ) {
                                ErrorPopup.showMessage( "Unexpected error encountered : " + throwable.getMessage() );
                                return true;
                            }
                        } ).abortProcessInstance( processInstanceId );
                    }
                } else {
                    Window.alert( "Process instance needs to be active in order to be aborted" );
                }
            }
        }, new ErrorCallback<Message>() {
            @Override
            public boolean error( Message message,
                                  Throwable throwable ) {
                ErrorPopup.showMessage( "Unexpected error encountered : " + throwable.getMessage() );
                return true;
            }
        } ).getProcessInstanceById( Long.parseLong( selectedItemId ) );
    }

    public void goToProcessInstanceModelPopup() {
        if ( place != null && !selectedItemId.equals( "" ) ) {
            dataServices.call( new RemoteCallback<List<NodeInstanceSummary>>() {
                @Override
                public void callback( List<NodeInstanceSummary> activeNodes ) {
                    final StringBuffer nodeParam = new StringBuffer();
                    for ( NodeInstanceSummary activeNode : activeNodes ) {
                        nodeParam.append( activeNode.getNodeUniqueName() + "," );
                    }
                    if ( nodeParam.length() > 0 ) {
                        nodeParam.deleteCharAt( nodeParam.length() - 1 );
                    }

                    dataServices.call( new RemoteCallback<List<NodeInstanceSummary>>() {
                        @Override
                        public void callback( List<NodeInstanceSummary> completedNodes ) {
                            StringBuffer completedNodeParam = new StringBuffer();
                            for ( NodeInstanceSummary completedNode : completedNodes ) {
                                if ( completedNode.isCompleted() ) {
                                    // insert outgoing sequence flow and node as this is for on entry event
                                    completedNodeParam.append( completedNode.getNodeUniqueName() + "," );
                                    completedNodeParam.append( completedNode.getConnection() + "," );
                                } else if ( completedNode.getConnection() != null ) {
                                    // insert only incoming sequence flow as node id was already inserted
                                    completedNodeParam.append( completedNode.getConnection() + "," );
                                }
                            }
                            completedNodeParam.deleteCharAt( completedNodeParam.length() - 1 );

                            placeManager.goTo( ProcessDiagramUtil.buildPlaceRequest( new DefaultPlaceRequest( "" )
                                                                                             .addParameter( "activeNodes", nodeParam.toString() )
                                                                                             .addParameter( "completedNodes", completedNodeParam.toString() )
                                                                                             .addParameter( "readOnly", "true" )
                                                                                             .addParameter( "processId", selectedItemName )
                                                                                             .addParameter( "deploymentId", selectedDeploymentId ) ) );

                        }
                    }, new ErrorCallback<Message>() {
                        @Override
                        public boolean error( Message message,
                                              Throwable throwable ) {
                            ErrorPopup.showMessage( "Unexpected error encountered : " + throwable.getMessage() );
                            return true;
                        }
                    } ).getProcessInstanceCompletedNodes( Long.parseLong( selectedItemId ) );

                }
            }, new ErrorCallback<Message>() {
                @Override
                public boolean error( Message message,
                                      Throwable throwable ) {
                    ErrorPopup.showMessage( "Unexpected error encountered : " + throwable.getMessage() );
                    return true;
                }
            } ).getProcessInstanceActiveNodes( Long.parseLong( selectedItemId ) );

        }
    }

    @OnClose
    public void onClose() {
        super.onClose();
    }

    @WorkbenchMenu
    public Menus buildMenu() {
        return MenuFactory
                .newTopLevelCustomMenu( new MenuFactory.CustomMenuBuilder() {
                    @Override
                    public void push( MenuFactory.CustomMenuBuilder element ) {
                    }

                    @Override
                    public MenuItem build() {
                        return new BaseMenuCustom<IsWidget>() {
                            @Override
                            public IsWidget build() {
                                return view.getOptionsButton();
                            }
                        };
                    }
                } ).endMenu()

                .newTopLevelCustomMenu( new MenuFactory.CustomMenuBuilder() {
                    @Override
                    public void push( MenuFactory.CustomMenuBuilder element ) {
                    }

                    @Override
                    public MenuItem build() {
                        return new BaseMenuCustom<IsWidget>() {
                            @Override
                            public IsWidget build() {
                                return view.getRefreshButton();
                            }
                        };
                    }
                } ).endMenu()

                .newTopLevelCustomMenu( new MenuFactory.CustomMenuBuilder() {
                    @Override
                    public void push( MenuFactory.CustomMenuBuilder element ) {
                    }

                    @Override
                    public MenuItem build() {
                        return new BaseMenuCustom<IsWidget>() {
                            @Override
                            public IsWidget build() {
                                return view.getCloseButton();
                            }
                        };
                    }
                } ).endMenu().build();
    }

}
