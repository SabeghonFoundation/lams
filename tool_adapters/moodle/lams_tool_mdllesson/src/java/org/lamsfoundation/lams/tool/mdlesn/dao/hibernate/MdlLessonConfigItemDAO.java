/****************************************************************
 * Copyright (C) 2005 LAMS Foundation (http://lamsfoundation.org)
 * =============================================================
 * License Information: http://lamsfoundation.org/licensing/lams/2.0/
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
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
package org.lamsfoundation.lams.tool.mdlesn.dao.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.lamsfoundation.lams.dao.hibernate.BaseDAO;
import org.lamsfoundation.lams.tool.mdlesn.dao.IMdlLessonConfigItemDAO;
import org.lamsfoundation.lams.tool.mdlesn.model.MdlLessonConfigItem;
import org.springframework.orm.hibernate3.HibernateCallback;

public class MdlLessonConfigItemDAO extends BaseDAO implements IMdlLessonConfigItemDAO {
    private static final String LOAD_CONFIG_ITEM_BY_KEY = "from MdlLessonConfigItem configuration"
	    + " where configuration.configKey=:key";

    public MdlLessonConfigItem getConfigItemByKey(final String configKey) {
	return (MdlLessonConfigItem) getHibernateTemplate().execute(new HibernateCallback() {
	    public Object doInHibernate(Session session) throws HibernateException {
		return session.createQuery(LOAD_CONFIG_ITEM_BY_KEY).setString("key", configKey).uniqueResult();
	    }
	});

    }

    public void saveOrUpdate(MdlLessonConfigItem mdlLessonConfigItem) {
	this.getHibernateTemplate().saveOrUpdate(mdlLessonConfigItem);
	this.getHibernateTemplate().flush();
    }
}
