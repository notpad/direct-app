/*
 * Copyright (C) 2010 TopCoder Inc., All Rights Reserved.
 */
package com.topcoder.direct.services.view.action.contest.launch;

import java.util.List;
import java.util.Collections;
import java.util.Comparator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts2.ServletActionContext;

import com.topcoder.direct.services.exception.DirectException;
import com.topcoder.direct.services.view.dto.CoPilotStatsDTO;
import com.topcoder.direct.services.view.dto.UserProjectsDTO;
import com.topcoder.direct.services.view.dto.contest.ContestBriefDTO;
import com.topcoder.direct.services.view.dto.contest.ContestDTO;
import com.topcoder.direct.services.view.dto.contest.ContestDetailsDTO;
import com.topcoder.direct.services.view.dto.contest.ContestStatsDTO;
import com.topcoder.direct.services.view.dto.contest.ContestStatus;
import com.topcoder.direct.services.view.dto.contest.ContestType;
import com.topcoder.direct.services.view.dto.project.ProjectBriefDTO;
import com.topcoder.direct.services.view.util.DataProvider;
import com.topcoder.direct.services.view.util.DirectUtils;
import com.topcoder.direct.services.view.util.SessionData;
import com.topcoder.service.facade.contest.ContestServiceFacade;
import com.topcoder.service.project.ProjectData;
import com.topcoder.service.project.SoftwareCompetition;
import com.topcoder.service.project.StudioCompetition;
import com.topcoder.shared.dataAccess.DataAccess;
import com.topcoder.shared.dataAccess.Request;
import com.topcoder.shared.dataAccess.resultSet.ResultSetContainer;
import com.topcoder.shared.util.DBMS;
import com.topcoder.web.common.CachedDataAccess;
import com.topcoder.web.common.cache.MaxAge;
import com.topcoder.security.TCSubject;
import com.topcoder.service.project.CompetitionPrize;
import com.topcoder.service.studio.ContestStatusData;

/**
 * <p>
 * This action will get a contest with the given id. This action requires <code>projectId</code> or
 * <code>contestId</code> parameter (mutual exclusive) be present for each call.
 * </p>
 * <p>
 * <b>Thread Safety</b>: In <b>Struts 2</b> framework, the action is constructed for every request so the thread
 * safety is not required (instead in Struts 1 the thread safety is required because the action instances are reused).
 * This class is mutable and stateful: it's not thread safe.
 * </p>
 * <p>
 * Version 1.1 - Direct - View/Edit/Activate Studio Contests Assembly Change Note
 * <ul>
 * <li>Adds the legacy code to show other parts of the contest detail page.</li>
 * <li>Preserves the studio competition to fill the details later.</li>
 * </ul>
 * </p>
 * <p>
 * Version 1.2 - View/Edit/Activate Software Contests v1.0 Assembly Change Note
 * <ul>
 * <li>Preserves the software competition to fill the details later.</li>
 * </ul>
 * </p>
 *
 * @author fabrizyo, FireIce, TCSDEVELOPER
 * @version 1.2
 */
public class GetContestAction extends ContestAction {
    /**
     * <p>
     * Represents the unique serial version id.
     * </p>
     */
    private static final long serialVersionUID = 7980735050638514625L;

    /**
     * <p>
     * This is the id of project of contest.
     * </p>
     * <p>
     * It's used to retrieve the software competition. It can be 0 (it means not present) or greater than 0 if it's
     * present. It's changed by the setter and returned by the getter.
     * </p>
     */
    private long projectId;

    /**
     * <p>
     * This is the id of contest.
     * </p>
     * <p>
     * It's used to retrieve the studio competition. It can be 0 (it means not present) or greater than 0 if it's
     * present. It's changed by the setter and returned by the getter.
     * </p>
     */
    private long contestId;

    /**
     * <p>
     * view data. It is copied from old details page to preserve some portion of the existing page.
     * </p>
     */
    private ContestDetailsDTO viewData;

    /**
     * <p>
     * Session data. It is copied from old details page to preserve some portion of the existing page.
     * </p>
     */
    private SessionData sessionData;

    /**
     * <p>
     * Preserve the retrieved contest.
     * </p>
     */
    private StudioCompetition studioCompetition;

    /**
     * <p>
     * <code>softwareCompetition</code> to hold the software competition.
     * </p>
     */
    private SoftwareCompetition softwareCompetition;

    /**
     * <p>
     * Creates a <code>GetContestAction</code> instance.
     * </p>
     */
    public GetContestAction() {
    }

    /**
     * <p>
     * Executes the action.
     * </p>
     * <p>
     * The returned software or studio contest will be available as result.
     * </p>
     *
     * @throws IllegalStateException if the contest service facade is not set.
     * @throws Exception if any other error occurs
     * @see ContestServiceFacade#getContest(com.topcoder.security.TCSubject, long)
     * @see ContestServiceFacade#getSoftwareContestByProjectId(com.topcoder.security.TCSubject, long)
     */
    protected void executeAction() throws Exception {
        ContestServiceFacade contestServiceFacade = getContestServiceFacade();

        if (null == contestServiceFacade) {
            throw new IllegalStateException("The contest service facade is not initialized.");
        }

        if (contestId <= 0 && projectId <= 0) {
            throw new DirectException("contestId and projectId both less than 0 or not defined.");
        }

        TCSubject currentUser = DirectStrutsActionsHelper.getTCSubjectFromSession();

        if (contestId > 0) {
            studioCompetition = contestServiceFacade.getContest(DirectStrutsActionsHelper.getTCSubjectFromSession(),
                contestId);
            setResult(studioCompetition);

            // Set contest stats
            ContestStatsDTO contestStats = DirectUtils.getContestStats(currentUser, contestId, true);
			List<CompetitionPrize> coll = studioCompetition.getPrizes();
			Collections.sort(coll, new PrizeSortByPlace());
			contestStats.setPrizes(coll);
			contestStats.setAdminFees(studioCompetition.getAdminFee());
            if (studioCompetition.getContestData().getMilestonePrizeData() != null)
            {
                contestStats.setMilestonePrizes(studioCompetition.getContestData().getMilestonePrizeData());
            }
			
            if (studioCompetition.getContestData().getPayments() != null && studioCompetition.getContestData().getPayments().size() > 0)
            {
                contestStats.setPaymentReferenceId(studioCompetition.getContestData().getPayments().get(0).getPaymentReferenceId());
            }
			
			contestStats.setForumId(studioCompetition.getContestData().getForumId());
            getViewData().setContestStats(contestStats);

        } else {
            softwareCompetition = contestServiceFacade.getSoftwareContestByProjectId(DirectStrutsActionsHelper
                .getTCSubjectFromSession(), projectId);
            setResult(softwareCompetition);

            // Set contest stats
            ContestStatsDTO contestStats = DirectUtils.getContestStats(currentUser, projectId, false);
            getViewData().setContestStats(contestStats);
        }
    }

    /**
     * <p>
     * Gets the project id.
     * </p>
     *
     * @return the project id
     */
    public long getProjectId() {
        return projectId;
    }

    /**
     * <p>
     * Sets the project id.
     * </p>
     * <p>
     * Don't perform argument checking by the usual exception.
     * </p>
     *
     * @param projectId the project id to set
     */
    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    /**
     * <p>
     * Gets the contest id.
     * </p>
     *
     * @return the contest id
     */
    public long getContestId() {
        return contestId;
    }

    /**
     * <p>
     * Sets the contest id.
     * </p>
     * <p>
     * Don't perform argument checking by the usual exception.
     * </p>
     *
     * @param contestId the contest id to set
     */
    public void setContestId(long contestId) {
        this.contestId = contestId;
    }

    /**
     * <p>
     * Determines if it is software contest or not.
     * </p>
     *
     * @return true if it is software contest
     */
    public boolean isSoftware() {
        return (softwareCompetition != null);
    }

    /**
     * <p>
     * Gets the view data.
     * </p>
     *
     * @return the view data
     * @throws Exception if any error occurs
     */
    public ContestDetailsDTO getViewData() throws Exception {

        if (viewData == null) {
            viewData = new ContestDetailsDTO();

            // real data
            ContestStatsDTO contestStats = new ContestStatsDTO();
            ContestBriefDTO contest = new ContestBriefDTO();
            ProjectBriefDTO contestProject = new ProjectBriefDTO();
			
            if (studioCompetition != null) {
                contestProject.setName(studioCompetition.getContestData().getTcDirectProjectName());
                contest.setId(studioCompetition.getContestData().getContestId());
                contest.setTitle(studioCompetition.getContestData().getName());
				contest.setProject(contestProject);
            }
            if (softwareCompetition != null) {
                contest.setId(softwareCompetition.getProjectHeader().getId());
                contest.setTitle(softwareCompetition.getProjectHeader().getProperty("Project Name"));
            }
            contestStats.setContest(contest);
            fillContestStats(contestStats);
            viewData.setContestStats(contestStats);

            final long testContestId = 4;
            ContestDTO contestDTO = DataProvider.getContest(testContestId);
			if (studioCompetition != null) {
				contestDTO.setContestType(ContestType.forIdAndFlag(studioCompetition.getContestData().getContestTypeId(),true));
			}
            viewData.setContest(contestDTO);

            // project

            // right side
            List<ProjectBriefDTO> projects = DataProvider.getUserProjects(getSessionData().getCurrentUserId());
            UserProjectsDTO userProjectsDTO = new UserProjectsDTO();
            userProjectsDTO.setProjects(projects);
            viewData.setUserProjects(userProjectsDTO);
        }

        return viewData;
    }
	public class PrizeSortByPlace implements Comparator<CompetitionPrize>{
		public int compare(CompetitionPrize o1, CompetitionPrize o2) {
			if (o1.getPlace() > o2.getPlace())
				return 1;
			else if(o1.getPlace() < o2.getPlace())
				return -1;
			else
				return 0;
		}
	}

    private void fillContestStats(ContestStatsDTO contestStats) throws Exception {

        DataAccess dataAccessor = new DataAccess(DBMS.TCS_OLTP_DATASOURCE_NAME);
        Request request = new Request();
        request.setContentHandle("direct_contest_stats");
        request.setProperty("ct", String.valueOf(contestStats.getContest().getId()));
        request.setProperty("uid", String.valueOf(getSessionData().getCurrentUserId()));

        final ResultSetContainer resultContainer = dataAccessor.getData(request).get("direct_contest_stats");
        final int recordNum = resultContainer.size();

        int recordIndex = 0;

        if (recordNum == 0) {
            // no record, directly return
            return;
        } else if (recordNum == 2) {
            // two records, this indicates there is one studio record and one sw record for the same contest id
            // and the first record is studio, the second is sw
            recordIndex = this.studioCompetition == null ? 1 : 0;
        }


        contestStats.setRegistrantsNumber(resultContainer.getIntItem(recordIndex, "number_of_registration"));
        contestStats.setSubmissionsNumber(resultContainer.getIntItem(recordIndex, "number_of_submission"));
        contestStats.setForumPostsNumber(resultContainer.getIntItem(recordIndex, "number_of_forum"));
        long forumId = -1;
        try
            {
        if (resultContainer.getStringItem(recordIndex, "forum_id") != null
                    && !resultContainer.getStringItem(recordIndex, "forum_id").equals(""))
            forumId = Long.parseLong(resultContainer.getStringItem(recordIndex, "forum_id"));
            contestStats.setForumId(forumId);
        }
        catch (NumberFormatException ne)
        {
        // ignore
        }
		
		contestStats.setStartTime(resultContainer.getTimestampItem(recordIndex, "start_date"));
		contestStats.setEndTime(resultContainer.getTimestampItem(recordIndex, "end_date"));

    }

    /**
     * <p>
     * Gets the session data.
     * </p>
     *
     * @return the session data
     * @throws Exception if any error occurs
     */
    public SessionData getSessionData() throws Exception {
        if (sessionData == null) {
            HttpServletRequest request = ServletActionContext.getRequest();

            HttpSession session = request.getSession(false);
            if (session != null) {
                sessionData = new SessionData(session);
                ProjectBriefDTO project = new ProjectBriefDTO();
                if (studioCompetition != null) {
                    project.setId(studioCompetition.getContestData().getContestId());
                    project.setName(studioCompetition.getContestData().getTcDirectProjectName());
                }
                if (softwareCompetition != null) {
                    project.setId(softwareCompetition.getProjectHeader().getId());
                    project.setName(getProjectName(softwareCompetition.getProjectHeader().getTcDirectProjectId()));

                }
                sessionData.setCurrentProjectContext(project);
            }
        }
        return sessionData;
    }

    /**
     * <p>
     * Gets project name. NOTE: it is fixing some bug which software competition project header is missing project
     * name population.
     * </p>
     *
     * @param projectId client project id
     * @return the project name. It could be null if no match is found.
     * @throws Exception if any error occurs
     */
    private String getProjectName(long projectId) throws Exception {
        try {
            for (ProjectData project : getProjects()) {
                if (projectId == project.getProjectId()) {
                    return project.getName();
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
