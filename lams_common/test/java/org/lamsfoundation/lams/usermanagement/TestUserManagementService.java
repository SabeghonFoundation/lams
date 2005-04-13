/*
 * Created on Mar 22, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.lamsfoundation.lams.usermanagement;

import java.io.IOException;
import java.util.Date;
import org.lamsfoundation.lams.AbstractLamsTestCase;
import org.lamsfoundation.lams.usermanagement.dao.hibernate.AuthenticationMethodDAO;
import org.lamsfoundation.lams.usermanagement.dao.hibernate.OrganisationDAO;
import org.lamsfoundation.lams.usermanagement.dao.hibernate.OrganisationTypeDAO;
import org.lamsfoundation.lams.usermanagement.dao.hibernate.RoleDAO;
import org.lamsfoundation.lams.usermanagement.dao.hibernate.UserDAO;
import org.lamsfoundation.lams.usermanagement.service.UserManagementService;

/**
 * @author Manpreet Minhas
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestUserManagementService extends AbstractLamsTestCase {
	
	protected UserDAO userDAO;
	protected RoleDAO roleDAO;
	protected OrganisationDAO organisationDAO;
	protected OrganisationTypeDAO organisationTypeDAO;
	protected UserManagementService userManagementService;
	protected AuthenticationMethodDAO authenticationMethodDAO;
	
	public TestUserManagementService(String name){
		super(name);
	}
	protected void setUp()throws Exception{
		super.setUp();
		userDAO =(UserDAO)context.getBean("userDAO");
		organisationDAO =(OrganisationDAO)context.getBean("organisationDAO");
		organisationTypeDAO =(OrganisationTypeDAO)context.getBean("organisationTypeDAO");
		userManagementService = (UserManagementService)context.getBean("userManagementService");
		authenticationMethodDAO =(AuthenticationMethodDAO)context.getBean("authenticationMethodDAO");
		roleDAO = (RoleDAO)context.getBean("roleDAO");				
	}

	/* (non-Javadoc)
	 * @see org.lamsfoundation.lams.AbstractLamsTestCase#getContextConfigLocation()
	 */
	protected String[] getContextConfigLocation() {
		return new String[] {"applicationContext.xml","org/lamsfoundation/lams/learningdesign/learningDesignApplicationContext.xml"};
	}
	public void testSaveOrganisation(){
		Organisation organisation = new Organisation("Test Organisation",
													 "Test Organisation Description",													
													 new Date(),													 
													 organisationTypeDAO.getOrganisationTypeById(new Integer(1)));
		
		Integer organisationID = userManagementService.saveOrganisation(organisation,new Integer(1));		
		assertNotNull(organisationID);
	}
	public void saveUser(){
		User user = new User();
		user.setLogin("Monu");
		user.setPassword("Monu");
		user.setDisabledFlag(new Boolean(false));
		user.setCreateDate(new Date());
		user.setAuthenticationMethod(authenticationMethodDAO.getAuthenticationMethodById(new Integer(2)));
		user.setBaseOrganisation(organisationDAO.getOrganisationById(new Integer(1)));
		user.setUserOrganisationID(new Integer(1));
		assertNotNull(userManagementService.saveUser(user, new Integer(4)));	
		
	}	
	public void testMoveLearningDesign()throws IOException{
		String packet = userManagementService.moveLearningDesign(new Long(1),new Integer(4),new Integer(1));
		System.out.println(packet);
	}
	public void testGetOrganisationsForUserByRole()throws IOException{
		String packet = userManagementService.getWDDXForOrganisationsForUserByRole(new Integer(4),"AUTHOR");
		System.out.println(packet);
	}
	public void testGetUsersFromOrganisationByRole() throws IOException{
		String packet = userManagementService.getUsersFromOrganisationByRole(new Integer(4),"AUTHOR");
		System.out.println(packet);
	}

}
