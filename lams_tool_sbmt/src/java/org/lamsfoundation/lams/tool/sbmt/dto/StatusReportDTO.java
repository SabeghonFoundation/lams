/****************************************************************
 * Copyright (C) 2005 LAMS Foundation (http://lamsfoundation.org)
 * =============================================================
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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
package org.lamsfoundation.lams.tool.sbmt.dto;

import java.io.Serializable;

/**
 * @author Manpreet Minhas
 */
public class StatusReportDTO implements Serializable {
	
	private static final long serialVersionUID = 4915274448120747612L;
	private Long userID; 
	private String login;
	private String fullName;
	private Boolean unMarked;
	

	public StatusReportDTO(Integer userID,String login, String fullName, Boolean unMarked) {
		super();
		this.login = login;
		this.fullName = fullName;
		this.unMarked = unMarked;
		this.userID = new Long(userID.intValue());
	}
	/**
	 * @return Returns the fullName.
	 */
	public String getFullName() {
		return fullName;
	}
	/**
	 * @param fullName The fullName to set.
	 */
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	/**
	 * @return Returns the login.
	 */
	public String getLogin() {
		return login;
	}
	/**
	 * @param login The login to set.
	 */
	public void setLogin(String login) {
		this.login = login;
	}
	
	/**
	 * @return Returns the unMarked.
	 */
	public Boolean getUnMarked() {
		return unMarked;
	}
	/**
	 * @param unMarked The unMarked to set.
	 */
	public void setUnMarked(Boolean unMarked) {
		this.unMarked = unMarked;
	}
	
	/**
	 * @return Returns the userID.
	 */
	public Long getUserID() {
		return userID;
	}
	/**
	 * @param userID The userID to set.
	 */
	public void setUserID(Long userID) {
		this.userID = userID;
	}
}
