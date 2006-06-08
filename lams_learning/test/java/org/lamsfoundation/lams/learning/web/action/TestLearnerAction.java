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
import org.lamsfoundation.lams.lesson.LearnerProgress;
import org.lamsfoundation.lams.web.util.AttributeNames;


/**
 * 
 * @author Jacky Fang
 * @since  2005-3-8
 * @version
 * 
 */
public class TestLearnerAction extends AbstractTestAction
{
    //---------------------------------------------------------------------
    // Instance variables
    //---------------------------------------------------------------------
	private static Logger log = Logger.getLogger(TestLearnerAction.class);
	
    private static final String TEST_USER_ID = "2";
    private static final String TEST_LESSON_ID = "2";
    private static final String TEST_ACTIVITY_ID = "27";
    
    //private static SessionBean joinedLessonBean = null;
    private static LearnerProgress testLearnerProgress = null;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        setConfigFile("/WEB-INF/struts/struts-config.xml");
        setRequestPathInfo("/learner.do");
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }


    /**
     * Constructor for TestLearnerAction.
     * @param testName
     */
    public TestLearnerAction(String testName)
    {
        super(testName);
    }

    public void testGetActiveLessons()
    {
        addRequestParameter("method", "getActiveLessons");

        actionPerform();

        verifyNoActionErrors();
    }
    
    public void testJoinLesson()
    {
        addRequestParameter("method", "joinLesson");
        addRequestParameter(AttributeNames.PARAM_LESSON_ID, TEST_LESSON_ID);
        
        actionPerform();
        
        verifyNoActionErrors();
        
        //joinedLessonBean = (SessionBean)httpSession.getAttribute(SessionBean.NAME);
        testLearnerProgress = (LearnerProgress)httpSession.getAttribute(ActivityAction.LEARNER_PROGRESS_REQUEST_ATTRIBUTE);

        //assertNotNull("verify the session bean",testLearnerProgress);
        assertNotNull("verify the learner progress",testLearnerProgress);
        assertEquals("verify the learner in the session bean",TEST_USER_ID,testLearnerProgress.getUser().getUserId().toString());
        assertEquals("verify the lesson in the session bean",TEST_LESSON_ID,testLearnerProgress.getLesson().getLessonId().toString());

    }

    public void testGetFlashProgressData()
    {
        httpSession.setAttribute(ActivityAction.LEARNER_PROGRESS_REQUEST_ATTRIBUTE,testLearnerProgress);
        addRequestParameter("method", "getFlashProgressData");
        addRequestParameter(AttributeNames.PARAM_LESSON_ID, TEST_LESSON_ID);
        
        actionPerform();
        verifyNoActionErrors();
    }
    
    public void testExitLesson()
    {
        addRequestParameter("method", "exitLesson");
        addRequestParameter(AttributeNames.PARAM_LESSON_ID, TEST_LESSON_ID);
        
        actionPerform();
        
        verifyForward("welcome");
        verifyTilesForward("welcome",".welcome");
        verifyNoActionErrors();
    }

    public void testGetLearnerActivityURL()
    {
        addRequestParameter("method", "getLearnerActivityURL");
        addRequestParameter(AttributeNames.PARAM_ACTIVITY_ID, TEST_ACTIVITY_ID);
        
        actionPerform();
        
        verifyNoActionErrors();
    }

 }
