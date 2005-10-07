/* ********************************************************************************
 *  Copyright Notice
 *  =================
 * This file contains propriety information of LAMS Foundation. 
 * Copying or reproduction with prior written permission is prohibited.
 * Copyright (c) 2004 
 * Created on 2004-12-23
 ******************************************************************************** */

package org.lamsfoundation.lams.tool.mc;

import java.util.HashSet;


/*
 * 
 * @author ozgurd
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

public class TestMcQueContent extends McDataAccessTestCase
{
	protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public TestMcQueContent(String name)
    {
        super(name);
    }
    
    
    public void testCreateMcQueContent()
    {
    	McContent mcContent = mcContentDAO.findMcContentById(new Long(2));
    	assertEquals("Check ids:", mcContent.getMcContentId() , new Long(2));
    	
    	
    	 McQueContent mcQueContent=  new McQueContent(TEST_QUE_ID1,
													 "What planet are you from?",
													 new Integer(444),
													 mcContent,
													 new HashSet(),
													 new HashSet()
    												);
    	
    	 mcQueContentDAO.saveOrUpdateMcQueContent(mcQueContent);
    	 
    	 McQueContent mcQueContent2=  new McQueContent(TEST_QUE_ID1,
				 "What is a good question?",
				 new Integer(555),
				 mcContent,
				 new HashSet(),
				 new HashSet()
				);

    	 mcQueContentDAO.saveOrUpdateMcQueContent(mcQueContent2);
    	 mcQueContentDAO.flush();
    }

    
    public void testSaveOrUpdateMcQueContent()
    {
    	McContent mcContent2 = mcContentDAO.findMcContentById(new Long(2));
    	
    	McQueContent mcQueContent2=  new McQueContent(TEST_QUE_ID1,
    												 "Where is the sky?",
    												 new Integer(777),
    												 mcContent2,
    												 new HashSet(),
    												 new HashSet()
    												);
    	
    	mcQueContentDAO.saveOrUpdateMcQueContent(mcQueContent2);
        	
    }
    
    
	
    public void testRetrieveMcQueContent()
    {
    	McQueContent mcQueContent= mcQueContentDAO.findMcQueContentById(new Long(20));
    	mcQueContent.setDisplayOrder(new Integer(88));
    	mcQueContentDAO.saveMcQueContent(mcQueContent);
    }
    
    public void testRemoveMcQueContentById()
    {
    	mcQueContentDAO.removeMcQueContentById(TEST_QUE_ID1);
    }
    
}