# StorageFactory: Getting Started Guide

In Gen2, If we receive  alarms or [JIRAs](https://jira-sd.mc1.oracleiaas.com/secure/Dashboard.jspa?selectPageId=61415) 
Refer [Steps to follow while debugging](#steps-to-follow-while-debugging) section to debug the issue. 

If you are already logged into VM refer [How to debug using log patterns](debug-using-log-patterns.md)

<span id="important_alarms"></span>
## Important Alarms :

Following three alarms needs to be addressed ASAP to unblock SFVMs so that they will accept new requests.
 
#### 1. Job has been in failed state for a long Period.
When volume creation fails, usually that sessions are auto aborted and accept new requests.
Sometimes you will receive alarms with subject [job has been in failed state for a long period](https://jira-sd.mc1.oracleiaas.com/browse/APLM-772).
Which means SF-VM is blocked and won't receive any further requests until manually abort that session. 

Refer [How to Unblock StorageFactory VM](unblock-storagefactory-vm.md) section.

#### 2. Queue locked due to busy storage factories.
If you see any Alarms with [[PHX] prod: APLM SF queue locked due to busy storage factories](https://jira-sd.mc1.oracleiaas.com/browse/APLM-918).
Based on the region(Ex: [PHX] from subject line), login to any one of VM within that region and run below command. if below command hungs and doesn't return any results immediately.
which means you need release DB Locks. 

```shell script
/scratch/tools/storagefactory-tool/bin/storagefactory-tool.sh list_job_infos -js PENDING
```

Login to SFVMs within that region check storage-factory logs for DB lock errors.

If found restart that docker would solve the problem.
```shell script
#check db errors
zgrep -l $sid /scratch/logs/storagefactory/*
#restart docker 
sudo odoctl docker restart storagefactory
```

Alternatively, follow steps on [How to clear DB Locks](https://devops.oci.oraclecorp.com/runbooks/FAAASHCP/StorageFactory/how-to/clear-db-lock) Runbook

#### 3. Abort Job Failed (Storage Factory MV/DV creator abort job failed)

Refer [How to clean up SF VM if abort fails](cleanup-storagefactory-if-abort-fails.md)


<span style="color:red">

### Do's and don'ts
- Don't make any changes to mv-creator session files directly, always run commands on `storage-factory-tool`.
    
    -  mv-creator sessions located  at `/scratch/.mv-creator`
    -  env-mgr sessions located at  `/scratch/sessions`

- Don't run commands on mv-creator tool. that won't update storage-factory job status.

- If you receive PROD OCEAN Ticket, There is corresponding Ticket generated in [#oci-facp-oncall](slack://channel?team=Cloud-Infra-GBU&id=C02RVKVMG8J). Please Do follow up with them to re-run the flow based on your findings.

- For any reason if you want to back-up mv-creator session file, change it to `mv-creator-session-state-$sid.json` so that status command can able read archived sessions.

</span>
 
 
<hr style="border:2px solid blue"/>

<span id="steps-to-follow-while-debugging"></span>
## Steps to follow while debugging

1)  Change JIRA status to `In Progess` and add Label `faaas-hcp-sf-dep`

      - If you found multiple JIRAs with same VM and Session Information, close them as duplicate.
      - Don't close any JIRA with in 15 min of alarm(before it reset), otherwise you amy receive another alarm.
      - If you find alarms from Beta Test VMs, close it after some time. [List of Test VMs](https://confluence.oci.oraclecorp.com/pages/viewpage.action?spaceKey=FACP&title=Storage+Factory+Test+VM+Reservation)
      
2) Try to find failureReason through [https://devops.oci.oraclecorp.com/logs](https://devops.oci.oraclecorp.com/logs) before you login into to SF_VM.
      1. Select region (you can find from JIRA)
      2. Select AD(Availability Domain) as ALL 
      3. Select Tenancy ( you can find from JIRA)
      4. Select Namespace (xxxx_storagefactory /xxx_storagefactorytool/xxx_mvcreator).
         - Ex: Following Namespaces you need to select for production incident
             - _APLM_prod_facpprod_storagefactory (Required)
             - _APLM_prod_facpprod_mvcreator (Select if available)
             - _APLM_prod_facpprod_storagefactorytool  (Select if available)
      5. Filter message column, keep this empty/ enter below string to filter 
           - failures
           - failureReason
           - FAILED
           - CHILD_PROCESS_EXCEPTION
      6. Select Time range when incident happens and search. 
      7. Search results may contains similar results 
        ```
             "failures" : [ "A failure occurred while executing actions of staging artifact actions." ]
             [CHILD_PROCESS_EXCEPTION] : Failed to finalize master volume.at com.oracle.fa.lcm.storage.env.mgr.impl.EnvManagerImpl.finalizeMasterVolumeCreation(EnvManagerImpl.java:803)
        ```
      8. To further debug server logs (keep Filter message column as empty and search again) - TBD  
        
3) Lumberjack not showing enough details, Login to SF_VM and  refer [How to debug using log patterns](debug-using-log-patterns.md) Runbook.
     
4) Update JIRA with your findings.

    - StorageFactory VM Host
    - mv-creator log file name
    - storage-factory log file name
    - Reason for failure 
    
### Important links    
                   
1) SF-VM Full HostName(s)/Deployed Docker Versions can be found from below links

      - [Beta Tenancy](https://devops.oci.oraclecorp.com/odo/applications/facp-storage-factory-sf-docker-pop-beta/uk-london-1-ad-2?poolsPage=1)
      - [PreProd Tenancy](https://devops.oci.oraclecorp.com/odo/applications/facp-storage-factory-sf-docker-pop-preprod/phx-ad-3?poolsPage=1)
      - [Pint Tenancy](https://devops.oci.oraclecorp.com/odo/applications/facp-storage-factory-sf-docker-pop-pint/iad-ad-1?poolsPage=1)
      - [Prod Tenancy](https://devops.oci.oraclecorp.com/odo/applications/facp-storage-factory-sf-docker-pop-prod/ap-singapore-1-ad-1?poolsPage=1)

2) Grafana Dashboard to find session/hostname details [FAaaS HCP StorageFactory Job Monitoring Dashboard](https://grafana.oci.oraclecorp.com/d/8gaLsda7z/faaas-hcp-storagefactory-job-monitoring-dashboard?orgId=1)

3) Refer Other Related Runbooks.

    - [How to unblock storageFactory VM](unblock-storagefactory-vm.md)
    - [How to debug using log patterns](debug-using-log-patterns.md)
    - [How to clean up the VM if abort fails](cleanup-storagefactory-if-abort-fails.md)
    - [How to clear DB Locks](clear-db-lock.md)
    - [How to handle OCI Operation failures](handle-oci-bv-operation-failure.md)
    - [Important commands to run local sessions](important-commands.md)


