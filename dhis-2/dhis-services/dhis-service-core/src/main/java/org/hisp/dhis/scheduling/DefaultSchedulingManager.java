package org.hisp.dhis.scheduling;

/*
 * Copyright (c) 2004-2017, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.hisp.dhis.scheduling.Configuration.JobConfiguration;
import org.hisp.dhis.system.scheduling.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * Cron refers to the cron expression used for scheduling. Key refers to the key
 * identifying the scheduled jobs.
 * 
 * @author Henning Håkonsen
 */
public class DefaultSchedulingManager
    implements SchedulingManager
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private Scheduler scheduler;

    @Autowired
    public void setScheduler( Scheduler scheduler )
    {
        this.scheduler = scheduler;
    }

    private Map<JobType, Job> jobMap = new HashMap<JobType, Job>();

    @Autowired
    private List<Job> jobs;

    @PostConstruct
    public void init( )
    {
        jobs.forEach( job -> jobMap.put( job.getJobType(), job ) );
    }

    // -------------------------------------------------------------------------
    // SchedulingManager implementation
    // -------------------------------------------------------------------------

    @Override
    public void scheduleJob( JobConfiguration jobConfiguration )
    {
        scheduler.scheduleJob( jobConfiguration, jobMap.get( jobConfiguration.getJobType() ) );
    }

    @Override
    public void stopJob( String jobKey )
    {
        scheduler.stopJob( jobKey );
    }

    @Override
    public void stopAllJobs()
    {
        scheduler.stopAllJobs();
    }

    @Override
    public void refreshJob( JobConfiguration jobConfiguration )
    {
        scheduler.stopJob( jobConfiguration.getKey() );
        scheduleJob( jobConfiguration );
    }

    @Override
    public void executeJob( JobConfiguration jobConfiguration )
    {
        if ( jobConfiguration != null && !isJobInProgress( jobConfiguration.getKey() ) )
        {
            scheduler.executeJob( jobConfiguration.getKey(), () -> jobMap.get( jobConfiguration.getJobType() ).execute( jobConfiguration ) );
        }
    }

    @Override
    public String getCronForJob( String jobKey )
    {
        return null;
    }

    public Map<String, ScheduledFuture<?>> getAllFutureJobs()
    {
        return scheduler.getAllFutureJobs();
    }

    @Override
    public JobStatus getJobStatus( String jobKey )
    {
        return scheduler.getJobStatus( jobKey );
    }



    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Returns a ScheduledTasks object for the given cron expression. The
     * ScheduledTasks object contains a list of tasks.
     */
    /*private ScheduledJobs getScheduledTasksForCron( String cron, ListMap<String, String> cronKeyMap )
    {
        ScheduledJobs scheduledJobs = new ScheduledJobs();

        scheduledJobs.addJobs( jobConfigs.stream().filter( job -> Objects.equals( job.getCronExpression(), cron ) ).map(
            JobConfiguration::getRunnable ).collect( Collectors.toList()) );
        
        return scheduledJobs;
    }*/

    @Override
    public boolean isJobInProgress(String jobKey)
    {
        return JobStatus.RUNNING == scheduler.getCurrentJobStatus( jobKey );
    }
}