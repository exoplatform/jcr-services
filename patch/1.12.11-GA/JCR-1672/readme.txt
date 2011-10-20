Summary

    * Status: Group parent id is null in the GroupHandlerImpl#addChild(Session, Group, Group, boolean)
    * CCP Issue: N/A. Product Jira Issue: JCR-1672.
    * Complexity: Low

The Proposal
Problem description

What is the problem to fix?
The error during server startup:
05.09.2011 13:31:44 *ERROR* [http-8080-1] DriveCmisRegistry: Unable get root folder id with path '/Groups/platform/administrators'.  (DriveCmisRegistry.java, line 199)

Fix description
Problem analysis
* The group parent id is null in the GroupHandlerImpl#addChild(Session, Group, Group, boolean)

How is the problem fixed?
* Push in events the same entity what was persisted.

Patch file: JCR-1672.patch

Tests to perform

Reproduction test

    * No

Tests performed at DevLevel

    * Functional testing of jcr-services project

Tests performed at QA/Support Level
*
Documentation changes

Documentation changes:

    * No

Configuration changes

Configuration changes:

    * No

Will previous configuration continue to work?

    * Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

    * No

Is there a performance risk/cost?

    * No

Validation (PM/Support/QA)

PM Comment
* Patch approved

Support Comment
* Patch validated

QA Feedbacks
*
