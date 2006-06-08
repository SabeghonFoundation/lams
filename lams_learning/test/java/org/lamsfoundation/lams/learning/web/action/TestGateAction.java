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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 * 
 * http://www.gnu.org/licenses/gpl.txt
 * ****************************************************************
 */

/* $$Id$$ */	
package org.lamsfoundation.lams.learning.web.action;

import org.apache.log4j.Logger;
import org.lamsfoundation.lams.learning.service.ILearnerService;
import org.lamsfoundation.lams.learning.web.util.LearningWebUtil;
import org.lamsfoundation.lams.learningdesign.Activity;
import org.lamsfoundation.lams.lesson.LearnerProgress;
import org.lamsfoundation.lams.web.util.AttributeNames;


/**
 * 
 * @author Jacky Fang
 * @since  2005-4-7
 * @version 1.1
 * 
 */
public class TestGateAction extends AbstractTestAction
{
    //---------------------------------------------------------------------
    // Instance variables
    //---------------------------------------------------------------------
	private static Logger log = Logger.getLogger(TestGateAction.class);

    private static final String TEST_LERNER_PROGRESS_ID = "1";
    private static final String TEST_LEARNER_ID = "2";
    private static final String TEST_LESSON_ID = "2";
    
    private ILearnerService learnerService;

    private static final String TEST_GATE_ACTIVITY_ID = "31";
    /**
     * Constructor for TestGateAction.
     * @param testName
     */
    public TestGateAction(String testName)
    {
        super(testName);
    }
    /*
     * @see AbstractLamsStrutsTestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        setConfigFile("/WEB-INF/struts/struts-config.xml");
        setRequestPathInfo("/gate.do");
        
        learnerService =  (ILearnerService)this.wac.getBean("learnerService");
    }

    /*
     * @see AbstractLamsStrutsTestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    public void testKnockClosedGate()
    {
        addRequestParameter("method", "knockGate");
        addRequestParameter(LearningWebUtil.PARAM_PROGRESS_ID,TEST_LERNER_PROGRESS_ID);
        addRequestParameter(AttributeNames.PARAM_ACTIVITY_ID,TEST_GATE_ACTIVITY_ID);
        addRequestParameter(AttributeNames.PARAM_LESSON_ID, TEST_LESSON_ID);
        
        initializeLearnerProgress();
        actionPerform();
        
        verifyNoActionErrors();
        verifyTilesForward("waiting",".gateWaiting");
    }

    public void testKnockOpenGate()
    {
        addRequestParameter("method", "knockGate");
        addRequestParameter(LearningWebUtil.PARAM_PROGRESS_ID,TEST_LERNER_PROGRESS_ID);
        addRequestParameter(AttributeNames.PARAM_ACTIVITY_ID,TEST_GATE_ACTIVITY_ID);
        addRequestParameter(AttributeNames.PARAM_LESSON_ID, TEST_LESSON_ID);
        
        initializeLearnerProgress();
        actionPerform();
        
        verifyNoActionErrors();
        
    }
    /**
     * 
     */
    private void initializeLearnerProgress()
    {
        Activity activity = LearningWebUtil.getActivityFromRequest(request,learnerService);
        LearnerProgress learnerProgress = LearningWebUtil.getLearnerProgressByID(request,context);
        learnerProgress.setNextActivity(activity);
        httpSession.setAttribute(ActivityAction.LEARNER_PROGRESS_REQUEST_ATTRIBUTE,
                                 learnerProgress);
    }
}
