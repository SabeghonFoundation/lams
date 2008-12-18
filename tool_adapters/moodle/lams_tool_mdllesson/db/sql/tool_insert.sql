-- CVS ID: $Id$
 
INSERT INTO lams_tool
(
tool_signature,
service_name,
tool_display_name,
description,
tool_identifier,
tool_version,
learning_library_id,
default_tool_content_id,
valid_flag,
grouping_support_type_id,
supports_run_offline_flag,
learner_url,
learner_preview_url,
learner_progress_url,
author_url,
monitor_url,
define_later_url,
export_pfolio_learner_url,
export_pfolio_class_url,
contribute_url,
moderation_url,
help_url,
admin_url,
language_file,
classpath_addition,
context_file,
create_date_time,
modified_date_time,
ext_lms_id,
supports_outputs
)
VALUES
(
'mdlesn10',
'mdlLessonService',
'MdlLesson',
'MdlLesson',
'mdlLesson',
'@tool_version@',
NULL,
NULL,
0,
2,
1,
'tool/mdlesn10/learning.do?mode=learner',
'tool/mdlesn10/learning.do?mode=author',
'tool/mdlesn10/learning.do?mode=teacher',
'tool/mdlesn10/authoring.do',
'tool/mdlesn10/monitoring.do',
'tool/mdlesn10/authoring.do?mode=teacher',
'tool/mdlesn10/exportPortfolio?mode=learner',
'tool/mdlesn10/exportPortfolio?mode=teacher',
'tool/mdlesn10/contribute.do',
'tool/mdlesn10/moderate.do',
'http://wiki.lamsfoundation.org/display/lamsdocs/mdlesn10',
'tool/mdlesn10/mdlesn10admin.do',
'org.lamsfoundation.lams.tool.mdlesn.ApplicationResources',
'lams-tool-mdlesn10.jar',
'/org/lamsfoundation/lams/tool/mdlesn/mdlLessonApplicationContext.xml',
NOW(),
NOW(),
'moodle',
true
)
