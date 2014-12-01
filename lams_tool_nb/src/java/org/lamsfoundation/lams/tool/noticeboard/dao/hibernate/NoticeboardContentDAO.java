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
package org.lamsfoundation.lams.tool.noticeboard.dao.hibernate;

import java.util.List;

import org.hibernate.FlushMode;
import org.lamsfoundation.lams.dao.hibernate.LAMSBaseDAO;
import org.lamsfoundation.lams.tool.noticeboard.NoticeboardContent;
import org.lamsfoundation.lams.tool.noticeboard.NoticeboardSession;
import org.lamsfoundation.lams.tool.noticeboard.dao.INoticeboardContentDAO;
import org.springframework.stereotype.Repository;

/**
 * @author mtruong
 * <p>Hibernate implementation for database access to Noticeboard content for the noticeboard tool.</p>
 */
@Repository
public class NoticeboardContentDAO extends LAMSBaseDAO implements INoticeboardContentDAO {
	
	private static final String FIND_NB_CONTENT = "from " + NoticeboardContent.class.getName() + " as nb where nb.nbContentId=?";
	
	
	private static final String LOAD_NB_BY_SESSION = "select nb from NoticeboardContent nb left join fetch "
        + "nb.nbSessions session where session.nbSessionId=:sessionId";

	
	/** @see org.lamsfoundation.lams.tool.noticeboard.dao.INoticeboardContentDAO#getNbContentByUID(java.lang.Long) */
	public NoticeboardContent getNbContentByUID(Long uid)
	{
		 return (NoticeboardContent) this.getSession()
         .get(NoticeboardContent.class, uid);
	}
	
	/** @see org.lamsfoundation.lams.tool.noticeboard.dao.INoticeboardContentDAO#findNbContentById(java.lang.Long) */
	public NoticeboardContent findNbContentById(Long nbContentId)
	{
	    String query = "from NoticeboardContent as nb where nb.nbContentId = ?";
		List content = doFind(query,nbContentId);
			
		if(content!=null && content.size() == 0)
		{			
			return null;
		}
		else
		{
			return (NoticeboardContent)content.get(0);
		}
	
	}
    	
	/** @see org.lamsfoundation.lams.tool.noticeboard.dao.INoticeboardContentDAO#getNbContentBySession(java.lang.Long) */
	public NoticeboardContent getNbContentBySession(final Long nbSessionId) {
		return (NoticeboardContent) getSession().createQuery(LOAD_NB_BY_SESSION)
				.setLong("sessionId", nbSessionId.longValue()).uniqueResult();
	}	
	
	/** @see org.lamsfoundation.lams.tool.noticeboard.dao.INoticeboardContentDAO#saveNbContent(org.lamsfoundation.lams.tool.noticeboard.NoticeboardContent) */
	public void saveNbContent(NoticeboardContent nbContent)
    {
    	this.getSession().save(nbContent);
    }
    
	/** @see org.lamsfoundation.lams.tool.noticeboard.dao.INoticeboardContentDAO#updateNbContent(org.lamsfoundation.lams.tool.noticeboard.NoticeboardContent) */
	public void updateNbContent(NoticeboardContent nbContent)
    {
    	this.getSession().update(nbContent);
    }

   
	/** @see org.lamsfoundation.lams.tool.noticeboard.dao.INoticeboardContentDAO#removeNoticeboard(java.lang.Long)*/
	public void removeNoticeboard(Long nbContentId)
    {       
       	if ( nbContentId != null) {
			//String query = "from org.lamsfoundation.lams.tool.noticeboard.NoticeboardContent as nb where nb.nbContentId=?";
			List list = getSessionFactory().getCurrentSession().createQuery(FIND_NB_CONTENT)
				.setLong(0,nbContentId.longValue())
				.list();
			
			if(list != null && list.size() > 0){
				NoticeboardContent nb = (NoticeboardContent) list.get(0);
				getSessionFactory().getCurrentSession().setFlushMode(FlushMode.AUTO);
				this.getSession().delete(nb);
				this.getSession().flush();
			}
		}
    }
    
	/** @see org.lamsfoundation.lams.tool.noticeboard.dao.INoticeboardContentDAO#removeNoticeboard(org.lamsfoundation.lams.tool.noticeboard.NoticeboardContent)*/
    public void removeNoticeboard(NoticeboardContent nbContent)
    {
    	removeNoticeboard(nbContent.getNbContentId());
    }
   
    /** @see org.lamsfoundation.lams.tool.noticeboard.dao.INoticeboardContentDAO#removeNbSessions(org.lamsfoundation.lams.tool.noticeboard.NoticeboardContent)*/
    public void removeNbSessions(NoticeboardContent nbContent)
    {
    	this.deleteAll(nbContent.getNbSessions());
    }
    
    /** @see org.lamsfoundation.lams.tool.noticeboard.dao.INoticeboardContentDAO#addNbSession(java.lang.Long, org.lamsfoundation.lams.tool.noticeboard.NoticeboardSession) */
    public void addNbSession(Long nbContentId, NoticeboardSession nbSession)
    {
        NoticeboardContent content = findNbContentById(nbContentId);
        nbSession.setNbContent(content);
        content.getNbSessions().add(nbSession);
        this.getSession().saveOrUpdate(nbSession);
        this.getSession().saveOrUpdate(content);        
    }

    @Override
    public void delete(Object object) {
    	getSession().delete(object);
    }
  
}
