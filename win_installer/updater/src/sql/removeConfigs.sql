-- This file removes any configs left by the dump so the dump can be used
-- directly without the need to remove anything (apart from unused users)

-- Remove configs 
update lams_configuration set config_value="" where config_key="STMPServer";
update lams_configuration set config_value="" where config_key="XmppPassword";
update lams_configuration set config_value="ldap://192.158.1.1" where config_key="LDAPProvierURL";

-- From 2.2 onwards. Remove gmap key
update tl_lagmap10_configuration set config_value="" where config_key="GmapKey";

-- From 2.2 onwards. Ensure spreadsheet is set to disabled
update lams_learning_library set valid_flag=0 where title="SpreadSheet";

