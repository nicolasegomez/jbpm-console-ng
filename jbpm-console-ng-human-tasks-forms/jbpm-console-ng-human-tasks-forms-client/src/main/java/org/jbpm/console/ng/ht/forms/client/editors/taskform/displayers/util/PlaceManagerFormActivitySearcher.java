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
package org.jbpm.console.ng.ht.forms.client.editors.taskform.displayers.util;

import com.google.gwt.user.client.ui.IsWidget;
import org.uberfire.client.mvp.AbstractWorkbenchScreenActivity;
import org.uberfire.client.mvp.ActivityManager;
import org.uberfire.mvp.impl.DefaultPlaceRequest;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Map;

/**
 * @author pefernan
 */
@Dependent
public class PlaceManagerFormActivitySearcher {

  @Inject
  private ActivityManager activityManager;

  private AbstractWorkbenchScreenActivity currentActivity;

  public IsWidget findFormActivityWidget(String name, Map<String, String> params) {
    DefaultPlaceRequest defaultPlaceRequest = new DefaultPlaceRequest(name + " Form", params);
    currentActivity = (AbstractWorkbenchScreenActivity) activityManager.getActivity(defaultPlaceRequest);
    if (currentActivity == null) {
      return null;
    }
    currentActivity.launch(defaultPlaceRequest, null);
    currentActivity.onStartup(defaultPlaceRequest);
    currentActivity.onOpen();
    return currentActivity.getWidget();
  }

  public void closeFormActivity() {
    if (currentActivity != null) {
      currentActivity.onClose();
    }
  }
}
