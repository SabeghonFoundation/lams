package org.lamsfoundation.lams.learning.discussion.dao.hibernate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.lamsfoundation.lams.dao.hibernate.LAMSBaseDAO;
import org.lamsfoundation.lams.learning.discussion.dao.IDiscussionSentimentDAO;
import org.lamsfoundation.lams.learning.discussion.model.DiscussionSentimentVote;

public class DiscussionSentimentDAO extends LAMSBaseDAO implements IDiscussionSentimentDAO {
    private static final String FIND_ACTIVE_DISCUSSION = "FROM " + DiscussionSentimentVote.class.getName()
	    + " WHERE lessonId = :lessonId AND userId IS NULL";

    @Override
    public DiscussionSentimentVote getActiveDiscussion(long lessonId) {
	return getSession().createQuery(FIND_ACTIVE_DISCUSSION, DiscussionSentimentVote.class)
		.setParameter("lessonId", lessonId).uniqueResult();
    }

    @Override
    public Map<Integer, Long> getDiscussionAggregatedVotes(long lessonId, long toolContentId, Long burningQuestionUid) {
	Map<String, Object> properties = new HashMap<>();
	properties.put("lessonId", lessonId);
	properties.put("toolContentId", toolContentId);
	if (burningQuestionUid != null) {
	    properties.put("burningQuestionUid", burningQuestionUid);
	}
	List<DiscussionSentimentVote> votes = findByProperties(DiscussionSentimentVote.class, properties);
	return votes.stream().filter(v -> v.getUserId() != null).collect(
		Collectors.groupingBy(DiscussionSentimentVote::getSelectedOption, TreeMap::new, Collectors.counting()));
    }
}