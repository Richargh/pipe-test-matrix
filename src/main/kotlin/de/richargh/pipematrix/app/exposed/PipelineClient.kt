package de.richargh.pipematrix.app.exposed

import com.gitlab.api.GitLabPipelineResponse
import com.gitlab.api.GitLabTestReportResponse

interface PipelineClient {

    suspend fun verifyConnection()
    suspend fun verifyProject(projectPath: ProjectPath)

    suspend fun fetchPipelines(
        projectPath: ProjectPath,
        branch: BranchName,
        maxCount: Int? = null,
        updatedAfter: IsoDate? = null,
        updatedBefore: IsoDate? = null
    ): List<GitLabPipelineResponse>
    suspend fun fetchTestReport(projectPath: ProjectPath, pipelineId: PipelineId): GitLabTestReportResponse

    fun close()
}