/****************************************************************
 * Copyright (C) 2005 LAMS Foundation (http://lamsfoundation.org)
 * =============================================================
 * License Information: http://lamsfoundation.org/licensing/lams/2.0/
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2.0 
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * USA
 * 
 * http://www.gnu.org/licenses/gpl.txt
 * ****************************************************************
 */

/* $$Id$$ */	
package org.lamsfoundation.lams.learning.web.util;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ForwardingActionForward;
import org.apache.struts.action.RedirectingActionForward;
import org.lamsfoundation.lams.learning.service.ICoreLearnerService;
import org.lamsfoundation.lams.learning.service.LearnerServiceException;
import org.lamsfoundation.lams.learning.web.action.ActivityAction;
import org.lamsfoundation.lams.learningdesign.Activity;
import org.lamsfoundation.lams.learningdesign.ToolActivity;
import org.lamsfoundation.lams.lesson.LearnerProgress;
import org.lamsfoundation.lams.lesson.Lesson;
import org.lamsfoundation.lams.tool.exception.LamsToolServiceException;
import org.lamsfoundation.lams.tool.service.ILamsCoreToolService;
import org.lamsfoundation.lams.usermanagement.User;
import org.lamsfoundation.lams.util.Configuration;
import org.lamsfoundation.lams.util.ConfigurationKeys;
import org.lamsfoundation.lams.util.WebUtil;
import org.lamsfoundation.lams.web.util.AttributeNames;

/**
 * This class contains the standard struts action mappings for errors as
 * well as methods that get required Action/URL to display an Activity or
 * LearnerProgress.
 * 
 * In order to return a URL this class needs to know the baseURL. This can
 * be set using in the application context.
 * 
 * @author daveg
 *
 */
public class ActivityMapping implements Serializable
{

    /* These are global struts forwards. */
    public static final String ERROR = "error";
    public static final String NO_SESSION_ERROR = "noSessionError";
    public static final String NO_ACCESS_ERROR = "noAccessError";
    public static final String DOUBLE_SUBMIT_ERROR = "doubleSubmitError";
    public static final String LEARNING = "learning";

    private ActivityMappingStrategy activityMappingStrategy = new ActivityMappingStrategy();

    private ILamsCoreToolService toolService;

    /**
     * Creates a Struts ActionForward to display an activity.
     * @param activity, the Activity to be displayed
     * @param progress, the LearnerProgress associated with the Activity and learner
     */
    public ActionForward getActivityForward(Activity activity,
                                            LearnerProgress progress,
                                            boolean redirect)
    {
        ActionForward actionForward = null;

        //String strutsAction = getActivityAction(activity, progress);
        String strutsAction = this.activityMappingStrategy.getActivityAction(activity);
        if (activity.isToolActivity())
        {
            // always use redirect false for a ToolActivity as ToolDisplayActivity
            // does it's own redirect
            actionForward = strutsActionToForward(strutsAction, activity, false);
        }
        else
        {
            actionForward = strutsActionToForward(strutsAction,
                                                  activity,
                                                  redirect);
        }

        return actionForward;
    }

    /**
     * Creates a Struts ActionForward to display a next activity. If the
     * previous activity was a ParallelActivity then the frames will be
     * cleared.
     * @param progress, the LearnerProgress associated with the Activity and learner
     * @param redirect, If true a RedirectActionForward is used
     * @return
     */
    public ActionForward getProgressForward(LearnerProgress progress,
                                            boolean redirect,
                                            HttpServletRequest request,
                                            ICoreLearnerService learnerService)
    {
        ActionForward actionForward = null;

        if (progress.isLessonComplete())
        {
            // If lesson complete forward to lesson complete action. This action will
            // cause a client request to clear ALL frames. Need to append the progress
        	// id as getting to the end from an activity can't have the progress in the request
        	// and there isn't an activity from which we can determine the lesson and hence
        	// the progress.
            String strutsAction = this.getActivityMappingStrategy()
                                      .getLessonCompleteAction();
            strutsAction = WebUtil.appendParameterToURL(strutsAction, LearningWebUtil.PARAM_PROGRESS_ID, progress.getLearnerProgressId().toString());
            actionForward = this.strutsActionToForward(strutsAction,
                                                       null,
                                                       redirect);
        }
        else
        {

            if (progress.getCurrentActivity()!=null && ! progress.getCurrentActivity().isParallelActivity() && progress.isParallelWaiting())
            {
                // progress is waiting, goto waiting page
                String strutsAction = this.getActivityMappingStrategy()
                                          .getWaitingAction();
                actionForward = this.strutsActionToForward(strutsAction,
                                                           null,
                                                           redirect);
            }
            else
            {
                // display next activity
                if (progress.getPreviousActivity()!=null && progress.getPreviousActivity().isParallelActivity())
                {
                    // if previous activity was a parallel activity then we need to
                    // clear frames.
                    this.getRedirectForward(progress, redirect);
                }
                else
                {
                    actionForward = getActivityForward(progress.getNextActivity(),
                                                       progress,
                                                       redirect);
                    //setup activity into request for display
                    Activity realActivity = learnerService.getActivity(progress.getNextActivity().getActivityId());
                    request.setAttribute(ActivityAction.ACTIVITY_REQUEST_ATTRIBUTE, realActivity);

                    LearningWebUtil.putActivityInRequest(request,
                                         progress.getNextActivity(),
                                         learnerService);
                }
            }
        }
        return actionForward;
    }
    
    public ActionForward getRedirectForward(LearnerProgress progress, boolean redirect) {
    	ActionForward actionForward = null;
    	
    	String strutsAction = "/requestDisplay.do";
        String activityURL = this.getActivityURL(progress.getNextActivity());
        strutsAction += "?url=" + activityURL;
        
    	actionForward = strutsActionToForward(strutsAction,
    											null,
    											redirect);
    	return actionForward;
    	
    }

    /**
     * Generates an ActivityURL for an Activity using it's progress. The URL is for
     * the client and so includes hostname etc.
     * Note that the URL could also be a wait message or a jsp to clear the frames.
     * @param activity, the Activity to be displayed
     * @param progress, the LearnerProgress associated with the Activity and learner
     */
    public String getActivityURL(Activity activity)
    {
        String strutsAction = this.activityMappingStrategy.getActivityAction(activity);
        return strutsActionToURL(strutsAction, activity, true);
    }

    /**
     * Generates an ActivityURL for the next Activity using it's progress. The URL
     * is for the client and so includes hostname etc.
     * Note that this method always returns a LAMS URLs, if a ToolActivity is next
     * the URL will be the action for displaying the tool.
     * Note that the URL could also be a wait message or a jsp to clear the frames.
     * @param progress, the current LearnerProgress.
     * @throws UnsupportedEncodingException
     */
    public String getProgressURL(LearnerProgress progress) throws UnsupportedEncodingException
    {
        String activityURL = null;
        
        if (progress.isLessonComplete())
        {
            // If lesson complete forward to lesson complete action. This action will
            // cause a client request to clear ALL frames. Need to append the progress
        	// id as getting to the end from an activity can't have the progress in the request
        	// and there isn't an activity from which we can determine the lesson and hence
        	// the progress.
            String strutsAction = this.getActivityMappingStrategy()
                                      .getLessonCompleteAction();
            strutsAction = WebUtil.appendParameterToURL(strutsAction, LearningWebUtil.PARAM_PROGRESS_ID, progress.getLearnerProgressId().toString());
            return activityURL = strutsActionToURL(strutsAction, null, true);
        }
        else
        {
            if (progress.isParallelWaiting())
            {
                // progress is waiting, goto waiting page
                String strutsAction = this.getActivityMappingStrategy()
                                          .getWaitingAction();
                activityURL = strutsActionToURL(strutsAction, null, true);
            }
            else
            {
                // display next activity
                activityURL = this.getActivityURL(progress.getNextActivity());
                if (progress.getPreviousActivity() != null && progress.getPreviousActivity().isParallelActivity())
                {
                    // if previous activity was a parallel activity then we need to
                    // clear frames.
                    String strutsAction = "/requestDisplay.do";
                    String redirectURL = strutsActionToURL(strutsAction,
                                                           null,
                                                           true);
                    activityURL = URLEncoder.encode(activityURL, "UTF-8");
                    activityURL = redirectURL + "?url=" + activityURL;
                } 
            }
        }

        return WebUtil.appendParameterToURL(activityURL,
                                            LearningWebUtil.PARAM_PROGRESS_ID,
                                            progress.getLearnerProgressId().toString());

    }

    /**
     * Generates an ActivityURL for a Tool Activity or SystemToolActivity. 
     * The URL is for the tool and not for the tool loading page. The URL also 
     * includes toolSessionId or toolContentId and all other required data.
     * @param activity, the Activity to be displayed
     * @param progress, the current LearnerProgress, used to get activity status
     */
    public String getLearnerToolURL(Lesson lesson, Activity activity, User learner)
    {
        try
        {
        	if ( lesson.isPreviewLesson() )
        		return toolService.getToolLearnerPreviewURL(lesson.getLessonId(),activity,learner);
        	else 
        		return toolService.getToolLearnerURL(lesson.getLessonId(),activity,learner);
        }
        catch (LamsToolServiceException e)
        {
            throw new LearnerServiceException(e.getMessage());
        }
    }

    /**
     * Creates a URL for a struts action and an activity.
     * @param strutsAction, the struts action path.
     * @param activity, the activity the action is for.
     * @param useContext, if true prepends the server and context to the URL.
     */
    public static String strutsActionToURL(String strutsAction,
                                       Activity activity,
                                       boolean useContext)
    {
        String url = strutsAction;

        if (activity != null)
        {
            url = WebUtil.appendParameterToURL(url,
            		AttributeNames.PARAM_ACTIVITY_ID,
            		activity.getActivityId().toString());
        }
        if (useContext)
        {
            String lamsUrl = Configuration.get(ConfigurationKeys.SERVER_URL) + LEARNING;
            url = lamsUrl + url;
        }

        return url;
    }

    /**
     * Creates a Struts ActionForward for an action and activity.
     * @param strutsAction, the struts action
     * @param activity, activity that is being displayed
     * @param redirect, should the action be a client redirect
     * @return
     */
    protected ActionForward strutsActionToForward(String strutsAction,
                                                  Activity activity,
                                                  boolean redirect)
    {
        ActionForward actionForward;
        if (redirect)
        {
            String activityURL = strutsActionToURL(strutsAction,
                                                   activity,
                                                   false);
            actionForward = new RedirectingActionForward(activityURL);
            actionForward.setName(WebUtil.getStrutsForwardNameFromPath(strutsAction));
        }
        else
        {
            actionForward = new ForwardingActionForward(strutsAction);
            actionForward.setName(WebUtil.getStrutsForwardNameFromPath(strutsAction));

        }

        return actionForward;
    }

    /**
     * Calculate the activity url for progress view at learner side.
     * @param learner the learner who owns the progress data
     * @param activity the activity the learner want to view
     * @return the url for that tool.
     */
    public String calculateActivityURLForProgressView(Lesson lesson, User learner,
                                                      Activity activity)
    {

        if (activity.isToolActivity() || activity.isSystemToolActivity())
        {
            return WebUtil.convertToFullURL(getLearnerToolURL(lesson, ((ToolActivity) activity), learner));
        } else {
        	// fall back to the strategy for complex activities
        	return getActivityURL(activity);
        }
    }

    public void setToolService(ILamsCoreToolService toolService)
    {
        this.toolService = toolService;
    }

    public ActivityMappingStrategy getActivityMappingStrategy()
    {
        return activityMappingStrategy;
    }

    public void setActivityMappingStrategy(ActivityMappingStrategy activityMappingStrategy)
    {
        this.activityMappingStrategy = activityMappingStrategy;
    }
    
    public static String getCloseURL() {
    	String lamsUrl = Configuration.get(ConfigurationKeys.SERVER_URL) + LEARNING;
    	String closeUrl = lamsUrl + "/close.do";
    	
    	return closeUrl;
    }

}