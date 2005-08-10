/*
 * Created on May 24, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.lamsfoundation.lams.tool.sbmt.dao.hibernate;

import java.util.List;

import net.sf.hibernate.FlushMode;
import net.sf.hibernate.Hibernate;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.type.Type;

import org.lamsfoundation.lams.learningdesign.dao.hibernate.BaseDAO;
import org.lamsfoundation.lams.tool.sbmt.SubmissionDetails;
import org.lamsfoundation.lams.tool.sbmt.SubmitFilesSession;
import org.lamsfoundation.lams.tool.sbmt.dao.ISubmissionDetailsDAO;
import org.springframework.orm.hibernate.HibernateCallback;

/**
 * @author Manpreet Minhas
 */
public class SubmissionDetailsDAO extends BaseDAO implements
		ISubmissionDetailsDAO {
	
	private static final String TABLENAME = "tl_lasbmt11_submission_details";
	
	private static final String FIND_BY_CONTENT_ID = "from " + TABLENAME + " in class " + SubmissionDetails.class.getName() +
													 " where content_id=? ORDER BY learner_id";
	
	private static final String FIND_BY_SESSION = "from " + TABLENAME +
													" in class " + SubmissionDetails.class.getName() +
													" where session_id=?";
	
	private static final String FIND_DISTINCT_USER = " select distinct learner.userID from SubmissionDetails details " +
													 ", Learner learner " +
													 " where details.submitFileSession =:sessionID " +
													 " and details.learner = learner.learnerID";


	/**
	 * (non-Javadoc)
	 * @see org.lamsfoundation.lams.tool.sbmt.dao.ISubmissionDetailsDAO#getSubmissionDetailsByID(java.lang.Long)
	 */
	public SubmissionDetails getSubmissionDetailsByID(Long submissionID) {
		return (SubmissionDetails) this.getHibernateTemplate().
								   get(SubmissionDetails.class, submissionID);
	}
	
	/**
	 * (non-Javadoc)
	 * @see org.lamsfoundation.lams.tool.sbmt.dao.ISubmissionDetailsDAO#getSubmissionDetailsByContentID(java.lang.Long)
	 */
	public List getSubmissionDetailsByContentID(Long contentID){
		return this.getHibernateTemplate().find(FIND_BY_CONTENT_ID,contentID);
	}
	
	public List getUsersForSession(final Long sessionID){			
		return (List) this.getHibernateTemplate().execute(new HibernateCallback(){
			public Object doInHibernate(Session session) throws HibernateException{
				return session.createQuery(FIND_DISTINCT_USER)
							  .setLong("sessionID",sessionID.longValue())
							  .list();
			}
		});		
	}

	/* (non-Javadoc)
	 * @see org.lamsfoundation.lams.tool.sbmt.dao.ISubmissionDetailsDAO#saveOrUpdate(org.lamsfoundation.lams.tool.sbmt.SubmitFilesSession)
	 */
	public void saveOrUpdate(SubmitFilesSession session) {
		
		this.getSession().setFlushMode(FlushMode.AUTO);
		this.getHibernateTemplate().saveOrUpdate(session);
		this.getHibernateTemplate().flush();
		this.getHibernateTemplate().clear();
		
	}
	/* (non-Javadoc)
	 * @see org.lamsfoundation.lams.tool.sbmt.dao.ISubmissionDetailsDAO#getSubmissionDetailsBySession(java.lang.Long)
	 */
	public List getSubmissionDetailsBySession(Long sessionID) {
		List list = this.getHibernateTemplate().find(FIND_BY_SESSION, 
				 new Object[]{sessionID},
				 new Type[]{Hibernate.LONG});
		return list;
	}
}
