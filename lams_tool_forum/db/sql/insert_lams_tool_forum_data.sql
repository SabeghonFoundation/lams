

insert into tl_lafrum11_forum (ID,TITLE, ALLOWANNOMITY, FORCEOFFLINE, LOCKWHENFINISHED, INSTRUCTIONS,
ONLINEINSTRUCTIONS, OFFLINEINSTRUCTIONS) VALUES (1, "TEST FORUM", false, false, false, "TEST INSTRUCTIONS", "TEST ONLINE INSTRUCTIONS", "TEST OFFLINE INSTRUCTIONS");

insert into tl_lafrum11_message (ID, SUBJECT, BODY, ISAUTHORED, ISANNONYMOUS, FORUM, PARENT) VALUES  (2, "TITLE", "BODY", true, false, 1, NULL) ;