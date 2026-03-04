package de.richargh.pipematrix.app.exposed

import com.gitlab.api.GitLabPipelineResponse
import com.gitlab.api.GitLabTestReportResponse

interface PipelineClient {

    suspend fun verifyConnection()
    suspend fun verifyProject(projectPath: ProjectPath)

    suspend fun fetchPipelines(
        projectPath: ProjectPath,
        branch: BranchName,
        count: Int = 10
    ): List<GitLabPipelineResponse>
    suspend fun fetchTestReport(projectPath: ProjectPath, pipelineId: PipelineId): GitLabTestReportResponse

    fun close()
}