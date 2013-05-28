/***************************************************************************
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
 * ***********************************************************************/
/* $$Id$$ */
package org.lamsfoundation.lams.tool.mc.service;

import java.io.InputStream;
import java.util.List;

import org.lamsfoundation.lams.contentrepository.ITicket;
import org.lamsfoundation.lams.contentrepository.NodeKey;
import org.lamsfoundation.lams.contentrepository.RepositoryCheckedException;
import org.lamsfoundation.lams.lesson.Lesson;
import org.lamsfoundation.lams.notebook.model.NotebookEntry;
import org.lamsfoundation.lams.tool.IToolVO;
import org.lamsfoundation.lams.tool.ToolSessionExportOutputData;
import org.lamsfoundation.lams.tool.exception.DataMissingException;
import org.lamsfoundation.lams.tool.exception.SessionDataExistsException;
import org.lamsfoundation.lams.tool.exception.ToolException;
import org.lamsfoundation.lams.tool.mc.McApplicationException;
import org.lamsfoundation.lams.tool.mc.pojos.McContent;
import org.lamsfoundation.lams.tool.mc.pojos.McOptsContent;
import org.lamsfoundation.lams.tool.mc.pojos.McQueContent;
import org.lamsfoundation.lams.tool.mc.pojos.McQueUsr;
import org.lamsfoundation.lams.tool.mc.pojos.McSession;
import org.lamsfoundation.lams.tool.mc.pojos.McUploadedFile;
import org.lamsfoundation.lams.tool.mc.pojos.McUsrAttempt;
import org.lamsfoundation.lams.usermanagement.User;

/**
 * @author Ozgur Demirtas
 * 
 *         Interface that defines the contract that all MCQ service provider must follow.
 */
public interface IMcService {
    public void configureContentRepository() throws McApplicationException;

    public void createMc(McContent mcContent) throws McApplicationException;

    public McContent retrieveMc(Long toolContentId) throws McApplicationException;

    public void createMcQue(McQueContent mcQueContent) throws McApplicationException;

    public void updateMcQueContent(McQueContent mcQueContent) throws McApplicationException;

    public List retrieveMcQueContentsByToolContentId(long mcContentId) throws McApplicationException;

    public McQueContent getQuestionContentByDisplayOrder(final Long displayOrder, final Long mcContentUid)
	    throws McApplicationException;

    public void createMcSession(McSession mcSession) throws McApplicationException;

    public void createMcQueUsr(McQueUsr mcQueUsr) throws McApplicationException;

    public McQueUsr getMcUserBySession(final Long queUsrId, final Long mcSessionId) throws McApplicationException;

    public McQueUsr retrieveMcQueUsr(Long userId) throws McApplicationException;

    public void createMcUsrAttempt(McUsrAttempt mcUsrAttempt) throws McApplicationException;

    public void updateMcUsrAttempt(McUsrAttempt mcUsrAttempt) throws McApplicationException;

    public McQueContent retrieveMcQueContentByUID(Long uid) throws McApplicationException;

    public void removeMcQueContent(McQueContent mcQueContent) throws McApplicationException;

    public McQueContent getMcQueContentByUID(Long uid) throws McApplicationException;

    public void saveOrUpdateMcQueContent(McQueContent mcQueContent) throws McApplicationException;

    public void removeQuestionContentByMcUid(final Long mcContentUid) throws McApplicationException;

    public McOptsContent getMcOptionsContentByUID(Long uid) throws McApplicationException;

    public void resetAllQuestions(final Long mcContentUid) throws McApplicationException;

    public List refreshQuestionContent(final Long mcContentId) throws McApplicationException;

    public List getAllQuestionEntriesSorted(final long mcContentId) throws McApplicationException;

    public List findMcOptionUidsByQueId(Long mcQueContentId) throws McApplicationException;

    public McQueContent findMcQuestionContentByUid(Long uid) throws McApplicationException;

    public void removeMcOptionsContentByQueId(Long mcQueContentId) throws McApplicationException;

    public void removeMcOptionsContent(McOptsContent mcOptsContent);

    public McQueContent getQuestionContentByQuestionText(final String question, final Long mcContentUid);

    public List getPersistedSelectedOptions(Long mcQueContentId);

    public void removeMcQueContentByUID(Long uid) throws McApplicationException;

    public McQueUsr getMcUserByUID(Long uid) throws McApplicationException;

    public List getCorrectOption(Long mcQueContentId);

    public List<McQueContent> getAllQuestionEntries(final Long mcContentId) throws McApplicationException;

    public McSession retrieveMcSession(Long mcSessionId) throws McApplicationException;

    public McContent retrieveMcBySessionId(Long mcSessionId) throws McApplicationException;

    public void updateMc(McContent mc) throws McApplicationException;

    public void updateMcSession(McSession mcSession) throws McApplicationException;

    public void updateMcQueUsr(McQueUsr mcQueUsr) throws McApplicationException;

    public List populateCandidateAnswersDTO(Long mcQueContentId) throws McApplicationException;

    public McSession getMcSessionByUID(Long uid) throws McApplicationException;

    public List getLatestAttemptsForAUser(final Long queUserUid) throws McApplicationException;

    public void deleteMc(McContent mc) throws McApplicationException;

    public void deleteMcById(Long mcId) throws McApplicationException;

    public void removeAttempt(McUsrAttempt attempt) throws McApplicationException;

    public void deleteMcQueUsr(McQueUsr mcQueUsr) throws McApplicationException;

    public List findMcOptionsContentByQueId(Long mcQueContentId) throws McApplicationException;

    public void saveMcOptionsContent(McOptsContent mcOptsContent) throws McApplicationException;

    public McOptsContent getOptionContentByOptionText(final String option, final Long mcQueContentUid);

    public void updateMcOptionsContent(McOptsContent mcOptsContent) throws McApplicationException;

    public void deleteMcOptionsContent(McOptsContent mcOptsContent) throws McApplicationException;

    public void deleteMcOptionsContentByUID(Long uid) throws McApplicationException;

    public User getCurrentUserData(String username) throws McApplicationException;

    public Lesson getCurrentLesson(long lessonId) throws McApplicationException;

    public void saveMcContent(McContent mc) throws McApplicationException;

    public boolean studentActivityOccurredGlobal(McContent mcContent) throws McApplicationException;

    /**
     * <p>
     * gets all the attempts for one questions for one user in one tool session <code>queUsrId</code>, ordered by the
     * attempt id. If there is more than one option selected for a question, the attempts are "batched".
     * </p>
     * 
     * @param queUsrId
     * @return
     */
    public List<McUsrAttempt> getAllAttemptsForAUserForOneQuestionContentOrderByAttempt(Long queUsrUid,
	    Long mcQueContentId) throws McApplicationException;

    public List<McUsrAttempt> getLatestAttemptsForAUserForOneQuestionContent(Long queUsrUid, Long mcQueContentId)
	    throws McApplicationException;

    public int countIncompleteSession(McContent mc) throws McApplicationException;

    public boolean studentActivityOccurred(McContent mc) throws McApplicationException;

    public void copyToolContent(Long fromContentId, Long toContentId) throws ToolException;

    public void setAsForceCompleteSession(Long toolSessionId) throws McApplicationException;

    public void setAsForceComplete(Long userId) throws McApplicationException;

    public void setAsDefineLater(Long toolContentId, boolean value) throws DataMissingException, ToolException;

    public void setAsRunOffline(Long toolContentId, boolean value) throws DataMissingException, ToolException;

    public void removeToolContent(Long toolContentId, boolean removeSessionData) throws SessionDataExistsException,
	    ToolException;

    public boolean existsSession(Long toolSessionId);

    public void createToolSession(Long toolSessionId, String toolSessionName, Long toolContentId) throws ToolException;

    public void removeToolSession(Long toolSessionId) throws DataMissingException, ToolException;

    public String leaveToolSession(Long toolSessionId, Long learnerId) throws DataMissingException, ToolException;

    public ToolSessionExportOutputData exportToolSession(Long toolSessionId) throws DataMissingException, ToolException;

    public ToolSessionExportOutputData exportToolSession(List toolSessionIds) throws DataMissingException,
	    ToolException;

    public IToolVO getToolBySignature(String toolSignature) throws McApplicationException;

    public long getToolDefaultContentIdBySignature(String toolSignature) throws McApplicationException;

    public McQueContent getToolDefaultQuestionContent(long contentId) throws McApplicationException;

    public ITicket getRepositoryLoginTicket() throws McApplicationException;

    public void deleteFromRepository(Long uuid, Long versionID);

    public NodeKey uploadFileToRepository(InputStream stream, String fileName) throws McApplicationException;

    public InputStream downloadFile(Long uuid, Long versionID) throws McApplicationException;

    public void persistFile(String uuid, boolean isOnlineFile, String fileName, McContent mcContent)
	    throws McApplicationException;

    public McSession findMcSessionById(Long mcSessionId) throws McApplicationException;

    public List getNextAvailableDisplayOrder(final long mcContentId) throws McApplicationException;

    public NodeKey uploadFile(InputStream istream, String filename, String contentType, String fileType)
	    throws RepositoryCheckedException;

    public NodeKey copyFile(Long uuid) throws RepositoryCheckedException;

    public List findMcOptionCorrectByQueId(Long mcQueContentId) throws McApplicationException;

    public List findMcOptionNamesByQueId(Long mcQueContentId) throws McApplicationException;

    public Long createNotebookEntry(Long id, Integer idType, String signature, Integer userID, String entry);

    public NotebookEntry getEntry(Long id, Integer idType, String signature, Integer userID);

    public void updateEntry(NotebookEntry notebookEntry);

    public void persistFile(McContent content, McUploadedFile file) throws McApplicationException;

    public void removeFile(Long submissionId) throws McApplicationException;

    public List<McUploadedFile> retrieveMcUploadedFiles(McContent mcContent) throws McApplicationException;

    /**
     * Return the top, lowest and average mark for all learners for one particular tool session.
     * 
     * @param request
     * @return top mark, lowest mark, average mark in that order
     */
    public Integer[] getMarkStatistics(McSession mcSession);

    /**
     * Returns whether activity is grouped and therefore it is expected more than one tool session.
     * 
     * @param toolContentID
     * @return
     */
    boolean isGroupedActivity(long toolContentID);

}
