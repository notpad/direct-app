/*
 * Copyright (C) 2010 TopCoder Inc., All Rights Reserved.
 */
package com.topcoder.direct.services.view.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.topcoder.shared.dataAccess.DataAccess;
import com.topcoder.shared.dataAccess.Request;
import com.topcoder.shared.dataAccess.resultSet.ResultSetContainer;
import com.topcoder.shared.util.DBMS;
import org.apache.struts2.ServletActionContext;

import com.opensymphony.xwork2.ActionContext;
import com.topcoder.direct.services.view.dto.contest.ContestBriefDTO;
import com.topcoder.direct.services.view.dto.contest.ContestStatsDTO;
import com.topcoder.direct.services.view.dto.project.ProjectBriefDTO;
import com.topcoder.project.phases.Phase;
import com.topcoder.project.service.ContestSaleData;
import com.topcoder.security.TCSubject;
import com.topcoder.service.facade.contest.CommonProjectContestData;
import com.topcoder.service.facade.contest.ContestServiceFacade;
import com.topcoder.service.project.SoftwareCompetition;
import com.topcoder.service.studio.PersistenceException;
import com.topcoder.shared.common.TCContext;

/**
 * <p>
 * Direct common utility class.
 * </p>
 * <p>
 * Version 1.1 (Direct Registrants List assembly) change notes:
 * <ul>
 * <li>Added {@link #getContestStats(TCSubject, long, boolean)} method.</li>
 * <li>Added {@link #getTCSubjectFromSession()} method.</li>
 * </ul>
 * </p>
 * <p>
 * Version 1.2 - Direct Launch Software Contests Assembly Change Note
 * <ul>
 * <li>Adds the new util function to get xml date from util date.</li>
 * </ul>
 * </p>
 * <p>
 * Version 1.3 - Edit Software Assembly Change Note
 * <ul>
 * <li>Adds the new util function to get date string.</li>
 * </ul>
 * <ul>
 * <li>Adds the new util function to get end date of software competition.</li>
 * </ul>
 * </p>
 *
 * @author BeBetter, TCSDEVELOPER
 * @version 1.3
 */
public final class DirectUtils {
    /**
     * Constant for date format.
     */
    public static final String DATE_FORMAT = "MM/dd/yyyy";

    /**
     * Draft status list.
     */
    public final static List<String> DRAFT_STATUS = Arrays
        .asList("Draft", "Unactive - Not Yet Published", "Inactive");

    /**
     * Scheduled status list.
     */
    public final static List<String> SCHEDULED_STATUS = Arrays.asList("Scheduled");

    /**
     * Active status list.
     */
    public final static List<String> ACTIVE_STATUS = Arrays.asList("Active - Public", "Active", "Registration",
        "Submission", "Screening", "Review", "Appeals", "Appeals Response", "Aggregation", "Aggregation Review",
        "Final Fix", "Final Review", "Approval", "Action Required", "In Danger", "Extended");

    /**
     * Finished status list.
     */
    public final static List<String> FINISHED_STATUS = Arrays.asList("Completed", "No Winner Chosen",
        "Insufficient Submissions - ReRun Possible", "Insufficient Submissions", "Abandoned", "Inactive - Removed",
        "Cancelled - Failed Review", "Cancelled - Failed Screening", "Cancelled - Zero Submissions",
        "Cancelled - Winner Unresponsive", "Cancelled - Client Request", "Cancelled - Requirements Infeasible",
        "Cancelled - Zero Registrations");

    /**
     * <p>
     * Default Constructor.
     * </p>
     */
    private DirectUtils() {

    }

    /**
     * Gets date from date string.
     *
     * @param dateString the date string. see <code>DATE_FORMAT</code> for the format.
     * @return the <code>Date</code> object. it might be null.
     */
    public static Date getDate(String dateString) {
        if (dateString == null || dateString.trim().length() == 0) {
            return null;
        }

        try {
            return new SimpleDateFormat(DATE_FORMAT).parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Gets date from the <code>XMLGregorianCalendar</code> object.
     *
     * @param calendarDate <code>XMLGregorianCalendar</code> object.
     * @return the <code>Date</code> object
     */
    public static Date getDate(XMLGregorianCalendar calendarDate) {
        if (calendarDate == null) {
            return null;
        }

        return calendarDate.toGregorianCalendar().getTime();
    }

    /**
     * Gets date without time portion.
     *
     * @param date the original date
     * @return the date without time information
     */
    public static Date getDateWithoutTime(Date date) {
        DateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        try {
            return formatter.parse(formatter.format(date));
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Gets <code>ContestServiceFacade</code> service bean.
     *
     * @return the <code>ContestServiceFacade</code> service bean
     * @throws NamingException if any naming exception occurs
     */
    public static ContestServiceFacade getContestServiceFacade() throws NamingException {
        Context context = TCContext.getContext(DirectProperties.CONTEST_SERVICE_FACADE_CONTEXT_FACTORY,
            DirectProperties.CONTEST_SERVICE_FACADE_PROVIDER_URL);
        return (ContestServiceFacade) context.lookup(DirectProperties.CONTEST_SERVICE_FACADE_JNDI_NAME);
    }

    /**
     * <p>
     * Gets the servlet request.
     * </p>
     *
     * @return the servlet request
     */
    public static HttpServletRequest getServletRequest() {
        return (HttpServletRequest) ActionContext.getContext().get(ServletActionContext.HTTP_REQUEST);
    }

    /**
     * <p>
     * Gets the statistics for the specified contest.
     * </p>
     *
     * @param currentUser a <code>TCSubject</code> representing the current user.
     * @param contestId a <code>long</code> providing the ID of a contest.
     * @param isStudio a flag indicates whether the contest to get is a studio contest.
     * @return a <code>ContestStatsDTO</code> providing the statistics for specified contest.
     * @throws Exception if an unexpected error occurs while accessing the persistent data store.
     * @since 1.1
     */
    public static ContestStatsDTO getContestStats(TCSubject currentUser,
                                                  long contestId, boolean isStudio) throws Exception {

        DataAccess dataAccessor = new DataAccess(DBMS.TCS_OLTP_DATASOURCE_NAME);
        Request request = new Request();
        request.setContentHandle("direct_contest_stats");
        request.setProperty("ct", String.valueOf(contestId));
        request.setProperty("uid", String.valueOf(currentUser.getUserId()));

        final ResultSetContainer resultContainer = dataAccessor.getData(request).get("direct_contest_stats");
        final int recordNum = resultContainer.size();

        if (recordNum == 0) {
            // return null, if no record is found
            return null;
        }

        int recordIndex = 0;

        if (recordNum > 1) {
            // there are two records, find out the correct one
            String[] types = new String[2];
            types[0] = resultContainer.getStringItem(0, "type").trim();
            types[1] = resultContainer.getStringItem(1, "type").trim();

            if (isStudio) {
                recordIndex = types[0].equals("Studio") ? 0 : 1;
            } else {
                recordIndex = types[0].equals("Studio") ? 1 : 0;
            }

        } else if (recordNum == 1) {

            // get the contest type first
            String contestType = resultContainer.getStringItem(0, "type").trim();

            if (isStudio && (!contestType.equals("Studio"))) {
                // contest type is not studio when param indicates the studio, return null
                return null;
            }

            if (!isStudio && contestType.equals("Studio")) {
                // contest type is studio when param indicate sw, return null
                return null;
            }
        }

        ProjectBriefDTO project = new ProjectBriefDTO();
        project.setId(resultContainer.getLongItem(recordIndex, "project_id"));
        project.setName(resultContainer.getStringItem(recordIndex, "project_name"));

        ContestBriefDTO contest = new ContestBriefDTO();
        contest.setId(resultContainer.getLongItem(recordIndex, "contest_id"));
        contest.setTitle(resultContainer.getStringItem(recordIndex, "contest_name"));
        contest.setProject(project);

        ContestStatsDTO dto = new ContestStatsDTO();
        dto.setEndTime(resultContainer.getTimestampItem(recordIndex, "end_date"));
        dto.setStartTime(resultContainer.getTimestampItem(recordIndex, "start_date"));
        dto.setSubmissionsNumber(resultContainer.getIntItem(recordIndex, "number_of_submission"));
        dto.setRegistrantsNumber(resultContainer.getIntItem(recordIndex, "number_of_registration"));
        dto.setForumPostsNumber(resultContainer.getIntItem(recordIndex, "number_of_forum"));
        long forumId = -1;
        try
            {
        if (resultContainer.getStringItem(recordIndex, "forum_id") != null
                    && !resultContainer.getStringItem(recordIndex, "forum_id").equals(""))
            forumId = Long.parseLong(resultContainer.getStringItem(recordIndex, "forum_id"));
            dto.setForumId(forumId);
        }
        catch (NumberFormatException ne)
        {
        // ignore
        }
        

        dto.setContest(contest);
        dto.setIsStudio(isStudio);
        return dto;
    }

    /**
     * <p>
     * Gets the TCSubject instance from session.
     * </p>
     *
     * @return the TCSubject instance from session.
     * @since 1.1
     */
    public static TCSubject getTCSubjectFromSession() {
        HttpServletRequest request = getServletRequest();
        if (request == null) {
            return null;
        }
        return new SessionData(request.getSession()).getCurrentUser();
    }

    /**
     * <p>
     * Creates the <code>XMLGregorianCalendar</code> from the given date.
     * </p>
     *
     * @param date the date
     * @return the created XMLGregorianCalendar instance
     * @throws DatatypeConfigurationException if fail to create the XMLGregorianCalendar instance
     */
    public static XMLGregorianCalendar newXMLGregorianCalendar(Date date) throws DatatypeConfigurationException {
        if (date == null) {
            date = new Date();
        }
        DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();

        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(date);

        return datatypeFactory.newXMLGregorianCalendar(gc);
    }

    /**
     * <p>
     * Gets the end date of software competition.
     * </p>
     *
     * @param softwareCompetition the software competition
     * @return the end date
     */
    public static Date getEndDate(SoftwareCompetition softwareCompetition) {
        if (softwareCompetition == null || softwareCompetition.getProjectPhases() == null) {
            return null;
        }

        Date endDate = null;
        for (Phase phase : softwareCompetition.getProjectPhases().getPhases()) {
            Date phaseEnd = (phase.getActualEndDate() != null) ? phase.getActualEndDate() : phase
                .getScheduledEndDate();
            if (endDate == null || phaseEnd.after(endDate)) {
                endDate = phaseEnd;
            }
        }
        return endDate;
    }

    /**
     * <p>
     * Gets the paid fee.
     * </p>
     *
     * @param softwareCompetition the software competition
     * @return the paid fee
     */
    public static double getPaidFee(SoftwareCompetition softwareCompetition) {
        if (softwareCompetition == null || softwareCompetition.getProjectData() == null) {
            return 0;
        }

        double pastPayment = 0;

        List<ContestSaleData> sales = softwareCompetition.getProjectData().getContestSales();
        if (sales != null) {
            for (ContestSaleData sale : sales) {
                pastPayment += sale.getPrice();
            }
        }

        return pastPayment;
    }

    /**
     * <p>
     * Gets date string from xml date.
     * </p>
     *
     * @param date the date object
     * @return the date string
     */
    public static String getDateString(Date date) {
        if (date == null) {
            return null;
        }
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        return dateFormat.format(date);
    }
}
